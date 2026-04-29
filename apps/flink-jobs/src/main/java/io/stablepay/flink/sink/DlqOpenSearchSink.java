package io.stablepay.flink.sink;

import java.io.IOException;

import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;

import io.stablepay.flink.model.DlqEnvelope;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DlqOpenSearchSink implements Sink<DlqEnvelope> {

    private final String opensearchUrl;
    private final String indexName;

    @Override
    public SinkWriter<DlqEnvelope> createWriter(WriterInitContext context) throws IOException {
        return new DlqOpenSearchSinkWriter(opensearchUrl, indexName);
    }
}
