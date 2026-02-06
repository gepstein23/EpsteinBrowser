package com.epstein.ingestion.service;

import com.epstein.common.model.Document;
import com.epstein.common.model.IngestionEvent;
import com.epstein.common.model.ProcessingState;
import com.epstein.common.repository.DocumentRepository;
import com.epstein.common.repository.IngestionEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentRegistrationServiceTest {

    private DocumentRepository documentRepository;
    private IngestionEventRepository eventRepository;
    private DocumentRegistrationService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        eventRepository = mock(IngestionEventRepository.class);
        service = new DocumentRegistrationService(documentRepository, eventRepository);
    }

    @Test
    void registerSavesDocumentWithCorrectFields() {
        Document savedDoc = new Document("http://example.com/test.pdf", "abc123",
                "raw/ds/test.pdf", "data-set-1", "test.pdf", 1024L);
        savedDoc.setId(42L);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        when(eventRepository.save(any(IngestionEvent.class))).thenReturn(new IngestionEvent());

        Document result = service.register(
                "http://example.com/test.pdf", "abc123", "raw/ds/test.pdf",
                "data-set-1", "test.pdf", 1024L, 1L
        );

        assertEquals(42L, result.getId());

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        Document captured = docCaptor.getValue();
        assertEquals("http://example.com/test.pdf", captured.getSourceUrl());
        assertEquals("abc123", captured.getFileHash());
        assertEquals("raw/ds/test.pdf", captured.getS3RawKey());
        assertEquals("data-set-1", captured.getDataSet());
        assertEquals("test.pdf", captured.getFileName());
        assertEquals(1024L, captured.getFileSizeBytes());
        assertEquals(ProcessingState.INGESTED, captured.getProcessingState());
    }

    @Test
    void registerCreatesIngestionEvent() {
        Document savedDoc = new Document();
        savedDoc.setId(10L);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        when(eventRepository.save(any(IngestionEvent.class))).thenReturn(new IngestionEvent());

        service.register("http://example.com/doc.pdf", "hash", "raw/ds/doc.pdf",
                "data-set-1", "doc.pdf", 500L, 5L);

        ArgumentCaptor<IngestionEvent> eventCaptor = ArgumentCaptor.forClass(IngestionEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        IngestionEvent event = eventCaptor.getValue();
        assertEquals(5L, event.getRunId());
        assertEquals("DOCUMENT_DOWNLOADED", event.getEventType());
        assertEquals("http://example.com/doc.pdf", event.getSourceUrl());
        assertEquals(10L, event.getDocumentId());
    }
}
