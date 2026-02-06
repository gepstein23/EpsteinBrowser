package com.epstein.common.repository;

import com.epstein.common.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByFileHash(String fileHash);

    boolean existsByFileHash(String fileHash);

    List<Document> findByDataSet(String dataSet);

    long countByDataSet(String dataSet);

    long countByProcessingState(com.epstein.common.model.ProcessingState processingState);
}
