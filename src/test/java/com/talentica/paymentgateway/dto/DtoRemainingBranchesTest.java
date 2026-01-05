package com.talentica.paymentgateway.dto;

import com.talentica.paymentgateway.dto.common.ApiResponse;
import com.talentica.paymentgateway.dto.common.StandardErrorResponse;
import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.dto.payment.PaymentErrorResponse;
import com.talentica.paymentgateway.dto.webhook.WebhookResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtoRemainingBranchesTest {

    @Test
    void apiResponse_pagination_and_errorinfo_fields() {
        ApiResponse.ResponseMetadata meta = new ApiResponse.ResponseMetadata("v1","corr");
        ApiResponse.PaginationInfo pg = new ApiResponse.PaginationInfo(0, 20, 100, 5);
        meta.setPagination(pg);
        ApiResponse<String> resp = new ApiResponse<>("d", meta);
        assertThat(resp.getMetadata().getPagination().isHasNext()).isTrue();
        assertThat(resp.getMetadata().getPagination().isHasPrevious()).isFalse();

        ApiResponse.ErrorInfo ei = new ApiResponse.ErrorInfo("E","m","desc");
        ei.setCorrelationId("corr");
        ApiResponse<String> err = ApiResponse.error(ei);
        assertThat(err.getError().getCorrelationId()).isEqualTo("corr");
    }

    @Test
    void standardError_validationError_rejectedValue() {
        StandardErrorResponse.ValidationError ve = new StandardErrorResponse.ValidationError("f","C","msg", 10);
        assertThat(ve.getRejectedValue()).isEqualTo(10);
    }

    @Test
    void paymentResponse_error_fields_and_retry() {
        PaymentResponse pr = new PaymentResponse();
        pr.setErrorCode("CODE");
        pr.setErrorMessage("msg");
        pr.setErrorCategory("CAT");
        pr.setRetryable(true);
        pr.setSuggestedAction("try");
        pr.setDetailedError("det");
        pr.setRetryAfterSeconds(10);
        pr.setMaxRetryAttempts(3);
        assertThat(pr.getErrorCode()).isEqualTo("CODE");
        pr.addValidationError("f","m");
        assertThat(pr.getValidationErrors()).isNotEmpty();
    }

    @Test
    void webhookResponse_all_factories() {
        WebhookResponse.error("e","c","m", new WebhookResponse.WebhookError("CODE","desc","sugg"));
        WebhookResponse.validationError("e","c","m", new WebhookResponse.WebhookError());
    }

    @Test
    void paymentError_detail_toString() {
        PaymentErrorResponse.ErrorDetail d = new PaymentErrorResponse.ErrorDetail("f","c","m");
        assertThat(d.toString()).contains("f");
    }
}
