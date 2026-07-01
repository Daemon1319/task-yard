package com.allan.task_yard.service;

import com.allan.task_yard.dto.PipelineEvent;
import com.allan.task_yard.enums.PipelineEventType;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class EventBus {

  private static final Logger log = LoggerFactory.getLogger(EventBus.class);

  // How long an emitter can go without any event before Spring itself
  // times it out. Kept finite (rather than 0L/infinite) so timeouts are
  // handled cleanly by our own onTimeout callback instead of relying on
  // the platform to silently kill the socket.
  private static final long EMITTER_TIMEOUT_MS = 60_000L;

  // How often to ping every connected client to keep the connection
  // alive at the infrastructure level.
  private static final long HEARTBEAT_INTERVAL_MS = 20_000L;

  private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
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

  /**
   * Periodic keep-alive so idle connections stay open at the
   * infrastructure level, and so dead emitters get cleaned up
   * proactively instead of waiting for a real event to reveal them.
   */
  @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
  public void heartbeat() {
    if (emitters.isEmpty()) {
      return;
    }
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().comment("keep-alive"));
      } catch (IOException | IllegalStateException e) {
        emitters.remove(emitter);
        log.trace("Removed dead SSE emitter during heartbeat: {}", e.getMessage());
      }
    }
  }
}