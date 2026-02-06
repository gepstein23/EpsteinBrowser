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

class FoiaScraperTest {

    private FoiaScraper scraper;

    @BeforeEach
    void setUp() {
        IngestionProperties properties = new IngestionProperties();
        scraper = new FoiaScraper(
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
        assertEquals("FOIA", scraper.getSourceType());
    }

    @Test
    void enumerateFoiaUrlsReturns4Records() {
        List<String> urls = scraper.enumerateFoiaUrls();
        assertEquals(4, urls.size());
    }

    @Test
    void enumerateFoiaUrlsHasCorrectFormat() {
        List<String> urls = scraper.enumerateFoiaUrls();

        for (int i = 0; i < 4; i++) {
            String url = urls.get(i);
            assertTrue(url.contains("Epstein%20Records%20" + (i + 1) + ".pdf"),
                    "URL " + i + " should contain correct record number: " + url);
        }
    }

    @Test
    void enumerateFoiaUrlsUsesConfiguredBaseUrl() {
        List<String> urls = scraper.enumerateFoiaUrls();
        assertTrue(urls.get(0).startsWith("https://www.justice.gov/epstein/foia"));
    }
}
