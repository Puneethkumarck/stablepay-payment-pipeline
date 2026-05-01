package io.stablepay.flink;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FactFlowsMergeJobTest {

  @Nested
  class EscapeSql {

    @Test
    void shouldEscapeSingleQuotes() {
      // when
      var result = FactFlowsMergeJob.escapeSql("it's a test");

      // then
      assertThat(result).isEqualTo("it''s a test");
    }

    @Test
    void shouldReturnEmptyStringForNull() {
      // when
      var result = FactFlowsMergeJob.escapeSql(null);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnValueUnchangedWhenNoQuotes() {
      // when
      var result = FactFlowsMergeJob.escapeSql("jdbc:postgresql://host:5432/db");

      // then
      assertThat(result).isEqualTo("jdbc:postgresql://host:5432/db");
    }

    @Test
    void shouldEscapeMultipleSingleQuotes() {
      // when
      var result = FactFlowsMergeJob.escapeSql("a'b'c");

      // then
      assertThat(result).isEqualTo("a''b''c");
    }
  }

  @Nested
  class BuildInsertOverwriteSql {

    @Test
    void shouldTargetFactFlowsTable() {
      // when
      var sql = FactFlowsMergeJob.buildInsertOverwriteSql();

      // then
      assertThat(sql).contains("INSERT OVERWRITE facts.fact_flows");
    }

    @Test
    void shouldJoinOnFlowId() {
      // when
      var sql = FactFlowsMergeJob.buildInsertOverwriteSql();

      // then
      assertThat(sql).contains("ON f.flow_id = aggs.flow_id");
      assertThat(sql).contains("ON f.flow_id = pi.flow_id");
      assertThat(sql).contains("ON f.flow_id = tr.flow_id");
      assertThat(sql).contains("ON f.flow_id = po.flow_id");
    }

    @Test
    void shouldIncludeBothFiatAndCryptoPayinSources() {
      // when
      var sql = FactFlowsMergeJob.buildInsertOverwriteSql();

      // then
      assertThat(sql).contains("raw.raw_payment_payin_fiat");
      assertThat(sql).contains("raw.raw_payment_payin_crypto");
    }

    @Test
    void shouldIncludeBothFiatAndCryptoPayoutSources() {
      // when
      var sql = FactFlowsMergeJob.buildInsertOverwriteSql();

      // then
      assertThat(sql).contains("raw.raw_payment_payout_fiat");
      assertThat(sql).contains("raw.raw_payment_payout_crypto");
    }

    @Test
    void shouldUseDeterministicRowNumberForLatestRow() {
      // when
      var sql = FactFlowsMergeJob.buildInsertOverwriteSql();

      // then
      assertThat(sql).contains("ROW_NUMBER()");
      assertThat(sql).contains("ORDER BY event_time DESC");
      assertThat(sql).doesNotContain("LAST_VALUE");
    }

    @Test
    void shouldComputeTotalDurationMs() {
      // when
      var sql = FactFlowsMergeJob.buildInsertOverwriteSql();

      // then
      assertThat(sql).contains("TIMESTAMPDIFF(SECOND, aggs.initiated_at, aggs.completed_at)");
    }
  }
}
