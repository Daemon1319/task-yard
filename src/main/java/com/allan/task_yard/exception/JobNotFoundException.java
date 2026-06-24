package com.allan.task_yard.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

  public JobNotFoundException(UUID jobId) {
    super("Job not found: " + jobId);
  }
}