package io.stablepay.flink.sink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OpenSearchBulkWriter {

    private static final int MAX_DOCS = 1000;
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String INDEX_NAME = "transactions";
    private static final int MAX_RETRIES = 3;

    private final OpenSearchClient client;
    private final List<BulkAction> buffer = new ArrayList<>();
    private long currentSizeEstimate;

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

        var permanentFailures = new ArrayList<BulkResult.FailedDoc>();
        var pending = new ArrayList<>(buffer);
        buffer.clear();
        currentSizeEstimate = 0;

        while (!pending.isEmpty()) {
            var bulkBuilder = new BulkRequest.Builder();
            for (var action : pending) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(INDEX_NAME)
                                .id(action.eventId())
                                .document(action.document())));
            }

            var response = client.bulk(bulkBuilder.build());
            var retryable = new ArrayList<BulkAction>();

            if (response.errors()) {
                for (int i = 0; i < response.items().size(); i++) {
                    var item = response.items().get(i);
                    if (item.error() != null) {
                        var status = item.status();
                        var isTransient = status == 429 || status == 503;
                        var original = pending.get(i);

                        if (isTransient && original.retryCount() < MAX_RETRIES) {
                            retryable.add(original.withRetry());
                            log.warn("opensearch_transient_failure: eventId={} status={} retry={}/{}",
                                    item.id(), status, original.retryCount() + 1, MAX_RETRIES);
                        } else {
                            permanentFailures.add(BulkResult.FailedDoc.builder()
                                    .eventId(item.id())
                                    .statusCode(status)
                                    .errorType(item.error().type())
                                    .errorReason(item.error().reason())
                                    .transient_(isTransient)
                                    .build());
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
        var size = 2L;
        for (var entry : doc.entrySet()) {
            size += entry.getKey().length() + 4;
            var val = entry.getValue();
            if (val instanceof String s) {
                size += s.length() + 2;
            } else if (val != null) {
                size += 20;
            } else {
                size += 4;
            }
        }
        return size;
    }
}
