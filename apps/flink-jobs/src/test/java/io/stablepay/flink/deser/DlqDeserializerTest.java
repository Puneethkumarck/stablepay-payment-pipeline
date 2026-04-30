package io.stablepay.flink.deser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import io.stablepay.flink.model.DlqEnvelope;

class DlqDeserializerTest {

    @Test
    void shouldDeserializeJsonPayloadIntoDlqEnvelope() throws Exception {
        // given
        var deserializer = new DlqDeserializer();
        var payload = new ObjectMapper().writeValueAsBytes(java.util.Map.of(
                "source_topic", "payment.payout.fiat.v1",
                "source_partition", 3,
                "source_offset", 42L,
                "error_class", "SchemaValidationException",
                "error_message", "invalid",
                "failed_at", 1_711_000_000_000L,
                "retry_count", 2));
        var record = new ConsumerRecord<byte[], byte[]>("dlq.schema-invalid.v1", 0, 0L, null, payload);
        var collected = new ArrayList<DlqEnvelope>();
        Collector<DlqEnvelope> collector = collectingCollector(collected);

        // when
        deserializer.deserialize(record, collector);

        // then
        var expected = DlqEnvelope.builder()
                .sourceTopic("payment.payout.fiat.v1")
                .sourcePartition(3)
                .sourceOffset(42L)
                .errorClass("SchemaValidationException")
                .errorMessage("invalid")
                .failedAt(1_711_000_000_000L)
                .retryCount(2)
                .build();
        assertThat(collected).usingRecursiveFieldByFieldElementComparator().containsExactly(expected);
    }

    @Test
    void shouldEmitFallbackEnvelopeWhenPayloadIsNotValidJson() throws Exception {
        // given
        var deserializer = new DlqDeserializer();
        var payload = "not-json".getBytes();
        var record = new ConsumerRecord<byte[], byte[]>(
                "dlq.schema-invalid.v1", 5, 99L, 1_711_500_000_000L,
                org.apache.kafka.common.record.TimestampType.CREATE_TIME,
                0, payload.length, null, payload,
                new org.apache.kafka.common.header.internals.RecordHeaders(), java.util.Optional.empty());
        var collected = new ArrayList<DlqEnvelope>();
        Collector<DlqEnvelope> collector = collectingCollector(collected);

        // when
        deserializer.deserialize(record, collector);

        // then
        var expected = DlqEnvelope.builder()
                .sourceTopic("dlq.schema-invalid.v1")
                .sourcePartition(5)
                .sourceOffset(99L)
                .errorClass(DlqDeserializer.DESERIALIZATION_FAILED)
                .errorMessage("JsonParseException")
                .originalPayloadBytes(payload)
                .failedAt(1_711_500_000_000L)
                .retryCount(0)
                .build();
        assertThat(collected).usingRecursiveFieldByFieldElementComparator().containsExactly(expected);
    }

    private static Collector<DlqEnvelope> collectingCollector(List<DlqEnvelope> sink) {
        return new Collector<>() {
            @Override
            public void collect(DlqEnvelope record) {
                sink.add(record);
            }

            @Override
            public void close() {}
        };
    }
}
