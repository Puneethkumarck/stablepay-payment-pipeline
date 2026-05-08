package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record AgentSqlResponse(
    List<String> columns,
    List<List<Object>> rows,
    @JsonProperty("row_count") int rowCount,
    @JsonProperty("query_id") String queryId,
    @JsonProperty("took_ms") long tookMs) {}
