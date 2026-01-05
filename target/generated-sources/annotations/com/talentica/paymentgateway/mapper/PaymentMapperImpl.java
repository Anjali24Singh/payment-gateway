package com.talentica.paymentgateway.mapper;

import com.talentica.paymentgateway.dto.payment.PaymentMethodResponse;
import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentMethod;
import com.talentica.paymentgateway.entity.Transaction;
import java.time.ZonedDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-05T19:18:44+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Override
    public PaymentResponse toPaymentResponse(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        PaymentResponse paymentResponse = new PaymentResponse();

        paymentResponse.setTransactionId( transaction.getTransactionId() );
        paymentResponse.setAuthnetTransactionId( transaction.getAuthnetTransactionId() );
        paymentResponse.setAmount( transaction.getAmount() );
        paymentResponse.setCurrency( transaction.getCurrency() );
        if ( transaction.getStatus() != null ) {
            paymentResponse.setStatus( transaction.getStatus().name() );
        }
        if ( transaction.getTransactionType() != null ) {
            paymentResponse.setTransactionType( transaction.getTransactionType().name() );
        }
        paymentResponse.setAuthorizationCode( transaction.getAuthnetAuthCode() );
        paymentResponse.setAvsResult( transaction.getAuthnetAvsResult() );
        paymentResponse.setCvvResult( transaction.getAuthnetCvvResult() );
        paymentResponse.setResponseCode( transaction.getAuthnetResponseCode() );
        paymentResponse.setResponseReasonText( transaction.getAuthnetResponseReason() );
        paymentResponse.setCustomerId( transactionCustomerCustomerReference( transaction ) );
        paymentResponse.setCorrelationId( transaction.getCorrelationId() );
        if ( transaction.getCreatedAt() != null ) {
            paymentResponse.setCreatedAt( ZonedDateTime.parse( formatDateTime( transaction.getCreatedAt() ) ) );
        }
        paymentResponse.setPaymentMethod( toPaymentMethodResponse( transaction.getPaymentMethod() ) );

        return paymentResponse;
    }

    @Override
    public PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        if ( paymentMethod == null ) {
            return null;
        }

        PaymentMethodResponse paymentMethodResponse = new PaymentMethodResponse();

        paymentMethodResponse.setType( paymentMethod.getPaymentType() );
        paymentMethodResponse.setMaskedCardNumber( paymentMethod.getCardLastFour() );
        paymentMethodResponse.setCardBrand( paymentMethod.getCardBrand() );
        paymentMethodResponse.setExpiryMonth( paymentMethod.getExpiryMonth() );
        paymentMethodResponse.setExpiryYear( paymentMethod.getExpiryYear() );
        paymentMethodResponse.setCardholderName( paymentMethod.getCardholderName() );
        paymentMethodResponse.setToken( paymentMethod.getPaymentToken() );

        return paymentMethodResponse;
    }

    private String transactionCustomerCustomerReference(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }
        Customer customer = transaction.getCustomer();
        if ( customer == null ) {
            return null;
        }
        String customerReference = customer.getCustomerReference();
        if ( customerReference == null ) {
            return null;
        }
        return customerReference;
    }
}
