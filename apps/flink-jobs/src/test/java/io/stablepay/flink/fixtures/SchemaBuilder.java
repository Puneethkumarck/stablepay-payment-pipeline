package io.stablepay.flink.fixtures;

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

    static Schema payoutFiat(Schema envelope, Schema money) {
        return record("PayoutFiatV1", "io.stablepay.events.payments",
                field("envelope", envelope),
                field("amount", money),
                field("internal_status", Schema.create(Schema.Type.STRING)));
    }

    static Schema payoutCrypto(Schema envelope, Schema money) {
        return record("PayoutCryptoV1", "io.stablepay.events.payments",
                field("envelope", envelope),
                field("amount", money),
                field("internal_status", Schema.create(Schema.Type.STRING)));
    }

    static Schema screening(Schema envelope) {
        return record("ScreeningResultV1", "io.stablepay.events.screening",
                field("envelope", envelope),
                field("outcome", Schema.create(Schema.Type.STRING)),
                field("provider", Schema.create(Schema.Type.STRING)),
                field("risk_score", Schema.create(Schema.Type.DOUBLE)));
    }

    private static Schema record(String name, String namespace, Schema.Field... fields) {
        return Schema.createRecord(name, null, namespace, false, java.util.List.of(fields));
    }

    private static Schema.Field field(String name, Schema schema) {
        return new Schema.Field(name, schema, null, null);
    }
}
