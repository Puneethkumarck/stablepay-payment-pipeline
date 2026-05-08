package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Schema(description = "Agent OpenSearch query result")
@Builder(toBuilder = true)
public record AgentSearchResponse(
    List<Map<String, Object>> hits,
    @JsonProperty("total_hits") long totalHits,
    @JsonProperty("resolved_size") int resolvedSize,
    @JsonProperty("took_ms") long tookMs) {}
