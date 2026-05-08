package io.stablepay.api.application.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "Agent transaction timeline")
@Builder(toBuilder = true)
public record AgentTimelineResponse(
    String reference, String markdown, List<TimelineEntryDto> entries) {}
