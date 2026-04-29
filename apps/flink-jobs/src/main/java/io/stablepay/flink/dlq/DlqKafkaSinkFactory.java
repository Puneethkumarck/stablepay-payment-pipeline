package io.stablepay.flink.dlq;

import org.apache.flink.connector.kafka.sink.KafkaSink;

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
}
