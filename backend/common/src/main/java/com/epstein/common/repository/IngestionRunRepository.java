package com.epstein.common.repository;

import com.epstein.common.model.IngestionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngestionRunRepository extends JpaRepository<IngestionRun, Long> {

    List<IngestionRun> findByDataSet(String dataSet);

    List<IngestionRun> findByStatus(String status);

    List<IngestionRun> findAllByOrderByStartedAtDesc();
}
