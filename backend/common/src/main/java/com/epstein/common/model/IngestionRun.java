package com.epstein.common.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ingestion_runs")
public class IngestionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_set", nullable = false, length = 100)
    private String dataSet;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "RUNNING";

    @Column(name = "total_discovered", nullable = false)
    private int totalDiscovered;

    @Column(name = "downloaded", nullable = false)
    private int downloaded;

    @Column(name = "failed", nullable = false)
    private int failed;

    @Column(name = "skipped_duplicate", nullable = false)
    private int skippedDuplicate;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public IngestionRun() {}

    public IngestionRun(String dataSet, String sourceType) {
        this.dataSet = dataSet;
        this.sourceType = sourceType;
        this.status = "RUNNING";
        this.startedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDataSet() { return dataSet; }
    public void setDataSet(String dataSet) { this.dataSet = dataSet; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalDiscovered() { return totalDiscovered; }
    public void setTotalDiscovered(int totalDiscovered) { this.totalDiscovered = totalDiscovered; }

    public int getDownloaded() { return downloaded; }
    public void setDownloaded(int downloaded) { this.downloaded = downloaded; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public int getSkippedDuplicate() { return skippedDuplicate; }
    public void setSkippedDuplicate(int skippedDuplicate) { this.skippedDuplicate = skippedDuplicate; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public void incrementDownloaded() { this.downloaded++; }
    public void incrementFailed() { this.failed++; }
    public void incrementSkippedDuplicate() { this.skippedDuplicate++; }
}
