package com.allan.task_yard.service;

import com.allan.task_yard.dto.PipelineEvent;
import com.allan.task_yard.enums.PipelineEventType;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory fan-out bus for pipeline events.
 * <p>
 * Services call {@link #emit(PipelineEventType, Map)} whenever something
 * interesting happens.  The {@code EventStreamController} registers
 * {@link SseEmitter} instances that receive every event in real time.
 */
@Component
public class EventBus {

  private static final Logger log = LoggerFactory.getLogger(EventBus.class);

  private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(0L); // no timeout
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(e -> emitters.remove(emitter));

    try {
      // Send an immediate dummy event to force the HTTP headers to flush.
      // Without this, the frontend EventSource stays in "Connecting" state
      // until the first actual job event is emitted.
      emitter.send(SseEmitter.event().name("ping").data("connected"));
    } catch (IOException e) {
      emitters.remove(emitter);
    }

    log.debug("SSE client connected ({} total)", emitters.size());
    return emitter;
  }

  public void emit(PipelineEventType type, Map<String, Object> data) {
    PipelineEvent event = PipelineEvent.of(type, data);

    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event()
            .name(type.name())
            .data(event));
      } catch (IOException | IllegalStateException ex) {
        emitters.remove(emitter);
        log.trace("Removed dead SSE emitter: {}", ex.getMessage());
      }
    }
  }
}
