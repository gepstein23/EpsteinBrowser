package com.epstein.ingestion.scraper;

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

import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DojDisclosureScraperTest {

    private RateLimitedHttpClient httpClient;
    private IngestionProperties properties;
    private DeduplicationService deduplicationService;
    private S3UploadService s3UploadService;
    private DocumentRegistrationService documentRegistrationService;
    private IngestionRunRepository runRepository;
    private IngestionEventRepository eventRepository;
    private DojDisclosureScraper scraper;

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

        scraper = new DojDisclosureScraper(httpClient, properties, deduplicationService,
                s3UploadService, documentRegistrationService, metrics, runRepository, eventRepository);
    }

    @Test
    void getSourceTypeReturnsCorrectValue() {
        assertEquals("DOJ_DISCLOSURES", scraper.getSourceType());
    }

    @Test
    void parsePdfLinksExtractsPdfUrls() {
        String html = """
                <html><body>
                <a href="/files/doc1.pdf">Document 1</a>
                <a href="/files/doc2.pdf">Document 2</a>
                <a href="/files/readme.txt">Readme</a>
                <a href="/files/doc3.PDF">Document 3</a>
                </body></html>
                """;

        List<String> urls = scraper.parsePdfLinks(html, "https://www.justice.gov/epstein/page");

        assertEquals(3, urls.size());
        assertTrue(urls.stream().allMatch(u -> u.toLowerCase().endsWith(".pdf")));
    }

    @Test
    void parsePdfLinksHandlesEmptyPage() {
        String html = "<html><body><p>No documents here</p></body></html>";
        List<String> urls = scraper.parsePdfLinks(html, "https://www.justice.gov/epstein/page");
        assertTrue(urls.isEmpty());
    }

    @Test
    void parsePdfLinksResolvesRelativeUrls() {
        String html = """
                <html><body>
                <a href="/epstein/files/doc.pdf">Doc</a>
                </body></html>
                """;

        List<String> urls = scraper.parsePdfLinks(html, "https://www.justice.gov/epstein/page");

        assertEquals(1, urls.size());
        assertTrue(urls.get(0).startsWith("https://www.justice.gov"));
    }

    @Test
    void extractFileNameFromUrl() {
        assertEquals("test.pdf", DojDisclosureScraper.extractFileName("https://example.com/files/test.pdf"));
        assertEquals("doc.pdf", DojDisclosureScraper.extractFileName("https://example.com/doc.pdf?v=1"));
        assertEquals("file.pdf", DojDisclosureScraper.extractFileName("https://example.com/a/b/c/file.pdf"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void discoverPdfUrlsPaginates() throws Exception {
        String pageWithLinks = """
                <html><body>
                <a href="https://justice.gov/files/doc1.pdf">Doc 1</a>
                <a href="https://justice.gov/files/doc2.pdf">Doc 2</a>
                </body></html>
                """;
        String emptyPage = "<html><body></body></html>";

        HttpResponse<String> pageResp = mock(HttpResponse.class);
        when(pageResp.body()).thenReturn(pageWithLinks);
        HttpResponse<String> emptyResp = mock(HttpResponse.class);
        when(emptyResp.body()).thenReturn(emptyPage);

        when(httpClient.getString(contains("page=0"))).thenReturn(pageResp);
        when(httpClient.getString(contains("page=1"))).thenReturn(emptyResp);

        IngestionRun run = new IngestionRun("data-set-9", "DOJ_DISCLOSURES");
        run.setId(1L);

        List<String> urls = scraper.discoverPdfUrls(9, run);

        assertEquals(2, urls.size());
        verify(httpClient, times(2)).getString(anyString());
    }
}
