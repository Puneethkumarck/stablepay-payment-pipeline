package io.stablepay.flink;

import java.time.Duration;
import java.util.List;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import io.stablepay.flink.config.FlinkConfig;
import io.stablepay.flink.correlator.FlowCorrelatorFunction;
import io.stablepay.flink.correlator.FlowEventSerializer;
import io.stablepay.flink.deser.AvroEnvelopeDeserializer;
import io.stablepay.flink.model.ValidatedEvent;
import io.stablepay.flink.model.ValidationResult;
import io.stablepay.flink.watermark.EnvelopeTimestampAssigner;

public class CorrelatorJob {

    private static final List<String> CORRELATOR_TOPICS = List.of(
            "payment.payout.fiat.v1",
            "payment.payout.crypto.v1",
            "payment.payin.fiat.v1",
            "payment.payin.crypto.v1",
            "payment.flow.v1",
            "chain.transaction.v1");

    public static void main(String[] args) throws Exception {
        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        var kafkaSource = KafkaSource.<ValidationResult>builder()
                .setBootstrapServers(FlinkConfig.kafkaBootstrapServers())
                .setTopics(CORRELATOR_TOPICS)
                .setGroupId(FlinkConfig.CORRELATOR_CONSUMER_GROUP)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(new AvroEnvelopeDeserializer(FlinkConfig.schemaRegistryUrl()))
                .build();

        var watermarkStrategy = WatermarkStrategy
                .<ValidatedEvent>forBoundedOutOfOrderness(Duration.ofSeconds(60))
                .withIdleness(Duration.ofMinutes(1))
                .withTimestampAssigner(new EnvelopeTimestampAssigner());

        var flowEvents = env
                .fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "correlator-kafka-source")
                .filter(r -> r instanceof ValidationResult.Valid)
                .map(r -> ((ValidationResult.Valid) r).event())
                .assignTimestampsAndWatermarks(watermarkStrategy)
                .filter(e -> e.flowId() != null && !e.flowId().isEmpty())
                .name("flow-id-filter");

        var lifecycleStream = flowEvents
                .keyBy(ValidatedEvent::flowId)
                .process(new FlowCorrelatorFunction())
                .name("flow-correlator");

        var flowSink = KafkaSink.<byte[]>builder()
                .setBootstrapServers(FlinkConfig.kafkaBootstrapServers())
                .setRecordSerializer(new FlowEventSerializer())
                .build();

        lifecycleStream.sinkTo(flowSink).name("flow-lifecycle-sink");

        env.execute("stablepay-correlator-job");
    }
}
