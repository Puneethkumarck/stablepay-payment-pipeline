package io.stablepay.flink.sink;

import java.util.List;
import lombok.Builder;

record BulkResult(List<FailedDoc> failed) {

  @Builder
  record FailedDoc(
      String eventId, int statusCode, String errorType, String errorReason, boolean transient_) {}
}
