package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "Agent Trino SQL execution result")
@Builder(toBuilder = true)
public record AgentSqlResponse(
    List<String> columns,
    List<List<Object>> rows,
    @JsonProperty("row_count") int rowCount,
    @JsonProperty("query_id") String queryId,
    @JsonProperty("took_ms") long tookMs) {}
