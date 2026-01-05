package com.talentica.paymentgateway.util;

import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.entity.PaymentMethod;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizeNetMapperTest {

    private final AuthorizeNetMapper mapper = new AuthorizeNetMapper();

    private MerchantAuthenticationType merchant() {
        MerchantAuthenticationType m = new MerchantAuthenticationType();
        m.setName("login");
        m.setTransactionKey("key");
        return m;
    }

    @Test
    @DisplayName("Maps purchase request to CreateTransactionRequest")
    void mapPurchase() {
        PaymentRequest req = new PaymentRequest();
        PaymentMethodRequest pm = PaymentMethodRequest.builder()
            .cardNumber("4111111111111111")
            .expiryMonth("12")
            .expiryYear("2026")
            .cvv("123")
            .cardholderName("John")
            .type("CREDIT_CARD")
            .build();
        req.setPaymentMethod(pm);
        req.setAmount(new BigDecimal("10.00"));
        req.setDescription("desc");
        req.setInvoiceNumber("inv");
        CreateTransactionRequest ctr = mapper.mapToPurchaseTransaction(req, merchant());
        assertThat(ctr.getTransactionRequest().getPayment()).isNotNull();
        assertThat(ctr.getTransactionRequest().getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Maps authorize request to CreateTransactionRequest")
    void mapAuthorize() {
        AuthorizeRequest req = new AuthorizeRequest();
        PaymentMethodRequest pm = PaymentMethodRequest.builder()
            .cardNumber("4111111111111111")
            .expiryMonth("12")
            .expiryYear("2026")
            .cvv("123")
            .cardholderName("John")
            .type("CREDIT_CARD")
            .build();
        req.setPaymentMethod(pm);
        req.setAmount(new BigDecimal("5.00"));
        CreateTransactionRequest ctr = mapper.mapToAuthorizeTransaction(req, merchant());
        assertThat(ctr.getTransactionRequest().getPayment()).isNotNull();
    }

    @Test
    @DisplayName("Maps capture/refund/void requests and response mapping")
    void mapOthersAndResponse() {
        CaptureRequest cap = new CaptureRequest();
        cap.setAmount(new BigDecimal("2.50"));
        CreateTransactionRequest capReq = mapper.mapToCaptureTransaction(cap, "tx123", merchant());
        assertThat(capReq.getTransactionRequest().getRefTransId()).isEqualTo("tx123");

        RefundRequest refund = new RefundRequest();
        refund.setDescription("d");
        PaymentMethod pmEntity = new PaymentMethod();
        pmEntity.setCardLastFour("1111");
        CreateTransactionRequest refundReq = mapper.mapToRefundTransaction(refund, "tx321", pmEntity, merchant());
        assertThat(refundReq.getTransactionRequest().getRefTransId()).isEqualTo("tx321");

        VoidRequest v = new VoidRequest();
        v.setDescription("x");
        CreateTransactionRequest voidReq = mapper.mapToVoidTransaction(v, "tx9", merchant());
        assertThat(voidReq.getTransactionRequest().getRefTransId()).isEqualTo("tx9");

        // Response mapping OK path
        CreateTransactionResponse ok = new CreateTransactionResponse();
        var msg = new net.authorize.api.contract.v1.MessagesType();
        msg.setResultCode(MessageTypeEnum.OK);
        ok.setMessages(msg);
        var tx = new TransactionResponse();
        tx.setTransId("authnetId");
        tx.setAuthCode("A1");
        tx.setResponseCode("1");
        var msgs = new TransactionResponse.Messages();
        var item = new TransactionResponse.Messages.Message();
        item.setCode("I00001");
        item.setDescription("Approved");
        msgs.getMessage().add(item);
        tx.setMessages(msgs);
        ok.setTransactionResponse(tx);
        PaymentMethodRequest paymentMethod = PaymentMethodRequest.builder()
            .cardNumber("4111111111111111")
            .expiryMonth("12")
            .expiryYear("2026")
            .cvv("123")
            .cardholderName("J")
            .type("CREDIT_CARD")
            .build();
        PaymentResponse pr = mapper.mapToPaymentResponse(ok, "internal", "PURCHASE", paymentMethod, "corr");
        assertThat(pr.getAuthnetTransactionId()).isEqualTo("authnetId");
        assertThat(pr.getStatus()).isEqualTo("CAPTURED");

        // Error mapping
        CreateTransactionResponse err = new CreateTransactionResponse();
        var msg2 = new net.authorize.api.contract.v1.MessagesType();
        msg2.setResultCode(MessageTypeEnum.ERROR);
        var mm = new net.authorize.api.contract.v1.MessagesType.Message();
        mm.setCode("2");
        mm.setText("Declined");
        msg2.getMessage().add(mm);
        err.setMessages(msg2);
        PaymentResponse pr2 = mapper.mapToPaymentResponse(err, "internal", "PURCHASE", null, "corr");
    }

    @Test
    void generateTransactionId() {
        String id = mapper.generateTransactionId();
        assertThat(id).startsWith("txn_").hasSizeGreaterThan(10);
    }
}
