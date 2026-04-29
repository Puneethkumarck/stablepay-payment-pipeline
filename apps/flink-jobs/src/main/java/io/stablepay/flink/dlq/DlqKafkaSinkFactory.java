package io.stablepay.flink.dlq;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.stablepay.flink.config.FlinkConfig;
import io.stablepay.flink.model.DlqEnvelope;

public final class DlqKafkaSinkFactory {

    private DlqKafkaSinkFactory() {}

    private static final Map<String, String> OUTPUT_TAG_TO_TOPIC = Map.of(
            "dlq-schema-invalid", FlinkConfig.DLQ_SCHEMA_INVALID,
            "dlq-late-event", FlinkConfig.DLQ_LATE_EVENTS,
            "dlq-illegal-transition", FlinkConfig.DLQ_PROCESSING_FAILED,
            "dlq-sink-failure", FlinkConfig.DLQ_SINK_FAILED);

    public static KafkaSink<DlqEnvelope> createSink(String dlqTopic) {
        return KafkaSink.<DlqEnvelope>builder()
                .setBootstrapServers(FlinkConfig.kafkaBootstrapServers())
                .setRecordSerializer(new DlqRecordSerializer(dlqTopic))
                .build();
    }

    private static class DlqRecordSerializer implements KafkaRecordSerializationSchema<DlqEnvelope> {

        private final String topic;

        DlqRecordSerializer(String topic) {
            this.topic = topic;
        }

        @Override
        public ProducerRecord<byte[], byte[]> serialize(
                DlqEnvelope envelope, KafkaSinkContext context, Long timestamp) {
            String json = toJson(envelope);
            byte[] key = envelope.sourceTopic() != null
                    ? envelope.sourceTopic().getBytes(StandardCharsets.UTF_8) : null;
            return new ProducerRecord<>(topic, key, json.getBytes(StandardCharsets.UTF_8));
        }

        private static String toJson(DlqEnvelope e) {
            return "{\"source_topic\":\"" + e.sourceTopic()
                    + "\",\"source_partition\":" + e.sourcePartition()
                    + ",\"source_offset\":" + e.sourceOffset()
                    + ",\"error_class\":\"" + e.errorClass()
                    + "\",\"error_message\":\"" + escape(e.errorMessage())
                    + "\",\"failed_at\":" + e.failedAt()
                    + ",\"retry_count\":" + e.retryCount() + "}";
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        }
    }
}
