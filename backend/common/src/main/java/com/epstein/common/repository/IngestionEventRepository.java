package com.epstein.common.repository;

import com.epstein.common.model.IngestionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionEventRepository extends JpaRepository<IngestionEvent, Long> {

    Page<IngestionEvent> findByRunIdOrderByTimestampDesc(Long runId, Pageable pageable);
}
