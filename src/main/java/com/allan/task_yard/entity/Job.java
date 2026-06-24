package com.allan.task_yard.entity;

import com.allan.task_yard.enums.JobStatus;
import com.allan.task_yard.enums.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * A unit of background work tracked end-to-end in Postgres.
 * <p>
 * The message published to RabbitMQ carries only the job's {@code id} —
 * this entity is the single source of truth for everything else
 * ("thin message, fat entity"). The frontend polls the {@code jobs}
 * table to render the live board.
 */
@Entity
@Table(name = "jobs")
public class Job {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_type", nullable = false, length = 32)
  private JobType jobType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private JobStatus status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount = 0;

  @Column(name = "max_retries", nullable = false)
  private int maxRetries;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  protected Job() {
  }

  public Job(JobType jobType, int maxRetries) {
    this.jobType = Objects.requireNonNull(jobType, "jobType must not be null");
    this.status = JobStatus.QUEUED;
    this.maxRetries = maxRetries;
  }

  public UUID getId() {
    return id;
  }

  public JobType getJobType() {
    return jobType;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public JobStatus getStatus() {
    return status;
  }

  public void setStatus(JobStatus status) {
    this.status = status;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getNextRetryAt() {
    return nextRetryAt;
  }

  public void setNextRetryAt(Instant nextRetryAt) {
    this.nextRetryAt = nextRetryAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Job job = (Job) o;
    return id != null && id.equals(job.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Job{"
        + "id=" + id
        + ", jobType=" + jobType
        + ", status=" + status
        + ", retryCount=" + retryCount
        + ", maxRetries=" + maxRetries
        + ", createdAt=" + createdAt
        + ", updatedAt=" + updatedAt
        + ", nextRetryAt=" + nextRetryAt
        + '}';
  }
}