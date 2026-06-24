package com.allan.task_yard.dto;

import com.allan.task_yard.entity.Job;
import com.allan.task_yard.enums.JobStatus;
import com.allan.task_yard.enums.JobType;
import java.time.Instant;
import java.util.UUID;
 
public record JobResponse(
    UUID id,
    JobType jobType,
    JobStatus status,
    String payload,
    int retryCount,
    int maxRetries,
    String lastError,
    Instant createdAt,
    Instant updatedAt,
    Instant nextRetryAt
) {

  public static JobResponse from(Job job) {
    return new JobResponse(
        job.getId(),
        job.getJobType(),
        job.getStatus(),
        job.getPayload(),
        job.getRetryCount(),
        job.getMaxRetries(),
        job.getLastError(),
        job.getCreatedAt(),
        job.getUpdatedAt(),
        job.getNextRetryAt()
    );
  }
}