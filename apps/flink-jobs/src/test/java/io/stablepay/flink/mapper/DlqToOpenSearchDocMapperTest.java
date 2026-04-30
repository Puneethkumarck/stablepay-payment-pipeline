package io.stablepay.flink.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.DlqEventIds;

class DlqToOpenSearchDocMapperTest {

    @Test
    void shouldBuildDocumentFromEnvelopeWithDeterministicEventId() {
        // given
        var payload = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        var envelope = DlqEnvelope.builder()
                .sourceTopic("payment.flow.v1")
                .sourcePartition(2)
                .sourceOffset(123L)
                .errorClass("LATE_EVENT")
                .errorMessage("event arrived late")
                .originalPayloadBytes(payload)
                .failedAt(1_700_000_000_000L)
                .retryCount(0)
                .build();

        var expected = new HashMap<String, Object>();
        expected.put("event_id", DlqEventIds.of(envelope));
        expected.put("source_topic", "payment.flow.v1");
        expected.put("source_partition", 2);
        expected.put("source_offset", 123L);
        expected.put("error_class", "LATE_EVENT");
        expected.put("error_message", "event arrived late");
        expected.put("failed_at", 1_700_000_000_000L);
        expected.put("retry_count", 0);
        expected.put("original_payload_json", "{\"a\":1}");

        // when
        Map<String, Object> doc = DlqToOpenSearchDocMapper.toDocument(envelope);

        // then
        assertThat(doc).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldOmitOriginalPayloadJsonWhenBytesAreNull() {
        // given
        var envelope = DlqEnvelope.builder()
                .sourceTopic("topic.v1")
                .sourcePartition(0)
                .sourceOffset(0L)
                .errorClass("SINK_FAILURE")
                .errorMessage("opensearch 503")
                .failedAt(1_700_000_000_000L)
                .retryCount(2)
                .build();

        // when
        var doc = DlqToOpenSearchDocMapper.toDocument(envelope);

        // then
        assertThat(doc).doesNotContainKey("original_payload_json");
    }

    @Test
    void shouldRejectEnvelopeWithNullErrorMessage() {
        // given
        var envelope = DlqEnvelope.builder()
                .sourceTopic("topic.v1")
                .sourcePartition(0)
                .sourceOffset(0L)
                .errorClass("LATE_EVENT")
                .errorMessage(null)
                .failedAt(1L)
                .retryCount(0)
                .build();

        // when / then
        assertThatThrownBy(() -> DlqToOpenSearchDocMapper.toDocument(envelope))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("errorMessage");
    }
}
