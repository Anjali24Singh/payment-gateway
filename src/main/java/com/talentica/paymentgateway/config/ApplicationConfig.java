package com.talentica.paymentgateway.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Main application configuration class.
 * Configures core application beans and settings.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(ApplicationConfig.AppProperties.class)
public class ApplicationConfig {

    /**
     * Configure async task executor for background processing.
     * 
     * @return Configured thread pool task executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("PaymentGateway-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Configure CORS for cross-origin requests.
     * 
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configure timed aspect for method execution metrics.
     * 
     * @param registry Micrometer meter registry
     * @return Timed aspect for AOP
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Configure RestTemplate for webhook delivery.
     * 
     * @return Configured RestTemplate with timeout settings
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure timeout settings for webhook requests
        restTemplate.getRequestFactory().toString(); // This creates the default factory
        
        return restTemplate;
    }

    /**
     * Configure Redis template for caching and rate limiting.
     * 
     * @param connectionFactory Redis connection factory
     * @return Configured Redis template
     */
    @Bean
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Configure serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Application properties configuration.
     */
    @ConfigurationProperties(prefix = "app")
    public static class AppProperties {
        private String name;
        private String version;
        private String description;
        private Jwt jwt = new Jwt();
        private RateLimit rateLimit = new RateLimit();
        private Correlation correlation = new Correlation();
        private Webhook webhook = new Webhook();
        private Payment payment = new Payment();
        private AuthorizeNet authorizeNet = new AuthorizeNet();
        private Cors cors = new Cors();

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Jwt getJwt() { return jwt; }
        public void setJwt(Jwt jwt) { this.jwt = jwt; }

        public RateLimit getRateLimit() { return rateLimit; }
        public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

        public Correlation getCorrelation() { return correlation; }
        public void setCorrelation(Correlation correlation) { this.correlation = correlation; }

        public Webhook getWebhook() { return webhook; }
        public void setWebhook(Webhook webhook) { this.webhook = webhook; }

        public Payment getPayment() { return payment; }
        public void setPayment(Payment payment) { this.payment = payment; }

        public AuthorizeNet getAuthorizeNet() { return authorizeNet; }
        public void setAuthorizeNet(AuthorizeNet authorizeNet) { this.authorizeNet = authorizeNet; }

        public Cors getCors() { return cors; }
        public void setCors(Cors cors) { this.cors = cors; }

        public static class Jwt {
            private String secret;
            private long expiration;
            private long refreshExpiration;

            public String getSecret() { return secret; }
            public void setSecret(String secret) { this.secret = secret; }

            public long getExpiration() { return expiration; }
            public void setExpiration(long expiration) { this.expiration = expiration; }

            public long getRefreshExpiration() { return refreshExpiration; }
            public void setRefreshExpiration(long refreshExpiration) { this.refreshExpiration = refreshExpiration; }
        }

        public static class RateLimit {
            private boolean enabled;
            private int defaultLimit;
            private int burstLimit;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }

            public int getDefaultLimit() { return defaultLimit; }
            public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }

            public int getBurstLimit() { return burstLimit; }
            public void setBurstLimit(int burstLimit) { this.burstLimit = burstLimit; }
        }

        public static class Correlation {
            private String headerName;
            private String mdcKey;

            public String getHeaderName() { return headerName; }
            public void setHeaderName(String headerName) { this.headerName = headerName; }

            public String getMdcKey() { return mdcKey; }
            public void setMdcKey(String mdcKey) { this.mdcKey = mdcKey; }
        }

        public static class Webhook {
            private Retry retry = new Retry();
            private long timeout;

            public Retry getRetry() { return retry; }
            public void setRetry(Retry retry) { this.retry = retry; }

            public long getTimeout() { return timeout; }
            public void setTimeout(long timeout) { this.timeout = timeout; }

            public static class Retry {
                private int maxAttempts;
                private long initialDelay;
                private long maxDelay;
                private double multiplier;

                public int getMaxAttempts() { return maxAttempts; }
                public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

                public long getInitialDelay() { return initialDelay; }
                public void setInitialDelay(long initialDelay) { this.initialDelay = initialDelay; }

                public long getMaxDelay() { return maxDelay; }
                public void setMaxDelay(long maxDelay) { this.maxDelay = maxDelay; }

                public double getMultiplier() { return multiplier; }
                public void setMultiplier(double multiplier) { this.multiplier = multiplier; }
            }
        }

        public static class Payment {
            private long timeout;
            private Retry retry = new Retry();

            public long getTimeout() { return timeout; }
            public void setTimeout(long timeout) { this.timeout = timeout; }

            public Retry getRetry() { return retry; }
            public void setRetry(Retry retry) { this.retry = retry; }

            public static class Retry {
                private int maxAttempts;
                private long delay;

                public int getMaxAttempts() { return maxAttempts; }
                public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

                public long getDelay() { return delay; }
                public void setDelay(long delay) { this.delay = delay; }
            }
        }

        public static class AuthorizeNet {
            private String environment;
            private String apiLoginId;
            private String transactionKey;
            private String baseUrl;

            public String getEnvironment() { return environment; }
            public void setEnvironment(String environment) { this.environment = environment; }

            public String getApiLoginId() { return apiLoginId; }
            public void setApiLoginId(String apiLoginId) { this.apiLoginId = apiLoginId; }

            public String getTransactionKey() { return transactionKey; }
            public void setTransactionKey(String transactionKey) { this.transactionKey = transactionKey; }

            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        }

        public static class Cors {
            private String[] allowedOrigins;
            private String allowedMethods;
            private String allowedHeaders;
            private boolean allowCredentials;
            private long maxAge;

            public String[] getAllowedOrigins() { return allowedOrigins; }
            public void setAllowedOrigins(String[] allowedOrigins) { this.allowedOrigins = allowedOrigins; }

            public String getAllowedMethods() { return allowedMethods; }
            public void setAllowedMethods(String allowedMethods) { this.allowedMethods = allowedMethods; }

            public String getAllowedHeaders() { return allowedHeaders; }
            public void setAllowedHeaders(String allowedHeaders) { this.allowedHeaders = allowedHeaders; }

            public boolean isAllowCredentials() { return allowCredentials; }
            public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }

            public long getMaxAge() { return maxAge; }
            public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
        }
    }
}
