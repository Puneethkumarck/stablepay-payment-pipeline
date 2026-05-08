package io.stablepay.api.application.web.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record AgentTimelineResponse(
    String reference, String markdown, List<TimelineEntryDto> entries) {

  @Builder(toBuilder = true)
  public record TimelineEntryDto(
      String eventId, String source, String status, String detail, Instant timestamp) {}
}
