package com.epstein.ingestion.scraper;

import com.epstein.common.model.IngestionEvent;
import com.epstein.common.model.IngestionRun;
import com.epstein.common.repository.IngestionEventRepository;
import com.epstein.common.repository.IngestionRunRepository;
import com.epstein.ingestion.config.IngestionProperties;
import com.epstein.ingestion.http.RateLimitedHttpClient;
import com.epstein.ingestion.metrics.IngestionMetrics;
import com.epstein.ingestion.service.DeduplicationService;
import com.epstein.ingestion.service.DocumentRegistrationService;
import com.epstein.ingestion.service.S3UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CourtRecordsScraper implements ScraperStrategy {

    private static final Logger log = LoggerFactory.getLogger(CourtRecordsScraper.class);
    private static final int MAX_DOCKET_NUMBER = 1334;

    private final RateLimitedHttpClient httpClient;
    private final IngestionProperties properties;
    private final DeduplicationService deduplicationService;
    private final S3UploadService s3UploadService;
    private final DocumentRegistrationService documentRegistrationService;
    private final IngestionMetrics metrics;
    private final IngestionRunRepository runRepository;
    private final IngestionEventRepository eventRepository;

    public CourtRecordsScraper(RateLimitedHttpClient httpClient,
                               IngestionProperties properties,
                               DeduplicationService deduplicationService,
                               S3UploadService s3UploadService,
                               DocumentRegistrationService documentRegistrationService,
                               IngestionMetrics metrics,
                               IngestionRunRepository runRepository,
                               IngestionEventRepository eventRepository) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.deduplicationService = deduplicationService;
        this.s3UploadService = s3UploadService;
        this.documentRegistrationService = documentRegistrationService;
        this.metrics = metrics;
        this.runRepository = runRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public String getSourceType() {
        return "COURT_RECORDS";
    }

    @Override
    public void scrape(IngestionRun run) {
        MDC.put("runId", String.valueOf(run.getId()));
        MDC.put("dataSet", run.getDataSet());

        try {
            List<String> docketUrls = enumerateDocketUrls();
            run.setTotalDiscovered(docketUrls.size());
            runRepository.save(run);

            log.info("Enumerated {} docket URLs", docketUrls.size());

            for (String url : docketUrls) {
                processDocket(url, run);
                updateProgress(run);
            }
        } finally {
            MDC.remove("runId");
            MDC.remove("dataSet");
        }
    }

    List<String> enumerateDocketUrls() {
        List<String> urls = new ArrayList<>();
        String baseUrl = properties.getSources().getCourtRecordsBaseUrl();

        for (int i = 1; i <= MAX_DOCKET_NUMBER; i++) {
            // Main docket PDF: 001.pdf through 1334.pdf
            urls.add(baseUrl + "/" + String.format("%03d", i) + ".pdf");
        }

        // Some dockets have sub-parts like 1334-1.pdf
        for (int sub = 1; sub <= 5; sub++) {
            urls.add(baseUrl + "/" + MAX_DOCKET_NUMBER + "-" + sub + ".pdf");
        }

        return urls;
    }

    private void processDocket(String url, IngestionRun run) {
        String dataSet = run.getDataSet();
        MDC.put("sourceUrl", url);
        metrics.incrementActiveDownloads(dataSet);

        try {
            HttpResponse<byte[]> response = httpClient.getBytes(url);
            byte[] content = response.body();
            String fileHash = deduplicationService.computeHash(content);

            if (deduplicationService.isDuplicate(fileHash)) {
                log.info("Skipping duplicate docket: {}", url);
                metrics.recordDocumentSkippedDuplicate(dataSet, getSourceType());
                run.incrementSkippedDuplicate();
                runRepository.save(run);
                eventRepository.save(new IngestionEvent(
                        run.getId(), "DOCUMENT_SKIPPED_DUPLICATE", url, "Hash: " + fileHash
                ));
                return;
            }

            String fileName = DojDisclosureScraper.extractFileName(url);
            String s3Key = s3UploadService.upload(content, dataSet, fileName);

            documentRegistrationService.register(
                    url, fileHash, s3Key, dataSet, fileName, (long) content.length, run.getId()
            );

            metrics.recordDocumentDownloaded(dataSet, getSourceType());
            metrics.recordBytesDownloaded(dataSet, content.length);
            run.incrementDownloaded();
            runRepository.save(run);
        } catch (Exception e) {
            // 404s are expected for non-existent docket numbers
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.debug("Docket not found (expected): {}", url);
            } else {
                log.warn("Failed to download docket {}: {}", url, e.getMessage());
                metrics.recordDocumentFailed(dataSet, getSourceType());
                run.incrementFailed();
                runRepository.save(run);
                eventRepository.save(new IngestionEvent(
                        run.getId(), "DOCUMENT_FAILED", url, e.getMessage()
                ));
            }
        } finally {
            metrics.decrementActiveDownloads(dataSet);
            MDC.remove("sourceUrl");
        }
    }

    private void updateProgress(IngestionRun run) {
        if (run.getTotalDiscovered() > 0) {
            int processed = run.getDownloaded() + run.getFailed() + run.getSkippedDuplicate();
            double pct = (double) processed / run.getTotalDiscovered() * 100.0;
            metrics.recordRunProgress(run.getDataSet(), pct);
        }
    }
}
