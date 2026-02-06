package com.epstein.ingestion.service;

import com.epstein.common.model.IngestionRun;
import com.epstein.common.repository.IngestionEventRepository;
import com.epstein.common.repository.IngestionRunRepository;
import com.epstein.ingestion.metrics.IngestionMetrics;
import com.epstein.ingestion.scraper.ScraperStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IngestionOrchestratorTest {

    private IngestionRunRepository runRepository;
    private IngestionEventRepository eventRepository;
    private IngestionMetrics metrics;
    private ScraperStrategy mockScraper;
    private IngestionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        runRepository = mock(IngestionRunRepository.class);
        eventRepository = mock(IngestionEventRepository.class);
        metrics = new IngestionMetrics(new SimpleMeterRegistry());

        mockScraper = mock(ScraperStrategy.class);
        when(mockScraper.getSourceType()).thenReturn("DOJ_DISCLOSURES");

        ScraperStrategy zipScraper = mock(ScraperStrategy.class);
        when(zipScraper.getSourceType()).thenReturn("ZIP_DOWNLOAD");

        ScraperStrategy courtScraper = mock(ScraperStrategy.class);
        when(courtScraper.getSourceType()).thenReturn("COURT_RECORDS");

        ScraperStrategy foiaScraper = mock(ScraperStrategy.class);
        when(foiaScraper.getSourceType()).thenReturn("FOIA");

        when(runRepository.save(any(IngestionRun.class))).thenAnswer(invocation -> {
            IngestionRun run = invocation.getArgument(0);
            if (run.getId() == null) run.setId(1L);
            return run;
        });

        orchestrator = new IngestionOrchestrator(
                List.of(mockScraper, zipScraper, courtScraper, foiaScraper),
                runRepository, eventRepository, metrics,
                Executors.newSingleThreadExecutor()
        );
    }

    @Test
    void resolveSourceTypeForDisclosures() {
        assertEquals("DOJ_DISCLOSURES", orchestrator.resolveSourceType("data-set-9"));
        assertEquals("DOJ_DISCLOSURES", orchestrator.resolveSourceType("data-set-10"));
        assertEquals("DOJ_DISCLOSURES", orchestrator.resolveSourceType("data-set-11"));
    }

    @Test
    void resolveSourceTypeForZip() {
        assertEquals("ZIP_DOWNLOAD", orchestrator.resolveSourceType("data-set-1"));
        assertEquals("ZIP_DOWNLOAD", orchestrator.resolveSourceType("data-set-8"));
        assertEquals("ZIP_DOWNLOAD", orchestrator.resolveSourceType("data-set-12"));
    }

    @Test
    void resolveSourceTypeForCourtRecords() {
        assertEquals("COURT_RECORDS", orchestrator.resolveSourceType("court-records"));
    }

    @Test
    void resolveSourceTypeForFoia() {
        assertEquals("FOIA", orchestrator.resolveSourceType("foia"));
    }

    @Test
    void resolveSourceTypeThrowsForUnknown() {
        assertThrows(IllegalArgumentException.class,
                () -> orchestrator.resolveSourceType("unknown-data-set"));
    }

    @Test
    void startIngestionCreatesRunAndSaves() {
        IngestionRun result = orchestrator.startIngestion("data-set-9");

        assertNotNull(result);
        assertEquals("data-set-9", result.getDataSet());
        assertEquals("DOJ_DISCLOSURES", result.getSourceType());
        assertEquals("RUNNING", result.getStatus());
        verify(runRepository, atLeastOnce()).save(any(IngestionRun.class));
    }

    @Test
    void startAllIngestionCreatesMultipleRuns() {
        List<IngestionRun> runs = orchestrator.startAllIngestion();

        // 9 ZIP + 3 disclosure + court-records + foia = 14
        assertEquals(14, runs.size());
    }
}
