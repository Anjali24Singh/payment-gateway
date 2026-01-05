package com.talentica.paymentgateway.dto;

import com.talentica.paymentgateway.dto.analytics.FailedPaymentAnalysis;
import com.talentica.paymentgateway.dto.analytics.TransactionReportResponse;
import com.talentica.paymentgateway.dto.common.ApiResponse;
import com.talentica.paymentgateway.dto.common.StandardErrorResponse;
import com.talentica.paymentgateway.dto.payment.AddressRequest;
import com.talentica.paymentgateway.dto.payment.PaymentErrorResponse;
import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.dto.webhook.AuthorizeNetWebhookRequest;
import com.talentica.paymentgateway.dto.webhook.WebhookResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DtoAdvancedTest {

    @Test
    void apiResponse_and_standardError_factories() {
        ApiResponse<String> ok = ApiResponse.success("ok");
        assertThat(ok.isSuccess()).isTrue();
        assertThat(ok.getData()).isEqualTo("ok");

        ApiResponse<String> okMeta = ApiResponse.success("data", new ApiResponse.ResponseMetadata("v1", "corr"));
        assertThat(okMeta.getMetadata().getVersion()).isEqualTo("v1");

        ApiResponse<String> err = ApiResponse.error("CODE", "msg", "desc");
        assertThat(err.isSuccess()).isFalse();
        assertThat(err.getError().getCode()).isEqualTo("CODE");

        StandardErrorResponse e1 = StandardErrorResponse.validation("bad", "corr");
        StandardErrorResponse e2 = StandardErrorResponse.authentication("bad", "corr");
        StandardErrorResponse e3 = StandardErrorResponse.authorization("bad", "corr");
        StandardErrorResponse e4 = StandardErrorResponse.notFound("bad", "corr");
        StandardErrorResponse e5 = StandardErrorResponse.paymentError("bad", "corr");
        StandardErrorResponse e6 = StandardErrorResponse.businessError("BIZ", "bad", "corr");
        StandardErrorResponse e7 = StandardErrorResponse.systemError("bad", "corr");
        StandardErrorResponse e8 = StandardErrorResponse.rateLimitExceeded("bad", "corr");
        assertThat(e1.getCategory()).isEqualTo("VALIDATION_ERROR");
        assertThat(e8.getStatus()).isEqualTo(429);
    }

    @Test
    void webhookResponse_and_authorizeNetWebhook_predicates() {
        WebhookResponse w1 = WebhookResponse.success("EV-1", "corr");
        assertThat(w1.isSuccess()).isTrue();
        WebhookResponse w2 = WebhookResponse.error("EV-1", "corr", "err");
        assertThat(w2.isError()).isTrue();
        WebhookResponse w3 = WebhookResponse.duplicateEvent("EV-1", "corr");
        assertThat(w3.isDuplicate()).isTrue();
        WebhookResponse.signatureError("EV-1", "corr");
        WebhookResponse.processingError("EV-1", "corr", "m", new RuntimeException("x"));

        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = new AuthorizeNetWebhookRequest.AuthorizeNetPayload();
        payload.setId("TXN-1");
        AuthorizeNetWebhookRequest req = new AuthorizeNetWebhookRequest("N-1", "net.authorize.payment.authcapture.created", ZonedDateTime.now(), "W-1", payload);
        assertThat(req.isPaymentEvent()).isTrue();
        assertThat(req.isSuccessfulPayment()).isTrue();
        assertThat(req.getTransactionType()).isEqualTo("PURCHASE");
        req.setEventType("net.authorize.payment.refund.created");
        assertThat(req.isRefundEvent()).isTrue();
        req.setEventType("net.authorize.payment.void.created");
        assertThat(req.isVoidEvent()).isTrue();
        req.setEventType("net.authorize.payment.fraud.approved");
        assertThat(req.isFraudEvent()).isTrue();
    }

    @Test
    void paymentError_and_response_helpers_and_address_helpers() {
        PaymentErrorResponse.cardDeclined("AVS mismatch", "corr");
        PaymentErrorResponse.insufficientFunds("corr");
        PaymentErrorResponse.invalidCard("corr");
        PaymentErrorResponse.processingError("oops", "corr");
        PaymentErrorResponse.networkError("corr");
        PaymentErrorResponse.validationError("bad", "corr");

        PaymentResponse pr = new PaymentResponse("T1", "A1", "SETTLED", new BigDecimal("10.00"), "USD");
        pr.addValidationError("amount", "too low");
        assertThat(pr.getValidationErrors()).isNotEmpty();
        assertThat(pr.toString()).contains("transactionId");

        AddressRequest ar = new AddressRequest();
        ar.setFirstName("John");
        ar.setLastName("Doe");
        ar.setAddress1("123 St");
        ar.setCity("NY");
        ar.setState("NY");
        ar.setZipCode("10001");
        ar.setCountry("US");
        assertThat(ar.getFullName()).isEqualTo("John Doe");
        assertThat(ar.getFullAddress()).contains("123 St").contains("NY").contains("10001").contains("US");
    }

    @Test
    void analytics_dtos_inner_classes() {
        // TransactionReportResponse
        TransactionReportResponse.ReportMetadata meta = new TransactionReportResponse.ReportMetadata(100, 0, 20, 5);
        meta.setPeriodStart(ZonedDateTime.now().minusDays(30));
        meta.setPeriodEnd(ZonedDateTime.now());
        TransactionReportResponse.TransactionAggregations aggr = new TransactionReportResponse.TransactionAggregations();
        aggr.setTotalTransactions(100);
        aggr.setSuccessfulTransactions(90);
        aggr.setFailedTransactions(10);
        aggr.setTotalVolume(new BigDecimal("1000.00"));
        aggr.setAverageAmount(new BigDecimal("10.00"));
        aggr.setMedianAmount(new BigDecimal("9.50"));
        TransactionReportResponse.ExportInfo ei = new TransactionReportResponse.ExportInfo("CSV", "file.csv");
        ei.setDownloadUrl("https://example.com");
        ei.setFileSizeBytes(12345);
        TransactionReportResponse.TimeSeriesData ts = new TransactionReportResponse.TimeSeriesData(ZonedDateTime.now(), 5, new BigDecimal("50.00"));
        ts.setAverageAmount(new BigDecimal("10.00"));
        ts.setPeriod("day");
        TransactionReportResponse resp = new TransactionReportResponse();
        resp.setMetadata(meta);
        resp.setAggregations(aggr);
        resp.setExportInfo(ei);
        resp.setTimeSeries(List.of(ts));
        assertThat(resp.getMetadata().getTotalRecords()).isEqualTo(100);

        // FailedPaymentAnalysis
        FailedPaymentAnalysis fpa = new FailedPaymentAnalysis();
        FailedPaymentAnalysis.FailureCodeAnalysis fca = new FailedPaymentAnalysis.FailureCodeAnalysis("E1", "desc", 5, 5.0);
        FailedPaymentAnalysis.GeographicFailurePattern g = new FailedPaymentAnalysis.GeographicFailurePattern();
        g.setRegion("NA");
        FailedPaymentAnalysis.TemporalFailurePattern t = new FailedPaymentAnalysis.TemporalFailurePattern();
        t.setTimePeriod("hour");
        FailedPaymentAnalysis.FraudRiskIndicators fri = new FailedPaymentAnalysis.FraudRiskIndicators();
        fri.setFraudScore(10.0);
        fpa.setErrorCodeBreakdown(Map.of("E1", fca));
        fpa.setGeographicPatterns(List.of(g));
        fpa.setTemporalPatterns(List.of(t));
        fpa.setRiskIndicators(fri);
        assertThat(fpa.getErrorCodeBreakdown().get("E1").getDescription()).isEqualTo("desc");
    }
}
