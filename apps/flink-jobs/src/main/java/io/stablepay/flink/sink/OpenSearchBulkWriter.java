package io.stablepay.flink.sink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenSearchBulkWriter {

    private static final int MAX_DOCS = 1000;
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String INDEX_NAME = "transactions";
    private static final int MAX_RETRIES = 3;

    private final OpenSearchClient client;
    private final List<BulkAction> buffer = new ArrayList<>();
    private long currentSizeEstimate;

    public OpenSearchBulkWriter(OpenSearchClient client) {
        this.client = client;
    }

    public record BulkAction(String eventId, Map<String, Object> document, int retryCount) {
        BulkAction(String eventId, Map<String, Object> document) {
            this(eventId, document, 0);
        }

        BulkAction withRetry() {
            return new BulkAction(eventId, document, retryCount + 1);
        }
    }

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

        List<BulkResult.FailedDoc> permanentFailures = new ArrayList<>();
        List<BulkAction> pending = new ArrayList<>(buffer);
        buffer.clear();
        currentSizeEstimate = 0;

        while (!pending.isEmpty()) {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (BulkAction action : pending) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(INDEX_NAME)
                                .id(action.eventId())
                                .document(action.document())));
            }

            BulkResponse response = client.bulk(bulkBuilder.build());
            List<BulkAction> retryable = new ArrayList<>();

            if (response.errors()) {
                for (int i = 0; i < response.items().size(); i++) {
                    BulkResponseItem item = response.items().get(i);
                    if (item.error() != null) {
                        int status = item.status();
                        boolean isTransient = status == 429 || status == 503;
                        BulkAction original = pending.get(i);

                        if (isTransient && original.retryCount() < MAX_RETRIES) {
                            retryable.add(original.withRetry());
                            log.warn("opensearch_transient_failure: eventId={} status={} retry={}/{}",
                                    item.id(), status, original.retryCount() + 1, MAX_RETRIES);
                        } else {
                            permanentFailures.add(new BulkResult.FailedDoc(
                                    item.id(), status,
                                    item.error().type(), item.error().reason(), isTransient));
                        }
                    }
                }
                log.warn("opensearch_bulk_errors: count={} retryable={} permanent={}",
                        permanentFailures.size() + retryable.size(), retryable.size(), permanentFailures.size());
            }

            pending = retryable;
        }

        return new BulkResult(permanentFailures);
    }

    private static long estimateSize(Map<String, Object> doc) {
        return doc.toString().length() * 2L;
    }
}
