package com.talentica.paymentgateway.config;

import net.authorize.Environment;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizeNetConfigTest {

    @Test
    @DisplayName("Creates environment and merchant authentication beans")
    void createsBeans() {
        AuthorizeNetConfig cfg = new AuthorizeNetConfig();
        cfg.setApiLoginId("login");
        cfg.setTransactionKey("key");
        cfg.setEnvironment(AuthorizeNetConfig.AuthNetEnvironment.SANDBOX);
        cfg.setBaseUrl("https://sandbox");

        Environment env = cfg.authorizeNetEnvironment();
        MerchantAuthenticationType auth = cfg.merchantAuthentication();

        assertThat(env).isEqualTo(Environment.SANDBOX);
        assertThat(auth.getName()).isEqualTo("login");
        assertThat(auth.getTransactionKey()).isEqualTo("key");
    }
}
