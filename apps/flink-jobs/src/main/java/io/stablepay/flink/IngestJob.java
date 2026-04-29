package io.stablepay.flink;

import java.time.Duration;
import java.util.Map;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;

import io.stablepay.flink.catalog.IcebergCatalogConfig;
import io.stablepay.flink.config.FlinkConfig;
import io.stablepay.flink.deser.AvroEnvelopeDeserializer;
import io.stablepay.flink.mapper.EventToIcebergRowMapper;
import io.stablepay.flink.model.ValidatedEvent;
import io.stablepay.flink.model.ValidationResult;
import io.stablepay.flink.sink.IcebergSinkFactory;
import io.stablepay.flink.sink.OpenSearchAsyncSink;
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

        // DLQ side outputs wired by Plan 2.5

        // --- Iceberg raw sink (one table per topic, independent branch) ---
        IcebergSinkFactory icebergSinkFactory = new IcebergSinkFactory();
        icebergSinkFactory.ensureTablesExist();

        for (Map.Entry<String, String> entry : IcebergCatalogConfig.TOPIC_TO_TABLE.entrySet()) {
            String topic = entry.getKey();
            String tableName = entry.getValue();

            DataStream<RowData> tableStream = validatedStream
                    .filter(e -> topic.equals(e.topic()))
                    .map(EventToIcebergRowMapper::toRowData)
                    .name("iceberg-map-" + tableName);

            icebergSinkFactory.addSink(tableStream, tableName);
        }

        // --- OpenSearch transactions sink (independent branch) ---
        validatedStream
                .addSink(new OpenSearchAsyncSink(FlinkConfig.opensearchUrl()))
                .name("opensearch-transactions-sink");

        env.execute("stablepay-ingest-job");
    }
}
