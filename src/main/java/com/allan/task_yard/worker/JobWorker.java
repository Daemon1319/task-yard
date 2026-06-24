package com.allan.task_yard.worker;

import com.allan.task_yard.config.RabbitMQConfig;
import com.allan.task_yard.config.RabbitMQProperties;
import com.allan.task_yard.entity.Job;
import com.allan.task_yard.enums.JobStatus;
import com.allan.task_yard.repository.JobRepository;
import com.allan.task_yard.service.ChaosState;
import com.allan.task_yard.service.JobPublisherService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class JobWorker {

  private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

  private final JobRepository jobRepository;
  private final JobPublisherService publisherService;
  private final ChaosState chaosState;
  private final RabbitMQProperties properties;

  public JobWorker(
      JobRepository jobRepository,
      JobPublisherService publisherService,
      ChaosState chaosState,
      RabbitMQProperties properties) {
    this.jobRepository = jobRepository;
    this.publisherService = publisherService;
    this.chaosState = chaosState;
    this.properties = properties;
  }

  @RabbitListener(queues = RabbitMQConfig.JOB_QUEUE)
  public void handleJob(UUID jobId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
    try {
      process(jobId);
    } catch (Exception ex) {

      log.error("Unrecoverable error handling job {} — could not record retry/DLQ state", jobId, ex);
    } finally {
      ackQuietly(channel, deliveryTag, jobId);
    }
  }

  private void process(UUID jobId) {
    Optional<Job> maybeJob = jobRepository.findById(jobId);
    if (maybeJob.isEmpty()) {
      log.warn("Job {} not found — skipping (likely a stale message from a reset)", jobId);
      return;
    }

    Job job = maybeJob.get();

    if (job.getStatus() == JobStatus.COMPLETED) {
      log.info("Job {} already COMPLETED — skipping duplicate delivery", jobId);
      return;
    }

    job.setStatus(JobStatus.PROCESSING);
    jobRepository.save(job);

    try {
      if (chaosState.shouldFail()) {
        throw new JobProcessingException("Simulated failure (chaos mode)");
      }

      job.setStatus(JobStatus.COMPLETED);
      job.setLastError(null);
      jobRepository.save(job);
      log.debug("Job {} completed", jobId);

    } catch (Exception ex) {
      String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
      handleFailure(job, reason);
    }
  }

  private void handleFailure(Job job, String reason) {
    int attempt = job.getRetryCount() + 1;
    job.setRetryCount(attempt);
    job.setLastError(reason);

    if (attempt <= job.getMaxRetries()) {
      job.setStatus(JobStatus.RETRYING);
      job.setNextRetryAt(Instant.now().plusMillis(delayForAttempt(attempt)));
      jobRepository.save(job);
      publisherService.publishForRetry(job.getId(), attempt);
      log.info("Job {} failed (attempt {}/{}) — retrying: {}", job.getId(), attempt, job.getMaxRetries(), reason);
    } else {
      job.setStatus(JobStatus.DEAD_LETTER);
      job.setNextRetryAt(null);
      jobRepository.save(job);
      publisherService.publishToDeadLetter(job.getId());
      log.warn("Job {} exhausted {} retries — moved to DEAD_LETTER: {}", job.getId(), job.getMaxRetries(), reason);
    }
  }

  private long delayForAttempt(int attempt) {
    List<Long> delays = properties.retryDelaysMs();
    int index = attempt - 1;
    return index < delays.size() ? delays.get(index) : delays.get(delays.size() - 1);
  }

  private void ackQuietly(Channel channel, long deliveryTag, UUID jobId) {
    try {
      channel.basicAck(deliveryTag, false);
    } catch (IOException ex) {
      log.error("Failed to ack message for job {}", jobId, ex);
    }
  }

  private static final class JobProcessingException extends RuntimeException {
    JobProcessingException(String message) {
      super(message);
    }
  }
}