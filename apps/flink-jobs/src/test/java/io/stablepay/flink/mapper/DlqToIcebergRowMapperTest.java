package io.stablepay.flink.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.DlqEventIds;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.junit.jupiter.api.Test;

class DlqToIcebergRowMapperTest {

  @Test
  void shouldBuildRowDataFromEnvelopeWithDeterministicEventId() {
    // given
    var payload = "{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8);
    var envelope =
        DlqEnvelope.builder()
            .sourceTopic("payment.flow.v1")
            .sourcePartition(2)
            .sourceOffset(123L)
            .errorClass("LATE_EVENT")
            .errorMessage("event arrived late")
            .originalPayloadBytes(payload)
            .failedAt(1_700_000_000_000L)
            .retryCount(1)
            .build();

    var expected = new GenericRowData(9);
    expected.setField(0, StringData.fromString(DlqEventIds.of(envelope)));
    expected.setField(1, StringData.fromString("payment.flow.v1"));
    expected.setField(2, 2);
    expected.setField(3, 123L);
    expected.setField(4, StringData.fromString("LATE_EVENT"));
    expected.setField(5, StringData.fromString("event arrived late"));
    expected.setField(6, TimestampData.fromInstant(Instant.ofEpochMilli(1_700_000_000_000L)));
    expected.setField(7, 1);
    expected.setField(8, StringData.fromString("{\"k\":\"v\"}"));

    // when
    var actual = DlqToIcebergRowMapper.toRowData(envelope);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldEmitNullPayloadFieldWhenBytesAreNull() {
    // given
    var envelope =
        DlqEnvelope.builder()
            .sourceTopic("topic.v1")
            .sourcePartition(0)
            .sourceOffset(0L)
            .errorClass("SINK_FAILURE")
            .errorMessage("opensearch 503")
            .failedAt(1L)
            .retryCount(0)
            .build();

    // when
    var actual = (GenericRowData) DlqToIcebergRowMapper.toRowData(envelope);

    // then
    assertThat(actual.getField(8)).isNull();
  }

  @Test
  void shouldRejectEnvelopeWithNullSourceTopic() {
    // given
    var envelope =
        DlqEnvelope.builder()
            .sourceTopic(null)
            .sourcePartition(0)
            .sourceOffset(0L)
            .errorClass("LATE_EVENT")
            .errorMessage("late")
            .failedAt(1L)
            .retryCount(0)
            .build();

    // when / then
    assertThatThrownBy(() -> DlqToIcebergRowMapper.toRowData(envelope))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("sourceTopic");
  }
}
