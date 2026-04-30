package io.stablepay.flink.agg;

import static io.stablepay.flink.fixtures.AggFixtures.screeningEvent;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.junit.jupiter.api.Test;

class ScreeningOutcomesAggregatorTest {

    @Test
    void shouldReadAvroFieldsAndComputeAverages() {
        // given
        var aggregator = new ScreeningOutcomesAggregator();
        var event1 = screeningEvent("CLEARED", "ACME", 0.10, 1_000L, 1_500L);
        var event2 = screeningEvent("CLEARED", "ACME", 0.30, 2_000L, 3_000L);

        // when
        var acc = aggregator.createAccumulator();
        acc = aggregator.add(event1, acc);
        acc = aggregator.add(event2, acc);
        var result = (GenericRowData) aggregator.getResult(acc);

        // then
        var expected = new GenericRowData(6);
        expected.setField(0, null);
        expected.setField(1, StringData.fromString("CLEARED"));
        expected.setField(2, StringData.fromString("ACME"));
        expected.setField(3, 2L);
        expected.setField(4, 750.0);
        expected.setField(5, 0.20);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
}
