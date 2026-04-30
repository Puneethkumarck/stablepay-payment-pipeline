package io.stablepay.flink.mapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.DlqEventIds;

public final class DlqToOpenSearchDocMapper {

    private DlqToOpenSearchDocMapper() {}

    public static Map<String, Object> toDocument(DlqEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(envelope.sourceTopic(), "sourceTopic");
        Objects.requireNonNull(envelope.errorClass(), "errorClass");
        Objects.requireNonNull(envelope.errorMessage(), "errorMessage");

        var doc = new HashMap<String, Object>();

        doc.put("event_id", DlqEventIds.of(envelope));
        doc.put("source_topic", envelope.sourceTopic());
        doc.put("source_partition", envelope.sourcePartition());
        doc.put("source_offset", envelope.sourceOffset());
        doc.put("error_class", envelope.errorClass());
        doc.put("error_message", envelope.errorMessage());
        doc.put("failed_at", envelope.failedAt());
        doc.put("retry_count", envelope.retryCount());

        var payload = envelope.originalPayloadBytes();
        if (payload != null) {
            doc.put("original_payload_json", new String(payload, StandardCharsets.UTF_8));
        }

        return doc;
    }
}
