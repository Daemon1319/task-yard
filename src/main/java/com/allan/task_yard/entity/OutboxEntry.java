package com.allan.task_yard.entity;

import com.allan.task_yard.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Tracks every message published (or attempted) to RabbitMQ.
 * <p>
 * Each row represents a single publish intent.  The lifecycle is
 * {@code PENDING → DISPATCHED → CONFIRMED} (happy path) or
 * {@code PENDING → FAILED} (publish error).  This makes the
 * normally-invisible outbox pattern fully visible to the frontend.
 */
@Entity
@Table(name = "outbox")
public class OutboxEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "job_id", nullable = false)
  private UUID jobId;

  @Column(nullable = false, length = 128)
  private String destination;

  @Column(nullable = false, length = 64)
  private String exchange;

  @Column(name = "routing_key", nullable = false, length = 64)
  private String routingKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private OutboxStatus status;

  @Column(columnDefinition = "text")
  private String payload;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "dispatched_at")
  private Instant dispatchedAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(columnDefinition = "text")
  private String error;

  protected OutboxEntry() {
  }

  public OutboxEntry(UUID jobId, String destination, String exchange, String routingKey, String payload) {
    this.jobId = jobId;
    this.destination = destination;
    this.exchange = exchange;
    this.routingKey = routingKey;
    this.payload = payload;
    this.status = OutboxStatus.PENDING;
  }

  // --- Lifecycle transitions ---

  public void markDispatched() {
    this.status = OutboxStatus.DISPATCHED;
    this.dispatchedAt = Instant.now();
  }

  public void markConfirmed() {
    this.status = OutboxStatus.CONFIRMED;
    this.confirmedAt = Instant.now();
  }

  public void markFailed(String reason) {
    this.status = OutboxStatus.FAILED;
    this.error = reason;
  }

  // --- Getters ---

  public UUID getId() {
    return id;
  }

  public UUID getJobId() {
    return jobId;
  }

  public String getDestination() {
    return destination;
  }

  public String getExchange() {
    return exchange;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getDispatchedAt() {
    return dispatchedAt;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public String getError() {
    return error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OutboxEntry that = (OutboxEntry) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "OutboxEntry{"
        + "id=" + id
        + ", jobId=" + jobId
        + ", destination='" + destination + '\''
        + ", status=" + status
        + ", createdAt=" + createdAt
        + ", dispatchedAt=" + dispatchedAt
        + ", confirmedAt=" + confirmedAt
        + '}';
  }
}
