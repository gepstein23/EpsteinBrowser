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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class DojDisclosureScraper implements ScraperStrategy {

    private static final Logger log = LoggerFactory.getLogger(DojDisclosureScraper.class);

    private final RateLimitedHttpClient httpClient;
    private final IngestionProperties properties;
    private final DeduplicationService deduplicationService;
    private final S3UploadService s3UploadService;
    private final DocumentRegistrationService documentRegistrationService;
    private final IngestionMetrics metrics;
    private final IngestionRunRepository runRepository;
    private final IngestionEventRepository eventRepository;

    public DojDisclosureScraper(RateLimitedHttpClient httpClient,
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
        return "DOJ_DISCLOSURES";
    }

    @Override
    public void scrape(IngestionRun run) {
        String dataSet = run.getDataSet();
        MDC.put("runId", String.valueOf(run.getId()));
        MDC.put("dataSet", dataSet);

        try {
            int dataSetNumber = extractDataSetNumber(dataSet);
            List<String> pdfUrls = discoverPdfUrls(dataSetNumber, run);

            run.setTotalDiscovered(pdfUrls.size());
            runRepository.save(run);
            log.info("Discovered {} PDF URLs for data set {}", pdfUrls.size(), dataSet);

            for (String pdfUrl : pdfUrls) {
                processPdf(pdfUrl, dataSet, run);
                updateProgress(run);
            }
        } finally {
            MDC.remove("runId");
            MDC.remove("dataSet");
        }
    }

    List<String> discoverPdfUrls(int dataSetNumber, IngestionRun run) {
        List<String> allUrls = new ArrayList<>();
        String baseUrl = properties.getSources().getDojDisclosuresBaseUrl();
        int page = 0;

        while (true) {
            String pageUrl = baseUrl + "/data-set-" + dataSetNumber + "-files?page=" + page;
            log.info("Fetching listing page: {}", pageUrl);

            try {
                HttpResponse<String> response = httpClient.getString(pageUrl);
                List<String> urls = parsePdfLinks(response.body(), pageUrl);

                if (urls.isEmpty()) {
                    break;
                }

                allUrls.addAll(urls);
                for (String url : urls) {
                    eventRepository.save(new IngestionEvent(
                            run.getId(), "DOCUMENT_DISCOVERED", url,
                            "Discovered on page " + page
                    ));
                }

                page++;
            } catch (Exception e) {
                log.error("Failed to fetch listing page {}: {}", pageUrl, e.getMessage());
                break;
            }
        }

        return allUrls;
    }

    List<String> parsePdfLinks(String html, String pageUrl) {
        List<String> urls = new ArrayList<>();
        var doc = Jsoup.parse(html, pageUrl);

        for (Element link : doc.select("a[href]")) {
            String href = link.absUrl("href");
            if (href.toLowerCase().endsWith(".pdf")) {
                urls.add(href);
            }
        }

        return urls;
    }

    private void processPdf(String pdfUrl, String dataSet, IngestionRun run) {
        MDC.put("sourceUrl", pdfUrl);
        metrics.incrementActiveDownloads(dataSet);

        try {
            HttpResponse<byte[]> response = httpClient.getBytes(pdfUrl);
            byte[] content = response.body();
            String fileHash = deduplicationService.computeHash(content);

            if (deduplicationService.isDuplicate(fileHash)) {
                log.info("Skipping duplicate: {}", pdfUrl);
                metrics.recordDocumentSkippedDuplicate(dataSet, getSourceType());
                run.incrementSkippedDuplicate();
                runRepository.save(run);
                eventRepository.save(new IngestionEvent(
                        run.getId(), "DOCUMENT_SKIPPED_DUPLICATE", pdfUrl,
                        "Hash: " + fileHash
                ));
                return;
            }

            String fileName = extractFileName(pdfUrl);
            String s3Key = s3UploadService.upload(content, dataSet, fileName);

            documentRegistrationService.register(
                    pdfUrl, fileHash, s3Key, dataSet, fileName, (long) content.length, run.getId()
            );

            metrics.recordDocumentDownloaded(dataSet, getSourceType());
            metrics.recordBytesDownloaded(dataSet, content.length);
            run.incrementDownloaded();
            runRepository.save(run);

            log.info("Downloaded: {} -> {} ({} bytes)", pdfUrl, s3Key, content.length);
        } catch (Exception e) {
            log.error("Failed to download {}: {}", pdfUrl, e.getMessage());
            metrics.recordDocumentFailed(dataSet, getSourceType());
            run.incrementFailed();
            runRepository.save(run);
            eventRepository.save(new IngestionEvent(
                    run.getId(), "DOCUMENT_FAILED", pdfUrl, e.getMessage()
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

    static String extractFileName(String url) {
        String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private int extractDataSetNumber(String dataSet) {
        // dataSet format: "data-set-1", "data-set-12", etc.
        return Integer.parseInt(dataSet.replaceAll("\\D+", ""));
    }
}
