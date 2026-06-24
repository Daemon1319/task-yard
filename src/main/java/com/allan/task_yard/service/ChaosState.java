package com.allan.task_yard.service;

import com.allan.task_yard.config.RabbitMQProperties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class ChaosState {

  public record State(boolean enabled, double failureRate) {
  }

  private final AtomicReference<State> state;

  public ChaosState(RabbitMQProperties properties) {
    RabbitMQProperties.ChaosProperties defaults = properties.chaos();
    this.state = new AtomicReference<>(new State(defaults.defaultEnabled(), defaults.defaultFailureRate()));
  }

  public State current() {
    return state.get();
  }

  public void update(boolean enabled, double failureRate) {
    state.set(new State(enabled, failureRate));
  }

  public boolean shouldFail() {
    State snapshot = state.get();
    return snapshot.enabled() && ThreadLocalRandom.current().nextDouble() < snapshot.failureRate();
  }
}