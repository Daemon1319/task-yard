package com.allan.task_yard.service;

import com.allan.task_yard.config.RabbitMQConfig;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobPublisherService {

  private static final Logger log = LoggerFactory.getLogger(JobPublisherService.class);

  private final RabbitTemplate rabbitTemplate;

  public JobPublisherService(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishForProcessing(UUID jobId) {
    log.debug("Publishing job {} to {}", jobId, RabbitMQConfig.JOB_QUEUE);
    rabbitTemplate.convertAndSend(RabbitMQConfig.JOB_EXCHANGE, RabbitMQConfig.JOB_ROUTING_KEY, jobId);
  }

  public void publishForRetry(UUID jobId, int attempt) {
    String routingKey = RabbitMQConfig.retryRoutingKey(attempt);
    log.debug("Routing job {} to retry attempt {} ({})", jobId, attempt, RabbitMQConfig.retryQueueName(attempt));
    rabbitTemplate.convertAndSend(RabbitMQConfig.RETRY_EXCHANGE, routingKey, jobId);
  }

  public void publishToDeadLetter(UUID jobId) {
    log.debug("Routing job {} to dead letter queue", jobId);
    rabbitTemplate.convertAndSend(RabbitMQConfig.DLQ_EXCHANGE, RabbitMQConfig.DLQ_ROUTING_KEY, jobId);
  }
}