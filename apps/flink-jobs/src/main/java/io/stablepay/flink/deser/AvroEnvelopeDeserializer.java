package io.stablepay.flink.deser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.stablepay.flink.model.DlqEnvelope;
import io.stablepay.flink.model.ValidatedEvent;
import io.stablepay.flink.model.ValidationResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AvroEnvelopeDeserializer implements KafkaRecordDeserializationSchema<ValidationResult> {

    private static final int MAGIC_BYTE = 0x0;
    private static final int SCHEMA_ID_SIZE = 4;

    private final String schemaRegistryUrl;
    private transient SchemaRegistryClient schemaRegistryClient;

    @Override
    public void open(DeserializationSchema.InitializationContext context) {
        this.schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 100);
    }

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<ValidationResult> out) throws IOException {
        String topic = record.topic();
        int partition = record.partition();
        long offset = record.offset();
        String key = record.key() != null ? new String(record.key()) : null;
        byte[] valueBytes = record.value();

        if (valueBytes == null || valueBytes.length < 1 + SCHEMA_ID_SIZE) {
            out.collect(toDlqResult(topic, partition, offset, "EMPTY_PAYLOAD", "Null or too-short value", valueBytes));
            return;
        }

        if (valueBytes[0] != MAGIC_BYTE) {
            out.collect(toDlqResult(topic, partition, offset, "INVALID_MAGIC_BYTE", "Not Confluent wire format", valueBytes));
            return;
        }

        int schemaId = ByteBuffer.wrap(valueBytes, 1, SCHEMA_ID_SIZE).getInt();

        try {
            Schema writerSchema = ((AvroSchema) schemaRegistryClient.getSchemaById(schemaId)).rawSchema();
            GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(writerSchema);
            var decoder = DecoderFactory.get().binaryDecoder(
                    new ByteArrayInputStream(valueBytes, 1 + SCHEMA_ID_SIZE, valueBytes.length - 1 - SCHEMA_ID_SIZE),
                    null);
            GenericRecord genericRecord = reader.read(null, decoder);
            out.collect(EnvelopeValidator.validate(topic, partition, offset, key, genericRecord, valueBytes));
        } catch (Exception e) {
            out.collect(toDlqResult(topic, partition, offset, "SCHEMA_INVALID", e.getMessage(), valueBytes));
        }
    }

    @Override
    public TypeInformation<ValidationResult> getProducedType() {
        return TypeInformation.of(ValidationResult.class);
    }

    private static ValidationResult toDlqResult(
            String topic, int partition, long offset, String errorClass, String errorMessage, byte[] rawBytes) {
        return new ValidationResult.Invalid(
                new DlqEnvelope(topic, partition, offset, errorClass, errorMessage, rawBytes,
                        java.time.Instant.now().toEpochMilli(), 0));
    }
}
