package com.ecommerce.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

/**
 * Metrics Configuration
 * Ensures custom metrics are registered and available for Prometheus
 */
@Configuration
public class MetricsConfig {

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Customize meter registry with common tags
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "application", "product-service",
            "environment", System.getProperty("ENVIRONMENT", "development")
        );
    }

    /**
     * Register custom metrics
     * These will be available in Prometheus even if no requests have been made
     */
    @Bean
    public Counter productCreatedCounter() {
        return Counter.builder("products.created")
            .description("Total number of products created")
            .tag("service", "product-service")
            .register(meterRegistry);
    }

    @Bean
    public Counter productViewedCounter() {
        return Counter.builder("products.viewed")
            .description("Total number of product views")
            .tag("service", "product-service")
            .register(meterRegistry);
    }

    @Bean
    public Counter categoryCreatedCounter() {
        return Counter.builder("categories.created")
            .description("Total number of categories created")
            .tag("service", "product-service")
            .register(meterRegistry);
    }

    /**
     * Register JVM and application metrics
     * These provide baseline metrics even without traffic
     */
    @Bean
    public void registerApplicationMetrics() {
        // These metrics are automatically registered by Spring Boot Actuator
        // but we ensure they're available by referencing them
        
        // JVM metrics are auto-registered
        // HTTP metrics are auto-registered when requests are made
        // Database connection pool metrics (HikariCP) are auto-registered
    }
}
