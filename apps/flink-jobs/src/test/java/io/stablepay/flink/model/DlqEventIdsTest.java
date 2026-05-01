package io.stablepay.flink.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DlqEventIdsTest {

  private static DlqEnvelope envelope(
      String topic, int partition, long offset, long failedAt, String errorClass) {
    return DlqEnvelope.builder()
        .sourceTopic(topic)
        .sourcePartition(partition)
        .sourceOffset(offset)
        .errorClass(errorClass)
        .errorMessage("ignored")
        .failedAt(failedAt)
        .retryCount(0)
        .build();
  }

  @Test
  void shouldReturnSameIdForIdenticalSourceCoordinates() {
    // given
    var first = envelope("payment.flow.v1", 3, 42L, 1_700_000_000_000L, "LATE_EVENT");
    var second = envelope("payment.flow.v1", 3, 42L, 1_700_000_000_000L, "LATE_EVENT");

    // when
    var firstId = DlqEventIds.of(first);
    var secondId = DlqEventIds.of(second);

    // then
    assertThat(firstId).isEqualTo(secondId);
  }

  @Test
  void shouldReturnDifferentIdsWhenAnyCoordinateChanges() {
    // given
    var base = envelope("payment.flow.v1", 3, 42L, 1_700_000_000_000L, "LATE_EVENT");

    // when
    var baseId = DlqEventIds.of(base);
    var differentTopic =
        DlqEventIds.of(envelope("payment.flow.v2", 3, 42L, 1_700_000_000_000L, "LATE_EVENT"));
    var differentPartition =
        DlqEventIds.of(envelope("payment.flow.v1", 4, 42L, 1_700_000_000_000L, "LATE_EVENT"));
    var differentOffset =
        DlqEventIds.of(envelope("payment.flow.v1", 3, 43L, 1_700_000_000_000L, "LATE_EVENT"));
    var differentFailedAt =
        DlqEventIds.of(envelope("payment.flow.v1", 3, 42L, 1_700_000_000_001L, "LATE_EVENT"));
    var differentClass =
        DlqEventIds.of(
            envelope("payment.flow.v1", 3, 42L, 1_700_000_000_000L, "ILLEGAL_TRANSITION"));

    // then
    assertThat(baseId)
        .isNotEqualTo(differentTopic)
        .isNotEqualTo(differentPartition)
        .isNotEqualTo(differentOffset)
        .isNotEqualTo(differentFailedAt)
        .isNotEqualTo(differentClass);
  }

  @Test
  void shouldReturnRfc4122UuidString() {
    // given
    var env = envelope("payment.flow.v1", 0, 0L, 1L, "SCHEMA_INVALID");

    // when
    var id = DlqEventIds.of(env);

    // then
    assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  }
}
