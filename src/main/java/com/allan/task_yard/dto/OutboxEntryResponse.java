package com.allan.task_yard.dto;

import com.allan.task_yard.entity.OutboxEntry;
import com.allan.task_yard.enums.OutboxStatus;
import java.time.Instant;
import java.util.UUID;

public record OutboxEntryResponse(
    UUID id,
    UUID jobId,
    String destination,
    OutboxStatus status,
    String payload,
    Instant createdAt,
    Instant dispatchedAt,
    Instant confirmedAt,
    String error
) {

  public static OutboxEntryResponse from(OutboxEntry entry) {
    return new OutboxEntryResponse(
        entry.getId(),
        entry.getJobId(),
        entry.getDestination(),
        entry.getStatus(),
        entry.getPayload(),
        entry.getCreatedAt(),
        entry.getDispatchedAt(),
        entry.getConfirmedAt(),
        entry.getError()
    );
  }
}
