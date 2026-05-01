package io.stablepay.flink.dlq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.flink.model.DlqEnvelope;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

@RequiredArgsConstructor
class DlqRecordSerializer implements KafkaRecordSerializationSchema<DlqEnvelope> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String topic;

  @Override
  public ProducerRecord<byte[], byte[]> serialize(
      DlqEnvelope envelope, KafkaSinkContext context, Long timestamp) {
    byte[] value = toJsonBytes(envelope);
    byte[] key =
        envelope.sourceTopic() != null
            ? envelope.sourceTopic().getBytes(StandardCharsets.UTF_8)
            : null;
    return new ProducerRecord<>(topic, key, value);
  }

  private static byte[] toJsonBytes(DlqEnvelope e) {
    try {
      var map = new LinkedHashMap<String, Object>();
      map.put("source_topic", e.sourceTopic());
      map.put("source_partition", e.sourcePartition());
      map.put("source_offset", e.sourceOffset());
      map.put("error_class", e.errorClass());
      map.put("error_message", e.errorMessage());
      map.put("failed_at", e.failedAt());
      map.put("retry_count", e.retryCount());
      return MAPPER.writeValueAsBytes(map);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize DLQ envelope to JSON", ex);
    }
  }
}
