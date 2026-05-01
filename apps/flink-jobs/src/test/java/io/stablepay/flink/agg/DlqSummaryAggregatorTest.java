package io.stablepay.flink.agg;

import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.flink.model.DlqEnvelope;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.junit.jupiter.api.Test;

class DlqSummaryAggregatorTest {

  @Test
  void shouldCountEventsAndTrackMaxRetry() {
    // given
    var aggregator = new DlqSummaryAggregator();
    var envelope1 =
        DlqEnvelope.builder()
            .sourceTopic("payment.payout.fiat.v1")
            .sourcePartition(0)
            .sourceOffset(1L)
            .errorClass("SchemaValidationException")
            .errorMessage("invalid")
            .failedAt(1_711_000_000_000L)
            .retryCount(2)
            .build();
    var envelope2 = envelope1.toBuilder().retryCount(5).build();

    // when
    var acc = aggregator.createAccumulator();
    acc = aggregator.add(envelope1, acc);
    acc = aggregator.add(envelope2, acc);
    var result = (GenericRowData) aggregator.getResult(acc);

    // then
    var expected = new GenericRowData(6);
    expected.setField(0, null);
    expected.setField(1, null);
    expected.setField(2, StringData.fromString("SchemaValidationException"));
    expected.setField(3, StringData.fromString("payment.payout.fiat.v1"));
    expected.setField(4, 2L);
    expected.setField(5, 5);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
