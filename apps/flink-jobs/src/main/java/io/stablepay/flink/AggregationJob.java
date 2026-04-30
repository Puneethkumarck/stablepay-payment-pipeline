package io.stablepay.flink;

import java.time.Duration;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;

import io.stablepay.flink.agg.Aggregators;
import io.stablepay.flink.config.FlinkConfig;
import io.stablepay.flink.deser.AvroEnvelopeDeserializer;
import io.stablepay.flink.deser.DlqDeserializer;
import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.ValidatedEvent;
import io.stablepay.flink.model.ValidationResult;
import io.stablepay.flink.sink.AggTableSinkFactory;
import io.stablepay.flink.watermark.EnvelopeTimestampAssigner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AggregationJob {

    public static void main(String[] args) throws Exception {
        log.info("Starting stablepay-aggregation-job");
        var env = StreamExecutionEnvironment.getExecutionEnvironment();

        var watermarkStrategy = WatermarkStrategy
                .<ValidatedEvent>forBoundedOutOfOrderness(Duration.ofSeconds(60))
                .withIdleness(Duration.ofMinutes(1))
                .withTimestampAssigner(new EnvelopeTimestampAssigner());

        var mainSource = KafkaSource.<ValidationResult>builder()
                .setBootstrapServers(FlinkConfig.kafkaBootstrapServers())
                .setTopics(FlinkConfig.INPUT_TOPICS)
                .setGroupId(FlinkConfig.AGGREGATION_CONSUMER_GROUP)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(new AvroEnvelopeDeserializer(FlinkConfig.schemaRegistryUrl()))
                .build();

        var validatedStream = env
                .fromSource(mainSource, WatermarkStrategy.noWatermarks(), "agg-kafka-source")
                .filter(r -> r instanceof ValidationResult.Valid)
                .map(r -> ((ValidationResult.Valid) r).event())
                .assignTimestampsAndWatermarks(watermarkStrategy)
                .name("agg-validated-events");

        var aggSinkFactory = new AggTableSinkFactory();
        aggSinkFactory.ensureAggTablesExist();

        // --- Volume aggregation: payment topics → keyBy(flowType, direction, currency) ---
        var volumeStream = validatedStream
                .filter(e -> FlinkConfig.PAYMENT_TOPICS.contains(e.topic()))
                .keyBy(e -> deriveFlowType(e.topic()) + "|" + deriveDirection(e.topic()) + "|" + extractCurrency(e))
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
                .aggregate(Aggregators.volume(), Aggregators.<String>windowTimestamptz(0, 1))
                .name("agg-volume-hourly");

        aggSinkFactory.addSink(volumeStream, "agg_volume_hourly");

        // --- Success rate aggregation: payment topics → keyBy(flowType) ---
        var successRateStream = validatedStream
                .filter(e -> FlinkConfig.PAYMENT_TOPICS.contains(e.topic()))
                .keyBy(e -> deriveFlowType(e.topic()))
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
                .aggregate(Aggregators.successRate(), Aggregators.<String>windowTimestamptz(0, 1))
                .name("agg-success-rate-hourly");

        aggSinkFactory.addSink(successRateStream, "agg_success_rate_hourly");

        // --- Screening outcomes aggregation: screening.result.v1 → keyBy(outcome, provider) ---
        var screeningStream = validatedStream
                .filter(e -> "screening.result.v1".equals(e.topic()))
                .keyBy(e -> extractField(e, "outcome") + "|" + extractField(e, "provider"))
                .window(TumblingEventTimeWindows.of(Duration.ofDays(1)))
                .aggregate(Aggregators.screeningOutcomes(), Aggregators.<String>windowDate(0))
                .name("agg-screening-outcomes-daily");

        aggSinkFactory.addSink(screeningStream, "agg_screening_outcomes_daily");

        // --- DLQ summary aggregation: DLQ topics → keyBy(errorClass, sourceTopic) ---
        var dlqWatermark = WatermarkStrategy
                .<DlqEnvelope>forBoundedOutOfOrderness(Duration.ofSeconds(60))
                .withIdleness(Duration.ofMinutes(1))
                .withTimestampAssigner((e, ts) -> e.failedAt());

        var dlqSource = KafkaSource.<DlqEnvelope>builder()
                .setBootstrapServers(FlinkConfig.kafkaBootstrapServers())
                .setTopics(FlinkConfig.DLQ_TOPICS)
                .setGroupId(FlinkConfig.AGGREGATION_CONSUMER_GROUP + "-dlq")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(new DlqDeserializer())
                .build();

        var dlqSummaryStream = env
                .fromSource(dlqSource, dlqWatermark, "agg-dlq-kafka-source")
                .keyBy(e -> e.errorClass() + "|" + e.sourceTopic())
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
                .aggregate(Aggregators.dlqSummary(), Aggregators.<String>windowTimestamptz(0, 1))
                .name("agg-dlq-summary-hourly");

        aggSinkFactory.addSink(dlqSummaryStream, "agg_dlq_summary_hourly");

        env.execute("stablepay-aggregation-job");
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

    private static String extractCurrency(ValidatedEvent event) {
        var record = event.toRecord();
        var amount = record.get("amount");
        if (amount instanceof org.apache.avro.generic.GenericRecord moneyRecord) {
            var code = moneyRecord.get("currency_code");
            if (code != null) return code.toString();
        }
        return "UNKNOWN";
    }

    private static String extractField(ValidatedEvent event, String fieldName) {
        var record = event.toRecord();
        var value = record.get(fieldName);
        return value != null ? value.toString() : "UNKNOWN";
    }
}
