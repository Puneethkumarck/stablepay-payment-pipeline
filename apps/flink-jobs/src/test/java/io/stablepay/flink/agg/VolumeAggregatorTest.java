package io.stablepay.flink.agg;

import static io.stablepay.flink.fixtures.AggFixtures.fiatPayoutEvent;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.junit.jupiter.api.Test;

class VolumeAggregatorTest {

    @Test
    void shouldAggregateAmountAndCountAcrossEvents() {
        // given
        var aggregator = new VolumeAggregator();
        var event1 = fiatPayoutEvent(1_000_000L, "USD", "COMPLETED");
        var event2 = fiatPayoutEvent(2_500_000L, "USD", "FAILED");

        // when
        var acc = aggregator.createAccumulator();
        acc = aggregator.add(event1, acc);
        acc = aggregator.add(event2, acc);
        var result = (GenericRowData) aggregator.getResult(acc);

        // then
        var expected = new GenericRowData(7);
        expected.setField(0, null);
        expected.setField(1, null);
        expected.setField(2, StringData.fromString("FIAT"));
        expected.setField(3, StringData.fromString("OUTBOUND"));
        expected.setField(4, StringData.fromString("USD"));
        expected.setField(5, 3_500_000L);
        expected.setField(6, 2L);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMergeAccumulatorsAndPreferKnownKeyingFields() {
        // given
        var aggregator = new VolumeAggregator();
        var populated = aggregator.add(
                fiatPayoutEvent(1_000_000L, "EUR", "COMPLETED"),
                aggregator.createAccumulator());
        var empty = aggregator.createAccumulator();

        // when
        var merged = aggregator.merge(empty, populated);

        // then
        var expected = VolumeAccumulator.builder()
                .totalAmountMicros(1_000_000L)
                .transactionCount(1L)
                .flowType("FIAT")
                .direction("OUTBOUND")
                .currencyCode("EUR")
                .build();
        assertThat(merged).usingRecursiveComparison().isEqualTo(expected);
    }
}
