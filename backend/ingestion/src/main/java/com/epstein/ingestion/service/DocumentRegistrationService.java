package com.epstein.ingestion.service;

import com.epstein.common.model.Document;
import com.epstein.common.model.IngestionEvent;
import com.epstein.common.repository.DocumentRepository;
import com.epstein.common.repository.IngestionEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentRegistrationService.class);

    private final DocumentRepository documentRepository;
    private final IngestionEventRepository eventRepository;

    public DocumentRegistrationService(DocumentRepository documentRepository,
                                       IngestionEventRepository eventRepository) {
        this.documentRepository = documentRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public Document register(String sourceUrl, String fileHash, String s3RawKey,
                             String dataSet, String fileName, long fileSizeBytes, Long runId) {
        Document document = new Document(sourceUrl, fileHash, s3RawKey, dataSet, fileName, fileSizeBytes);
        document = documentRepository.save(document);

        IngestionEvent event = new IngestionEvent(
                runId, "DOCUMENT_DOWNLOADED", sourceUrl,
                String.format("Downloaded %s (%d bytes) -> %s", fileName, fileSizeBytes, s3RawKey)
        );
        event.setDocumentId(document.getId());
        eventRepository.save(event);

        log.info("Registered document: id={}, dataSet={}, fileName={}, hash={}",
                document.getId(), dataSet, fileName, fileHash);
        return document;
    }
}
