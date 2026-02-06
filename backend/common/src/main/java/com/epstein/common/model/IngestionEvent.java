package com.epstein.common.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ingestion_events")
public class IngestionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "message")
    private String message;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();

    public IngestionEvent() {}

    public IngestionEvent(Long runId, String eventType, String sourceUrl, String message) {
        this.runId = runId;
        this.eventType = eventType;
        this.sourceUrl = sourceUrl;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
