package io.stablepay.flink;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
import io.stablepay.flink.dlq.DlqKafkaSinkFactory;
import io.stablepay.flink.dlq.DlqOutputTags;
import io.stablepay.flink.mapper.EventToFactScreeningMapper;
import io.stablepay.flink.mapper.EventToFactTransactionMapper;
import io.stablepay.flink.mapper.EventToIcebergRowMapper;
import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.ValidatedEvent;
import io.stablepay.flink.model.ValidationResult;
import io.stablepay.flink.process.ValidateAndRouteFunction;
import io.stablepay.flink.sink.FactTableSinkFactory;
import io.stablepay.flink.sink.IcebergSinkFactory;
import io.stablepay.flink.sink.OpenSearchAsyncSink;
import io.stablepay.flink.watermark.EnvelopeTimestampAssigner;

public class IngestJob {

    public static void main(String[] args) throws Exception {
        var env = StreamExecutionEnvironment.getExecutionEnvironment();

        var watermarkStrategy = WatermarkStrategy
                .<ValidatedEvent>forBoundedOutOfOrderness(Duration.ofSeconds(60))
                .withIdleness(Duration.ofMinutes(1))
                .withTimestampAssigner(new EnvelopeTimestampAssigner());

        var kafkaSource = KafkaSource.<ValidationResult>builder()
                .setBootstrapServers(FlinkConfig.kafkaBootstrapServers())
                .setTopics(FlinkConfig.INPUT_TOPICS)
                .setGroupId(FlinkConfig.INGEST_CONSUMER_GROUP)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(new AvroEnvelopeDeserializer(FlinkConfig.schemaRegistryUrl()))
                .build();

        var rawStream = env
                .fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "kafka-source")
                .name("avro-deserialized-events");

        var schemaInvalidStream = rawStream
                .filter(r -> r instanceof ValidationResult.Invalid)
                .map(r -> ((ValidationResult.Invalid) r).dlqEnvelope())
                .name("schema-invalid-events");

        schemaInvalidStream
                .sinkTo(DlqKafkaSinkFactory.createSink(FlinkConfig.DLQ_SCHEMA_INVALID))
                .name("dlq-schema-invalid-sink");

        var validatedStream = rawStream
                .filter(r -> r instanceof ValidationResult.Valid)
                .map(r -> ((ValidationResult.Valid) r).event())
                .assignTimestampsAndWatermarks(watermarkStrategy)
                .name("validated-events");

        var routedStream = validatedStream
                .keyBy(IngestJob::extractEntityKey)
                .process(new ValidateAndRouteFunction())
                .name("validate-and-route");

        // DLQ side outputs to Kafka
        routedStream.getSideOutput(DlqOutputTags.LATE_EVENT)
                .sinkTo(DlqKafkaSinkFactory.createSink(FlinkConfig.DLQ_LATE_EVENTS))
                .name("dlq-late-event-sink");

        routedStream.getSideOutput(DlqOutputTags.ILLEGAL_TRANSITION)
                .sinkTo(DlqKafkaSinkFactory.createSink(FlinkConfig.DLQ_PROCESSING_FAILED))
                .name("dlq-illegal-transition-sink");

        routedStream.getSideOutput(DlqOutputTags.SINK_FAILURE)
                .sinkTo(DlqKafkaSinkFactory.createSink(FlinkConfig.DLQ_SINK_FAILED))
                .name("dlq-sink-failure-sink");

        // --- Iceberg raw sink (one table per topic, independent branch) ---
        var icebergSinkFactory = new IcebergSinkFactory();
        icebergSinkFactory.ensureTablesExist();

        for (var entry : IcebergCatalogConfig.TOPIC_TO_TABLE.entrySet()) {
            var topic = entry.getKey();
            var tableName = entry.getValue();

            var tableStream = routedStream
                    .filter(e -> topic.equals(e.topic()))
                    .map(EventToIcebergRowMapper::toRowData)
                    .name("iceberg-map-" + tableName);

            icebergSinkFactory.addSink(tableStream, tableName);
        }

        // --- OpenSearch transactions sink (independent branch) ---
        routedStream
                .sinkTo(new OpenSearchAsyncSink(FlinkConfig.opensearchUrl()))
                .name("opensearch-transactions-sink");

        // --- Fact table sinks (independent branches) ---
        var factSinkFactory = new FactTableSinkFactory();
        factSinkFactory.ensureFactTablesExist();

        var factTxStream = routedStream
                .filter(e -> FlinkConfig.FACT_TX_TOPICS.contains(e.topic()))
                .map(EventToFactTransactionMapper::toRowData)
                .name("fact-transactions-map");
        factSinkFactory.addSink(factTxStream, "fact_transactions");

        var factScreeningStream = routedStream
                .filter(e -> "screening.result.v1".equals(e.topic()))
                .map(EventToFactScreeningMapper::toRowData)
                .name("fact-screening-map");
        factSinkFactory.addSink(factScreeningStream, "fact_screening_outcomes");

        env.execute("stablepay-ingest-job");
    }

    private static String extractEntityKey(ValidatedEvent event) {
        var record = event.toRecord();
        return Stream.of("payout_reference", "payin_reference", "tx_hash")
                .map(record::get)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .findFirst()
                .or(() -> Optional.ofNullable(event.flowId()))
                .orElse(event.eventId());
    }
}
