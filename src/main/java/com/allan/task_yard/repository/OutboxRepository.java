package com.allan.task_yard.repository;

import com.allan.task_yard.entity.OutboxEntry;
import com.allan.task_yard.enums.OutboxStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEntry, UUID> {
  List<OutboxEntry> findTop50ByOrderByCreatedAtDesc();
  List<OutboxEntry> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
