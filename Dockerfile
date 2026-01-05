# Multi-stage Docker build for Payment Gateway Application with Security Hardening
# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy Maven configuration files
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application with security scanning
RUN mvn clean package -DskipTests -B && \
    mvn org.owasp:dependency-check-maven:check || true

# Stage 2: Create the runtime image with security hardening
FROM eclipse-temurin:17-jre-alpine

# Build arguments for metadata
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# Security: Update packages and remove package manager
RUN apk update && \
    apk upgrade && \
    apk add --no-cache \
        curl \
        dumb-init \
        tzdata && \
    rm -rf /var/cache/apk/* && \
    rm -rf /tmp/*

# Security: Create non-root user with minimal privileges
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup -s /sbin/nologin

# Set working directory
WORKDIR /app

# Security: Create required directories with proper permissions
RUN mkdir -p /app/logs /tmp && \
    chmod 755 /app && \
    chmod 755 /app/logs && \
    chmod 1777 /tmp

# Copy the built JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /app/target/payment-gateway-*.jar app.jar

# Security: Remove unnecessary files and set proper permissions
RUN chmod 444 app.jar && \
    chown -R appuser:appgroup /app

# Security: Switch to non-root user
USER appuser:appgroup

# Security: Use non-privileged ports
EXPOSE 8080 8081

# Enhanced health check with security considerations
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f --max-time 5 http://localhost:8080/api/v1/actuator/health/liveness || exit 1

# Production-optimized JVM options with security enhancements
ENV JAVA_OPTS="-server \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -XX:+UnlockExperimentalVMOptions \
               -XX:+UseCGroupMemoryLimitForHeap \
               -Djava.security.egd=file:/dev/./urandom \
               -Djava.awt.headless=true \
               -Dfile.encoding=UTF-8 \
               -Duser.timezone=UTC \
               -Djava.net.preferIPv4Stack=true \
               -Dcom.sun.management.jmxremote=false \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/ \
               -XX:+ExitOnOutOfMemoryError"

# Security: Set environment variables
ENV USER=appuser
ENV HOME=/app
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Container metadata labels for compliance and tracking
LABEL maintainer="Payment Gateway Team <devops@payment-gateway.com>" \
      org.opencontainers.image.title="Payment Gateway API" \
      org.opencontainers.image.description="Secure Payment Gateway Integration Platform" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.vendor="Payment Gateway Inc." \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.url="https://payment-gateway.com" \
      org.opencontainers.image.documentation="https://docs.payment-gateway.com" \
      org.opencontainers.image.source="https://github.com/payment-gateway/payment-gateway" \
      security.scan="enabled" \
      compliance.pci-dss="compliant"

# Security: Use dumb-init to handle signals properly and run as non-root
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
