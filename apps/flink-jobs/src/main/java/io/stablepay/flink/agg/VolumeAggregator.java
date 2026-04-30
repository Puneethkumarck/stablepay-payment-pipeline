package io.stablepay.flink.agg;

import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;

import io.stablepay.flink.model.ValidatedEvent;

class VolumeAggregator implements AggregateFunction<ValidatedEvent, VolumeAccumulator, RowData> {

    static final String UNKNOWN = "UNKNOWN";

    @Override
    public VolumeAccumulator createAccumulator() {
        return new VolumeAccumulator(0L, 0L, UNKNOWN, UNKNOWN, UNKNOWN);
    }

    @Override
    public VolumeAccumulator add(ValidatedEvent event, VolumeAccumulator acc) {
        var record = event.toRecord();
        var amountMicros = extractAmountMicros(record);
        var flowType = deriveFlowType(event.topic());
        var direction = deriveDirection(event.topic());
        var currency = extractCurrencyCode(record);

        return acc.toBuilder()
                .totalAmountMicros(acc.totalAmountMicros() + amountMicros)
                .transactionCount(acc.transactionCount() + 1)
                .flowType(flowType)
                .direction(direction)
                .currencyCode(currency)
                .build();
    }

    @Override
    public RowData getResult(VolumeAccumulator acc) {
        var row = new GenericRowData(7);
        row.setField(0, null);
        row.setField(1, null);
        row.setField(2, StringData.fromString(acc.flowType()));
        row.setField(3, StringData.fromString(acc.direction()));
        row.setField(4, StringData.fromString(acc.currencyCode()));
        row.setField(5, acc.totalAmountMicros());
        row.setField(6, acc.transactionCount());
        return row;
    }

    @Override
    public VolumeAccumulator merge(VolumeAccumulator a, VolumeAccumulator b) {
        return a.toBuilder()
                .totalAmountMicros(a.totalAmountMicros() + b.totalAmountMicros())
                .transactionCount(a.transactionCount() + b.transactionCount())
                .flowType(preferKnown(a.flowType(), b.flowType()))
                .direction(preferKnown(a.direction(), b.direction()))
                .currencyCode(preferKnown(a.currencyCode(), b.currencyCode()))
                .build();
    }

    private static long extractAmountMicros(GenericRecord record) {
        var amount = record.get("amount");
        if (amount instanceof GenericRecord moneyRecord) {
            var micros = moneyRecord.get("amount_micros");
            if (micros instanceof Long l) return l;
            if (micros instanceof Number n) return n.longValue();
        }
        return 0L;
    }

    private static String extractCurrencyCode(GenericRecord record) {
        var amount = record.get("amount");
        if (amount instanceof GenericRecord moneyRecord) {
            var code = moneyRecord.get("currency_code");
            if (code != null) return code.toString();
        }
        return UNKNOWN;
    }

    private static String deriveFlowType(String topic) {
        if (topic.contains("crypto") || topic.contains("chain")) return "CRYPTO";
        if (topic.contains("fiat")) return "FIAT";
        return "MIXED";
    }

    private static String deriveDirection(String topic) {
        if (topic.contains("payin")) return "INBOUND";
        if (topic.contains("payout")) return "OUTBOUND";
        return "INTERNAL";
    }

    private static String preferKnown(String a, String b) {
        return UNKNOWN.equals(a) ? b : a;
    }
}
