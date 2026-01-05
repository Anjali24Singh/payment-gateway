package com.talentica.paymentgateway.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputSanitizerTest {

    private final InputSanitizer sanitizer = new InputSanitizer();

    @Test
    @DisplayName("sanitizeText removes dangerous content and trims")
    void sanitizeText() {
        String result = sanitizer.sanitizeText(" <script> alert('x') </script>  Hello   World ");
        assertThat(result).doesNotContain("<", ">", "script");
        assertThat(result).contains("Hello World");
    }

    @Test
    void sanitizeEmail() {
        assertThat(sanitizer.sanitizeEmail("  TeSt@Example.com ")).isEqualTo("test@example.com");
        assertThat(sanitizer.sanitizeEmail("bad@com")).isNull();
        assertThat(sanitizer.sanitizeEmail(null)).isNull();
    }

    @Test
    void sanitizePhone() {
        assertThat(sanitizer.sanitizePhone("+1 (555) 123-4567")).isEqualTo("+15551234567");
        assertThat(sanitizer.sanitizePhone("abc")).isNull();
    }

    @Test
    void sanitizeAlphanumeric() {
        assertThat(sanitizer.sanitizeAlphanumeric("  A1b2C3 ")).isEqualTo("A1b2C3");
        assertThat(sanitizer.sanitizeAlphanumeric("abc-123")).isNull();
    }

    @Test
    void sanitizeSafeString() {
        assertThat(sanitizer.sanitizeSafeString(" John_Doe-99 ")).isEqualTo("John_Doe-99");
        assertThat(sanitizer.sanitizeSafeString("<bad>")).isNull();
    }

    @Test
    void sanitizeAmount() {
        assertThat(sanitizer.sanitizeAmount(" 12.34 ")).isEqualTo("12.34");
        assertThat(sanitizer.sanitizeAmount("12.345")).isNull();
    }

    @Test
    void sanitizeCardNumber() {
        assertThat(sanitizer.sanitizeCardNumber("4111-1111-1111-1111")).isEqualTo("4111111111111111");
        assertThat(sanitizer.sanitizeCardNumber("bad")).isNull();
    }

    @Test
    void sanitizeCvv() {
        assertThat(sanitizer.sanitizeCvv(" 123 ")).isEqualTo("123");
        assertThat(sanitizer.sanitizeCvv("12")).isNull();
    }

    @Test
    void sanitizeExpiryDate() {
        assertThat(sanitizer.sanitizeExpiryDate("03/2026")).isEqualTo("03/2026");
        assertThat(sanitizer.sanitizeExpiryDate("13/26")).isNull();
    }

    @Test
    void sanitizeUrl() {
        assertThat(sanitizer.sanitizeUrl("https://example.com/path?x=1")).isEqualTo("https://example.com/path?x=1");
        assertThat(sanitizer.sanitizeUrl("ftp://bad")).isNull();
    }

    @Test
    void sanitizeIpAddress() {
        assertThat(sanitizer.sanitizeIpAddress("192.168.1.10")).isEqualTo("192.168.1.10");
        assertThat(sanitizer.sanitizeIpAddress("gggg")).isNull();
    }

    @Test
    void isSafeInput_and_truncate_and_removeHtml() {
        assertThat(sanitizer.isSafeInput("hello onclick=\"x\"")).isFalse();
        assertThat(sanitizer.isSafeInput("hello")).isTrue();
        assertThat(sanitizer.sanitizeAndTruncate("   Hello World   ", 5)).isEqualTo("Hello");
        assertThat(sanitizer.removeHtmlTags("<b>Bold</b> <i>i</i>")).isEqualTo("Bold i");
    }
}
