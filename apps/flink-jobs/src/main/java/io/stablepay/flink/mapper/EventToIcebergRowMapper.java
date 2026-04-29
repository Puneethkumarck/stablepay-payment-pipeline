package io.stablepay.flink.mapper;

import java.time.Instant;

import org.apache.avro.generic.GenericRecord;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;

import io.stablepay.flink.catalog.IcebergCatalogConfig;
import io.stablepay.flink.model.ValidatedEvent;

public final class EventToIcebergRowMapper {

    private EventToIcebergRowMapper() {}

    public static String resolveTable(ValidatedEvent event) {
        return IcebergCatalogConfig.TOPIC_TO_TABLE.getOrDefault(event.topic(), null);
    }

    public static RowData toRowData(ValidatedEvent event) {
        GenericRecord record = event.record();
        boolean isChainTx = "chain.transaction.v1".equals(event.topic());

        GenericRowData row = new GenericRowData(isChainTx ? 11 : 10);

        row.setField(0, StringData.fromString(event.eventId()));
        row.setField(1, TimestampData.fromInstant(Instant.ofEpochMilli(event.eventTimeMillis())));

        long ingestTime = extractIngestTime(record);
        row.setField(2, TimestampData.fromInstant(Instant.ofEpochMilli(ingestTime)));

        row.setField(3, stringDataOrNull(event.schemaVersion()));
        row.setField(4, stringDataOrNull(event.flowId()));
        row.setField(5, stringDataOrNull(extractCorrelationId(record)));
        row.setField(6, stringDataOrNull(extractTraceId(record)));
        row.setField(7, StringData.fromString(event.topic()));
        row.setField(8, stringDataOrNull(event.key()));
        row.setField(9, StringData.fromString(record.toString()));

        if (isChainTx) {
            var txHash = record.get("tx_hash");
            row.setField(10, StringData.fromString(txHash != null ? txHash.toString() : ""));
        }

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

    private static String extractCorrelationId(GenericRecord record) {
        var envelope = record.get("envelope");
        if (envelope instanceof GenericRecord env) {
            var val = env.get("correlation_id");
            return val != null ? val.toString() : null;
        }
        return null;
    }

    private static String extractTraceId(GenericRecord record) {
        var envelope = record.get("envelope");
        if (envelope instanceof GenericRecord env) {
            var val = env.get("trace_id");
            return val != null ? val.toString() : null;
        }
        return null;
    }

    private static StringData stringDataOrNull(String value) {
        return value != null ? StringData.fromString(value) : null;
    }
}
