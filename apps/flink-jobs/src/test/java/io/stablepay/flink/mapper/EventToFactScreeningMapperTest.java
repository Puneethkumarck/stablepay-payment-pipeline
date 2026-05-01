package io.stablepay.flink.mapper;

import static io.stablepay.flink.fixtures.FactFixtures.SOME_EVENT_ID;
import static io.stablepay.flink.fixtures.FactFixtures.SOME_EVENT_TIME_MILLIS;
import static io.stablepay.flink.fixtures.FactFixtures.screeningEvent;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EventToFactScreeningMapperTest {

  @Test
  void shouldProduceRowDataWith10Fields() {
    // given
    var event = screeningEvent("SCR-001", "cust-001", "PASS", "chainalysis", 0.1, 250L);

    // when
    var result = (GenericRowData) EventToFactScreeningMapper.toRowData(event);

    // then
    assertThat(result.getArity()).isEqualTo(10);
  }

  @Test
  void shouldMapAllFieldsCorrectly() {
    // given
    var event = screeningEvent("SCR-001", "cust-001", "PASS", "chainalysis", 0.85, 250L);

    // when
    var result = (GenericRowData) EventToFactScreeningMapper.toRowData(event);

    // then
    var expected = new GenericRowData(10);
    expected.setField(0, StringData.fromString(SOME_EVENT_ID));
    expected.setField(1, StringData.fromString("SCR-001"));
    expected.setField(2, StringData.fromString("cust-001"));
    expected.setField(3, StringData.fromString("TX-REF-001"));
    expected.setField(4, StringData.fromString("PASS"));
    expected.setField(5, StringData.fromString("chainalysis"));
    expected.setField(6, StringData.fromString("RULE-01"));
    expected.setField(7, 0.85);
    expected.setField(8, TimestampData.fromInstant(Instant.ofEpochMilli(SOME_EVENT_TIME_MILLIS)));
    expected.setField(9, 250L);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Nested
  class CustomerIdIsNotMasked {

    @Test
    void shouldStoreCustomerIdUnmasked() {
      // given
      var customerId = "a1b2c3d4-e5f6-7890-abcd-1234";
      var event = screeningEvent("SCR-001", customerId, "PASS", "provider", 0.5, 100L);

      // when
      var result = (GenericRowData) EventToFactScreeningMapper.toRowData(event);

      // then
      assertThat(result.getString(2).toString()).isEqualTo(customerId);
    }
  }

  @Nested
  class ScoreUsesDouble {

    @Test
    void shouldStoreScoreAsDouble() {
      // given
      var event = screeningEvent("SCR-001", "cust-001", "FLAG", "provider", 0.95, 100L);

      // when
      var result = (GenericRowData) EventToFactScreeningMapper.toRowData(event);

      // then
      assertThat(result.getDouble(7)).isEqualTo(0.95);
    }
  }

  @Nested
  class DurationUsesLong {

    @Test
    void shouldStoreDurationMsAsLong() {
      // given
      var event = screeningEvent("SCR-001", "cust-001", "PASS", "provider", 0.1, 3500L);

      // when
      var result = (GenericRowData) EventToFactScreeningMapper.toRowData(event);

      // then
      assertThat(result.getLong(9)).isEqualTo(3500L);
    }
  }
}
