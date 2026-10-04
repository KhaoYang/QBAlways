package dev.qbalways.indexer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    private final IndexCoordinator coordinator;
    private final SyncRunRepository runs;

    public IndexController(IndexCoordinator coordinator, SyncRunRepository runs) {
        this.coordinator = coordinator;
        this.runs = runs;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> start(@RequestParam(defaultValue = "0") int maxSets) {
        if (maxSets < 0) throw new IllegalArgumentException("maxSets cannot be negative");
        UUID runId = coordinator.start(maxSets);
        return ResponseEntity.accepted()
                .location(URI.create("/api/index/runs/" + runId))
                .body(Map.of("runId", runId, "status", "RUNNING"));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<SyncRunRepository.SyncRun> run(@PathVariable UUID id) {
        return ResponseEntity.of(runs.find(id));
    }

    @GetMapping("/runs")
    public List<SyncRunRepository.SyncRun> recentRuns() {
        return runs.recent();
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("running", coordinator.isRunning());
    }

    @ExceptionHandler(IndexCoordinator.SyncAlreadyRunningException.class)
    ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "An indexing run is already in progress"));
    }
}
