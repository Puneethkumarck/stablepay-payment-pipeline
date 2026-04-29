package io.stablepay.flink.mapper;

import java.time.Instant;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;

import io.stablepay.flink.model.ValidatedEvent;

public final class EventToFactTransactionMapper {

    private EventToFactTransactionMapper() {}

    private static final Map<String, String> TOPIC_TO_EVENT_TYPE = Map.of(
            "payment.payout.fiat.v1", "PAYOUT_FIAT",
            "payment.payout.crypto.v1", "PAYOUT_CRYPTO",
            "payment.payin.fiat.v1", "PAYIN_FIAT",
            "payment.payin.crypto.v1", "PAYIN_CRYPTO",
            "chain.transaction.v1", "CHAIN_TRANSACTION");

    public static RowData toRowData(ValidatedEvent event) {
        var record = event.toRecord();
        var topic = event.topic();
        var row = new GenericRowData(41);

        row.setField(0, StringData.fromString(event.eventId()));
        row.setField(1, TimestampData.fromInstant(Instant.ofEpochMilli(event.eventTimeMillis())));
        row.setField(2, TimestampData.fromInstant(Instant.ofEpochMilli(extractIngestTime(record))));
        row.setField(3, stringDataOrNull(event.flowId()));
        row.setField(4, stringDataOrNull(extractEnvelopeField(record, "correlation_id")));
        row.setField(5, stringDataOrNull(extractEnvelopeField(record, "trace_id")));
        row.setField(6, StringData.fromString(TOPIC_TO_EVENT_TYPE.getOrDefault(topic, "UNKNOWN")));
        row.setField(7, StringData.fromString(deriveFlowType(topic)));
        row.setField(8, StringData.fromString(deriveDirection(topic)));
        row.setField(9, isCrypto(topic));
        row.setField(10, extractBoolean(record, "is_user_facing"));
        row.setField(11, stringDataOrNull(extractTransactionReference(record, topic)));
        row.setField(12, stringDataOrNull(stringVal(record.get("customer_id"))));
        row.setField(13, stringDataOrNull(stringVal(record.get("account_id"))));

        extractMoneyField(record, "amount", row, 14, 15);
        extractMoneyField(record, "fee", row, 16, 17);
        extractMoneyField(record, "source_amount", row, 18, 19);
        extractMoneyField(record, "target_amount", row, 20, 21);

        var fxRate = record.get("fx_rate");
        row.setField(22, fxRate instanceof Number n ? n.doubleValue() : null);

        row.setField(23, stringDataOrNull(stringVal(record.get("internal_status"))));
        row.setField(24, stringDataOrNull(stringVal(record.get("customer_status"))));
        row.setField(25, stringDataOrNull(stringVal(record.get("screening_outcome"))));
        row.setField(26, stringDataOrNull(stringVal(record.get("chain"))));
        row.setField(27, stringDataOrNull(stringVal(record.get("asset"))));
        row.setField(28, stringDataOrNull(stringVal(record.get("source_address"))));
        row.setField(29, stringDataOrNull(stringVal(record.get("destination_address"))));
        row.setField(30, stringDataOrNull(stringVal(record.get("tx_hash"))));

        var confirmations = record.get("confirmations");
        row.setField(31, confirmations instanceof Number n ? n.intValue() : null);

        var gasFee = record.get("gas_fee_micros");
        row.setField(32, gasFee instanceof Number n ? n.longValue() : null);

        var blockNumber = record.get("block_number");
        row.setField(33, blockNumber instanceof Number n ? n.longValue() : null);

        var blockTimestamp = record.get("block_timestamp");
        row.setField(34, blockTimestamp instanceof Long ts
                ? TimestampData.fromInstant(Instant.ofEpochMilli(ts)) : null);

        row.setField(35, stringDataOrNull(stringVal(record.get("provider"))));
        row.setField(36, stringDataOrNull(stringVal(record.get("route"))));

        row.setField(37, extractNestedName(record, "beneficiary"));
        row.setField(38, extractNestedName(record, "sender"));

        row.setField(39, stringDataOrNull(stringVal(record.get("description"))));
        row.setField(40, stringDataOrNull(stringVal(record.get("notes"))));

        return row;
    }

    private static long extractIngestTime(GenericRecord record) {
        var envelope = record.get("envelope");
        if (envelope instanceof GenericRecord env) {
            var ingestTime = env.get("ingest_time");
            if (ingestTime instanceof Long l) return l;
        }
        return Instant.now().toEpochMilli();
    }

    private static String extractEnvelopeField(GenericRecord record, String field) {
        var envelope = record.get("envelope");
        if (envelope instanceof GenericRecord env) {
            var val = env.get(field);
            return val != null ? val.toString() : null;
        }
        return null;
    }

    private static void extractMoneyField(
            GenericRecord record, String field, GenericRowData row,
            int microsIndex, int currencyIndex) {
        var money = record.get(field);
        if (money instanceof GenericRecord moneyRecord) {
            var micros = moneyRecord.get("amount_micros");
            row.setField(microsIndex, micros instanceof Number n ? n.longValue() : null);
            row.setField(currencyIndex, stringDataOrNull(stringVal(moneyRecord.get("currency_code"))));
        } else {
            row.setField(microsIndex, null);
            row.setField(currencyIndex, null);
        }
    }

    private static Boolean extractBoolean(GenericRecord record, String field) {
        var val = record.get(field);
        return val instanceof Boolean b ? b : null;
    }

    private static StringData extractNestedName(GenericRecord record, String field) {
        var nested = record.get(field);
        if (nested instanceof GenericRecord party) {
            return stringDataOrNull(stringVal(party.get("name")));
        }
        return null;
    }

    private static String extractTransactionReference(GenericRecord record, String topic) {
        if (topic.contains("payout")) return stringVal(record.get("payout_reference"));
        if (topic.contains("payin")) return stringVal(record.get("payin_reference"));
        if (topic.contains("chain.transaction")) return stringVal(record.get("tx_hash"));
        return stringVal(record.get("flow_id"));
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

    private static boolean isCrypto(String topic) {
        return topic.contains("crypto") || topic.contains("chain");
    }

    private static StringData stringDataOrNull(String value) {
        return value != null ? StringData.fromString(value) : null;
    }

    private static String stringVal(Object value) {
        return value != null ? value.toString() : null;
    }
}
