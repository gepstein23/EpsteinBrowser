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

import java.io.ByteArrayInputStream;
import java.net.http.HttpResponse;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipDownloadScraper implements ScraperStrategy {

    private static final Logger log = LoggerFactory.getLogger(ZipDownloadScraper.class);

    private final RateLimitedHttpClient httpClient;
    private final IngestionProperties properties;
    private final DeduplicationService deduplicationService;
    private final S3UploadService s3UploadService;
    private final DocumentRegistrationService documentRegistrationService;
    private final IngestionMetrics metrics;
    private final IngestionRunRepository runRepository;
    private final IngestionEventRepository eventRepository;

    public ZipDownloadScraper(RateLimitedHttpClient httpClient,
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
        return "ZIP_DOWNLOAD";
    }

    @Override
    public void scrape(IngestionRun run) {
        String dataSet = run.getDataSet();
        MDC.put("runId", String.valueOf(run.getId()));
        MDC.put("dataSet", dataSet);

        try {
            String zipUrl = buildZipUrl(dataSet);
            log.info("Downloading ZIP archive: {}", zipUrl);

            eventRepository.save(new IngestionEvent(
                    run.getId(), "DOCUMENT_DISCOVERED", zipUrl, "ZIP archive for " + dataSet
            ));

            HttpResponse<byte[]> response = httpClient.getBytes(zipUrl);
            byte[] zipContent = response.body();
            log.info("Downloaded ZIP: {} ({} bytes)", zipUrl, zipContent.length);

            processZip(zipContent, dataSet, zipUrl, run);
        } catch (Exception e) {
            log.error("Failed to download/process ZIP for {}: {}", dataSet, e.getMessage());
            metrics.recordDocumentFailed(dataSet, getSourceType());
            run.incrementFailed();
            runRepository.save(run);
            eventRepository.save(new IngestionEvent(
                    run.getId(), "DOCUMENT_FAILED", null,
                    "ZIP download failed: " + e.getMessage()
            ));
        } finally {
            MDC.remove("runId");
            MDC.remove("dataSet");
        }
    }

    void processZip(byte[] zipContent, String dataSet, String zipUrl, IngestionRun run) {
        int discovered = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipContent))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (!entryName.toLowerCase().endsWith(".pdf")) {
                    continue;
                }

                discovered++;
                String fileName = extractEntryFileName(entryName);
                String sourceUrl = zipUrl + "#" + entryName;

                metrics.incrementActiveDownloads(dataSet);
                try {
                    byte[] content = zis.readAllBytes();
                    String fileHash = deduplicationService.computeHash(content);

                    if (deduplicationService.isDuplicate(fileHash)) {
                        log.info("Skipping duplicate from ZIP: {}", entryName);
                        metrics.recordDocumentSkippedDuplicate(dataSet, getSourceType());
                        run.incrementSkippedDuplicate();
                        runRepository.save(run);
                        eventRepository.save(new IngestionEvent(
                                run.getId(), "DOCUMENT_SKIPPED_DUPLICATE", sourceUrl,
                                "Hash: " + fileHash
                        ));
                        continue;
                    }

                    String s3Key = s3UploadService.upload(content, dataSet, fileName);
                    documentRegistrationService.register(
                            sourceUrl, fileHash, s3Key, dataSet, fileName, (long) content.length, run.getId()
                    );

                    metrics.recordDocumentDownloaded(dataSet, getSourceType());
                    metrics.recordBytesDownloaded(dataSet, content.length);
                    run.incrementDownloaded();
                    runRepository.save(run);

                    log.info("Extracted from ZIP: {} -> {} ({} bytes)", entryName, s3Key, content.length);
                } catch (Exception e) {
                    log.error("Failed to process ZIP entry {}: {}", entryName, e.getMessage());
                    metrics.recordDocumentFailed(dataSet, getSourceType());
                    run.incrementFailed();
                    runRepository.save(run);
                    eventRepository.save(new IngestionEvent(
                            run.getId(), "DOCUMENT_FAILED", sourceUrl, e.getMessage()
                    ));
                } finally {
                    metrics.decrementActiveDownloads(dataSet);
                }
            }
        } catch (Exception e) {
            log.error("Error reading ZIP for {}: {}", dataSet, e.getMessage());
            throw new RuntimeException("Failed to process ZIP", e);
        }

        run.setTotalDiscovered(run.getTotalDiscovered() + discovered);
        runRepository.save(run);
    }

    String buildZipUrl(String dataSet) {
        int num = Integer.parseInt(dataSet.replaceAll("\\D+", ""));
        return properties.getSources().getDojDisclosuresBaseUrl()
                + "/data-set-" + num + ".zip";
    }

    static String extractEntryFileName(String entryName) {
        int lastSlash = entryName.lastIndexOf('/');
        return lastSlash >= 0 ? entryName.substring(lastSlash + 1) : entryName;
    }
}
