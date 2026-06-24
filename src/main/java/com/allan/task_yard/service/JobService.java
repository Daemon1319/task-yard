package com.allan.task_yard.service;

import com.allan.task_yard.config.RabbitMQConfig;
import com.allan.task_yard.config.RabbitMQProperties;
import com.allan.task_yard.dto.ChaosRequest;
import com.allan.task_yard.dto.CreateJobRequest;
import com.allan.task_yard.dto.FloodRequest;
import com.allan.task_yard.dto.JobResponse;
import com.allan.task_yard.entity.Job;
import com.allan.task_yard.enums.JobStatus;
import com.allan.task_yard.enums.JobType;
import com.allan.task_yard.exception.JobNotFoundException;
import com.allan.task_yard.repository.JobRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class JobService {

  private static final Logger log = LoggerFactory.getLogger(JobService.class);

  private static final int MAX_LIMIT = 200;

  private final JobRepository jobRepository;
  private final JobPublisherService publisherService;
  private final ChaosState chaosState;
  private final AmqpAdmin amqpAdmin;
  private final RabbitMQProperties properties;

  public JobService(
      JobRepository jobRepository,
      JobPublisherService publisherService,
      ChaosState chaosState,
      AmqpAdmin amqpAdmin,
      RabbitMQProperties properties) {
    this.jobRepository = jobRepository;
    this.publisherService = publisherService;
    this.chaosState = chaosState;
    this.amqpAdmin = amqpAdmin;
    this.properties = properties;
  }

  public JobResponse createJob(CreateJobRequest request) {
    Job job = jobRepository.save(new Job(request.jobType(), properties.maxRetries()));
    publisherService.publishForProcessing(job.getId());
    log.info("Created job {} ({})", job.getId(), job.getJobType());
    return JobResponse.from(job);
  }

  public List<JobResponse> floodJobs(FloodRequest request) {
    JobType[] types = JobType.values();
    List<JobResponse> created = new ArrayList<>(request.count());

    for (int i = 0; i < request.count(); i++) {
      JobType type = types[ThreadLocalRandom.current().nextInt(types.length)];
      Job job = jobRepository.save(new Job(type, properties.maxRetries()));
      publisherService.publishForProcessing(job.getId());
      created.add(JobResponse.from(job));
    }

    log.info("Flooded {} jobs", request.count());
    return created;
  }

  public List<JobResponse> listJobs(JobStatus status, int limit) {
    int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
    Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
    List<Job> jobs = status != null
        ? jobRepository.findByStatus(status, pageable)
        : jobRepository.findAll(pageable).getContent();
    return jobs.stream().map(JobResponse::from).toList();
  }

  public List<JobResponse> listDeadLetterJobs(int limit) {
    return listJobs(JobStatus.DEAD_LETTER, limit);
  }

  public Map<JobStatus, Long> getStats() {
    Map<JobStatus, Long> stats = new EnumMap<>(JobStatus.class);
    for (JobStatus status : JobStatus.values()) {
      stats.put(status, 0L);
    }
    for (JobRepository.JobStatusCount row : jobRepository.countByStatus()) {
      stats.put(row.getStatus(), row.getCount());
    }
    return stats;
  }

  public JobResponse retryDeadLetterJob(UUID jobId) {
    Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));

    if (job.getStatus() != JobStatus.DEAD_LETTER) {
      throw new IllegalStateException(
          "Job " + jobId + " is not in DEAD_LETTER status (currently " + job.getStatus()
              + ") and cannot be manually retried");
    }

    job.setStatus(JobStatus.QUEUED);
    job.setRetryCount(0);
    job.setNextRetryAt(null);
    job = jobRepository.save(job);

    publisherService.publishForProcessing(job.getId());
    log.info("Manually retrying dead-lettered job {}", jobId);
    return JobResponse.from(job);
  }

  public ChaosState.State getChaosState() {
    return chaosState.current();
  }

  public ChaosState.State updateChaos(ChaosRequest request) {
    chaosState.update(request.enabled(), request.failureRate());
    log.info("Chaos mode updated: enabled={}, failureRate={}", request.enabled(), request.failureRate());
    return chaosState.current();
  }

  public void reset() {
    jobRepository.deleteAllInBatch();

    amqpAdmin.purgeQueue(RabbitMQConfig.JOB_QUEUE, false);
    amqpAdmin.purgeQueue(RabbitMQConfig.DLQ_QUEUE, false);

    int retryQueueCount = properties.retryDelaysMs().size();
    for (int attempt = 1; attempt <= retryQueueCount; attempt++) {
      amqpAdmin.purgeQueue(RabbitMQConfig.retryQueueName(attempt), false);
    }

    log.info("Reset: cleared all jobs and purged {} queues", retryQueueCount + 2);
  }
}