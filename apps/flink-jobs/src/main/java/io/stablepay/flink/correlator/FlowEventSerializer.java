package io.stablepay.flink.correlator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.stablepay.events.flow.PaymentFlowV1;
import io.stablepay.flink.config.FlinkConfig;

public class FlowEventSerializer implements KafkaRecordSerializationSchema<byte[]> {

    private static final String TOPIC = "payment.flow.v1";
    private static final Schema SCHEMA = PaymentFlowV1.getClassSchema();

    private transient SchemaRegistryClient schemaRegistryClient;
    private transient int schemaId;
    private transient boolean initialized;

    private void ensureInitialized() {
        if (!initialized) {
            schemaRegistryClient = new CachedSchemaRegistryClient(FlinkConfig.schemaRegistryUrl(), 100);
            try {
                String subject = TOPIC + "-value";
                schemaId = schemaRegistryClient.register(subject, new AvroSchema(SCHEMA));
            } catch (Exception e) {
                throw new RuntimeException("Failed to register schema with Schema Registry", e);
            }
            initialized = true;
        }
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            byte[] avroBytes, KafkaSinkContext context, Long timestamp) {
        ensureInitialized();
        String flowId = extractFlowId(avroBytes);
        byte[] key = flowId != null ? flowId.getBytes(StandardCharsets.UTF_8) : null;
        byte[] value = prependConfluentHeader(avroBytes);
        return new ProducerRecord<>(TOPIC, key, value);
    }

    private String extractFlowId(byte[] avroBytes) {
        try {
            var reader = new GenericDatumReader<GenericRecord>(SCHEMA);
            var decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(avroBytes), null);
            var record = reader.read(null, decoder);
            var flowId = record.get("flow_id");
            return flowId != null ? flowId.toString() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] prependConfluentHeader(byte[] avroBytes) {
        try {
            var out = new ByteArrayOutputStream(1 + 4 + avroBytes.length);
            out.write(0x0);
            out.write(ByteBuffer.allocate(4).putInt(schemaId).array());
            out.write(avroBytes);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build Confluent wire format", e);
        }
    }
}
