package com.allan.task_yard.service;

import com.allan.task_yard.config.RabbitMQConfig;
import com.allan.task_yard.entity.OutboxEntry;
import com.allan.task_yard.enums.OutboxStatus;
import com.allan.task_yard.enums.PipelineEventType;
import com.allan.task_yard.repository.OutboxRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Publishes job messages to RabbitMQ <em>through the outbox</em>.
 * <p>
 * Implements the STRICT asynchronous outbox pattern:
 * 1. The web thread calls `publishForProcessing` which ONLY saves a PENDING entry.
 * 2. The `@Scheduled` worker thread polls PENDING entries and dispatches them.
 */
@Service
public class JobPublisherService {

  private static final Logger log = LoggerFactory.getLogger(JobPublisherService.class);

  private final RabbitTemplate rabbitTemplate;
  private final OutboxRepository outboxRepository;
  private final EventBus eventBus;

  public JobPublisherService(RabbitTemplate rabbitTemplate,
                             OutboxRepository outboxRepository,
                             EventBus eventBus) {
    this.rabbitTemplate = rabbitTemplate;
    this.outboxRepository = outboxRepository;
    this.eventBus = eventBus;
  }

  public void publishForProcessing(UUID jobId) {
    String destination = RabbitMQConfig.JOB_EXCHANGE + " / " + RabbitMQConfig.JOB_ROUTING_KEY;
    dispatchThroughOutbox(jobId, destination, RabbitMQConfig.JOB_EXCHANGE, RabbitMQConfig.JOB_ROUTING_KEY);
  }

  public void publishForRetry(UUID jobId, int attempt) {
    String routingKey = RabbitMQConfig.retryRoutingKey(attempt);
    String destination = RabbitMQConfig.RETRY_EXCHANGE + " / " + routingKey
        + " → " + RabbitMQConfig.retryQueueName(attempt);
    dispatchThroughOutbox(jobId, destination, RabbitMQConfig.RETRY_EXCHANGE, routingKey);
  }

  public void publishToDeadLetter(UUID jobId) {
    String destination = RabbitMQConfig.DLQ_EXCHANGE + " / " + RabbitMQConfig.DLQ_ROUTING_KEY;
    dispatchThroughOutbox(jobId, destination, RabbitMQConfig.DLQ_EXCHANGE, RabbitMQConfig.DLQ_ROUTING_KEY);
  }

  // ---- Record Intent (Runs in Web Thread's DB Transaction) ----

  private void dispatchThroughOutbox(UUID jobId, String destination, String exchange, String routingKey) {
    // 1. Create PENDING outbox entry ONLY. No RabbitMQ network call here!
    OutboxEntry entry = outboxRepository.save(new OutboxEntry(jobId, destination, exchange, routingKey, jobId.toString()));
    log.debug("Outbox entry {} created (PENDING) for job {} → {}", entry.getId(), jobId, destination);
    
    eventBus.emit(PipelineEventType.OUTBOX_PENDING, Map.of(
        "outboxId", entry.getId().toString(),
        "jobId", jobId.toString(),
        "destination", destination
    ));
  }

  // ---- Process Intent (Runs in Background Relay Thread) ----

  @Scheduled(fixedDelay = 500)
  public void processOutbox() {
    List<OutboxEntry> pendingEntries = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    if (pendingEntries.isEmpty()) return;

    for (OutboxEntry entry : pendingEntries) {
      try {
        // 2. Publish to RabbitMQ
        rabbitTemplate.convertAndSend(entry.getExchange(), entry.getRoutingKey(), entry.getJobId());

        // 3. Mark DISPATCHED
        entry.markDispatched();
        outboxRepository.save(entry);
        log.debug("Outbox entry {} dispatched", entry.getId());
        eventBus.emit(PipelineEventType.OUTBOX_DISPATCHED, Map.of(
            "outboxId", entry.getId().toString(),
            "jobId", entry.getJobId().toString()
        ));

        // 4. Mark CONFIRMED 
        entry.markConfirmed();
        outboxRepository.save(entry);
        log.debug("Outbox entry {} confirmed", entry.getId());
        eventBus.emit(PipelineEventType.OUTBOX_CONFIRMED, Map.of(
            "outboxId", entry.getId().toString(),
            "jobId", entry.getJobId().toString()
        ));

      } catch (Exception ex) {
        entry.markFailed(ex.getMessage());
        outboxRepository.save(entry);
        log.error("Outbox entry {} FAILED for job {}: {}", entry.getId(), entry.getJobId(), ex.getMessage());
        eventBus.emit(PipelineEventType.OUTBOX_FAILED, Map.of(
            "outboxId", entry.getId().toString(),
            "jobId", entry.getJobId().toString(),
            "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
        ));
        // We do NOT re-throw here. We catch it, mark it FAILED, and move to the next entry.
      }
    }
  }
}