package io.stablepay.flink.correlator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.EncoderFactory;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.stablepay.events.flow.PaymentFlowV1;

public class FlowEventSerializer implements KafkaRecordSerializationSchema<GenericRecord> {

    private static final String TOPIC = "payment.flow.v1";
    private static final int SCHEMA_ID_PLACEHOLDER = 0;

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            GenericRecord record, KafkaSinkContext context, Long timestamp) {
        String flowId = record.get("flow_id") != null ? record.get("flow_id").toString() : null;
        byte[] key = flowId != null ? flowId.getBytes(StandardCharsets.UTF_8) : null;
        byte[] value = serializeAvro(record);
        return new ProducerRecord<>(TOPIC, key, value);
    }

    private static byte[] serializeAvro(GenericRecord record) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0x0);
            out.write(ByteBuffer.allocate(4).putInt(SCHEMA_ID_PLACEHOLDER).array());

            GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(PaymentFlowV1.getClassSchema());
            var encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize flow event", e);
        }
    }
}
