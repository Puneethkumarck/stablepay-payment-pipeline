package io.stablepay.api.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "API error response")
public record ApiError(
    @JsonProperty("error_code") String errorCode, String message, Instant timestamp) {}
