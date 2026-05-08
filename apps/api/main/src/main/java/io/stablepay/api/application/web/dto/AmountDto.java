package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import lombok.Builder;

@Schema(description = "Monetary amount in micros with currency code")
@Builder(toBuilder = true)
public record AmountDto(
    @JsonProperty("amount_micros") long amountMicros,
    @JsonProperty("currency_code") String currencyCode) {

  public AmountDto {
    Objects.requireNonNull(currencyCode, "currencyCode");
  }
}
