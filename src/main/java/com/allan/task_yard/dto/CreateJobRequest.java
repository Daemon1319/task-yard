package com.allan.task_yard.dto;

import com.allan.task_yard.enums.JobType;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequest(
    @NotNull(message = "jobType is required") JobType jobType
) {
}