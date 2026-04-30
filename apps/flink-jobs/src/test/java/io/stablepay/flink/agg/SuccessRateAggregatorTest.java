package io.stablepay.flink.agg;

import static io.stablepay.flink.fixtures.AggFixtures.fiatPayoutEvent;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.junit.jupiter.api.Test;

class SuccessRateAggregatorTest {

    @Test
    void shouldClassifyTerminalStatusesAndComputeSuccessRate() {
        // given
        var aggregator = new SuccessRateAggregator();
        var completed = fiatPayoutEvent(100L, "USD", "COMPLETED");
        var failed = fiatPayoutEvent(100L, "USD", "FAILED");
        var rejected = fiatPayoutEvent(100L, "USD", "REJECTED");
        var pending = fiatPayoutEvent(100L, "USD", "PENDING");

        // when
        var acc = aggregator.createAccumulator();
        for (var event : new io.stablepay.flink.model.ValidatedEvent[] {completed, failed, rejected, pending}) {
            acc = aggregator.add(event, acc);
        }
        var result = (GenericRowData) aggregator.getResult(acc);

        // then
        var expected = new GenericRowData(7);
        expected.setField(0, null);
        expected.setField(1, null);
        expected.setField(2, StringData.fromString("FIAT"));
        expected.setField(3, 4L);
        expected.setField(4, 1L);
        expected.setField(5, 2L);
        expected.setField(6, 0.25);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldEmitZeroSuccessRateForEmptyAccumulator() {
        // given
        var aggregator = new SuccessRateAggregator();

        // when
        var result = (GenericRowData) aggregator.getResult(aggregator.createAccumulator());

        // then
        var expected = new GenericRowData(7);
        expected.setField(0, null);
        expected.setField(1, null);
        expected.setField(2, StringData.fromString("UNKNOWN"));
        expected.setField(3, 0L);
        expected.setField(4, 0L);
        expected.setField(5, 0L);
        expected.setField(6, 0.0);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
}
