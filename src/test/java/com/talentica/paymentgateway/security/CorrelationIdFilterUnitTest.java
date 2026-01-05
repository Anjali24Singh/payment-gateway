package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.config.ApplicationConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CorrelationIdFilter.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterUnitTest {

    @Mock
    private ApplicationConfig.AppProperties appProperties;

    @Mock
    private ApplicationConfig.AppProperties.Correlation correlation;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter correlationIdFilter;

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter(appProperties);
        MDC.clear();
    }

    @Test
    void constructor_WithAppProperties_ShouldCreateFilter() {
        // When & Then
        assertNotNull(correlationIdFilter);
    }

    @Test
    void doFilterInternal_WithExistingCorrelationId_ShouldUseExisting() throws ServletException, IOException {
        // Given
        String existingId = "existing-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(existingId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", existingId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithoutCorrelationId_ShouldGenerateNew() throws ServletException, IOException {
        // Given
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(response.getStatus()).thenReturn(201);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("X-Correlation-ID"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithCustomHeaderName_ShouldUseCustomHeader() throws ServletException, IOException {
        // Given
        String customHeader = "X-Custom-Trace-ID";
        String correlationId = "custom-trace-123";
        
        when(appProperties.getCorrelation()).thenReturn(correlation);
        when(correlation.getHeaderName()).thenReturn(customHeader);
        when(correlation.getMdcKey()).thenReturn("customTraceId");
        
        when(request.getHeader(customHeader)).thenReturn(correlationId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(customHeader, correlationId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithAlternativeHeader_ShouldUseAlternative() throws ServletException, IOException {
        // Given
        String alternativeId = "request-id-123";
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(request.getHeader("X-Request-ID")).thenReturn(alternativeId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", alternativeId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithTraceIdHeader_ShouldUseTraceId() throws ServletException, IOException {
        // Given
        String traceId = "trace-id-456";
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(request.getHeader("X-Request-ID")).thenReturn(null);
        when(request.getHeader("X-Trace-ID")).thenReturn(traceId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", traceId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidCorrelationId_ShouldGenerateNew() throws ServletException, IOException {
        // Given
        String invalidId = "invalid@correlation#id!";
        when(request.getHeader("X-Correlation-ID")).thenReturn(invalidId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("X-Correlation-ID"), argThat(id -> !id.equals(invalidId)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithTooLongCorrelationId_ShouldTruncate() throws ServletException, IOException {
        // Given
        String longId = "a".repeat(150); // Longer than 128 characters
        when(request.getHeader("X-Correlation-ID")).thenReturn(longId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("X-Correlation-ID"), argThat(id -> id.length() == 128));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithWhitespaceCorrelationId_ShouldTrim() throws ServletException, IOException {
        // Given
        String whitespaceId = "  correlation-id-123  ";
        String trimmedId = "correlation-id-123";
        when(request.getHeader("X-Correlation-ID")).thenReturn(whitespaceId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", trimmedId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithEmptyCorrelationId_ShouldGenerateNew() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Correlation-ID")).thenReturn("");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("X-Correlation-ID"), argThat(id -> !id.isEmpty()));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNullAppProperties_ShouldUseDefaults() throws ServletException, IOException {
        // Given
        CorrelationIdFilter filterWithNullProps = new CorrelationIdFilter(null);
        String correlationId = "test-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        filterWithNullProps.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", correlationId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNullCorrelationConfig_ShouldUseDefaults() throws ServletException, IOException {
        // Given
        when(appProperties.getCorrelation()).thenReturn(null);
        String correlationId = "test-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", correlationId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithEmptyHeaderName_ShouldUseDefault() throws ServletException, IOException {
        // Given
        when(appProperties.getCorrelation()).thenReturn(correlation);
        when(correlation.getHeaderName()).thenReturn("");
        when(correlation.getMdcKey()).thenReturn("correlationId");
        
        String correlationId = "test-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", correlationId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithFilterChainException_ShouldPropagateException() throws ServletException, IOException {
        // Given
        String correlationId = "test-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        
        ServletException expectedException = new ServletException("Filter chain error");
        doThrow(expectedException).when(filterChain).doFilter(request, response);

        // When & Then
        ServletException thrownException = assertThrows(ServletException.class, () -> {
            correlationIdFilter.doFilterInternal(request, response, filterChain);
        });
        
        assertEquals(expectedException, thrownException);
        verify(response).setHeader("X-Correlation-ID", correlationId);
    }

    @Test
    void doFilterInternal_ShouldClearMDCInFinally() throws ServletException, IOException {
        // Given
        String correlationId = "test-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        // MDC should be cleared after filter execution
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void getCurrentCorrelationId_WithMDCSet_ShouldReturnId() {
        // Given
        String correlationId = "test-correlation-id";
        MDC.put("correlationId", correlationId);

        // When
        String result = CorrelationIdFilter.getCurrentCorrelationId();

        // Then
        assertEquals(correlationId, result);
    }

    @Test
    void getCurrentCorrelationId_WithoutMDC_ShouldReturnNull() {
        // Given
        MDC.clear();

        // When
        String result = CorrelationIdFilter.getCurrentCorrelationId();

        // Then
        assertNull(result);
    }

    @Test
    void setCorrelationId_WithValidId_ShouldSetMDC() {
        // Given
        String correlationId = "test-correlation-id";

        // When
        CorrelationIdFilter.setCorrelationId(correlationId);

        // Then
        assertEquals(correlationId, MDC.get("correlationId"));
    }

    @Test
    void setCorrelationId_WithNullId_ShouldNotSetMDC() {
        // Given
        MDC.clear();

        // When
        CorrelationIdFilter.setCorrelationId(null);

        // Then
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void setCorrelationId_WithEmptyId_ShouldNotSetMDC() {
        // Given
        MDC.clear();

        // When
        CorrelationIdFilter.setCorrelationId("");

        // Then
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void clearCorrelationId_ShouldRemoveFromMDC() {
        // Given
        MDC.put("correlationId", "test-id");

        // When
        CorrelationIdFilter.clearCorrelationId();

        // Then
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void doFilterInternal_WithValidAlphanumericId_ShouldAccept() throws ServletException, IOException {
        // Given
        String validId = "abc123-def456_ghi789";
        when(request.getHeader("X-Correlation-ID")).thenReturn(validId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", validId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithSpecialCharacters_ShouldReject() throws ServletException, IOException {
        // Given
        String invalidId = "test@correlation.id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(invalidId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("X-Correlation-ID"), argThat(id -> !id.equals(invalidId)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithRequestIdHeader_ShouldUseRequestId() throws ServletException, IOException {
        // Given
        String requestId = "request-id-789";
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(request.getHeader("X-Request-ID")).thenReturn(null);
        when(request.getHeader("X-Trace-ID")).thenReturn(null);
        when(request.getHeader("Request-ID")).thenReturn(requestId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", requestId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithTraceIdLowercase_ShouldUseTraceId() throws ServletException, IOException {
        // Given
        String traceId = "trace-id-lowercase";
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        when(request.getHeader("X-Request-ID")).thenReturn(null);
        when(request.getHeader("X-Trace-ID")).thenReturn(null);
        when(request.getHeader("Request-ID")).thenReturn(null);
        when(request.getHeader("Trace-ID")).thenReturn(null);
        when(request.getHeader("X-Request-Id")).thenReturn(traceId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(response.getStatus()).thenReturn(200);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Correlation-ID", traceId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldLogRequestStartAndCompletion() throws ServletException, IOException {
        // Given
        String correlationId = "test-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(response.getStatus()).thenReturn(201);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        // Note: We can't easily verify log messages in unit tests without additional setup
        // but we can verify that the filter chain was called and response header was set
        verify(response).setHeader("X-Correlation-ID", correlationId);
    }
}
