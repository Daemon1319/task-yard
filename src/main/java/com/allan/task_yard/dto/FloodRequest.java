package com.allan.task_yard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FloodRequest(
    @NotNull(message = "count is required")
    @Min(value = 1, message = "count must be at least 1")
    @Max(value = 500, message = "count must not exceed 500")
    Integer count
) {
}