package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyHelperTest {

    @Test
    void permissions_and_expiry() {
        ApiKey key = new ApiKey();
        key.setPermissions(new String[]{"READ"});
        java.util.List<String> perms = new java.util.ArrayList<>(java.util.Arrays.asList(key.getPermissions()));
        perms.add("WRITE");
        key.setPermissionsList(perms);
        assertThat(key.hasPermission("READ")).isTrue();
        assertThat(key.hasPermission("WRITE")).isTrue();
        perms.remove("READ");
        key.setPermissionsList(perms);
        assertThat(key.hasPermission("READ")).isFalse();

        key.setExpiresAt(ZonedDateTime.now().minusDays(1));
        assertThat(key.isExpired()).isTrue();
    }
}
