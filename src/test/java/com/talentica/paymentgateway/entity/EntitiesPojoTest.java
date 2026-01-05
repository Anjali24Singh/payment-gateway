package com.talentica.paymentgateway.entity;

import com.talentica.paymentgateway.util.PojoTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntitiesPojoTest {

    @Test
    @DisplayName("Exercise getters/setters for core entities")
    void exerciseCoreEntities() {
        PojoTester.exerciseClass(User.class);
        PojoTester.exerciseClass(Customer.class);
        PojoTester.exerciseClass(PaymentMethod.class);
        PojoTester.exerciseClass(Order.class);
        PojoTester.exerciseClass(Transaction.class);
        PojoTester.exerciseClass(Subscription.class);
        PojoTester.exerciseClass(SubscriptionInvoice.class);
        PojoTester.exerciseClass(SubscriptionPlan.class);
        PojoTester.exerciseClass(ApiKey.class);
        PojoTester.exerciseClass(Webhook.class);
        PojoTester.exerciseClass(AuditLog.class);
        PojoTester.exerciseClass(PaymentStatus.class);
        PojoTester.exerciseClass(TransactionType.class);
        PojoTester.exerciseClass(SubscriptionStatus.class);
        PojoTester.exerciseClass(WebhookStatus.class);
    }
}
