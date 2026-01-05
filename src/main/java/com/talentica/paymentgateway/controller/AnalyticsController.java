package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.DashboardOverviewResponse;
import com.talentica.paymentgateway.dto.analytics.*;
import com.talentica.paymentgateway.dto.metrics.DashboardMetrics;
import com.talentica.paymentgateway.service.AnalyticsService;
import com.talentica.paymentgateway.service.MetricsService;
import com.talentica.paymentgateway.util.CorrelationIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * REST Controller for analytics and reporting operations.
 * 
 * Provides comprehensive analytics endpoints including:
 * - Transaction reporting with filtering and export capabilities
 * - Real-time analytics dashboard metrics
 * - Revenue tracking and subscription performance
 * - Failed payment analysis and fraud detection
 * - Compliance reporting for audit and regulatory requirements
 * 
 * All endpoints support real-time data and include comprehensive filtering options.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/analytics")
@Tag(name = "Analytics & Reporting", description = "Business intelligence and reporting operations")
@SecurityRequirement(name = "Bearer Authentication")
@ConditionalOnProperty(name = "app.features.analytics", havingValue = "true", matchIfMissing = true)
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final MetricsService metricsService;

    public AnalyticsController(AnalyticsService analyticsService, MetricsService metricsService) {
        this.analyticsService = analyticsService;
        this.metricsService = metricsService;
    }

    /**
     * Get basic dashboard overview.
     */
    @GetMapping("/dashboard")
    @Operation(
        summary = "Get dashboard overview",
        description = "Provides basic dashboard overview with key metrics"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('USER')")
    public ResponseEntity<DashboardOverviewResponse> getDashboardOverview() {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Dashboard overview request - CorrelationId: {}", correlationId);
        
        // Return basic dashboard data
        DashboardOverviewResponse response = DashboardOverviewResponse.builder()
                .message("Dashboard overview")
                .totalTransactions(0)
                .totalRevenue(java.math.BigDecimal.ZERO)
                .activeSubscriptions(0)
                .build();
        
        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    /**
     * Generate comprehensive transaction report with filtering and export capabilities.
     */
    @PostMapping("/reports/transactions")
    @Operation(
        summary = "Generate transaction report",
        description = "Creates a comprehensive transaction report with advanced filtering, aggregations, and export options. " +
                     "Supports pagination, time series data, and multiple export formats (JSON, CSV, PDF)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction report generated successfully",
                    content = @Content(schema = @Schema(implementation = TransactionReportResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('USER')")
    public ResponseEntity<TransactionReportResponse> generateTransactionReport(
            @Valid @RequestBody TransactionReportRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Transaction report request - CorrelationId: {}, Period: {} to {}", 
                   correlationId, request.getStartDate(), request.getEndDate());

        // Record metrics
        metricsService.recordAnalyticsRequest("transaction_report");

        TransactionReportResponse response = analyticsService.generateTransactionReport(request);

        log.info("Transaction report generated - CorrelationId: {}, Records: {}", 
                   correlationId, response.getMetadata() != null ? response.getMetadata().getTotalRecords() : 0);

        return ResponseEntity.ok(response);
    }

    /**
     * Get real-time analytics dashboard metrics.
     */
    @PostMapping("/dashboard")
    @Operation(
        summary = "Get analytics dashboard metrics",
        description = "Retrieves real-time analytics dashboard metrics including transaction volumes, " +
                     "revenue tracking, subscription performance, and key business indicators."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard metrics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DashboardMetrics.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('USER')")
    public ResponseEntity<DashboardMetrics> getDashboardMetrics(
            @Valid @RequestBody AnalyticsDashboardRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Dashboard metrics request - CorrelationId: {}, Period: {} to {}", 
                   correlationId, request.getStartDate(), request.getEndDate());

        // Record metrics
        metricsService.recordAnalyticsRequest("dashboard");

        DashboardMetrics metrics = analyticsService.generateDashboardMetrics(request);

        log.info("Dashboard metrics generated - CorrelationId: {}", correlationId);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Get quick dashboard metrics for a predefined time period.
     */
    @GetMapping("/dashboard/quick")
    @Operation(
        summary = "Get quick dashboard metrics",
        description = "Retrieves dashboard metrics for common time periods (24h, 7d, 30d, 90d) " +
                     "with predefined configurations for rapid dashboard loading."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quick metrics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid period parameter"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('USER')")
    public ResponseEntity<DashboardMetrics> getQuickDashboardMetrics(
            @Parameter(description = "Time period (24h, 7d, 30d, 90d)", example = "30d")
            @RequestParam(defaultValue = "30d") String period) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Quick dashboard metrics request - CorrelationId: {}, Period: {}", correlationId, period);

        // Parse period and create request
        AnalyticsDashboardRequest request = createQuickDashboardRequest(period);
        
        // Record metrics
        metricsService.recordAnalyticsRequest("dashboard_quick");

        DashboardMetrics metrics = analyticsService.generateDashboardMetrics(request);

        log.info("Quick dashboard metrics generated - CorrelationId: {}", correlationId);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Analyze failed payments and detect fraud patterns.
     */
    @GetMapping("/failed-payments")
    @Operation(
        summary = "Analyze failed payments",
        description = "Provides comprehensive analysis of failed payments including error patterns, " +
                     "fraud detection, geographic trends, and actionable recommendations for improvement."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Failed payment analysis completed",
                    content = @Content(schema = @Schema(implementation = FailedPaymentAnalysis.class))),
        @ApiResponse(responseCode = "400", description = "Invalid date parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('USER')")
    public ResponseEntity<FailedPaymentAnalysis> analyzeFailedPayments(
            @Parameter(description = "Analysis start date", example = "2025-01-01T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "Analysis end date", example = "2025-01-31T23:59:59Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Failed payment analysis request - CorrelationId: {}, Period: {} to {}", 
                   correlationId, startDate, endDate);

        // Record metrics
        metricsService.recordAnalyticsRequest("failed_payment_analysis");

        FailedPaymentAnalysis analysis = analyticsService.analyzeFailedPayments(startDate, endDate);

        log.info("Failed payment analysis completed - CorrelationId: {}, Failed: {}", 
                   correlationId, analysis.getTotalFailedPayments());

        return ResponseEntity.ok(analysis);
    }

    /**
     * Generate compliance report for audit and regulatory requirements.
     */
    @GetMapping("/compliance/{reportType}")
    @Operation(
        summary = "Generate compliance report",
        description = "Creates comprehensive compliance reports for audit and regulatory requirements. " +
                     "Supports multiple report types including PCI DSS, GDPR, SOX, and general audit reports."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Compliance report generated successfully",
                    content = @Content(schema = @Schema(implementation = ComplianceReport.class))),
        @ApiResponse(responseCode = "400", description = "Invalid report type or date parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ComplianceReport> generateComplianceReport(
            @Parameter(description = "Report type (AUDIT, PCI_DSS, GDPR, SOX)", example = "PCI_DSS")
            @PathVariable String reportType,
            @Parameter(description = "Report start date", example = "2025-01-01T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "Report end date", example = "2025-01-31T23:59:59Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Compliance report request - CorrelationId: {}, Type: {}, Period: {} to {}", 
                   correlationId, reportType, startDate, endDate);

        // Validate report type
        if (!isValidReportType(reportType)) {
            return ResponseEntity.badRequest().build();
        }

        // Record metrics
        metricsService.recordAnalyticsRequest("compliance_report");

        ComplianceReport report = analyticsService.generateComplianceReport(reportType, startDate, endDate);

        log.info("Compliance report generated - CorrelationId: {}, Type: {}", correlationId, reportType);

        return ResponseEntity.ok(report);
    }

    /**
     * Get revenue analytics with advanced metrics.
     */
    @GetMapping("/revenue")
    @Operation(
        summary = "Get revenue analytics",
        description = "Provides comprehensive revenue analytics including total revenue, recurring revenue, " +
                     "refunds, net revenue, and growth trends with comparative analysis."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Revenue analytics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('FINANCE') or hasRole('USER')")
    public ResponseEntity<?> getRevenueAnalytics(
            @Parameter(description = "Analysis start date", example = "2025-01-01T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "Analysis end date", example = "2025-01-31T23:59:59Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @Parameter(description = "Include comparative analysis", example = "true")
            @RequestParam(defaultValue = "false") boolean includeComparison) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Revenue analytics request - CorrelationId: {}, Period: {} to {}", 
                   correlationId, startDate, endDate);

        // Record metrics
        metricsService.recordAnalyticsRequest("revenue_analytics");

        // Generate revenue metrics
        var revenueMetrics = analyticsService.generateRevenueMetrics(startDate, endDate);

        log.info("Revenue analytics generated - CorrelationId: {}", correlationId);

        return ResponseEntity.ok(revenueMetrics);
    }

    /**
     * Get subscription performance analytics.
     */
    @GetMapping("/subscriptions")
    @Operation(
        summary = "Get subscription analytics",
        description = "Provides detailed subscription performance analytics including churn rates, " +
                     "MRR growth, plan performance, and customer lifecycle metrics."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription analytics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST') or hasRole('USER')")
    public ResponseEntity<?> getSubscriptionAnalytics(
            @Parameter(description = "Analysis start date", example = "2025-01-01T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "Analysis end date", example = "2025-01-31T23:59:59Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Subscription analytics request - CorrelationId: {}, Period: {} to {}", 
                   correlationId, startDate, endDate);

        // Record metrics
        metricsService.recordAnalyticsRequest("subscription_analytics");

        // Generate subscription metrics
        var subscriptionMetrics = analyticsService.generateSubscriptionMetrics(startDate, endDate);

        log.info("Subscription analytics generated - CorrelationId: {}", correlationId);

        return ResponseEntity.ok(subscriptionMetrics);
    }

    /**
     * Export analytics data in various formats.
     */
    @PostMapping("/export")
    @Operation(
        summary = "Export analytics data",
        description = "Exports analytics data in various formats (CSV, PDF, Excel) with customizable content and filtering."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Export initiated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid export parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<?> exportAnalyticsData(
            @Valid @RequestBody TransactionReportRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        log.info("Analytics export request - CorrelationId: {}, Format: {}", 
                   correlationId, request.getExportFormat());

        // Record metrics
        metricsService.recordAnalyticsRequest("export");

        // Generate export (this would typically be an async operation)
        TransactionReportResponse response = analyticsService.generateTransactionReport(request);

        log.info("Analytics export completed - CorrelationId: {}", correlationId);

        return ResponseEntity.ok(response.getExportInfo());
    }

    // Helper methods

    private AnalyticsDashboardRequest createQuickDashboardRequest(String period) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime startDate;

        switch (period) {
            case "24h":
                startDate = now.minusDays(1);
                break;
            case "7d":
                startDate = now.minusDays(7);
                break;
            case "30d":
                startDate = now.minusDays(30);
                break;
            case "90d":
                startDate = now.minusDays(90);
                break;
            default:
                startDate = now.minusDays(30); // Default to 30 days
        }

        AnalyticsDashboardRequest request = new AnalyticsDashboardRequest(startDate, now);
        request.setIncludeRealTime(true);
        request.setIncludeBreakdowns(true);
        request.setResolution(determineResolution(period));

        return request;
    }

    private String determineResolution(String period) {
        switch (period) {
            case "24h":
                return "HOUR";
            case "7d":
                return "DAY";
            case "30d":
            case "90d":
                return "DAY";
            default:
                return "DAY";
        }
    }

    private boolean isValidReportType(String reportType) {
        return Arrays.asList("AUDIT", "PCI_DSS", "GDPR", "SOX", "SECURITY", "FINANCIAL")
                .contains(reportType.toUpperCase());
    }
}
