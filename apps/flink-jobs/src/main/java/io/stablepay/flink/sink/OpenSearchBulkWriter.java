package io.stablepay.flink.sink;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchBulkWriter implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(OpenSearchBulkWriter.class);

    private static final int MAX_DOCS = 1000;
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String INDEX_NAME = "transactions";

    private final transient OpenSearchClient client;
    private final List<BulkAction> buffer = new ArrayList<>();
    private long currentSizeEstimate;

    public OpenSearchBulkWriter(OpenSearchClient client) {
        this.client = client;
    }

    public record BulkAction(String eventId, Map<String, Object> document) implements Serializable {}

    public record BulkResult(List<FailedDoc> failed) {
        public record FailedDoc(String eventId, int statusCode, String errorType, String errorReason, boolean transient_) {}
    }

    public void add(String eventId, Map<String, Object> document) throws IOException {
        buffer.add(new BulkAction(eventId, document));
        currentSizeEstimate += estimateSize(document);

        if (buffer.size() >= MAX_DOCS || currentSizeEstimate >= MAX_SIZE_BYTES) {
            flush();
        }
    }

    public boolean shouldFlush() {
        return !buffer.isEmpty();
    }

    public BulkResult flush() throws IOException {
        if (buffer.isEmpty()) {
            return new BulkResult(List.of());
        }

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (BulkAction action : buffer) {
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(INDEX_NAME)
                            .id(action.eventId())
                            .document(action.document())));
        }

        BulkResponse response = client.bulk(bulkBuilder.build());
        List<BulkResult.FailedDoc> failures = new ArrayList<>();

        if (response.errors()) {
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    int status = item.status();
                    boolean isTransient = status == 429 || status == 503;
                    failures.add(new BulkResult.FailedDoc(
                            item.id(),
                            status,
                            item.error().type(),
                            item.error().reason(),
                            isTransient));
                }
            }
            log.warn("opensearch_bulk_errors", failures.size());
        }

        buffer.clear();
        currentSizeEstimate = 0;
        return new BulkResult(failures);
    }

    private static long estimateSize(Map<String, Object> doc) {
        return doc.toString().length() * 2L;
    }
}
