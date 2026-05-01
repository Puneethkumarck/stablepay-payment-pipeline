package io.stablepay.flink.fixtures;

import io.stablepay.flink.model.ValidatedEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

public final class AggFixtures {

  public static final long SOME_EVENT_TIME_MILLIS = 1_711_000_000_000L;
  public static final long SOME_INGEST_TIME_MILLIS = 1_711_000_000_500L;
  public static final String SOME_EVENT_ID = "evt-001";
  public static final String SOME_FLOW_ID = "flow-001";
  public static final String SOME_SCHEMA_VERSION = "1.0.0";

  private static final Schema MONEY_SCHEMA = SchemaBuilder.money();
  private static final Schema ENVELOPE_SCHEMA = SchemaBuilder.envelope();
  private static final Schema PAYOUT_FIAT_SCHEMA =
      SchemaBuilder.payoutFiat(ENVELOPE_SCHEMA, MONEY_SCHEMA);
  private static final Schema PAYOUT_CRYPTO_SCHEMA =
      SchemaBuilder.payoutCrypto(ENVELOPE_SCHEMA, MONEY_SCHEMA);
  private static final Schema SCREENING_SCHEMA = SchemaBuilder.screening(ENVELOPE_SCHEMA);

  private AggFixtures() {}

  public static ValidatedEvent fiatPayoutEvent(
      long amountMicros, String currency, String internalStatus) {
    var envelope = envelope();
    var money =
        new GenericRecordBuilder(MONEY_SCHEMA)
            .set("amount_micros", amountMicros)
            .set("currency_code", currency)
            .build();
    var record =
        new GenericRecordBuilder(PAYOUT_FIAT_SCHEMA)
            .set("envelope", envelope)
            .set("amount", money)
            .set("internal_status", internalStatus)
            .build();
    return ValidatedEvent.fromRecord(
        "payment.payout.fiat.v1",
        0,
        0L,
        "key-1",
        record,
        SOME_EVENT_ID,
        SOME_EVENT_TIME_MILLIS,
        SOME_FLOW_ID,
        SOME_SCHEMA_VERSION);
  }

  public static ValidatedEvent cryptoPayinEvent(long amountMicros, String currency) {
    var envelope = envelope();
    var money =
        new GenericRecordBuilder(MONEY_SCHEMA)
            .set("amount_micros", amountMicros)
            .set("currency_code", currency)
            .build();
    var record =
        new GenericRecordBuilder(PAYOUT_CRYPTO_SCHEMA)
            .set("envelope", envelope)
            .set("amount", money)
            .set("internal_status", "PENDING")
            .build();
    return ValidatedEvent.fromRecord(
        "payment.payin.crypto.v1",
        0,
        0L,
        "key-1",
        record,
        SOME_EVENT_ID,
        SOME_EVENT_TIME_MILLIS,
        SOME_FLOW_ID,
        SOME_SCHEMA_VERSION);
  }

  public static ValidatedEvent screeningEvent(
      String outcome, String provider, double riskScore, long eventTime, long ingestTime) {
    var envelope = envelope(eventTime, ingestTime);
    var record =
        new GenericRecordBuilder(SCREENING_SCHEMA)
            .set("envelope", envelope)
            .set("outcome", outcome)
            .set("provider", provider)
            .set("risk_score", riskScore)
            .build();
    return ValidatedEvent.fromRecord(
        "screening.result.v1",
        0,
        0L,
        "key-1",
        record,
        SOME_EVENT_ID,
        eventTime,
        SOME_FLOW_ID,
        SOME_SCHEMA_VERSION);
  }

  private static GenericRecord envelope() {
    return envelope(SOME_EVENT_TIME_MILLIS, SOME_INGEST_TIME_MILLIS);
  }

  private static GenericRecord envelope(long eventTime, long ingestTime) {
    var rec = new GenericData.Record(ENVELOPE_SCHEMA);
    rec.put("event_id", SOME_EVENT_ID);
    rec.put("event_time", eventTime);
    rec.put("ingest_time", ingestTime);
    rec.put("schema_version", SOME_SCHEMA_VERSION);
    rec.put("flow_id", SOME_FLOW_ID);
    return rec;
  }
}
