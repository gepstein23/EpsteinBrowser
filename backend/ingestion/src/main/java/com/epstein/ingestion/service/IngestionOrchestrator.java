package com.epstein.ingestion.service;

import com.epstein.common.model.IngestionEvent;
import com.epstein.common.model.IngestionRun;
import com.epstein.common.repository.IngestionEventRepository;
import com.epstein.common.repository.IngestionRunRepository;
import com.epstein.ingestion.metrics.IngestionMetrics;
import com.epstein.ingestion.scraper.ScraperStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(IngestionOrchestrator.class);

    // Data sets that support ZIP download
    private static final Set<String> ZIP_DATA_SETS = Set.of(
            "data-set-1", "data-set-2", "data-set-3", "data-set-4",
            "data-set-5", "data-set-6", "data-set-7", "data-set-8", "data-set-12"
    );

    // Data sets that require page-by-page scraping
    private static final Set<String> DISCLOSURE_DATA_SETS = Set.of(
            "data-set-9", "data-set-10", "data-set-11"
    );

    private final Map<String, ScraperStrategy> scrapers;
    private final IngestionRunRepository runRepository;
    private final IngestionEventRepository eventRepository;
    private final IngestionMetrics metrics;
    private final ExecutorService downloadExecutor;
    private final ConcurrentHashMap<Long, IngestionRun> activeRuns = new ConcurrentHashMap<>();

    public IngestionOrchestrator(List<ScraperStrategy> scraperList,
                                 IngestionRunRepository runRepository,
                                 IngestionEventRepository eventRepository,
                                 IngestionMetrics metrics,
                                 ExecutorService downloadExecutor) {
        this.scrapers = scraperList.stream()
                .collect(Collectors.toMap(ScraperStrategy::getSourceType, Function.identity()));
        this.runRepository = runRepository;
        this.eventRepository = eventRepository;
        this.metrics = metrics;
        this.downloadExecutor = downloadExecutor;
    }

    public IngestionRun startIngestion(String dataSet) {
        String sourceType = resolveSourceType(dataSet);
        ScraperStrategy scraper = scrapers.get(sourceType);
        if (scraper == null) {
            throw new IllegalArgumentException("No scraper found for source type: " + sourceType);
        }

        IngestionRun run = new IngestionRun(dataSet, sourceType);
        run = runRepository.save(run);
        activeRuns.put(run.getId(), run);

        eventRepository.save(new IngestionEvent(
                run.getId(), "INGESTION_RUN_STARTED", null,
                "Starting ingestion for " + dataSet + " using " + sourceType
        ));

        log.info("Starting ingestion run {} for dataSet={} sourceType={}", run.getId(), dataSet, sourceType);

        final IngestionRun savedRun = run;
        downloadExecutor.submit(() -> executeRun(savedRun, scraper));

        return run;
    }

    public List<IngestionRun> startAllIngestion() {
        List<String> allDataSets = new java.util.ArrayList<>();
        allDataSets.addAll(ZIP_DATA_SETS);
        allDataSets.addAll(DISCLOSURE_DATA_SETS);
        allDataSets.add("court-records");
        allDataSets.add("foia");
        allDataSets.sort(String::compareTo);

        return allDataSets.stream()
                .map(this::startIngestion)
                .collect(Collectors.toList());
    }

    private void executeRun(IngestionRun run, ScraperStrategy scraper) {
        MDC.put("runId", String.valueOf(run.getId()));
        MDC.put("dataSet", run.getDataSet());

        Instant start = Instant.now();
        try {
            scraper.scrape(run);
            run.setStatus("COMPLETED");
            log.info("Ingestion run {} completed: downloaded={}, failed={}, skipped={}",
                    run.getId(), run.getDownloaded(), run.getFailed(), run.getSkippedDuplicate());
        } catch (Exception e) {
            run.setStatus("FAILED");
            log.error("Ingestion run {} failed: {}", run.getId(), e.getMessage(), e);
            eventRepository.save(new IngestionEvent(
                    run.getId(), "INGESTION_RUN_FAILED", null, e.getMessage()
            ));
        } finally {
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            activeRuns.remove(run.getId());

            Duration duration = Duration.between(start, Instant.now());
            eventRepository.save(new IngestionEvent(
                    run.getId(), "INGESTION_RUN_COMPLETED", null,
                    String.format("Duration: %s, Downloaded: %d, Failed: %d, Skipped: %d",
                            duration, run.getDownloaded(), run.getFailed(), run.getSkippedDuplicate())
            ));

            metrics.recordRunProgress(run.getDataSet(), 100.0);
            MDC.remove("runId");
            MDC.remove("dataSet");
        }
    }

    String resolveSourceType(String dataSet) {
        if (ZIP_DATA_SETS.contains(dataSet)) {
            return "ZIP_DOWNLOAD";
        }
        if (DISCLOSURE_DATA_SETS.contains(dataSet)) {
            return "DOJ_DISCLOSURES";
        }
        if ("court-records".equals(dataSet)) {
            return "COURT_RECORDS";
        }
        if ("foia".equals(dataSet)) {
            return "FOIA";
        }
        throw new IllegalArgumentException("Unknown data set: " + dataSet);
    }

    public Map<Long, IngestionRun> getActiveRuns() {
        return Map.copyOf(activeRuns);
    }
}
