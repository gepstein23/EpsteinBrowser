package com.epstein.ingestion.metrics;

import com.epstein.ingestion.http.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngestionMetricsTest {

    private MeterRegistry registry;
    private IngestionMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new IngestionMetrics(registry);
    }

    @Test
    void recordDocumentDownloadedIncrementsCounter() {
        metrics.recordDocumentDownloaded("data-set-1", "ZIP_DOWNLOAD");
        metrics.recordDocumentDownloaded("data-set-1", "ZIP_DOWNLOAD");

        Counter counter = registry.find("ingestion.documents.downloaded")
                .tag("data_set", "data-set-1")
                .tag("source_type", "ZIP_DOWNLOAD")
                .counter();

        assertNotNull(counter);
        assertEquals(2.0, counter.count());
    }

    @Test
    void recordDocumentFailedIncrementsCounter() {
        metrics.recordDocumentFailed("data-set-5", "DOJ_DISCLOSURES");

        Counter counter = registry.find("ingestion.documents.failed")
                .tag("data_set", "data-set-5")
                .tag("source_type", "DOJ_DISCLOSURES")
                .counter();

        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordDocumentSkippedDuplicateIncrementsCounter() {
        metrics.recordDocumentSkippedDuplicate("data-set-1", "ZIP_DOWNLOAD");

        Counter counter = registry.find("ingestion.documents.skipped.duplicate")
                .tag("data_set", "data-set-1")
                .counter();

        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordBytesDownloadedIncrementsCorrectly() {
        metrics.recordBytesDownloaded("data-set-1", 1024);
        metrics.recordBytesDownloaded("data-set-1", 2048);

        Counter counter = registry.find("ingestion.bytes.downloaded")
                .tag("data_set", "data-set-1")
                .counter();

        assertNotNull(counter);
        assertEquals(3072.0, counter.count());
    }

    @Test
    void recordHttpRequestIncrementsCounterAndTimer() {
        metrics.recordHttpRequest(200, 150);

        Counter counter = registry.find("ingestion.http.requests")
                .tag("status", "200")
                .counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());

        Timer timer = registry.find("ingestion.http.request.duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordS3UploadRecordsTimer() {
        metrics.recordS3Upload(200);
        metrics.recordS3Upload(300);

        Timer timer = registry.find("ingestion.s3.upload.duration").timer();
        assertNotNull(timer);
        assertEquals(2, timer.count());
    }

    @Test
    void recordRateLimitWaitRecordsTimer() {
        metrics.recordRateLimitWait(50);

        Timer timer = registry.find("ingestion.rate.limit.wait").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void activeDownloadsGaugeTracksCorrectly() {
        metrics.incrementActiveDownloads("data-set-1");
        metrics.incrementActiveDownloads("data-set-1");
        metrics.decrementActiveDownloads("data-set-1");

        // The gauge should have been registered
        assertNotNull(registry.find("ingestion.downloads.active")
                .tag("data_set", "data-set-1")
                .gauge());
    }

    @Test
    void runProgressGaugeRecords() {
        metrics.recordRunProgress("data-set-1", 75.5);

        assertNotNull(registry.find("ingestion.run.progress.percent")
                .tag("data_set", "data-set-1")
                .gauge());
    }

    @Test
    void circuitBreakerStateRecords() {
        metrics.recordCircuitBreakerState(CircuitBreaker.State.CLOSED);
        metrics.recordCircuitBreakerState(CircuitBreaker.State.OPEN);
        // Should not throw
    }

    @Test
    void differentDataSetsTrackedSeparately() {
        metrics.recordDocumentDownloaded("data-set-1", "ZIP_DOWNLOAD");
        metrics.recordDocumentDownloaded("data-set-2", "ZIP_DOWNLOAD");

        Counter counter1 = registry.find("ingestion.documents.downloaded")
                .tag("data_set", "data-set-1")
                .counter();
        Counter counter2 = registry.find("ingestion.documents.downloaded")
                .tag("data_set", "data-set-2")
                .counter();

        assertNotNull(counter1);
        assertNotNull(counter2);
        assertEquals(1.0, counter1.count());
        assertEquals(1.0, counter2.count());
    }
}
