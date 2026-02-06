package com.epstein.ingestion.scraper;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CourtRecordsScraperTest {

    private CourtRecordsScraper scraper;

    @BeforeEach
    void setUp() {
        IngestionProperties properties = new IngestionProperties();
        scraper = new CourtRecordsScraper(
                mock(RateLimitedHttpClient.class),
                properties,
                mock(DeduplicationService.class),
                mock(S3UploadService.class),
                mock(DocumentRegistrationService.class),
                new IngestionMetrics(new SimpleMeterRegistry()),
                mock(IngestionRunRepository.class),
                mock(IngestionEventRepository.class)
        );
    }

    @Test
    void getSourceTypeReturnsCorrectValue() {
        assertEquals("COURT_RECORDS", scraper.getSourceType());
    }

    @Test
    void enumerateDocketUrlsGeneratesExpectedCount() {
        List<String> urls = scraper.enumerateDocketUrls();

        // 1334 main dockets + 5 sub-parts
        assertEquals(1339, urls.size());
    }

    @Test
    void enumerateDocketUrlsHasCorrectFormat() {
        List<String> urls = scraper.enumerateDocketUrls();

        // First URL should be 001.pdf
        assertTrue(urls.get(0).endsWith("/001.pdf"));

        // Last main docket is 1334.pdf (at index 1333)
        assertTrue(urls.get(1333).endsWith("/1334.pdf"));

        // Sub-parts follow: 1334-1.pdf through 1334-5.pdf
        assertTrue(urls.get(1334).endsWith("/1334-1.pdf"));
        assertTrue(urls.get(1338).endsWith("/1334-5.pdf"));
    }

    @Test
    void enumerateDocketUrlsUsesConfiguredBaseUrl() {
        List<String> urls = scraper.enumerateDocketUrls();
        assertTrue(urls.get(0).startsWith("https://www.justice.gov/epstein/court-records"));
    }
}
