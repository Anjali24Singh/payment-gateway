package com.talentica.paymentgateway.dto;

import com.talentica.paymentgateway.dto.analytics.*;
import com.talentica.paymentgateway.dto.common.*;
import com.talentica.paymentgateway.dto.customer.*;
import com.talentica.paymentgateway.dto.metrics.*;
import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.dto.subscription.*;
import com.talentica.paymentgateway.dto.user.*;
import com.talentica.paymentgateway.dto.webhook.*;
import com.talentica.paymentgateway.util.PojoTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DtoPojoTest {

    @Test
    @DisplayName("Exercise getters/setters for DTOs")
    void exerciseDtos() {
        // auth
        PojoTester.exerciseClass(AuthenticationRequest.class);
        PojoTester.exerciseClass(AuthenticationResponse.class);
        PojoTester.exerciseClass(RegistrationRequest.class);
        // payment
        PojoTester.exerciseClass(AddressRequest.class);
        PojoTester.exerciseClass(CustomerRequest.class);
        PojoTester.exerciseClass(PaymentMethodRequest.class);
        PojoTester.exerciseClass(PaymentMethodResponse.class);
        PojoTester.exerciseClass(PaymentRequest.class);
        PojoTester.exerciseClass(PurchaseRequest.class);
        PojoTester.exerciseClass(AuthorizeRequest.class);
        PojoTester.exerciseClass(CaptureRequest.class);
        PojoTester.exerciseClass(VoidRequest.class);
        PojoTester.exerciseClass(RefundRequest.class);
        PojoTester.exerciseClass(PaymentResponse.class);
        PojoTester.exerciseClass(PaymentErrorResponse.class);
        PojoTester.exerciseClass(TransactionStatusResponse.class);
        // user
        PojoTester.exerciseClass(UpdateUserRequest.class);
        PojoTester.exerciseClass(UserResponse.class);
        // subscription
        PojoTester.exerciseClass(CreatePlanRequest.class);
        PojoTester.exerciseClass(PlanResponse.class);
        PojoTester.exerciseClass(ARBSubscriptionRequest.class);
        PojoTester.exerciseClass(ARBSubscriptionResponse.class);
        PojoTester.exerciseClass(CreateSubscriptionRequest.class);
        PojoTester.exerciseClass(UpdateSubscriptionRequest.class);
        PojoTester.exerciseClass(CancelSubscriptionRequest.class);
        PojoTester.exerciseClass(SubscriptionResponse.class);
        // customer
        PojoTester.exerciseClass(CustomerProfileRequest.class);
        PojoTester.exerciseClass(CustomerProfileResponse.class);
        PojoTester.exerciseClass(CustomerPaymentProfileResponse.class);
        PojoTester.exerciseClass(CustomerAddressResponse.class);
        PojoTester.exerciseClass(PaymentProfileRequest.class);
        // analytics
        PojoTester.exerciseClass(AnalyticsDashboardRequest.class);
        PojoTester.exerciseClass(ComplianceReport.class);
        PojoTester.exerciseInnerClasses(ComplianceReport.class);
        PojoTester.exerciseClass(TransactionReportRequest.class);
        PojoTester.exerciseClass(TransactionReportResponse.class);
        PojoTester.exerciseInnerClasses(TransactionReportResponse.class);
        PojoTester.exerciseClass(FailedPaymentAnalysis.class);
        PojoTester.exerciseInnerClasses(FailedPaymentAnalysis.class);
        PojoTester.exerciseClass(TransactionMetrics.class);
        PojoTester.exerciseClass(RevenueMetrics.class);
        PojoTester.exerciseClass(SubscriptionMetrics.class);
        PojoTester.exerciseClass(DashboardMetrics.class);
        // webhook
        PojoTester.exerciseClass(AuthorizeNetWebhookRequest.class);
        PojoTester.exerciseInnerClasses(AuthorizeNetWebhookRequest.class);
        PojoTester.exerciseClass(WebhookResponse.class);
        PojoTester.exerciseInnerClasses(WebhookResponse.class);
        // common
        PojoTester.exerciseClass(StandardErrorResponse.class);
        PojoTester.exerciseInnerClasses(StandardErrorResponse.class);
        PojoTester.exerciseClass(ApiResponse.class);
        PojoTester.exerciseInnerClasses(ApiResponse.class);
    }
}
