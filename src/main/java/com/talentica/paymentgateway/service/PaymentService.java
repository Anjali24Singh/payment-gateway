package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.config.AuthorizeNetConfig;
import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.TransactionRepository;
import com.talentica.paymentgateway.repository.PaymentMethodRepository;
import com.talentica.paymentgateway.repository.OrderRepository;
import com.talentica.paymentgateway.repository.CustomerRepository;
import com.talentica.paymentgateway.util.AuthorizeNetMapper;
import lombok.extern.slf4j.Slf4j;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.GetTransactionDetailsController;
import net.authorize.api.controller.base.ApiOperationBase;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for processing payments through Authorize.Net.
 * Handles all payment operations including purchase, authorize, capture, refund, and void.
 * 
 * Features:
 * - Idempotency support to prevent duplicate transactions
 * - Comprehensive error handling
 * - Transaction logging and audit trail
 * - Integration with Authorize.Net SDK
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class PaymentService {

    private final AuthorizeNetConfig config;
    private final MerchantAuthenticationType merchant;
    private final Environment environment;
    private final AuthorizeNetMapper mapper;
    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final MetricsService metricsService;
    private final AuthorizeNetCustomerService authorizeNetCustomerService;

    public PaymentService(AuthorizeNetConfig config,
                         MerchantAuthenticationType merchant,
                         Environment environment,
                         AuthorizeNetMapper mapper,
                         TransactionRepository transactionRepository,
                         PaymentMethodRepository paymentMethodRepository,
                         OrderRepository orderRepository,
                         CustomerRepository customerRepository,
                         MetricsService metricsService,
                         AuthorizeNetCustomerService authorizeNetCustomerService) {
        this.config = config;
        this.merchant = merchant;
        this.environment = environment;
        this.mapper = mapper;
        this.transactionRepository = transactionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.metricsService = metricsService;
        this.authorizeNetCustomerService = authorizeNetCustomerService;
        
        // Set environment for Authorize.Net SDK
        ApiOperationBase.setEnvironment(environment);
    }

    /**
     * Processes a direct purchase transaction (auth + capture).
     * 
     * @param request Purchase request details
     * @return Payment response with transaction results
     * @throws PaymentProcessingException if payment processing fails
     */
    public PaymentResponse processPurchase(PurchaseRequest request) {
        String correlationId = getOrGenerateCorrelationId();
        String transactionId = mapper.generateTransactionId();
        Instant startTime = Instant.now();
        
        log.info("Processing purchase transaction - TransactionId: {}, Amount: {}, CorrelationId: {}", 
                   transactionId, request.getAmount(), correlationId);

        // Track payment request metrics
        metricsService.recordPaymentRequest();
        metricsService.recordPaymentMethodUsage("credit_card"); // Assuming credit card for now

        // Check for idempotency
        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existingTransaction = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
            if (existingTransaction.isPresent()) {
                log.info("Returning existing transaction for idempotency key: {}", 
                           request.getIdempotencyKey());
                
                // Track metrics for idempotent request
                Duration processingTime = Duration.between(startTime, Instant.now());
                metricsService.recordTransaction(TransactionType.PURCHASE, processingTime);
                
                return buildResponseFromTransaction(existingTransaction.get());
            }
        }

        try {
            // Validate payment method
            validatePaymentMethod(request.getPaymentMethod());

            // Create transaction entity
            Transaction transaction = createTransactionEntity(request, transactionId, 
                                                            TransactionType.PURCHASE, correlationId);
            transaction = transactionRepository.save(transaction);

            // Map to Authorize.Net request
            CreateTransactionRequest authNetRequest = mapper.mapToPurchaseTransaction(request, merchant);
            
            // Execute payment
            CreateTransactionController controller = new CreateTransactionController(authNetRequest);
            controller.execute();
            CreateTransactionResponse response = controller.getApiResponse();

            // Process response
            PaymentResponse paymentResponse = mapper.mapToPaymentResponse(
                response, transactionId, "PURCHASE", request.getPaymentMethod(), correlationId);
            
            // Set the amount from the original request as mapper can't extract it from Authorize.Net response
            paymentResponse.setAmount(request.getAmount());

            // Add customer information to response
            if (transaction.getCustomer() != null) {
                Customer customer = transaction.getCustomer();
                paymentResponse.setCustomerId(customer.getCustomerId()); 
                paymentResponse.setCustomerReference(customer.getCustomerReference());
                paymentResponse.setCustomerProfileId(customer.getAuthorizeNetCustomerProfileId());
            }

            // Update transaction with results
            updateTransactionFromResponse(transaction, paymentResponse, response);
            transactionRepository.save(transaction);

            log.info("Purchase transaction completed - TransactionId: {}, Status: {}, Success: {}", 
                       transactionId, paymentResponse.getStatus(), paymentResponse.getSuccess());

            // Track metrics for successful completion
            Duration processingTime = Duration.between(startTime, Instant.now());
            PaymentStatus status = paymentResponse.getSuccess() ? PaymentStatus.SETTLED : PaymentStatus.FAILED;
            metricsService.recordPaymentCompletion(status, request.getAmount(), processingTime);
            metricsService.recordTransaction(TransactionType.PURCHASE, processingTime);

            return paymentResponse;

        } catch (Exception e) {
            log.error("Purchase transaction failed - TransactionId: {}, Error: {}", 
                        transactionId, e.getMessage(), e);
            
            // Track metrics for error
            Duration processingTime = Duration.between(startTime, Instant.now());
            metricsService.recordPaymentCompletion(PaymentStatus.FAILED, request.getAmount(), processingTime);
            metricsService.recordPaymentError("processing_exception", e.getClass().getSimpleName());
            
            // Update transaction status to failed
            updateTransactionStatus(transactionId, PaymentStatus.FAILED, e.getMessage());
            
            throw new PaymentProcessingException("Purchase transaction failed", e, correlationId);
        }
    }

    /**
     * Processes an authorization-only transaction.
     * 
     * @param request Authorization request details
     * @return Payment response with authorization results
     * @throws PaymentProcessingException if authorization fails
     */
    public PaymentResponse processAuthorization(AuthorizeRequest request) {
        String correlationId = getOrGenerateCorrelationId();
        String transactionId = mapper.generateTransactionId();
        
        log.info("Processing authorization transaction - TransactionId: {}, Amount: {}, CorrelationId: {}", 
                   transactionId, request.getAmount(), correlationId);

        // Check for idempotency
        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existingTransaction = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
            if (existingTransaction.isPresent()) {
                log.info("Returning existing transaction for idempotency key: {}", 
                           request.getIdempotencyKey());
                return buildResponseFromTransaction(existingTransaction.get());
            }
        }

        try {
            // Validate payment method
            validatePaymentMethod(request.getPaymentMethod());

            // Create transaction entity
            Transaction transaction = createTransactionEntity(request, transactionId, 
                                                            TransactionType.AUTHORIZE, correlationId);
            transaction = transactionRepository.save(transaction);

            // Map to Authorize.Net request
            CreateTransactionRequest authNetRequest = mapper.mapToAuthorizeTransaction(request, merchant);
            
            // Execute authorization
            CreateTransactionController controller = new CreateTransactionController(authNetRequest);
            controller.execute();
            CreateTransactionResponse response = controller.getApiResponse();

            // Process response
            PaymentResponse paymentResponse = mapper.mapToPaymentResponse(
                response, transactionId, "AUTHORIZE", request.getPaymentMethod(), correlationId);
            
            // Set the amount from the original request
            paymentResponse.setAmount(request.getAmount());

            // Update transaction with results
            updateTransactionFromResponse(transaction, paymentResponse, response);
            transactionRepository.save(transaction);

            log.info("Authorization transaction completed - TransactionId: {}, Status: {}, Success: {}", 
                       transactionId, paymentResponse.getStatus(), paymentResponse.getSuccess());

            return paymentResponse;

        } catch (Exception e) {
            log.error("Authorization transaction failed - TransactionId: {}, Error: {}", 
                        transactionId, e.getMessage(), e);
            
            // Update transaction status to failed
            updateTransactionStatus(transactionId, PaymentStatus.FAILED, e.getMessage());
            
            throw new PaymentProcessingException("Authorization transaction failed", e, correlationId);
        }
    }

    /**
     * Captures a previously authorized transaction.
     * 
     * @param request Capture request details
     * @return Payment response with capture results
     * @throws PaymentProcessingException if capture fails
     */
    public PaymentResponse processCapture(CaptureRequest request) {
        String correlationId = getOrGenerateCorrelationId();
        String transactionId = mapper.generateTransactionId();
        
        log.info("Processing capture request - AuthTransactionId: {}, Amount: {}, CorrelationId: {}", 
                   request.getTransactionId(), request.getAmount(), correlationId);

        // Find original authorization transaction
        Optional<Transaction> originalTransaction = transactionRepository
            .findByTransactionId(request.getTransactionId());
        
        if (originalTransaction.isEmpty()) {
            throw new PaymentProcessingException("Original authorization transaction not found: " + 
                                               request.getTransactionId(), correlationId);
        }

        Transaction authTransaction = originalTransaction.get();
        if (authTransaction.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentProcessingException("Transaction is not in authorized status: " + 
                                               authTransaction.getStatus(), correlationId);
        }

        try {
            // Create capture transaction entity
            Transaction transaction = createBasicTransactionEntity(transactionId, TransactionType.CAPTURE, 
                                                                 request.getAmount() != null ? request.getAmount() : authTransaction.getAmount(),
                                                                 correlationId, request.getIdempotencyKey());
            transaction.setParentTransaction(authTransaction);
            transaction.setOrder(authTransaction.getOrder());
            transaction.setCustomer(authTransaction.getCustomer());
            transaction.setPaymentMethod(authTransaction.getPaymentMethod());
            transaction = transactionRepository.save(transaction);

            // Map to Authorize.Net request
            CreateTransactionRequest authNetRequest = mapper.mapToCaptureTransaction(
                request, authTransaction.getAuthnetTransactionId(), merchant);
            
            // Execute capture
            CreateTransactionController controller = new CreateTransactionController(authNetRequest);
            controller.execute();
            CreateTransactionResponse response = controller.getApiResponse();

            // Log detailed Authorize.Net response for debugging
            log.info("Authorize.Net Capture Response - TransactionId: {}, ResultCode: {}, MessageCount: {}", 
                       transactionId, 
                       response.getMessages() != null ? response.getMessages().getResultCode() : "NULL",
                       response.getMessages() != null ? response.getMessages().getMessage().size() : 0);
            
            if (response.getMessages() != null && response.getMessages().getMessage() != null) {
                for (var message : response.getMessages().getMessage()) {
                    log.info("Authorize.Net Message - Code: {}, Text: {}", message.getCode(), message.getText());
                }
            }
            
            if (response.getTransactionResponse() != null) {
                log.info("Transaction Response - ResponseCode: {}, AuthCode: {}, TransId: {}", 
                           response.getTransactionResponse().getResponseCode(),
                           response.getTransactionResponse().getAuthCode(),
                           response.getTransactionResponse().getTransId());
                
                if (response.getTransactionResponse().getErrors() != null) {
                    for (var error : response.getTransactionResponse().getErrors().getError()) {
                        log.error("Transaction Error - Code: {}, Text: {}", error.getErrorCode(), error.getErrorText());
                    }
                }
            }

            // Process response
            PaymentResponse paymentResponse = mapper.mapToPaymentResponse(
                response, transactionId, "CAPTURE", authTransaction.getPaymentMethod() != null ? 
                    convertToPaymentMethodRequest(authTransaction.getPaymentMethod()) : null, correlationId);
            
            // Set the amount from the capture request
            paymentResponse.setAmount(request.getAmount() != null ? request.getAmount() : authTransaction.getAmount());

            // Update transaction with results
            updateTransactionFromResponse(transaction, paymentResponse, response);
            transactionRepository.save(transaction);

            // Update original authorization status if capture was successful
            if (paymentResponse.getSuccess()) {
                authTransaction.setStatus(PaymentStatus.CAPTURED);
                transactionRepository.save(authTransaction);
            }

            log.info("Capture transaction completed - TransactionId: {}, Status: {}, Success: {}", 
                       transactionId, paymentResponse.getStatus(), paymentResponse.getSuccess());

            return paymentResponse;

        } catch (Exception e) {
            log.error("Capture transaction failed - TransactionId: {}, Error: {}", 
                        transactionId, e.getMessage(), e);
            
            // Update transaction status to failed
            updateTransactionStatus(transactionId, PaymentStatus.FAILED, e.getMessage());
            
            throw new PaymentProcessingException("Capture transaction failed", e, correlationId);
        }
    }

    /**
     * Processes a void transaction.
     * 
     * @param request Void request details
     * @return Payment response with void results
     * @throws PaymentProcessingException if void fails
     */
    public PaymentResponse processVoid(VoidRequest request) {
        String correlationId = getOrGenerateCorrelationId();
        String transactionId = mapper.generateTransactionId();
        
        log.info("Processing void request - OriginalTransactionId: {}, CorrelationId: {}", 
                   request.getTransactionId(), correlationId);

        // Find original transaction
        Optional<Transaction> originalTransaction = transactionRepository
            .findByTransactionId(request.getTransactionId());
        
        if (originalTransaction.isEmpty()) {
            throw new PaymentProcessingException("Original transaction not found: " + 
                                               request.getTransactionId(), correlationId);
        }

        Transaction origTransaction = originalTransaction.get();
        if (origTransaction.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentProcessingException("Only authorized transactions can be voided: " + 
                                               origTransaction.getStatus(), correlationId);
        }

        try {
            // Create void transaction entity - use original amount since voids don't have separate amounts
            Transaction transaction = createBasicTransactionEntity(transactionId, TransactionType.VOID, 
                                                                 origTransaction.getAmount(), correlationId, request.getIdempotencyKey());
            transaction.setParentTransaction(origTransaction);
            transaction.setOrder(origTransaction.getOrder());
            transaction.setCustomer(origTransaction.getCustomer());
            transaction.setPaymentMethod(origTransaction.getPaymentMethod());
            transaction = transactionRepository.save(transaction);

            // Map to Authorize.Net request
            CreateTransactionRequest authNetRequest = mapper.mapToVoidTransaction(
                request, origTransaction.getAuthnetTransactionId(), merchant);
            
            // Execute void
            CreateTransactionController controller = new CreateTransactionController(authNetRequest);
            controller.execute();
            CreateTransactionResponse response = controller.getApiResponse();

            // Log detailed Authorize.Net response for debugging
            log.info("Authorize.Net Void Response - TransactionId: {}, ResultCode: {}, MessageCount: {}", 
                       transactionId, 
                       response.getMessages() != null ? response.getMessages().getResultCode() : "NULL",
                       response.getMessages() != null ? response.getMessages().getMessage().size() : 0);
            
            if (response.getMessages() != null && response.getMessages().getMessage() != null) {
                for (var message : response.getMessages().getMessage()) {
                    log.info("Authorize.Net Message - Code: {}, Text: {}", message.getCode(), message.getText());
                }
            }
            
            if (response.getTransactionResponse() != null) {
                log.info("Transaction Response - ResponseCode: {}, AuthCode: {}, TransId: {}", 
                           response.getTransactionResponse().getResponseCode(),
                           response.getTransactionResponse().getAuthCode(),
                           response.getTransactionResponse().getTransId());
                
                if (response.getTransactionResponse().getErrors() != null) {
                    for (var error : response.getTransactionResponse().getErrors().getError()) {
                        log.error("Transaction Error - Code: {}, Text: {}", error.getErrorCode(), error.getErrorText());
                    }
                }
            }

            // Process response - void transactions don't need payment method details, pass null
            PaymentResponse paymentResponse = mapper.mapToPaymentResponse(
                response, transactionId, "VOID", null, correlationId);
            
            // Set original amount for void transactions (voids reference the original amount)
            paymentResponse.setAmount(origTransaction.getAmount());

            // Update transaction with results
            updateTransactionFromResponse(transaction, paymentResponse, response);
            transactionRepository.save(transaction);

            // Update original transaction status if void was successful
            if (paymentResponse.getSuccess()) {
                origTransaction.setStatus(PaymentStatus.VOIDED);
                transactionRepository.save(origTransaction);
            }

            log.info("Void transaction completed - TransactionId: {}, Status: {}, Success: {}", 
                       transactionId, paymentResponse.getStatus(), paymentResponse.getSuccess());

            return paymentResponse;

        } catch (Exception e) {
            log.error("Void transaction failed - TransactionId: {}, Error: {}", 
                        transactionId, e.getMessage(), e);
            
            // Update transaction status to failed
            updateTransactionStatus(transactionId, PaymentStatus.FAILED, e.getMessage());
            
            throw new PaymentProcessingException("Void transaction failed", e, correlationId);
        }
    }

    /**
     * Processes a refund transaction (full or partial).
     * 
     * @param request Refund request details
     * @return Payment response with refund results
     * @throws PaymentProcessingException if refund fails
     */
    public PaymentResponse processRefund(RefundRequest request) {
        String correlationId = getOrGenerateCorrelationId();
        String transactionId = mapper.generateTransactionId();
        
        log.info("Processing refund request - OriginalTransactionId: {}, Amount: {}, CorrelationId: {}", 
                   request.getTransactionId(), request.getAmount(), correlationId);

        // Check for idempotency
        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existingTransaction = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
            if (existingTransaction.isPresent()) {
                log.info("Returning existing transaction for idempotency key: {}", 
                           request.getIdempotencyKey());
                return buildResponseFromTransaction(existingTransaction.get());
            }
        }

        // Find original transaction with payment method eagerly loaded
        Optional<Transaction> originalTransaction = transactionRepository
            .findByTransactionIdWithPaymentMethod(request.getTransactionId());
        
        if (originalTransaction.isEmpty()) {
            throw new PaymentProcessingException("Original transaction not found: " + 
                                               request.getTransactionId(), correlationId);
        }

        Transaction origTransaction = originalTransaction.get();
        if (origTransaction.getStatus() != PaymentStatus.CAPTURED && 
            origTransaction.getStatus() != PaymentStatus.SETTLED) {
            throw new PaymentProcessingException("Only captured/settled transactions can be refunded: " + 
                                               origTransaction.getStatus(), correlationId);
        }

        // Determine refund amount (full refund if amount not specified)
        BigDecimal refundAmount = request.getAmount() != null ? 
            request.getAmount() : origTransaction.getAmount();

        // Validate refund amount
        if (refundAmount.compareTo(origTransaction.getAmount()) > 0) {
            throw new PaymentProcessingException("Refund amount cannot exceed original transaction amount", correlationId);
        }

        try {
            // Create refund transaction entity
            Transaction transaction = createBasicTransactionEntity(transactionId, TransactionType.REFUND, 
                                                                 refundAmount, correlationId, request.getIdempotencyKey());
            transaction.setParentTransaction(origTransaction);
            transaction.setOrder(origTransaction.getOrder());
            transaction.setCustomer(origTransaction.getCustomer());
            transaction.setPaymentMethod(origTransaction.getPaymentMethod());
            
            // Add refund-specific data
            transaction.getResponseData().put("refundReason", request.getReason());
            if (request.getDescription() != null) {
                transaction.getResponseData().put("refundDescription", request.getDescription());
            }
            if (request.getReferenceNumber() != null) {
                transaction.getResponseData().put("refundReference", request.getReferenceNumber());
            }
            
            transaction = transactionRepository.save(transaction);

            // Log payment method details for debugging
            log.info("Refund Debug - Original Transaction PaymentMethod: {}, AuthnetTransactionId: {}", 
                       origTransaction.getPaymentMethod() != null ? "NOT NULL" : "NULL",
                       origTransaction.getAuthnetTransactionId());
            
            if (origTransaction.getPaymentMethod() != null) {
                log.info("PaymentMethod Details - Type: {}, CardNumber: {}, LastFour: {}", 
                           origTransaction.getPaymentMethod().getType(),
                           origTransaction.getPaymentMethod().getCardNumber() != null ? "NOT NULL" : "NULL",
                           origTransaction.getPaymentMethod().getCardLastFour());
            }

            // Map to Authorize.Net request
            CreateTransactionRequest authNetRequest = mapper.mapToRefundTransaction(
                request, origTransaction.getAuthnetTransactionId(), origTransaction.getPaymentMethod(), merchant);
            
            // Execute refund
            CreateTransactionController controller = new CreateTransactionController(authNetRequest);
            controller.execute();
            CreateTransactionResponse response = controller.getApiResponse();

            // Log detailed Authorize.Net response for debugging
            log.info("Authorize.Net Refund Response - TransactionId: {}, ResultCode: {}, MessageCount: {}", 
                       transactionId, 
                       response.getMessages() != null ? response.getMessages().getResultCode() : "NULL",
                       response.getMessages() != null ? response.getMessages().getMessage().size() : 0);
            
            if (response.getMessages() != null && response.getMessages().getMessage() != null) {
                for (var message : response.getMessages().getMessage()) {
                    log.info("Authorize.Net Message - Code: {}, Text: {}", message.getCode(), message.getText());
                }
            }
            
            if (response.getTransactionResponse() != null) {
                log.info("Transaction Response - ResponseCode: {}, AuthCode: {}, TransId: {}", 
                           response.getTransactionResponse().getResponseCode(),
                           response.getTransactionResponse().getAuthCode(),
                           response.getTransactionResponse().getTransId());
                
                if (response.getTransactionResponse().getErrors() != null) {
                    for (var error : response.getTransactionResponse().getErrors().getError()) {
                        log.error("Transaction Error - Code: {}, Text: {}", error.getErrorCode(), error.getErrorText());
                    }
                }
            }

            // Process response
            PaymentResponse paymentResponse = mapper.mapToPaymentResponse(
                response, transactionId, "REFUND", origTransaction.getPaymentMethod() != null ? 
                    convertToPaymentMethodRequest(origTransaction.getPaymentMethod()) : null, correlationId);
            
            // Set the refund amount
            paymentResponse.setAmount(refundAmount);

            // Update transaction with results
            updateTransactionFromResponse(transaction, paymentResponse, response);
            transactionRepository.save(transaction);

            // Update original transaction status if refund was successful
            if (paymentResponse.getSuccess()) {
                // Check if this is a full refund
                if (refundAmount.compareTo(origTransaction.getAmount()) == 0) {
                    origTransaction.setStatus(PaymentStatus.REFUNDED);
                } else {
                    origTransaction.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
                }
                transactionRepository.save(origTransaction);
            }

            log.info("Refund transaction completed - TransactionId: {}, Status: {}, Success: {}, Amount: {}", 
                       transactionId, paymentResponse.getStatus(), paymentResponse.getSuccess(), refundAmount);

            return paymentResponse;

        } catch (Exception e) {
            log.error("Refund transaction failed - TransactionId: {}, Error: {}", 
                        transactionId, e.getMessage(), e);
            
            // Update transaction status to failed
            updateTransactionStatus(transactionId, PaymentStatus.FAILED, e.getMessage());
            
            throw new PaymentProcessingException("Refund transaction failed", e, correlationId);
        }
    }

    /**
     * Retrieves transaction status and details.
     * 
     * @param transactionId Internal transaction ID
     * @return Payment response with current transaction status
     * @throws PaymentProcessingException if transaction not found or inquiry fails
     */
    public PaymentResponse getTransactionStatus(String transactionId) {
        String correlationId = getOrGenerateCorrelationId();
        
        log.info("Getting transaction status - TransactionId: {}, CorrelationId: {}", 
                   transactionId, correlationId);

        try {
            // Find transaction in database
            Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
            if (transactionOpt.isEmpty()) {
                throw new PaymentProcessingException("Transaction not found: " + transactionId, correlationId);
            }

            Transaction transaction = transactionOpt.get();
            
            // Build response from stored transaction data
            PaymentResponse response = buildResponseFromTransaction(transaction);
            
            // If transaction has been processed through Authorize.Net, we could optionally
            // query their API for the latest status, but for now we'll use our stored data
            
            log.info("Transaction status retrieved - TransactionId: {}, Status: {}", 
                       transactionId, response.getStatus());

            return response;

        } catch (Exception e) {
            log.error("Failed to get transaction status - TransactionId: {}, Error: {}", 
                        transactionId, e.getMessage(), e);
            
            if (e instanceof PaymentProcessingException) {
                throw e;
            }
            throw new PaymentProcessingException("Failed to retrieve transaction status", e, correlationId);
        }
    }

    /**
     * Retrieves transaction details directly from Authorize.Net API.
     * This confirms that transactions are actually reaching Authorize.Net.
     * 
     * @param authnetTransactionId Authorize.Net transaction ID
     * @return Payment response with details from Authorize.Net
     * @throws PaymentProcessingException if transaction not found or inquiry fails
     */
    public PaymentResponse getAuthNetTransactionDetails(String authnetTransactionId) {
        String correlationId = getOrGenerateCorrelationId();
        
        log.info("Querying Authorize.Net directly - AuthNet TransactionId: {}, CorrelationId: {}", 
                   authnetTransactionId, correlationId);

        try {
            // Create transaction details request
            GetTransactionDetailsRequest request = new GetTransactionDetailsRequest();
            request.setMerchantAuthentication(merchant);
            request.setTransId(authnetTransactionId);

            // Execute the request
            GetTransactionDetailsController controller = new GetTransactionDetailsController(request);
            controller.execute();
            GetTransactionDetailsResponse response = controller.getApiResponse();

            if (response == null) {
                throw new PaymentProcessingException("No response from Authorize.Net for transaction: " + authnetTransactionId, correlationId);
            }

            if (response.getMessages().getResultCode() != MessageTypeEnum.OK) {
                String errorMessage = response.getMessages().getMessage().get(0).getText();
                throw new PaymentProcessingException("Authorize.Net error: " + errorMessage, correlationId);
            }

            // Map the response
            PaymentResponse paymentResponse = mapAuthNetDetailsToPaymentResponse(response, correlationId);
            
            log.info("Successfully retrieved transaction from Authorize.Net - AuthNet ID: {}, Status: {}, Amount: {}", 
                       authnetTransactionId, paymentResponse.getStatus(), paymentResponse.getAmount());

            return paymentResponse;

        } catch (Exception e) {
            log.error("Failed to retrieve transaction from Authorize.Net - AuthNet ID: {}, Error: {}", 
                        authnetTransactionId, e.getMessage(), e);
            
            if (e instanceof PaymentProcessingException) {
                throw e;
            }
            throw new PaymentProcessingException("Failed to retrieve transaction from Authorize.Net", e, correlationId);
        }
    }

    /**
     * Validates payment method information before processing.
        
// Execute refund
CreateTransactionController controller = new CreateTransactionController(authNetRequest);
controller.execute();
CreateTransactionResponse response = controller.getApiResponse();

// Log detailed Authorize.Net response for debugging
log.info("Authorize.Net Refund Response - TransactionId: {}, ResultCode: {}, MessageCount: {}", 
           transactionId, 
           response.getMessages() != null ? response.getMessages().getResultCode() : "NULL",
           response.getMessages() != null ? response.getMessages().getMessage().size() : 0);
            }

            // Credit card validation
            if ("CREDIT_CARD".equals(paymentMethod.getType())) {
                if (paymentMethod.getCardNumber() == null || paymentMethod.getCardNumber().trim().isEmpty()) {
                    throw new PaymentProcessingException("Card number is required", correlationId);
                }
                
                if (paymentMethod.getExpiryMonth() == null || paymentMethod.getExpiryYear() == null) {
                    throw new PaymentProcessingException("Card expiry date is required", correlationId);
                }
                
                if (paymentMethod.getCvv() == null || paymentMethod.getCvv().trim().isEmpty()) {
                    throw new PaymentProcessingException("CVV is required", correlationId);
                }

                // Basic card number validation (Luhn algorithm would be implemented in validator)
                String cardNumber = paymentMethod.getCardNumber().replaceAll("\\s+", "");
                if (cardNumber.length() < 13 || cardNumber.length() > 19) {
                    throw new PaymentProcessingException("Invalid card number length", correlationId);
                }

                // CVV validation
                String cvv = paymentMethod.getCvv();
                if (cvv.length() < 3 || cvv.length() > 4) {
                    throw new PaymentProcessingException("Invalid CVV length", correlationId);
                }

                // Expiry date validation
                int currentYear = java.time.Year.now().getValue();
                int currentMonth = java.time.MonthDay.now().getMonthValue();
                
                try {
                    int cardYear = Integer.parseInt(paymentMethod.getExpiryYear());
                    int cardMonth = Integer.parseInt(paymentMethod.getExpiryMonth());
                    
                    if (cardYear < currentYear || (cardYear == currentYear && cardMonth < currentMonth)) {
                        throw new PaymentProcessingException("Card has expired", correlationId);
                    }
                } catch (NumberFormatException e) {
                    throw new PaymentProcessingException("Invalid expiry date format", correlationId);
                }
            }

            log.debug("Payment method validation successful - Type: {}, CorrelationId: {}", 
                        paymentMethod.getType(), correlationId);

        } catch (Exception e) {
            log.error("Payment method validation failed - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId);
            
            if (e instanceof PaymentProcessingException) {
                throw e;
            }
            throw new PaymentProcessingException("Payment method validation failed", e, correlationId);
        }
    }

    // Helper methods

    private Transaction createTransactionEntity(PaymentRequest request, String transactionId, 
                                          TransactionType type, String correlationId) {
        log.error("🚨 CREATE TRANSACTION ENTITY CALLED - TransactionId: {}, Type: {}", transactionId, type);
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionType(type);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setCorrelationId(correlationId);
        
        if (request.getIdempotencyKey() != null) {
            transaction.setIdempotencyKey(request.getIdempotencyKey());
        }
        
        // Create or find customer for the transaction
        log.info("🔍 ABOUT TO CALL findOrCreateCustomer - Email: {}, CorrelationId: {}", 
                   request.getCustomer() != null ? request.getCustomer().getEmail() : "null", correlationId);
        Customer customer = findOrCreateCustomer(request.getCustomer());
        log.info("🔍 RETURNED FROM findOrCreateCustomer - Customer ID: {}, AuthNet Profile ID: {}", 
                   customer.getCustomerId(), customer.getAuthorizeNetCustomerProfileId());
        transaction.setCustomer(customer);
        
        // Set order reference if available
        if (request.getOrderNumber() != null) {
            Optional<Order> order = orderRepository.findByOrderNumber(request.getOrderNumber());
            order.ifPresent(o -> transaction.setOrder(o));
        }
        
        // Create and associate payment method
        if (request.getPaymentMethod() != null) {
            PaymentMethod paymentMethod = findOrCreatePaymentMethod(request.getPaymentMethod(), customer);
            transaction.setPaymentMethod(paymentMethod);
        }
        
        return transaction;
    }

    private Transaction createBasicTransactionEntity(String transactionId, TransactionType type, 
                                                   BigDecimal amount, String correlationId, String idempotencyKey) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrency("USD");
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setCorrelationId(correlationId);
        
        if (idempotencyKey != null) {
            transaction.setIdempotencyKey(idempotencyKey);
        }
        
        return transaction;
    }

    private void updateTransactionFromResponse(Transaction transaction, PaymentResponse response, 
                                             CreateTransactionResponse authNetResponse) {
        transaction.setStatus(PaymentStatus.valueOf(response.getStatus()));
        
        if (response.getSuccess()) {
            transaction.setAuthnetTransactionId(response.getAuthnetTransactionId());
            transaction.setAuthnetAuthCode(response.getAuthorizationCode());
            transaction.setAuthnetResponseCode(response.getResponseCode());
            transaction.setAuthnetResponseReason(response.getResponseReasonText());
            transaction.setAuthnetAvsResult(response.getAvsResult());
            transaction.setAuthnetCvvResult(response.getCvvResult());
        } else {
            // Store error details in response data
            transaction.getResponseData().put("errorMessage", response.getError().getMessage());
            transaction.getResponseData().put("errorCode", response.getError().getCode());
        }
        
        transaction.setProcessedAt(ZonedDateTime.now());
    }

    private void updateTransactionStatus(String transactionId, PaymentStatus status, String errorMessage) {
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                transaction.setStatus(status);
                if (errorMessage != null) {
                    transaction.getResponseData().put("errorMessage", errorMessage);
                }
                transaction.setProcessedAt(ZonedDateTime.now());
                transactionRepository.save(transaction);
            }
        } catch (Exception e) {
            log.error("Failed to update transaction status - TransactionId: {}, Status: {}, Error: {}", 
                        transactionId, status, e.getMessage());
        }
    }

    private PaymentResponse buildResponseFromTransaction(Transaction transaction) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(transaction.getTransactionId());
        response.setAuthnetTransactionId(transaction.getAuthnetTransactionId());
        response.setStatus(transaction.getStatus().name());
        response.setTransactionType(transaction.getTransactionType().name());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setAuthorizationCode(transaction.getAuthnetAuthCode());
        response.setResponseCode(transaction.getAuthnetResponseCode());
        response.setResponseReasonText(transaction.getAuthnetResponseReason());
        response.setSuccess(transaction.getStatus() != PaymentStatus.FAILED);
        response.setCreatedAt(transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()));
        response.setCorrelationId(transaction.getCorrelationId());
        return response;
    }

    private String getOrGenerateCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
            MDC.put("correlationId", correlationId);
        }
        return correlationId;
    }

    /**
     * Converts PaymentMethod entity to PaymentMethodRequest DTO for mapper.
     */
    private PaymentMethodRequest convertToPaymentMethodRequest(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }
        
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setType(paymentMethod.getType());
        request.setCardNumber(paymentMethod.getCardNumber());
        request.setExpiryMonth(paymentMethod.getExpiryMonth());
        request.setExpiryYear(paymentMethod.getExpiryYear());
        request.setCardholderName(paymentMethod.getCardholderName());
        
        return request;
    }

    /**
     * Finds an existing payment method or creates a new one.
     */
    private PaymentMethod findOrCreatePaymentMethod(PaymentMethodRequest request, Customer customer) {
        if (request == null) {
            return null;
        }
        
        // Create new payment method (simplified approach for now)
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setCustomer(customer);
        paymentMethod.setPaymentType(request.getType());
        paymentMethod.setPaymentToken("token_" + UUID.randomUUID().toString().substring(0, 8));
        paymentMethod.setCardNumber(request.getCardNumber()); // Store full number for sandbox
        paymentMethod.setCardLastFour(request.getCardNumber().length() >= 4 ? 
            request.getCardNumber().substring(request.getCardNumber().length() - 4) : 
            request.getCardNumber());
        paymentMethod.setExpiryMonth(request.getExpiryMonth());
        paymentMethod.setExpiryYear(request.getExpiryYear());
        paymentMethod.setCardholderName(request.getCardholderName());
        
        // Set card expiry as integers
        try {
            paymentMethod.setCardExpiryMonth(Integer.parseInt(request.getExpiryMonth()));
            paymentMethod.setCardExpiryYear(Integer.parseInt(request.getExpiryYear()));
        } catch (NumberFormatException e) {
            log.warn("Invalid expiry date format: {}/{}", request.getExpiryMonth(), request.getExpiryYear());
        }
        
        return paymentMethodRepository.save(paymentMethod);
    }

    /**
     * Finds an existing customer by email or creates a new one.
     * 
     * @param customerRequest Customer information from the payment request
     * @return Customer entity (existing or newly created)
     */
    private Customer findOrCreateCustomer(CustomerRequest customerRequest) {
        log.error("🚨 FINDORCREATE CUSTOMER METHOD CALLED - Email: {}", 
                    customerRequest != null ? customerRequest.getEmail() : "NULL_REQUEST");
        
        if (customerRequest == null || customerRequest.getEmail() == null) {
            throw new PaymentProcessingException("Customer information is required for payment processing", getOrGenerateCorrelationId());
        }

        // Try to find existing customer by email
        Optional<Customer> existingCustomer = customerRepository.findByEmailIgnoreCase(customerRequest.getEmail());
        
        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();
            log.info("Found existing customer - Email: {}, Customer ID: {}, AuthNet Profile ID: {}", 
                       customerRequest.getEmail(), 
                       customer.getCustomerId(),
                       customer.getAuthorizeNetCustomerProfileId());
            
            // Check if existing customer needs Authorize.Net profile creation
            if (customer.getAuthorizeNetCustomerProfileId() == null || customer.getAuthorizeNetCustomerProfileId().trim().isEmpty()) {
                log.info("🔧 EXISTING CUSTOMER MISSING AUTHNET PROFILE - Creating CIM profile for: {}", customer.getEmail());
                try {
                    String customerProfileId = authorizeNetCustomerService.createCustomerProfile(customer, customerRequest);
                    customer.setAuthorizeNetCustomerProfileId(customerProfileId);
                    customer = customerRepository.save(customer);
                    log.info("✅ CIM PROFILE CREATED for existing customer - Profile ID: {}, Customer ID: {}", 
                               customerProfileId, customer.getCustomerId());
                } catch (Exception e) {
                    log.error("❌ Failed to create CIM profile for existing customer: {} - Error: {}", 
                                customer.getCustomerId(), e.getMessage(), e);
                }
            }
            
            return customer;
        }

        // Create new customer
        Customer customer = new Customer();
        customer.setEmail(customerRequest.getEmail());
        customer.setFirstName(customerRequest.getFirstName());
        customer.setLastName(customerRequest.getLastName());
        customer.setPhone(customerRequest.getPhone());
        
        // Auto-generate customer reference if not provided
        String customerReference = customerRequest.getCustomerReference();
        if (customerReference == null || customerReference.trim().isEmpty()) {
            customerReference = "CUST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("🔧 Auto-generated customer reference: {}", customerReference);
        }
        customer.setCustomerReference(customerReference);
        
        // Set billing address if provided
        if (customerRequest.getBillingAddress() != null) {
            AddressRequest addr = customerRequest.getBillingAddress();
            customer.setBillingAddressLine1(addr.getAddress1());
            customer.setBillingAddressLine2(addr.getAddress2());
            customer.setBillingCity(addr.getCity());
            customer.setBillingState(addr.getState());
            customer.setBillingPostalCode(addr.getZipCode());
            customer.setBillingCountry(addr.getCountry());
        }

        customer = customerRepository.save(customer);
        log.info("🆕 CREATING NEW CUSTOMER - ID: {}, Email: {}, About to create AuthNet profile...", customer.getId(), customer.getEmail());
        
        // Create Authorize.Net customer profile - THIS IS THE CRITICAL FIX
        // This makes customers appear in the Authorize.Net portal for subscription management
        try {
            String customerProfileId = authorizeNetCustomerService.createCustomerProfile(customer, customerRequest);
            customer.setAuthorizeNetCustomerProfileId(customerProfileId);
            customer = customerRepository.save(customer);
            log.info("🎯 SOLUTION IMPLEMENTED: Customer profile created in Authorize.Net - Profile ID: {}, Customer ID: {}", 
                       customerProfileId, customer.getCustomerId());
        } catch (Exception e) {
            log.error("⚠️ Failed to create Authorize.Net customer profile for customer: {} - Error: {}", 
                        customer.getCustomerId(), e.getMessage(), e);
            // Continue without failing the payment - customer profile creation is optional for basic payments
            // but required for subscriptions to appear in portal
        }
        
        return customer;
    }

    /**
     * Maps Authorize.Net GetTransactionDetailsResponse to PaymentResponse.
     */
    private PaymentResponse mapAuthNetDetailsToPaymentResponse(GetTransactionDetailsResponse authNetResponse, String correlationId) {
        TransactionDetailsType transaction = authNetResponse.getTransaction();
        
        PaymentResponse response = new PaymentResponse();
        response.setAuthnetTransactionId(transaction.getTransId());
        response.setCorrelationId(correlationId);
        response.setCreatedAt(ZonedDateTime.now());
        response.setSuccess(true);
        
        // Map basic transaction details
        response.setAmount(transaction.getAuthAmount());
        response.setCurrency("USD"); // Authorize.Net primarily uses USD
        response.setResponseCode(String.valueOf(transaction.getResponseCode()));
        response.setResponseReasonCode(String.valueOf(transaction.getResponseReasonCode()));
        response.setResponseReasonText(transaction.getResponseReasonDescription());
        
        // Map authorization details
        if (transaction.getAuthCode() != null) {
            response.setAuthorizationCode(transaction.getAuthCode());
        }
        
        // Map AVS and CVV results
        if (transaction.getAVSResponse() != null) {
            response.setAvsResult(transaction.getAVSResponse());
        }
        if (transaction.getCardCodeResponse() != null) {
            response.setCvvResult(transaction.getCardCodeResponse());
        }
        
        // Determine transaction status based on response code and settlement state
        if (Integer.valueOf(1).equals(transaction.getResponseCode())) {
            if (transaction.getSettleAmount() != null && transaction.getSettleAmount().compareTo(BigDecimal.ZERO) > 0) {
                response.setStatus("CAPTURED");
                response.setTransactionType("PURCHASE");
            } else {
                response.setStatus("AUTHORIZED");
                response.setTransactionType("AUTHORIZE");
            }
        } else {
            response.setStatus("FAILED");
            response.setSuccess(false);
        }
        
        // Map payment method details if available
        if (transaction.getPayment() != null && transaction.getPayment().getCreditCard() != null) {
            PaymentMethodResponse pmResponse = new PaymentMethodResponse();
            pmResponse.setType("CREDIT_CARD");
            pmResponse.setMaskedCardNumber(transaction.getPayment().getCreditCard().getCardNumber());
            pmResponse.setExpiryMonth(transaction.getPayment().getCreditCard().getExpirationDate().substring(0, 2));
            pmResponse.setExpiryYear("20" + transaction.getPayment().getCreditCard().getExpirationDate().substring(2, 4));
            response.setPaymentMethod(pmResponse);
        }
        
        // Set test mode (always true for sandbox)
        response.setTestMode(false);
        
        return response;
    }

    private String getOrGenerateCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
            MDC.put("correlationId", correlationId);
        }
        return correlationId;
    }

    private PaymentResponse buildResponseFromTransaction(Transaction transaction) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(transaction.getTransactionId());
        response.setAuthnetTransactionId(transaction.getAuthnetTransactionId());
        response.setStatus(transaction.getStatus().name());
        response.setTransactionType(transaction.getTransactionType().name());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setAuthorizationCode(transaction.getAuthnetAuthCode());
        response.setResponseCode(transaction.getAuthnetResponseCode());
        response.setResponseReasonText(transaction.getAuthnetResponseReason());
        response.setSuccess(transaction.getStatus() != PaymentStatus.FAILED);
        response.setCreatedAt(transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()));
        response.setCorrelationId(transaction.getCorrelationId());
        return response;
    }

    public void validatePaymentMethod(PaymentMethodRequest paymentMethod) {
        String correlationId = getOrGenerateCorrelationId();
        
        log.debug("Validating payment method - Type: {}, CorrelationId: {}", 
                    paymentMethod != null ? paymentMethod.getType() : "NULL", correlationId);

        try {
            // Basic validation
            if (paymentMethod == null) {
                throw new PaymentProcessingException("Payment method is required", correlationId);
            }

            if (paymentMethod.getType() == null || paymentMethod.getType().trim().isEmpty()) {
                throw new PaymentProcessingException("Payment method type is required", correlationId);
            }

            // Credit card validation
            if ("CREDIT_CARD".equals(paymentMethod.getType())) {
                if (paymentMethod.getCardNumber() == null || paymentMethod.getCardNumber().trim().isEmpty()) {
                    throw new PaymentProcessingException("Card number is required", correlationId);
                }
                
                // Validate card number length (13-19 digits after removing spaces)
                String cardNumberDigits = paymentMethod.getCardNumber().replaceAll("\\s+", "");
                if (cardNumberDigits.length() < 13 || cardNumberDigits.length() > 19) {
                    throw new PaymentProcessingException("Invalid card number length", correlationId);
                }
                
                if (paymentMethod.getExpiryMonth() == null || paymentMethod.getExpiryYear() == null) {
                    throw new PaymentProcessingException("Card expiry date is required", correlationId);
                }
                
                // Validate expiry date format (must be numeric)
                try {
                    Integer.parseInt(paymentMethod.getExpiryMonth());
                    Integer.parseInt(paymentMethod.getExpiryYear());
                } catch (NumberFormatException e) {
                    throw new PaymentProcessingException("Invalid expiry date format", correlationId);
                }
                
                // Validate card has not expired
                int expiryMonth = Integer.parseInt(paymentMethod.getExpiryMonth());
                int expiryYear = Integer.parseInt(paymentMethod.getExpiryYear());
                
                // Handle 2-digit years
                if (expiryYear < 100) {
                    expiryYear += 2000;
                }
                
                java.time.YearMonth expiryYearMonth = java.time.YearMonth.of(expiryYear, expiryMonth);
                java.time.YearMonth currentYearMonth = java.time.YearMonth.now();
                
                if (expiryYearMonth.isBefore(currentYearMonth)) {
                    throw new PaymentProcessingException("Card has expired", correlationId);
                }
                
                if (paymentMethod.getCvv() == null || paymentMethod.getCvv().trim().isEmpty()) {
                    throw new PaymentProcessingException("CVV is required", correlationId);
                }
                
                // Validate CVV length (3-4 digits)
                String cvv = paymentMethod.getCvv().trim();
                if (cvv.length() < 3 || cvv.length() > 4) {
                    throw new PaymentProcessingException("Invalid CVV length", correlationId);
                }
                
                if (paymentMethod.getCardholderName() == null || paymentMethod.getCardholderName().trim().isEmpty()) {
                    throw new PaymentProcessingException("Cardholder name is required", correlationId);
                }
            }

            log.debug("Payment method validation successful - Type: {}, CorrelationId: {}", 
                        paymentMethod.getType(), correlationId);

        } catch (Exception e) {
            log.error("Payment method validation failed - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId);
            
            if (e instanceof PaymentProcessingException) {
                throw e;
            }
            throw new PaymentProcessingException("Payment method validation failed", e, correlationId);
        }
    }

    private Transaction createTransactionEntity(PaymentRequest request, String transactionId, 
                                          TransactionType type, String correlationId) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionType(type);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setCorrelationId(correlationId);
        
        // Find or create customer
        Customer customer = findOrCreateCustomer(request.getCustomer());
        transaction.setCustomer(customer);
        
        // Set order reference if available
        if (request.getOrderNumber() != null) {
            Optional<Order> order = orderRepository.findByOrderNumber(request.getOrderNumber());
            order.ifPresent(o -> transaction.setOrder(o));
        }
        
        // Create and associate payment method
        if (request.getPaymentMethod() != null) {
            PaymentMethod paymentMethod = findOrCreatePaymentMethod(request.getPaymentMethod(), customer);
            transaction.setPaymentMethod(paymentMethod);
        }
        
        return transaction;
    }

    private Transaction createBasicTransactionEntity(String transactionId, TransactionType type, 
                                               BigDecimal amount, String correlationId, String idempotencyKey) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrency("USD");
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setCorrelationId(correlationId);
        
        if (idempotencyKey != null) {
            transaction.setIdempotencyKey(idempotencyKey);
        }
        
        return transaction;
    }

    private void updateTransactionFromResponse(Transaction transaction, PaymentResponse response, 
                                         CreateTransactionResponse authNetResponse) {
        transaction.setStatus(PaymentStatus.valueOf(response.getStatus()));
        
        if (response.getSuccess()) {
            transaction.setAuthnetTransactionId(response.getAuthnetTransactionId());
            transaction.setAuthnetAuthCode(response.getAuthorizationCode());
            transaction.setAuthnetResponseCode(response.getResponseCode());
            transaction.setAuthnetResponseReason(response.getResponseReasonText());
            transaction.setAuthnetAvsResult(response.getAvsResult());
            transaction.setAuthnetCvvResult(response.getCvvResult());
        } else {
            // Store error details in response data
            transaction.getResponseData().put("errorMessage", response.getError().getMessage());
            transaction.getResponseData().put("errorCode", response.getError().getCode());
        }
        
        transaction.setProcessedAt(ZonedDateTime.now());
    }

    private void updateTransactionStatus(String transactionId, PaymentStatus status, String errorMessage) {
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                transaction.setStatus(status);
                if (errorMessage != null) {
                    transaction.getResponseData().put("errorMessage", errorMessage);
                }
                transaction.setProcessedAt(ZonedDateTime.now());
                transactionRepository.save(transaction);
            }
        } catch (Exception e) {
            log.error("Failed to update transaction status - TransactionId: {}, Status: {}, Error: {}", 
                        transactionId, status, e.getMessage());
        }
    }

}