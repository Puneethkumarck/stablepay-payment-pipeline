package io.stablepay.flink.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;

import lombok.Builder;

@Builder(toBuilder = true)
public record ValidatedEvent(
    String topic,
    int sourcePartition,
    long sourceOffset,
    String key,
    byte[] recordBytes,
    String recordSchemaJson,
    String eventId,
    long eventTimeMillis,
    String flowId,
    String schemaVersion
) {
    private static final ConcurrentMap<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<>();

    public ValidatedEvent {
        recordBytes = recordBytes != null ? recordBytes.clone() : null;
    }

    public static ValidatedEvent fromRecord(
            String topic, int sourcePartition, long sourceOffset, String key, GenericRecord record,
            String eventId, long eventTimeMillis, String flowId, String schemaVersion) {
        try {
            var schema = record.getSchema();
            var out = new ByteArrayOutputStream();
            var writer = new GenericDatumWriter<GenericRecord>(schema);
            var encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(record, encoder);
            encoder.flush();
            return ValidatedEvent.builder()
                    .topic(topic)
                    .sourcePartition(sourcePartition)
                    .sourceOffset(sourceOffset)
                    .key(key)
                    .recordBytes(out.toByteArray())
                    .recordSchemaJson(schema.toString())
                    .eventId(eventId)
                    .eventTimeMillis(eventTimeMillis)
                    .flowId(flowId)
                    .schemaVersion(schemaVersion)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize GenericRecord", e);
        }
    }

    public GenericRecord toRecord() {
        try {
            var schema = SCHEMA_CACHE.computeIfAbsent(
                    recordSchemaJson, json -> new Schema.Parser().parse(json));
            var reader = new GenericDatumReader<GenericRecord>(schema);
            var decoder = DecoderFactory.get().binaryDecoder(
                    new ByteArrayInputStream(recordBytes), null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize GenericRecord", e);
        }
    }
}
