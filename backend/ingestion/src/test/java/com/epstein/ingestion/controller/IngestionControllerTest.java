package com.epstein.ingestion.controller;

import com.epstein.common.model.IngestionEvent;
import com.epstein.common.model.IngestionRun;
import com.epstein.common.repository.DocumentRepository;
import com.epstein.common.repository.IngestionEventRepository;
import com.epstein.common.repository.IngestionRunRepository;
import com.epstein.ingestion.config.JpaConfig;
import com.epstein.ingestion.service.IngestionOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = IngestionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfig.class))
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngestionOrchestrator orchestrator;

    @MockitoBean
    private IngestionRunRepository runRepository;

    @MockitoBean
    private IngestionEventRepository eventRepository;

    @MockitoBean
    private DocumentRepository documentRepository;

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/ingestion/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("ingestion"));
    }

    @Test
    void startIngestionForDataSet() throws Exception {
        IngestionRun run = createTestRun(1L, "data-set-1", "ZIP_DOWNLOAD");
        when(orchestrator.startIngestion("data-set-1")).thenReturn(run);

        mockMvc.perform(post("/api/v1/ingestion/start").param("dataSet", "data-set-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Started ingestion for data-set-1"))
                .andExpect(jsonPath("$.run.id").value(1))
                .andExpect(jsonPath("$.run.dataSet").value("data-set-1"));
    }

    @Test
    void startIngestionForAll() throws Exception {
        List<IngestionRun> runs = List.of(
                createTestRun(1L, "data-set-1", "ZIP_DOWNLOAD"),
                createTestRun(2L, "data-set-9", "DOJ_DISCLOSURES")
        );
        when(orchestrator.startAllIngestion()).thenReturn(runs);

        mockMvc.perform(post("/api/v1/ingestion/start").param("dataSet", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Started ingestion for all data sets"))
                .andExpect(jsonPath("$.runs").isArray());
    }

    @Test
    void listRunsReturnsAll() throws Exception {
        List<IngestionRun> runs = List.of(
                createTestRun(1L, "data-set-1", "ZIP_DOWNLOAD"),
                createTestRun(2L, "data-set-9", "DOJ_DISCLOSURES")
        );
        when(runRepository.findAllByOrderByStartedAtDesc()).thenReturn(runs);

        mockMvc.perform(get("/api/v1/ingestion/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getRunDetailReturnsRun() throws Exception {
        IngestionRun run = createTestRun(1L, "data-set-1", "ZIP_DOWNLOAD");
        run.setTotalDiscovered(100);
        run.setDownloaded(50);
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));

        mockMvc.perform(get("/api/v1/ingestion/runs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.progressPercent").value(50.0));
    }

    @Test
    void getRunDetailReturns404() throws Exception {
        when(runRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/ingestion/runs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRunEventsReturnsPaginated() throws Exception {
        IngestionEvent event = new IngestionEvent(1L, "DOCUMENT_DOWNLOADED", "http://example.com/test.pdf", "Downloaded");
        when(eventRepository.findByRunIdOrderByTimestampDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        mockMvc.perform(get("/api/v1/ingestion/runs/1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getStatusReturnsOverview() throws Exception {
        when(documentRepository.count()).thenReturn(500L);
        when(orchestrator.getActiveRuns()).thenReturn(Map.of());
        when(documentRepository.countByDataSet(anyString())).thenReturn(0L);
        when(runRepository.findByStatus("RUNNING")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ingestion/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocuments").value(500))
                .andExpect(jsonPath("$.activeRuns").value(0));
    }

    private IngestionRun createTestRun(Long id, String dataSet, String sourceType) {
        IngestionRun run = new IngestionRun(dataSet, sourceType);
        run.setId(id);
        run.setStartedAt(Instant.now());
        return run;
    }
}
