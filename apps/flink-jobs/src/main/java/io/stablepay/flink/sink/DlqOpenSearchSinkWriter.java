package io.stablepay.flink.sink;

import java.io.IOException;

import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import io.stablepay.flink.mapper.DlqToOpenSearchDocMapper;
import io.stablepay.flink.model.DlqEnvelope;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DlqOpenSearchSinkWriter implements SinkWriter<DlqEnvelope> {

    private static final long FLUSH_INTERVAL_MS = 5000;

    private final OpenSearchBulkWriter bulkWriter;
    private long lastFlushTime;

    DlqOpenSearchSinkWriter(String opensearchUrl, String indexName) {
        try {
            var transport = ApacheHttpClient5TransportBuilder
                    .builder(HttpHost.create(opensearchUrl))
                    .setMapper(new JacksonJsonpMapper())
                    .build();
            this.bulkWriter = new OpenSearchBulkWriter(new OpenSearchClient(transport), indexName);
            this.lastFlushTime = System.currentTimeMillis();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Invalid OpenSearch URL: " + opensearchUrl, e);
        }
    }

    @Override
    public void write(DlqEnvelope envelope, Context context) throws IOException, InterruptedException {
        var doc = DlqToOpenSearchDocMapper.toDocument(envelope);
        var eventId = doc.getOrDefault("event_id", "unknown").toString();
        bulkWriter.add(eventId, doc);

        if (System.currentTimeMillis() - lastFlushTime >= FLUSH_INTERVAL_MS) {
            flushAndHandleErrors();
        }
    }

    @Override
    public void flush(boolean endOfInput) throws IOException, InterruptedException {
        if (bulkWriter.shouldFlush()) {
            flushAndHandleErrors();
        }
    }

    @Override
    public void close() throws Exception {
        if (bulkWriter.shouldFlush()) {
            flushAndHandleErrors();
        }
    }

    private void flushAndHandleErrors() throws IOException {
        var result = bulkWriter.flush();
        lastFlushTime = System.currentTimeMillis();

        for (var failed : result.failed()) {
            if (failed.transient_()) {
                log.warn("dlq_opensearch_transient_failure: eventId={} status={} reason={}",
                        failed.eventId(), failed.statusCode(), failed.errorReason());
            } else {
                log.error("dlq_opensearch_permanent_failure: eventId={} status={} type={} reason={}",
                        failed.eventId(), failed.statusCode(), failed.errorType(), failed.errorReason());
            }
        }
    }
}
