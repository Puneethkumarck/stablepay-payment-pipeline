package io.stablepay.api.infrastructure.trino;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.domain.model.DlqEvent;
import io.stablepay.api.domain.model.fixtures.DlqEventFixtures;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrinoDlqRepositoryTest {

  @Mock ResultSet rs;

  @Test
  void shouldBuildDlqEventWhenAllColumnsPresent() throws Exception {
    // given
    given(rs.getString("dlq_id")).willReturn(DlqEventFixtures.SOME_DLQ_ID.value().toString());
    given(rs.getString("error_class")).willReturn("DeserializationException");
    given(rs.getString("source_topic")).willReturn("crypto.payin.events");
    given(rs.getInt("source_partition")).willReturn(2);
    given(rs.getLong("source_offset")).willReturn(12345L);
    given(rs.getString("error_message")).willReturn("Failed to deserialize Avro payload");
    given(rs.getTimestamp("failed_at"))
        .willReturn(Timestamp.from(DlqEventFixtures.SOME_DLQ_FAILED_AT));
    given(rs.getInt("retry_count")).willReturn(1);
    given(rs.getString("sink_type")).willReturn("opensearch");
    given(rs.getTimestamp("watermark_at"))
        .willReturn(Timestamp.from(DlqEventFixtures.SOME_DLQ_WATERMARK_AT));
    given(rs.getString("original_payload_json")).willReturn("{\"reference\":\"TXN-REF-9999\"}");
    var expected = DlqEventFixtures.SOME_DLQ_EVENT;

    // when
    var actual = TrinoDlqRepository.DLQ_ROW_MAPPER.mapRow(rs, 0);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldBuildDlqEventWithEmptyOptionalsWhenNullableColumnsAreNull() throws Exception {
    // given
    given(rs.getString("dlq_id")).willReturn(DlqEventFixtures.SOME_DLQ_ID.value().toString());
    given(rs.getString("error_class")).willReturn("DeserializationException");
    given(rs.getString("source_topic")).willReturn("crypto.payin.events");
    given(rs.getInt("source_partition")).willReturn(0);
    given(rs.getLong("source_offset")).willReturn(0L);
    given(rs.getString("error_message")).willReturn("err");
    given(rs.getTimestamp("failed_at"))
        .willReturn(Timestamp.from(DlqEventFixtures.SOME_DLQ_FAILED_AT));
    given(rs.getInt("retry_count")).willReturn(0);
    given(rs.getString("sink_type")).willReturn(null);
    given(rs.getTimestamp("watermark_at")).willReturn(null);
    given(rs.getString("original_payload_json")).willReturn(null);
    var expected =
        DlqEvent.builder()
            .id(DlqEventFixtures.SOME_DLQ_ID)
            .errorClass("DeserializationException")
            .sourceTopic("crypto.payin.events")
            .sourcePartition(0)
            .sourceOffset(0L)
            .errorMessage("err")
            .failedAt(DlqEventFixtures.SOME_DLQ_FAILED_AT)
            .retryCount(0)
            .sinkType(Optional.empty())
            .watermarkAt(Optional.empty())
            .originalPayloadJson(Optional.empty())
            .build();

    // when
    var actual = TrinoDlqRepository.DLQ_ROW_MAPPER.mapRow(rs, 0);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldRoundTripCursorEncodeAndDecode() {
    // given
    var failedAtMillis = DlqEventFixtures.SOME_DLQ_FAILED_AT.toEpochMilli();
    var dlqIdString = DlqEventFixtures.SOME_DLQ_ID.value().toString();
    var encoded = TrinoDlqRepository.encodeCursor(failedAtMillis, dlqIdString);
    var expected =
        TrinoDlqCursor.builder()
            .failedAt(DlqEventFixtures.SOME_DLQ_FAILED_AT)
            .dlqId(dlqIdString)
            .build();

    // when
    var actual = TrinoDlqRepository.decodeCursor(encoded);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
