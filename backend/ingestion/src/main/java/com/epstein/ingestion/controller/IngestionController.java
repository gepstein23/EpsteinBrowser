package com.epstein.ingestion.controller;

import com.epstein.common.model.IngestionEvent;
import com.epstein.common.model.IngestionRun;
import com.epstein.common.repository.DocumentRepository;
import com.epstein.common.repository.IngestionEventRepository;
import com.epstein.common.repository.IngestionRunRepository;
import com.epstein.ingestion.service.IngestionOrchestrator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestionOrchestrator orchestrator;
    private final IngestionRunRepository runRepository;
    private final IngestionEventRepository eventRepository;
    private final DocumentRepository documentRepository;

    public IngestionController(IngestionOrchestrator orchestrator,
                               IngestionRunRepository runRepository,
                               IngestionEventRepository eventRepository,
                               DocumentRepository documentRepository) {
        this.orchestrator = orchestrator;
        this.runRepository = runRepository;
        this.eventRepository = eventRepository;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startIngestion(@RequestParam(required = false) String dataSet) {
        if (dataSet == null || dataSet.equals("all")) {
            List<IngestionRun> runs = orchestrator.startAllIngestion();
            return ResponseEntity.ok(Map.of(
                    "message", "Started ingestion for all data sets",
                    "runs", runs.stream().map(this::toRunSummary).toList()
            ));
        }

        IngestionRun run = orchestrator.startIngestion(dataSet);
        return ResponseEntity.ok(Map.of(
                "message", "Started ingestion for " + dataSet,
                "run", toRunSummary(run)
        ));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<Map<String, Object>>> listRuns() {
        List<IngestionRun> runs = runRepository.findAllByOrderByStartedAtDesc();
        return ResponseEntity.ok(runs.stream().map(this::toRunSummary).toList());
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<?> getRunDetail(@PathVariable Long id) {
        return runRepository.findById(id)
                .map(run -> ResponseEntity.ok(toRunDetail(run)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/runs/{id}/events")
    public ResponseEntity<Page<IngestionEvent>> getRunEvents(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(eventRepository.findByRunIdOrderByTimestampDesc(id, pageRequest));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalDocuments", documentRepository.count());
        status.put("activeRuns", orchestrator.getActiveRuns().size());

        List<String> dataSets = List.of(
                "data-set-1", "data-set-2", "data-set-3", "data-set-4",
                "data-set-5", "data-set-6", "data-set-7", "data-set-8",
                "data-set-9", "data-set-10", "data-set-11", "data-set-12",
                "court-records", "foia"
        );

        Map<String, Long> docsPerDataSet = new HashMap<>();
        for (String ds : dataSets) {
            long count = documentRepository.countByDataSet(ds);
            if (count > 0) {
                docsPerDataSet.put(ds, count);
            }
        }
        status.put("documentsPerDataSet", docsPerDataSet);

        List<IngestionRun> running = runRepository.findByStatus("RUNNING");
        status.put("runningIngestions", running.stream().map(this::toRunSummary).toList());

        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ingestion"));
    }

    private Map<String, Object> toRunSummary(IngestionRun run) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", run.getId());
        summary.put("dataSet", run.getDataSet());
        summary.put("sourceType", run.getSourceType());
        summary.put("status", run.getStatus());
        summary.put("totalDiscovered", run.getTotalDiscovered());
        summary.put("downloaded", run.getDownloaded());
        summary.put("failed", run.getFailed());
        summary.put("skippedDuplicate", run.getSkippedDuplicate());
        summary.put("startedAt", run.getStartedAt());
        summary.put("completedAt", run.getCompletedAt());
        return summary;
    }

    private Map<String, Object> toRunDetail(IngestionRun run) {
        Map<String, Object> detail = toRunSummary(run);
        if (run.getTotalDiscovered() > 0) {
            int processed = run.getDownloaded() + run.getFailed() + run.getSkippedDuplicate();
            detail.put("progressPercent", (double) processed / run.getTotalDiscovered() * 100.0);
        } else {
            detail.put("progressPercent", 0.0);
        }
        return detail;
    }
}
