package io.stablepay.flink.sink;

import java.io.IOException;

import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;

import io.stablepay.flink.model.ValidatedEvent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OpenSearchAsyncSink implements Sink<ValidatedEvent> {

    private final String opensearchUrl;

    @Override
    public SinkWriter<ValidatedEvent> createWriter(WriterInitContext context) throws IOException {
        return new OpenSearchSinkWriter(opensearchUrl);
    }
}
