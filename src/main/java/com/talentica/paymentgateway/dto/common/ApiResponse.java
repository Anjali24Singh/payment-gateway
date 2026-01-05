package com.talentica.paymentgateway.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for consistent response format across all endpoints.
 * 
 * @param <T> Type of the response data
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response data payload")
    private T data;

    @Schema(description = "Error information (present only when success=false)")
    private ErrorInfo error;

    @Schema(description = "Response metadata")
    private ResponseMetadata metadata;

    @Schema(description = "Response timestamp", example = "2025-09-10T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(T data) {
        this();
        this.success = true;
        this.data = data;
    }

    public ApiResponse(ErrorInfo error) {
        this();
        this.success = false;
        this.error = error;
    }

    public ApiResponse(T data, ResponseMetadata metadata) {
        this();
        this.success = true;
        this.data = data;
        this.metadata = metadata;
    }

    // Static factory methods for convenience
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }

    public static <T> ApiResponse<T> success(T data, ResponseMetadata metadata) {
        return new ApiResponse<>(data, metadata);
    }

    public static <T> ApiResponse<T> error(ErrorInfo error) {
        return new ApiResponse<>(error);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(new ErrorInfo(code, message));
    }

    public static <T> ApiResponse<T> error(String code, String message, String description) {
        return new ApiResponse<>(new ErrorInfo(code, message, description));
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorInfo getError() {
        return error;
    }

    public void setError(ErrorInfo error) {
        this.error = error;
    }

    public ResponseMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Error information structure for API responses.
     */
    @Schema(description = "Error information")
    public static class ErrorInfo {
        @Schema(description = "Error code", example = "PAYMENT_FAILED")
        private String code;

        @Schema(description = "Error message", example = "Payment processing failed")
        private String message;

        @Schema(description = "Detailed error description")
        private String description;

        @Schema(description = "Correlation ID for tracking", example = "corr-12345678")
        private String correlationId;

        public ErrorInfo() {}

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorInfo(String code, String message, String description) {
            this.code = code;
            this.message = message;
            this.description = description;
        }

        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    }

    /**
     * Response metadata for additional context.
     */
    @Schema(description = "Response metadata")
    public static class ResponseMetadata {
        @Schema(description = "API version", example = "v1")
        private String version;

        @Schema(description = "Request correlation ID", example = "corr-12345678")
        private String correlationId;

        @Schema(description = "Processing time in milliseconds", example = "150")
        private Long processingTimeMs;

        @Schema(description = "Pagination information")
        private PaginationInfo pagination;

        public ResponseMetadata() {}

        public ResponseMetadata(String version, String correlationId) {
            this.version = version;
            this.correlationId = correlationId;
        }

        // Getters and setters
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public Long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        public PaginationInfo getPagination() { return pagination; }
        public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    }

    /**
     * Pagination information for list responses.
     */
    @Schema(description = "Pagination information")
    public static class PaginationInfo {
        @Schema(description = "Current page number (0-based)", example = "0")
        private int page;

        @Schema(description = "Number of items per page", example = "20")
        private int size;

        @Schema(description = "Total number of items", example = "150")
        private long totalElements;

        @Schema(description = "Total number of pages", example = "8")
        private int totalPages;

        @Schema(description = "Whether there are more pages", example = "true")
        private boolean hasNext;

        @Schema(description = "Whether there are previous pages", example = "false")
        private boolean hasPrevious;

        public PaginationInfo() {}

        public PaginationInfo(int page, int size, long totalElements, int totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.hasNext = page < totalPages - 1;
            this.hasPrevious = page > 0;
        }

        // Getters and setters
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }
}
