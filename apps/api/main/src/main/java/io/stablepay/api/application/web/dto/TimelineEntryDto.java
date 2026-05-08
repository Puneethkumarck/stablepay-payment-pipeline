package io.stablepay.api.application.web.dto;

import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record TimelineEntryDto(
    String eventId, String source, String status, String detail, Instant timestamp) {}
