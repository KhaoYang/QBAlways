package dev.qbalways.indexer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SyncRunRepository {
    private final JdbcClient jdbc;

    public SyncRunRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void create(UUID id) {
        jdbc.sql("""
                INSERT INTO sync_runs (id, status, started_at, sets_processed, packets_processed, documents_indexed)
                VALUES (:id, 'RUNNING', :startedAt, 0, 0, 0)
                """)
                .param("id", id)
                .param("startedAt", OffsetDateTime.now(ZoneOffset.UTC))
                .update();
    }

    public void progress(UUID id, int sets, int packets, int documents) {
        jdbc.sql("""
                UPDATE sync_runs
                SET sets_processed = :sets, packets_processed = :packets, documents_indexed = :documents
                WHERE id = :id
                """)
                .param("id", id)
                .param("sets", sets)
                .param("packets", packets)
                .param("documents", documents)
                .update();
    }

    public void complete(UUID id, int sets, int packets, int documents) {
        jdbc.sql("""
                UPDATE sync_runs
                SET status = 'COMPLETED', completed_at = :completedAt,
                    sets_processed = :sets, packets_processed = :packets, documents_indexed = :documents
                WHERE id = :id
                """)
                .param("id", id)
                .param("completedAt", OffsetDateTime.now(ZoneOffset.UTC))
                .param("sets", sets)
                .param("packets", packets)
                .param("documents", documents)
                .update();
    }

    public void fail(UUID id, String message) {
        jdbc.sql("""
                UPDATE sync_runs
                SET status = 'FAILED', completed_at = :completedAt, error_message = :message
                WHERE id = :id
                """)
                .param("id", id)
                .param("completedAt", OffsetDateTime.now(ZoneOffset.UTC))
                .param("message", message == null ? "Unknown indexing error" : message.substring(0, Math.min(1000, message.length())))
                .update();
    }

    public Optional<SyncRun> find(UUID id) {
        return jdbc.sql("SELECT * FROM sync_runs WHERE id = :id")
                .param("id", id)
                .query(SyncRun.class)
                .optional();
    }

    public List<SyncRun> recent() {
        return jdbc.sql("SELECT * FROM sync_runs ORDER BY started_at DESC LIMIT 20")
                .query(SyncRun.class)
                .list();
    }

    public record SyncRun(
            UUID id,
            String status,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            int setsProcessed,
            int packetsProcessed,
            int documentsIndexed,
            String errorMessage
    ) {}
}
