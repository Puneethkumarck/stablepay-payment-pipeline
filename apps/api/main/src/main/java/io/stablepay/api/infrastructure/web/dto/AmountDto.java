package io.stablepay.api.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record AmountDto(
    @JsonProperty("amount_micros") long amountMicros,
    @JsonProperty("currency_code") String currencyCode) {

  public AmountDto {
    Objects.requireNonNull(currencyCode, "currencyCode");
  }
}
