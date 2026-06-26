package com.allan.task_yard.dto;

import com.allan.task_yard.enums.PipelineEventType;
import java.time.Instant;
import java.util.Map;

public record PipelineEvent(
    PipelineEventType type,
    Instant timestamp,
    Map<String, Object> data
) {

  public static PipelineEvent of(PipelineEventType type, Map<String, Object> data) {
    return new PipelineEvent(type, Instant.now(), data);
  }
}
