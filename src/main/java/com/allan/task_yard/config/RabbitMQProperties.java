package com.allan.task_yard.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("task-yard")
public record RabbitMQProperties(

    @Min(value = 0, message = "max-retries must be >= 0")
    int maxRetries,

    @NotEmpty(message = "retry-delays-ms must not be empty")
    List<Long> retryDelaysMs,

    ChaosProperties chaos
) {

public record ChaosProperties(
    boolean defaultEnabled,

    @DecimalMin(value = "0.0", message = "default-failure-rate must be >= 0.0")
    @DecimalMax(value = "1.0", message = "default-failure-rate must be <= 1.0")
    double defaultFailureRate
) {
}
}