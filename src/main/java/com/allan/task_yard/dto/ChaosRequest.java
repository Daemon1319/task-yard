package com.allan.task_yard.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ChaosRequest(
    @NotNull(message = "enabled is required") Boolean enabled,

    @NotNull(message = "failureRate is required")
    @DecimalMin(value = "0.0", message = "failureRate must be >= 0.0")
    @DecimalMax(value = "1.0", message = "failureRate must be <= 1.0")
    Double failureRate
) {
}