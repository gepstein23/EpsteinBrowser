package com.epstein.ingestion.scraper;

import com.epstein.common.model.Document;
import com.epstein.common.model.IngestionRun;
import com.epstein.common.repository.IngestionEventRepository;
import com.epstein.common.repository.IngestionRunRepository;
import com.epstein.ingestion.config.IngestionProperties;
import com.epstein.ingestion.http.RateLimitedHttpClient;
import com.epstein.ingestion.metrics.IngestionMetrics;
import com.epstein.ingestion.service.DeduplicationService;
import com.epstein.ingestion.service.DocumentRegistrationService;
import com.epstein.ingestion.service.S3UploadService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ZipDownloadScraperTest {

    private RateLimitedHttpClient httpClient;
    private IngestionProperties properties;
    private DeduplicationService deduplicationService;
    private S3UploadService s3UploadService;
    private DocumentRegistrationService documentRegistrationService;
    private IngestionRunRepository runRepository;
    private IngestionEventRepository eventRepository;
    private ZipDownloadScraper scraper;

    @BeforeEach
    void setUp() {
        httpClient = mock(RateLimitedHttpClient.class);
        properties = new IngestionProperties();
        deduplicationService = mock(DeduplicationService.class);
        s3UploadService = mock(S3UploadService.class);
        documentRegistrationService = mock(DocumentRegistrationService.class);
        IngestionMetrics metrics = new IngestionMetrics(new SimpleMeterRegistry());
        runRepository = mock(IngestionRunRepository.class);
        eventRepository = mock(IngestionEventRepository.class);

        when(runRepository.save(any(IngestionRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deduplicationService.computeHash(any())).thenReturn("somehash");
        when(deduplicationService.isDuplicate(anyString())).thenReturn(false);
        when(s3UploadService.upload(any(), anyString(), anyString())).thenReturn("raw/ds/file.pdf");
        when(documentRegistrationService.register(any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenReturn(new Document());

        scraper = new ZipDownloadScraper(httpClient, properties, deduplicationService,
                s3UploadService, documentRegistrationService, metrics, runRepository, eventRepository);
    }

    @Test
    void getSourceTypeReturnsCorrectValue() {
        assertEquals("ZIP_DOWNLOAD", scraper.getSourceType());
    }

    @Test
    void buildZipUrlGeneratesCorrectUrl() {
        String url = scraper.buildZipUrl("data-set-3");
        assertTrue(url.endsWith("/data-set-3.zip"));
    }

    @Test
    void extractEntryFileNameHandlesNestedPaths() {
        assertEquals("doc.pdf", ZipDownloadScraper.extractEntryFileName("folder/subfolder/doc.pdf"));
        assertEquals("test.pdf", ZipDownloadScraper.extractEntryFileName("test.pdf"));
    }

    @Test
    void processZipExtractsPdfEntries() throws Exception {
        byte[] zipContent = createTestZip("doc1.pdf", "doc2.pdf", "readme.txt");

        IngestionRun run = new IngestionRun("data-set-1", "ZIP_DOWNLOAD");
        run.setId(1L);

        scraper.processZip(zipContent, "data-set-1", "https://example.com/test.zip", run);

        // Should only process 2 PDFs, not the txt file
        verify(s3UploadService, times(2)).upload(any(), eq("data-set-1"), anyString());
        assertEquals(2, run.getTotalDiscovered());
    }

    @Test
    void processZipSkipsDuplicates() throws Exception {
        when(deduplicationService.isDuplicate("somehash")).thenReturn(true);

        byte[] zipContent = createTestZip("doc1.pdf");

        IngestionRun run = new IngestionRun("data-set-1", "ZIP_DOWNLOAD");
        run.setId(1L);

        scraper.processZip(zipContent, "data-set-1", "https://example.com/test.zip", run);

        verify(s3UploadService, never()).upload(any(), anyString(), anyString());
        assertEquals(1, run.getSkippedDuplicate());
    }

    @Test
    void processZipSkipsDirectories() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add a directory entry
            zos.putNextEntry(new ZipEntry("folder/"));
            zos.closeEntry();
            // Add a PDF
            zos.putNextEntry(new ZipEntry("folder/doc.pdf"));
            zos.write("pdf content".getBytes());
            zos.closeEntry();
        }

        IngestionRun run = new IngestionRun("data-set-1", "ZIP_DOWNLOAD");
        run.setId(1L);

        scraper.processZip(baos.toByteArray(), "data-set-1", "https://example.com/test.zip", run);

        verify(s3UploadService, times(1)).upload(any(), eq("data-set-1"), eq("doc.pdf"));
    }

    private byte[] createTestZip(String... fileNames) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String name : fileNames) {
                zos.putNextEntry(new ZipEntry(name));
                zos.write(("content of " + name).getBytes());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
