package io.stablepay.flink.dlq;

import java.nio.charset.StandardCharsets;

import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.stablepay.flink.config.FlinkConfig;
import io.stablepay.flink.model.DlqEnvelope;

public final class DlqKafkaSinkFactory {

    private DlqKafkaSinkFactory() {}

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
            return "{\"source_topic\":\"" + escape(e.sourceTopic())
                    + "\",\"source_partition\":" + e.sourcePartition()
                    + ",\"source_offset\":" + e.sourceOffset()
                    + ",\"error_class\":\"" + escape(e.errorClass())
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
