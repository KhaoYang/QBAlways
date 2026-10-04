package dev.qbalways.indexer;

import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class IndexingJob {
    private final QbReaderClient qbReader;
    private final QuestionTransformer transformer;
    private final SolrIndexClient solr;
    private final SyncRunRepository runs;

    public IndexingJob(
            QbReaderClient qbReader,
            QuestionTransformer transformer,
            SolrIndexClient solr,
            SyncRunRepository runs
    ) {
        this.qbReader = qbReader;
        this.transformer = transformer;
        this.solr = solr;
        this.runs = runs;
    }

    public void run(UUID runId, int maxSets) {
        int setsProcessed = 0;
        int packetsProcessed = 0;
        int documentsIndexed = 0;

        try {
            solr.ensureSchema();
            JsonNode sets = qbReader.fetchSets();
            int setLimit = maxSets <= 0 ? sets.size() : Math.min(maxSets, sets.size());

            for (int setIndex = 0; setIndex < setLimit; setIndex++) {
                JsonNode set = sets.get(setIndex);
                String setName = set.path("setName").asText();
                int packetCount = set.path("packetsCount").asInt();

                for (int packetNumber = 1; packetNumber <= packetCount; packetNumber++) {
                    JsonNode packet = qbReader.fetchPacket(setName, packetNumber);
                    List<Map<String, Object>> documents = transformer.transformPacket(packet, set);
                    solr.index(documents);
                    packetsProcessed++;
                    documentsIndexed += documents.size();
                    runs.progress(runId, setsProcessed, packetsProcessed, documentsIndexed);
                }

                setsProcessed++;
                runs.progress(runId, setsProcessed, packetsProcessed, documentsIndexed);
            }

            solr.commit();
            runs.complete(runId, setsProcessed, packetsProcessed, documentsIndexed);
        } catch (RuntimeException error) {
            runs.fail(runId, error.getMessage());
            throw error;
        }
    }
}
