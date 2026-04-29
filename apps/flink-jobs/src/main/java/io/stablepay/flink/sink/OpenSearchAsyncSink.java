package io.stablepay.flink.sink;

import java.util.Map;

import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import io.stablepay.flink.mapper.EventToOpenSearchDocMapper;
import io.stablepay.flink.model.ValidatedEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("deprecation")
public class OpenSearchAsyncSink implements SinkFunction<ValidatedEvent> {
    private static final long FLUSH_INTERVAL_MS = 5000;

    private final String opensearchUrl;
    private transient OpenSearchBulkWriter bulkWriter;
    private transient long lastFlushTime;

    public OpenSearchAsyncSink(String opensearchUrl) {
        this.opensearchUrl = opensearchUrl;
    }

    private void ensureInitialized() {
        if (bulkWriter == null) {
            try {
                var transport = ApacheHttpClient5TransportBuilder
                        .builder(HttpHost.create(opensearchUrl))
                        .setMapper(new JacksonJsonpMapper())
                        .build();
                bulkWriter = new OpenSearchBulkWriter(new OpenSearchClient(transport));
                lastFlushTime = System.currentTimeMillis();
            } catch (java.net.URISyntaxException e) {
                throw new IllegalArgumentException("Invalid OpenSearch URL: " + opensearchUrl, e);
            }
        }
    }

    @Override
    public void invoke(ValidatedEvent event, Context context) throws Exception {
        ensureInitialized();
        Map<String, Object> doc = EventToOpenSearchDocMapper.toDocument(event);
        bulkWriter.add(event.eventId(), doc);

        if (System.currentTimeMillis() - lastFlushTime >= FLUSH_INTERVAL_MS) {
            flushAndHandleErrors();
        }
    }

    @Override
    public void finish() throws Exception {
        if (bulkWriter != null && bulkWriter.shouldFlush()) {
            flushAndHandleErrors();
        }
    }

    private void flushAndHandleErrors() throws Exception {
        var result = bulkWriter.flush();
        lastFlushTime = System.currentTimeMillis();

        for (var failed : result.failed()) {
            if (failed.transient_()) {
                log.warn("opensearch_transient_failure: eventId={} status={} reason={}",
                        failed.eventId(), failed.statusCode(), failed.errorReason());
            } else {
                log.error("opensearch_permanent_failure: eventId={} status={} type={} reason={}",
                        failed.eventId(), failed.statusCode(), failed.errorType(), failed.errorReason());
            }
        }
    }
}
