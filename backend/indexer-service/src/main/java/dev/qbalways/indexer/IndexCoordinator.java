package dev.qbalways.indexer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexCoordinator {
    private final IndexingJob job;
    private final SyncRunRepository runs;
    private final TaskExecutor executor;
    private final AtomicBoolean running = new AtomicBoolean();
    private final boolean scheduledSyncEnabled;

    public IndexCoordinator(
            IndexingJob job,
            SyncRunRepository runs,
            TaskExecutor indexingExecutor,
            @Value("${qbalways.sync.enabled:false}") boolean scheduledSyncEnabled
    ) {
        this.job = job;
        this.runs = runs;
        this.executor = indexingExecutor;
        this.scheduledSyncEnabled = scheduledSyncEnabled;
    }

    public UUID start(int maxSets) {
        if (!running.compareAndSet(false, true)) throw new SyncAlreadyRunningException();
        UUID runId = UUID.randomUUID();
        try {
            runs.create(runId);
        } catch (RuntimeException error) {
            running.set(false);
            throw error;
        }
        executor.execute(() -> {
            try {
                job.run(runId, maxSets);
            } finally {
                running.set(false);
            }
        });
        return runId;
    }

    @Scheduled(cron = "${qbalways.sync.cron:0 0 3 * * *}")
    public void scheduledSync() {
        if (scheduledSyncEnabled && !running.get()) start(0);
    }

    public boolean isRunning() {
        return running.get();
    }

    public static class SyncAlreadyRunningException extends RuntimeException {}
}
