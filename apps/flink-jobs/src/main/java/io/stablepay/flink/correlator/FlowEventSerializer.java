package io.stablepay.flink.correlator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.EncoderFactory;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.stablepay.events.flow.PaymentFlowV1;
import io.stablepay.flink.config.FlinkConfig;

public class FlowEventSerializer implements KafkaRecordSerializationSchema<GenericRecord> {

    private static final String TOPIC = "payment.flow.v1";
    private static final Schema SCHEMA = PaymentFlowV1.getClassSchema();

    private transient SchemaRegistryClient schemaRegistryClient;
    private transient int schemaId = -1;

    private void ensureInitialized() {
        if (schemaRegistryClient == null) {
            schemaRegistryClient = new CachedSchemaRegistryClient(FlinkConfig.schemaRegistryUrl(), 100);
        }
        if (schemaId == -1) {
            try {
                String subject = TOPIC + "-value";
                schemaId = schemaRegistryClient.register(subject, new AvroSchema(SCHEMA));
            } catch (Exception e) {
                throw new RuntimeException("Failed to register schema with Schema Registry", e);
            }
        }
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            GenericRecord record, KafkaSinkContext context, Long timestamp) {
        ensureInitialized();
        String flowId = record.get("flow_id") != null ? record.get("flow_id").toString() : null;
        byte[] key = flowId != null ? flowId.getBytes(StandardCharsets.UTF_8) : null;
        byte[] value = serializeAvro(record);
        return new ProducerRecord<>(TOPIC, key, value);
    }

    private byte[] serializeAvro(GenericRecord record) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0x0);
            out.write(ByteBuffer.allocate(4).putInt(schemaId).array());

            GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(SCHEMA);
            var encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize flow event", e);
        }
    }
}
