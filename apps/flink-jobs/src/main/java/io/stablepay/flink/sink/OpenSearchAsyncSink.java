package io.stablepay.flink.sink;

import io.stablepay.flink.model.ValidatedEvent;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;

@RequiredArgsConstructor
public class OpenSearchAsyncSink implements Sink<ValidatedEvent> {

  private final String opensearchUrl;
  private final String indexName;

  public OpenSearchAsyncSink(String opensearchUrl) {
    this(opensearchUrl, "transactions-write");
  }

  @Override
  public SinkWriter<ValidatedEvent> createWriter(WriterInitContext context) throws IOException {
    return new OpenSearchSinkWriter(opensearchUrl, indexName);
  }
}
