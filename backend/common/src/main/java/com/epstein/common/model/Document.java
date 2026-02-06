package com.epstein.common.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "s3_raw_key", nullable = false, length = 512)
    private String s3RawKey;

    @Column(name = "data_set", nullable = false, length = 100)
    private String dataSet;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_state", nullable = false, length = 30)
    private ProcessingState processingState = ProcessingState.INGESTED;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Document() {}

    public Document(String sourceUrl, String fileHash, String s3RawKey, String dataSet,
                    String fileName, Long fileSizeBytes) {
        this.sourceUrl = sourceUrl;
        this.fileHash = fileHash;
        this.s3RawKey = s3RawKey;
        this.dataSet = dataSet;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.processingState = ProcessingState.INGESTED;
        this.ingestedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getS3RawKey() { return s3RawKey; }
    public void setS3RawKey(String s3RawKey) { this.s3RawKey = s3RawKey; }

    public String getDataSet() { return dataSet; }
    public void setDataSet(String dataSet) { this.dataSet = dataSet; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public ProcessingState getProcessingState() { return processingState; }
    public void setProcessingState(ProcessingState processingState) { this.processingState = processingState; }

    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
