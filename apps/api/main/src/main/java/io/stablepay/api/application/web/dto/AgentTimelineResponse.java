package io.stablepay.api.application.web.dto;

import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record AgentTimelineResponse(
    String reference, String markdown, List<TimelineEntryDto> entries) {}
