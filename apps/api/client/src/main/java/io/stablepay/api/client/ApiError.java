package io.stablepay.api.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ApiError(
    @JsonProperty("error_code") String errorCode, String message, Instant timestamp) {}
