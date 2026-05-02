package io.stablepay.api.domain.model.fixtures;

import io.stablepay.api.domain.model.DlqEvent;
import io.stablepay.api.domain.model.DlqId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class DlqEventFixtures {

  public static final DlqId SOME_DLQ_ID =
      DlqId.of(UUID.fromString("00000000-0000-0000-0000-000000000020"));

  public static final Instant SOME_DLQ_FAILED_AT = Instant.parse("2026-05-01T10:00:00Z");

  public static final Instant SOME_DLQ_WATERMARK_AT = Instant.parse("2026-05-01T09:59:00Z");

  public static final DlqEvent SOME_DLQ_EVENT =
      DlqEvent.builder()
          .id(SOME_DLQ_ID)
          .errorClass("DeserializationException")
          .sourceTopic("crypto.payin.events")
          .sourcePartition(2)
          .sourceOffset(12345L)
          .errorMessage("Failed to deserialize Avro payload")
          .failedAt(SOME_DLQ_FAILED_AT)
          .retryCount(1)
          .sinkType(Optional.of("opensearch"))
          .watermarkAt(Optional.of(SOME_DLQ_WATERMARK_AT))
          .originalPayloadJson(Optional.of("{\"reference\":\"TXN-REF-9999\"}"))
          .build();

  private DlqEventFixtures() {}
}
