package io.stablepay.flink.sink;

import java.util.List;

record BulkResult(List<FailedDoc> failed) {

    record FailedDoc(String eventId, int statusCode, String errorType, String errorReason, boolean transient_) {}
}
