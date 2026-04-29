package io.stablepay.flink.config;

import java.util.List;

public final class FlinkConfig {

    private FlinkConfig() {}

    public static final List<String> INPUT_TOPICS = List.of(
            "payment.payout.fiat.v1",
            "payment.payout.crypto.v1",
            "payment.payin.fiat.v1",
            "payment.payin.crypto.v1",
            "payment.flow.v1",
            "chain.transaction.v1",
            "signing.request.v1",
            "screening.result.v1",
            "approval.decision.v1");

    public static final String DLQ_SCHEMA_INVALID = "dlq.schema-invalid.v1";
    public static final String DLQ_LATE_EVENTS = "dlq.late-events.v1";
    public static final String DLQ_PROCESSING_FAILED = "dlq.processing-failed.v1";
    public static final String DLQ_SINK_FAILED = "dlq.sink-failed.v1";

    public static final String INGEST_CONSUMER_GROUP = "stablepay-ingest-job";
    public static final String CORRELATOR_CONSUMER_GROUP = "stablepay-correlator-job";

    private static final String ENV_KAFKA_BOOTSTRAP = "STBLPAY_KAFKA_BOOTSTRAP_SERVERS";
    private static final String ENV_SCHEMA_REGISTRY = "STBLPAY_SCHEMA_REGISTRY_URL";

    private static final String DEFAULT_KAFKA_BOOTSTRAP = "kafka:9092";
    private static final String DEFAULT_SCHEMA_REGISTRY = "http://schema-registry:8081";

    public static String kafkaBootstrapServers() {
        return System.getenv().getOrDefault(ENV_KAFKA_BOOTSTRAP, DEFAULT_KAFKA_BOOTSTRAP);
    }

    public static String schemaRegistryUrl() {
        return System.getenv().getOrDefault(ENV_SCHEMA_REGISTRY, DEFAULT_SCHEMA_REGISTRY);
    }
}
