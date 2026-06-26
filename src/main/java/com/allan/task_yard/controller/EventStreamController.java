package com.allan.task_yard.controller;

import com.allan.task_yard.service.EventBus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/queue")
public class EventStreamController {

  private final EventBus eventBus;

  public EventStreamController(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents() {
    return eventBus.subscribe();
  }
}
