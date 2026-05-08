package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder(toBuilder = true)
public record AgentSearchResponse(
    List<Map<String, Object>> hits,
    @JsonProperty("total_hits") long totalHits,
    @JsonProperty("resolved_size") int resolvedSize,
    @JsonProperty("took_ms") long tookMs) {}
