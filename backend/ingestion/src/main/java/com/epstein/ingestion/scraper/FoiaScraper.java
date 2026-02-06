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
public class FoiaScraper implements ScraperStrategy {

    private static final Logger log = LoggerFactory.getLogger(FoiaScraper.class);
    private static final int FOIA_RECORD_COUNT = 4;

    private final RateLimitedHttpClient httpClient;
    private final IngestionProperties properties;
    private final DeduplicationService deduplicationService;
    private final S3UploadService s3UploadService;
    private final DocumentRegistrationService documentRegistrationService;
    private final IngestionMetrics metrics;
    private final IngestionRunRepository runRepository;
    private final IngestionEventRepository eventRepository;

    public FoiaScraper(RateLimitedHttpClient httpClient,
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
        return "FOIA";
    }

    @Override
    public void scrape(IngestionRun run) {
        MDC.put("runId", String.valueOf(run.getId()));
        MDC.put("dataSet", run.getDataSet());

        try {
            List<String> foiaUrls = enumerateFoiaUrls();
            run.setTotalDiscovered(foiaUrls.size());
            runRepository.save(run);

            log.info("Enumerated {} FOIA record URLs", foiaUrls.size());

            for (String url : foiaUrls) {
                processFoiaRecord(url, run);
                updateProgress(run);
            }
        } finally {
            MDC.remove("runId");
            MDC.remove("dataSet");
        }
    }

    List<String> enumerateFoiaUrls() {
        List<String> urls = new ArrayList<>();
        String baseUrl = properties.getSources().getFoiaBaseUrl();

        for (int i = 1; i <= FOIA_RECORD_COUNT; i++) {
            urls.add(baseUrl + "/Epstein%20Records%20" + i + ".pdf");
        }

        return urls;
    }

    private void processFoiaRecord(String url, IngestionRun run) {
        String dataSet = run.getDataSet();
        MDC.put("sourceUrl", url);
        metrics.incrementActiveDownloads(dataSet);

        try {
            HttpResponse<byte[]> response = httpClient.getBytes(url);
            byte[] content = response.body();
            String fileHash = deduplicationService.computeHash(content);

            if (deduplicationService.isDuplicate(fileHash)) {
                log.info("Skipping duplicate FOIA record: {}", url);
                metrics.recordDocumentSkippedDuplicate(dataSet, getSourceType());
                run.incrementSkippedDuplicate();
                runRepository.save(run);
                eventRepository.save(new IngestionEvent(
                        run.getId(), "DOCUMENT_SKIPPED_DUPLICATE", url, "Hash: " + fileHash
                ));
                return;
            }

            String fileName = "Epstein_Records_" + url.charAt(url.lastIndexOf(' ') + 1) + ".pdf";
            // Better filename extraction
            fileName = DojDisclosureScraper.extractFileName(url);

            String s3Key = s3UploadService.upload(content, dataSet, fileName);

            documentRegistrationService.register(
                    url, fileHash, s3Key, dataSet, fileName, (long) content.length, run.getId()
            );

            metrics.recordDocumentDownloaded(dataSet, getSourceType());
            metrics.recordBytesDownloaded(dataSet, content.length);
            run.incrementDownloaded();
            runRepository.save(run);

            log.info("Downloaded FOIA record: {} -> {} ({} bytes)", url, s3Key, content.length);
        } catch (Exception e) {
            log.error("Failed to download FOIA record {}: {}", url, e.getMessage());
            metrics.recordDocumentFailed(dataSet, getSourceType());
            run.incrementFailed();
            runRepository.save(run);
            eventRepository.save(new IngestionEvent(
                    run.getId(), "DOCUMENT_FAILED", url, e.getMessage()
            ));
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
