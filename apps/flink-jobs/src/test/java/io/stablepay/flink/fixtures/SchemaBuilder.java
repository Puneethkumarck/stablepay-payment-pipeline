package io.stablepay.flink.fixtures;

import java.util.List;

import org.apache.avro.Schema;

final class SchemaBuilder {

    private SchemaBuilder() {}

    static Schema money() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "Money",
                  "namespace": "io.stablepay.events.common",
                  "fields": [
                    {"name": "amount_micros", "type": "long"},
                    {"name": "currency_code", "type": "string"}
                  ]
                }
                """);
    }

    static Schema envelope() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "EventEnvelope",
                  "namespace": "io.stablepay.events.common",
                  "fields": [
                    {"name": "event_id", "type": "string"},
                    {"name": "event_time", "type": {"type": "long", "logicalType": "timestamp-millis"}},
                    {"name": "ingest_time", "type": {"type": "long", "logicalType": "timestamp-millis"}},
                    {"name": "schema_version", "type": "string"},
                    {"name": "flow_id", "type": ["null", "string"], "default": null},
                    {"name": "correlation_id", "type": ["null", "string"], "default": null},
                    {"name": "trace_id", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }

    static Schema party() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "Party",
                  "namespace": "io.stablepay.events.common",
                  "fields": [
                    {"name": "name", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }

    static Schema payoutFiat(Schema envelope, Schema money, Schema party) {
        return record("PayoutFiatV1", "io.stablepay.events.payments",
                field("envelope", envelope),
                field("customer_id", Schema.create(Schema.Type.STRING)),
                field("account_id", Schema.create(Schema.Type.STRING)),
                field("amount", money),
                field("fee", nullable(money)),
                field("source_amount", nullable(money)),
                field("target_amount", nullable(money)),
                field("fx_rate", nullable(Schema.create(Schema.Type.DOUBLE))),
                field("internal_status", Schema.create(Schema.Type.STRING)),
                field("customer_status", nullable(Schema.create(Schema.Type.STRING))),
                field("screening_outcome", nullable(Schema.create(Schema.Type.STRING))),
                field("payout_reference", nullable(Schema.create(Schema.Type.STRING))),
                field("is_user_facing", nullable(Schema.create(Schema.Type.BOOLEAN))),
                field("chain", nullable(Schema.create(Schema.Type.STRING))),
                field("asset", nullable(Schema.create(Schema.Type.STRING))),
                field("source_address", nullable(Schema.create(Schema.Type.STRING))),
                field("destination_address", nullable(Schema.create(Schema.Type.STRING))),
                field("tx_hash", nullable(Schema.create(Schema.Type.STRING))),
                field("confirmations", nullable(Schema.create(Schema.Type.INT))),
                field("gas_fee_micros", nullable(Schema.create(Schema.Type.LONG))),
                field("block_number", nullable(Schema.create(Schema.Type.LONG))),
                field("block_timestamp", nullable(Schema.create(Schema.Type.LONG))),
                field("provider", nullable(Schema.create(Schema.Type.STRING))),
                field("route", nullable(Schema.create(Schema.Type.STRING))),
                field("beneficiary", nullable(party)),
                field("sender", nullable(party)),
                field("description", nullable(Schema.create(Schema.Type.STRING))),
                field("notes", nullable(Schema.create(Schema.Type.STRING))));
    }

    static Schema screeningResult(Schema envelope) {
        return record("ScreeningResultV1", "io.stablepay.events.screening",
                field("envelope", envelope),
                field("screening_id", nullable(Schema.create(Schema.Type.STRING))),
                field("customer_id", nullable(Schema.create(Schema.Type.STRING))),
                field("transaction_reference", nullable(Schema.create(Schema.Type.STRING))),
                field("outcome", nullable(Schema.create(Schema.Type.STRING))),
                field("provider", nullable(Schema.create(Schema.Type.STRING))),
                field("rule_triggered", nullable(Schema.create(Schema.Type.STRING))),
                field("score", nullable(Schema.create(Schema.Type.DOUBLE))),
                field("duration_ms", nullable(Schema.create(Schema.Type.LONG))));
    }

    private static Schema nullable(Schema schema) {
        return Schema.createUnion(Schema.create(Schema.Type.NULL), schema);
    }

    private static Schema record(String name, String namespace, Schema.Field... fields) {
        return Schema.createRecord(name, null, namespace, false, List.of(fields));
    }

    private static Schema.Field field(String name, Schema schema) {
        return new Schema.Field(name, schema, null, null);
    }
}
