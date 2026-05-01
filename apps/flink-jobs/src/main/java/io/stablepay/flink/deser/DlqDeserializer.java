package io.stablepay.flink.deser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.flink.model.DlqEnvelope;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Slf4j
public class DlqDeserializer implements KafkaRecordDeserializationSchema<DlqEnvelope> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  static final String DESERIALIZATION_FAILED = "DESERIALIZATION_FAILED";

  @Override
  public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<DlqEnvelope> out)
      throws IOException {
    if (record.value() == null) return;

    try {
      var node = MAPPER.readTree(record.value());
      var envelope =
          DlqEnvelope.builder()
              .sourceTopic(textOrDefault(node, "source_topic", record.topic()))
              .sourcePartition(intOrDefault(node, "source_partition", record.partition()))
              .sourceOffset(longOrDefault(node, "source_offset", record.offset()))
              .errorClass(textOrDefault(node, "error_class", "UNKNOWN"))
              .errorMessage(textOrDefault(node, "error_message", ""))
              .failedAt(longOrDefault(node, "failed_at", record.timestamp()))
              .retryCount(intOrDefault(node, "retry_count", 0))
              .build();
      out.collect(envelope);
    } catch (Exception e) {
      log.warn(
          "Failed to deserialize DLQ record from topic={} partition={} offset={} cause={}",
          record.topic(),
          record.partition(),
          record.offset(),
          e.getClass().getSimpleName());
      out.collect(
          DlqEnvelope.builder()
              .sourceTopic(record.topic())
              .sourcePartition(record.partition())
              .sourceOffset(record.offset())
              .errorClass(DESERIALIZATION_FAILED)
              .errorMessage(e.getClass().getSimpleName())
              .originalPayloadBytes(record.value())
              .failedAt(record.timestamp())
              .retryCount(0)
              .build());
    }
  }

  @Override
  public TypeInformation<DlqEnvelope> getProducedType() {
    return TypeInformation.of(DlqEnvelope.class);
  }

  private static String textOrDefault(JsonNode node, String field, String defaultValue) {
    var child = node.get(field);
    return child != null && !child.isNull() ? child.asText() : defaultValue;
  }

  private static int intOrDefault(JsonNode node, String field, int defaultValue) {
    var child = node.get(field);
    return child != null && !child.isNull() ? child.asInt(defaultValue) : defaultValue;
  }

  private static long longOrDefault(JsonNode node, String field, long defaultValue) {
    var child = node.get(field);
    return child != null && !child.isNull() ? child.asLong(defaultValue) : defaultValue;
  }
}
