package io.stablepay.flink.mapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;

import io.stablepay.flink.model.ValidatedEvent;

public final class EventToOpenSearchDocMapper {

    private EventToOpenSearchDocMapper() {}

    private static final Map<String, String> TOPIC_TO_EVENT_TYPE = Map.of(
            "payment.payout.fiat.v1", "PAYOUT_FIAT",
            "payment.payout.crypto.v1", "PAYOUT_CRYPTO",
            "payment.payin.fiat.v1", "PAYIN_FIAT",
            "payment.payin.crypto.v1", "PAYIN_CRYPTO",
            "payment.flow.v1", "PAYMENT_FLOW",
            "chain.transaction.v1", "CHAIN_TRANSACTION",
            "signing.request.v1", "SIGNING_REQUEST",
            "screening.result.v1", "SCREENING_RESULT",
            "approval.decision.v1", "APPROVAL_DECISION");

    public static Map<String, Object> toDocument(ValidatedEvent event) {
        var doc = new HashMap<String, Object>();
        var record = event.record();
        String topic = event.topic();

        doc.put("event_id", event.eventId());
        doc.put("event_time", event.eventTimeMillis());
        doc.put("schema_version", event.schemaVersion());
        doc.put("flow_id", event.flowId());

        extractEnvelopeFields(record, doc);

        String eventType = TOPIC_TO_EVENT_TYPE.getOrDefault(topic, "UNKNOWN");
        doc.put("event_type", eventType);
        doc.put("flow_type", deriveFlowType(topic));
        doc.put("direction", deriveDirection(topic));
        doc.put("is_crypto", isCrypto(topic));

        extractCommonPayloadFields(record, doc);
        extractMoneyFields(record, doc);
        extractCryptoFields(record, doc);
        extractTransactionReference(record, topic, doc);

        return doc;
    }

    private static void extractEnvelopeFields(GenericRecord record, Map<String, Object> doc) {
        var envelope = record.get("envelope");
        if (envelope instanceof GenericRecord env) {
            putIfPresent(doc, "ingest_time", env.get("ingest_time"));
            putIfPresent(doc, "correlation_id", env.get("correlation_id"));
            putIfPresent(doc, "trace_id", env.get("trace_id"));
        }
    }

    private static void extractCommonPayloadFields(GenericRecord record, Map<String, Object> doc) {
        putStringIfPresent(doc, "customer_id", record.get("customer_id"));
        putStringIfPresent(doc, "account_id", record.get("account_id"));
        putStringIfPresent(doc, "internal_status", record.get("internal_status"));
        putStringIfPresent(doc, "customer_status", record.get("customer_status"));
        putIfPresent(doc, "is_user_facing", record.get("is_user_facing"));
        putStringIfPresent(doc, "provider", record.get("provider"));
        putStringIfPresent(doc, "route", record.get("route"));
        putStringIfPresent(doc, "description", record.get("description"));
        putStringIfPresent(doc, "notes", record.get("notes"));
        putStringIfPresent(doc, "screening_outcome", record.get("screening_outcome"));
    }

    private static void extractMoneyFields(GenericRecord record, Map<String, Object> doc) {
        extractMoney(record, "amount", "amount_micros", "currency_code", doc);
        extractMoney(record, "fee", "fee_amount_micros", "fee_currency_code", doc);
        extractMoney(record, "source_amount", "source_amount_micros", "source_currency_code", doc);
        extractMoney(record, "target_amount", "target_amount_micros", "target_currency_code", doc);

        putIfPresent(doc, "fx_rate", record.get("fx_rate"));
    }

    private static void extractMoney(
            GenericRecord record, String field, String microsKey, String currencyKey, Map<String, Object> doc) {
        var money = record.get(field);
        if (money instanceof GenericRecord moneyRecord) {
            putIfPresent(doc, microsKey, moneyRecord.get("amount_micros"));
            putStringIfPresent(doc, currencyKey, moneyRecord.get("currency_code"));
        }
    }

    private static void extractCryptoFields(GenericRecord record, Map<String, Object> doc) {
        putStringIfPresent(doc, "chain", record.get("chain"));
        putStringIfPresent(doc, "asset", record.get("asset"));
        putStringIfPresent(doc, "source_address", record.get("source_address"));
        putStringIfPresent(doc, "destination_address", record.get("destination_address"));
        putStringIfPresent(doc, "tx_hash", record.get("tx_hash"));
        putIfPresent(doc, "confirmations", record.get("confirmations"));
        putIfPresent(doc, "gas_fee_micros", record.get("gas_fee_micros"));
        putIfPresent(doc, "block_number", record.get("block_number"));
        putIfPresent(doc, "block_timestamp", record.get("block_timestamp"));

        extractBeneficiary(record, doc);
        extractSender(record, doc);
    }

    private static void extractBeneficiary(GenericRecord record, Map<String, Object> doc) {
        var beneficiary = record.get("beneficiary");
        if (beneficiary instanceof GenericRecord party) {
            putStringIfPresent(doc, "beneficiary_name", party.get("name"));
        }
    }

    private static void extractSender(GenericRecord record, Map<String, Object> doc) {
        var sender = record.get("sender");
        if (sender instanceof GenericRecord party) {
            putStringIfPresent(doc, "sender_name", party.get("name"));
        }
    }

    private static void extractTransactionReference(GenericRecord record, String topic, Map<String, Object> doc) {
        String ref = null;
        if (topic.contains("payout")) {
            ref = stringOrNull(record.get("payout_reference"));
        } else if (topic.contains("payin")) {
            ref = stringOrNull(record.get("payin_reference"));
        } else if (topic.contains("flow")) {
            ref = stringOrNull(record.get("flow_id"));
        } else if (topic.contains("chain.transaction")) {
            ref = stringOrNull(record.get("tx_hash"));
        }
        if (ref != null) {
            doc.put("transaction_reference", ref);
        }
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

    private static void putIfPresent(Map<String, Object> doc, String key, Object value) {
        if (value != null) {
            doc.put(key, value);
        }
    }

    private static void putStringIfPresent(Map<String, Object> doc, String key, Object value) {
        if (value != null) {
            doc.put(key, value.toString());
        }
    }

    private static String stringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }
}
