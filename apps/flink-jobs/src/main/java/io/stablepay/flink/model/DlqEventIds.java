package io.stablepay.flink.model;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DlqEventIds {

    private DlqEventIds() {}

    public static String of(DlqEnvelope envelope) {
        var seed = (envelope.sourceTopic() == null ? "" : envelope.sourceTopic())
                + "|" + envelope.sourcePartition()
                + "|" + envelope.sourceOffset()
                + "|" + envelope.failedAt()
                + "|" + (envelope.errorClass() == null ? "" : envelope.errorClass());
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
