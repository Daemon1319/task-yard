package com.allan.task_yard.repository;

import com.allan.task_yard.entity.Job;
import com.allan.task_yard.enums.JobStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface JobRepository extends JpaRepository<Job, UUID> {
  List<Job> findByStatus(JobStatus status, Pageable pageable);

  @Query("select j.status as status, count(j) as count from Job j group by j.status")
  List<JobStatusCount> countByStatus();

  interface JobStatusCount {
    JobStatus getStatus();

    long getCount();
  }
}