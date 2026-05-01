package io.stablepay.flink.sink;

import java.util.Map;

record BulkAction(String eventId, Map<String, Object> document, int retryCount) {

  BulkAction(String eventId, Map<String, Object> document) {
    this(eventId, document, 0);
  }

  BulkAction withRetry() {
    return new BulkAction(eventId, document, retryCount + 1);
  }
}
