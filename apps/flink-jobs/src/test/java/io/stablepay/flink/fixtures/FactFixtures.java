package io.stablepay.flink.fixtures;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

import io.stablepay.flink.model.ValidatedEvent;

public final class FactFixtures {

    public static final long SOME_EVENT_TIME_MILLIS = 1_711_000_000_000L;
    public static final long SOME_INGEST_TIME_MILLIS = 1_711_000_000_500L;
    public static final String SOME_EVENT_ID = "evt-fact-001";
    public static final String SOME_FLOW_ID = "flow-fact-001";
    public static final String SOME_SCHEMA_VERSION = "1.0.0";
    public static final String SOME_CUSTOMER_ID = "cust-a1b2c3d4-e5f6-7890";
    public static final String SOME_ACCOUNT_ID = "acc-x1y2z3w4-5678-9012";
    public static final String SOME_CORRELATION_ID = "corr-001";
    public static final String SOME_TRACE_ID = "trace-001";

    private static final Schema MONEY_SCHEMA = SchemaBuilder.money();
    private static final Schema ENVELOPE_SCHEMA = SchemaBuilder.envelope();
    private static final Schema PARTY_SCHEMA = SchemaBuilder.party();
    private static final Schema PAYOUT_FIAT_SCHEMA =
            SchemaBuilder.payoutFiat(ENVELOPE_SCHEMA, MONEY_SCHEMA, PARTY_SCHEMA);
    private static final Schema SCREENING_SCHEMA =
            SchemaBuilder.screeningResult(ENVELOPE_SCHEMA);

    private FactFixtures() {}

    public static ValidatedEvent fiatPayoutEvent(
            long amountMicros, String currency, String internalStatus) {
        var envelope = envelope(SOME_CORRELATION_ID, SOME_TRACE_ID);
        var money = money(amountMicros, currency);
        var beneficiary = party("Jane Beneficiary");
        var sender = party("John Sender");

        var record = new GenericRecordBuilder(PAYOUT_FIAT_SCHEMA)
                .set("envelope", envelope)
                .set("customer_id", SOME_CUSTOMER_ID)
                .set("account_id", SOME_ACCOUNT_ID)
                .set("amount", money)
                .set("fee", null)
                .set("source_amount", null)
                .set("target_amount", null)
                .set("fx_rate", null)
                .set("internal_status", internalStatus)
                .set("customer_status", "PROCESSING")
                .set("screening_outcome", null)
                .set("payout_reference", "PAY-REF-001")
                .set("is_user_facing", null)
                .set("chain", null)
                .set("asset", null)
                .set("source_address", null)
                .set("destination_address", null)
                .set("tx_hash", null)
                .set("confirmations", null)
                .set("gas_fee_micros", null)
                .set("block_number", null)
                .set("block_timestamp", null)
                .set("provider", "provider-x")
                .set("route", "route-1")
                .set("beneficiary", beneficiary)
                .set("sender", sender)
                .set("description", "Payment to vendor")
                .set("notes", "Monthly invoice")
                .build();

        return ValidatedEvent.fromRecord(
                "payment.payout.fiat.v1", 0, 0L, "key-1", record,
                SOME_EVENT_ID, SOME_EVENT_TIME_MILLIS, SOME_FLOW_ID, SOME_SCHEMA_VERSION);
    }

    public static ValidatedEvent screeningEvent(
            String screeningId, String customerId, String outcome,
            String provider, double score, long durationMs) {
        var envelope = envelope(null, null);
        var record = new GenericRecordBuilder(SCREENING_SCHEMA)
                .set("envelope", envelope)
                .set("screening_id", screeningId)
                .set("customer_id", customerId)
                .set("transaction_reference", "TX-REF-001")
                .set("outcome", outcome)
                .set("provider", provider)
                .set("rule_triggered", "RULE-01")
                .set("score", score)
                .set("duration_ms", durationMs)
                .build();

        return ValidatedEvent.fromRecord(
                "screening.result.v1", 0, 0L, "key-1", record,
                SOME_EVENT_ID, SOME_EVENT_TIME_MILLIS, SOME_FLOW_ID, SOME_SCHEMA_VERSION);
    }

    private static GenericRecord envelope(String correlationId, String traceId) {
        var rec = new GenericData.Record(ENVELOPE_SCHEMA);
        rec.put("event_id", SOME_EVENT_ID);
        rec.put("event_time", SOME_EVENT_TIME_MILLIS);
        rec.put("ingest_time", SOME_INGEST_TIME_MILLIS);
        rec.put("schema_version", SOME_SCHEMA_VERSION);
        rec.put("flow_id", SOME_FLOW_ID);
        rec.put("correlation_id", correlationId);
        rec.put("trace_id", traceId);
        return rec;
    }

    private static GenericRecord money(long amountMicros, String currency) {
        return new GenericRecordBuilder(MONEY_SCHEMA)
                .set("amount_micros", amountMicros)
                .set("currency_code", currency)
                .build();
    }

    private static GenericRecord party(String name) {
        return new GenericRecordBuilder(PARTY_SCHEMA)
                .set("name", name)
                .build();
    }
}
