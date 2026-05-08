package io.stablepay.api.application.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Schema(description = "Single timeline event entry")
@Builder(toBuilder = true)
public record TimelineEntryDto(
    String eventId, String source, String status, String detail, Instant timestamp) {}
