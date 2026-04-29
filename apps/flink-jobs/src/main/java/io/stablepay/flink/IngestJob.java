package io.stablepay.flink;

import java.time.Duration;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import io.stablepay.flink.config.FlinkConfig;
import io.stablepay.flink.deser.AvroEnvelopeDeserializer;
import io.stablepay.flink.model.ValidatedEvent;
import io.stablepay.flink.model.ValidationResult;
import io.stablepay.flink.watermark.EnvelopeTimestampAssigner;

public class IngestJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        WatermarkStrategy<ValidatedEvent> watermarkStrategy = WatermarkStrategy
                .<ValidatedEvent>forBoundedOutOfOrderness(Duration.ofSeconds(60))
                .withIdleness(Duration.ofMinutes(1))
                .withTimestampAssigner(new EnvelopeTimestampAssigner());

        KafkaSource<ValidationResult> kafkaSource = KafkaSource.<ValidationResult>builder()
                .setBootstrapServers(FlinkConfig.kafkaBootstrapServers())
                .setTopics(FlinkConfig.INPUT_TOPICS)
                .setGroupId(FlinkConfig.INGEST_CONSUMER_GROUP)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(new AvroEnvelopeDeserializer(FlinkConfig.schemaRegistryUrl()))
                .build();

        DataStream<ValidationResult> rawStream = env
                .fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "kafka-source")
                .name("avro-deserialized-events");

        SingleOutputStreamOperator<ValidatedEvent> validatedStream = rawStream
                .filter(r -> r instanceof ValidationResult.Valid)
                .map(r -> ((ValidationResult.Valid) r).event())
                .assignTimestampsAndWatermarks(watermarkStrategy)
                .name("validated-events");

        // Sinks plugged in by Plan 2.3
        // DLQ side outputs wired by Plan 2.5

        env.execute("stablepay-ingest-job");
    }
}
