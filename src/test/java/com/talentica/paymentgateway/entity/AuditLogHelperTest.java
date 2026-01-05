package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogHelperTest {

    @Test
    void actor_and_change_descriptions_and_flags() {
        User user = new User();
        user.setUsername("alice");
        AuditLog log = new AuditLog("Order", UUID.randomUUID(), "CREATE", user);
        log.addChange("status", "PENDING", "PAID");
        assertThat(log.hasChanges()).isTrue();
        assertThat(log.getActorDescription()).contains("User");
        assertThat(log.getChangeDescription()).contains("performed CREATE");
        assertThat(log.isCreation()).isTrue();
        log.setAction("UPDATE");
        assertThat(log.isUpdate()).isTrue();
        log.setAction("DELETE");
        assertThat(log.isDeletion()).isTrue();
    }
}
