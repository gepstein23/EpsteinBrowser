package com.epstein.ingestion.metrics;

import com.epstein.ingestion.http.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class IngestionMetrics {

    private final MeterRegistry registry;
    private final Map<String, AtomicInteger> activeDownloads = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<Double>> runProgress = new ConcurrentHashMap<>();
    private final Timer s3UploadTimer;
    private final Timer rateLimitWaitTimer;
    private final Timer httpRequestTimer;

    public IngestionMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.s3UploadTimer = Timer.builder("ingestion.s3.upload.duration")
                .description("Time to upload documents to S3")
                .register(registry);
        this.rateLimitWaitTimer = Timer.builder("ingestion.rate.limit.wait")
                .description("Time spent waiting on rate limiter")
                .register(registry);
        this.httpRequestTimer = Timer.builder("ingestion.http.request.duration")
                .description("HTTP request duration")
                .register(registry);
    }

    public void recordDocumentDownloaded(String dataSet, String sourceType) {
        Counter.builder("ingestion.documents.downloaded")
                .tag("data_set", dataSet)
                .tag("source_type", sourceType)
                .register(registry)
                .increment();
    }

    public void recordDocumentFailed(String dataSet, String sourceType) {
        Counter.builder("ingestion.documents.failed")
                .tag("data_set", dataSet)
                .tag("source_type", sourceType)
                .register(registry)
                .increment();
    }

    public void recordDocumentSkippedDuplicate(String dataSet, String sourceType) {
        Counter.builder("ingestion.documents.skipped.duplicate")
                .tag("data_set", dataSet)
                .tag("source_type", sourceType)
                .register(registry)
                .increment();
    }

    public void recordBytesDownloaded(String dataSet, long bytes) {
        Counter.builder("ingestion.bytes.downloaded")
                .tag("data_set", dataSet)
                .register(registry)
                .increment(bytes);
    }

    public void incrementActiveDownloads(String dataSet) {
        activeDownloads.computeIfAbsent(dataSet, k -> {
            AtomicInteger gauge = new AtomicInteger(0);
            registry.gauge("ingestion.downloads.active", io.micrometer.core.instrument.Tags.of("data_set", k), gauge);
            return gauge;
        }).incrementAndGet();
    }

    public void decrementActiveDownloads(String dataSet) {
        AtomicInteger gauge = activeDownloads.get(dataSet);
        if (gauge != null) {
            gauge.decrementAndGet();
        }
    }

    public void recordRunProgress(String dataSet, double percent) {
        runProgress.computeIfAbsent(dataSet, k -> {
            AtomicReference<Double> ref = new AtomicReference<>(0.0);
            registry.gauge("ingestion.run.progress.percent",
                    io.micrometer.core.instrument.Tags.of("data_set", k), ref, AtomicReference::get);
            return ref;
        }).set(percent);
    }

    public void recordHttpRequest(int statusCode, long durationMs) {
        Counter.builder("ingestion.http.requests")
                .tag("status", String.valueOf(statusCode))
                .register(registry)
                .increment();
        httpRequestTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordS3Upload(long durationMs) {
        s3UploadTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRateLimitWait(long durationMs) {
        rateLimitWaitTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordCircuitBreakerState(CircuitBreaker.State state) {
        int stateValue = switch (state) {
            case CLOSED -> 0;
            case HALF_OPEN -> 1;
            case OPEN -> 2;
        };
        registry.gauge("ingestion.circuit.breaker.state", stateValue);
    }
}
