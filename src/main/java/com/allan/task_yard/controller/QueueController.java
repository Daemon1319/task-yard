package com.allan.task_yard.controller;

import com.allan.task_yard.dto.ChaosRequest;
import com.allan.task_yard.dto.CreateJobRequest;
import com.allan.task_yard.dto.FloodRequest;
import com.allan.task_yard.dto.JobResponse;
import com.allan.task_yard.dto.OutboxEntryResponse;
import com.allan.task_yard.enums.JobStatus;
import com.allan.task_yard.service.ChaosState;
import com.allan.task_yard.service.JobService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

  private static final int DEFAULT_LIMIT = 50;

  private final JobService jobService;

  public QueueController(JobService jobService) {
    this.jobService = jobService;
  }

  @PostMapping("/jobs")
  public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
  }

  @PostMapping("/jobs/flood")
  public ResponseEntity<List<JobResponse>> floodJobs(@Valid @RequestBody FloodRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(jobService.floodJobs(request));
  }

  @GetMapping("/jobs")
  public ResponseEntity<List<JobResponse>> listJobs(
      @RequestParam(required = false) JobStatus status,
      @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
    return ResponseEntity.ok(jobService.listJobs(status, limit));
  }

  @GetMapping("/jobs/dead-letter")
  public ResponseEntity<List<JobResponse>> listDeadLetterJobs(
      @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
    return ResponseEntity.ok(jobService.listDeadLetterJobs(limit));
  }

  @PostMapping("/jobs/{id}/retry")
  public ResponseEntity<JobResponse> retryJob(@PathVariable("id") UUID jobId) {
    return ResponseEntity.ok(jobService.retryDeadLetterJob(jobId));
  }

  @GetMapping("/stats")
  public ResponseEntity<Map<JobStatus, Long>> getStats() {
    return ResponseEntity.ok(jobService.getStats());
  }

  @GetMapping("/outbox")
  public ResponseEntity<List<OutboxEntryResponse>> getOutboxEntries() {
    return ResponseEntity.ok(jobService.getOutboxEntries());
  }

  @GetMapping("/chaos")
  public ResponseEntity<ChaosState.State> getChaosState() {
    return ResponseEntity.ok(jobService.getChaosState());
  }

  @PostMapping("/chaos")
  public ResponseEntity<ChaosState.State> updateChaos(@Valid @RequestBody ChaosRequest request) {
    return ResponseEntity.ok(jobService.updateChaos(request));
  }

  @PostMapping("/reset")
  public ResponseEntity<Void> reset() {
    jobService.reset();
    return ResponseEntity.noContent().build();
  }
}