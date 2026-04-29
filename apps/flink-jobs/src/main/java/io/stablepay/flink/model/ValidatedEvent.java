package io.stablepay.flink.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;

public record ValidatedEvent(
    String topic,
    String key,
    byte[] recordBytes,
    String recordSchemaJson,
    String eventId,
    long eventTimeMillis,
    String flowId,
    String schemaVersion
) {
    public ValidatedEvent {
        recordBytes = recordBytes != null ? recordBytes.clone() : null;
    }

    public static ValidatedEvent fromRecord(
            String topic, String key, GenericRecord record,
            String eventId, long eventTimeMillis, String flowId, String schemaVersion) {
        try {
            var schema = record.getSchema();
            var out = new ByteArrayOutputStream();
            var writer = new GenericDatumWriter<GenericRecord>(schema);
            var encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(record, encoder);
            encoder.flush();
            return new ValidatedEvent(
                    topic, key, out.toByteArray(), schema.toString(),
                    eventId, eventTimeMillis, flowId, schemaVersion);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize GenericRecord", e);
        }
    }

    public GenericRecord toRecord() {
        try {
            var schema = new Schema.Parser().parse(recordSchemaJson);
            var reader = new GenericDatumReader<GenericRecord>(schema);
            var decoder = DecoderFactory.get().binaryDecoder(
                    new ByteArrayInputStream(recordBytes), null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize GenericRecord", e);
        }
    }
}
