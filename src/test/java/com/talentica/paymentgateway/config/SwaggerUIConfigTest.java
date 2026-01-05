package com.talentica.paymentgateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

import static org.assertj.core.api.Assertions.assertThatCode;

class SwaggerUIConfigTest {

    @Test
    @DisplayName("adds swagger resource handlers and view controller without error")
    void configuresSwagger() {
        SwaggerUIConfig cfg = new SwaggerUIConfig();
        StaticApplicationContext context = new StaticApplicationContext();
        ResourceHandlerRegistry resourceRegistry = new ResourceHandlerRegistry(context, null);
        ViewControllerRegistry viewRegistry = new ViewControllerRegistry(context);
        assertThatCode(() -> cfg.addResourceHandlers(resourceRegistry)).doesNotThrowAnyException();
        assertThatCode(() -> cfg.addViewControllers(viewRegistry)).doesNotThrowAnyException();
    }
}
