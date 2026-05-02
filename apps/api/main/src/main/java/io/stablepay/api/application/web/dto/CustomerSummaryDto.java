package io.stablepay.api.application.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record CustomerSummaryDto(
    String id,
    String name,
    String email,
    String kyc,
    AmountDto balance,
    @JsonProperty("total_sent") AmountDto totalSent,
    @JsonProperty("txn_count") int txnCount,
    Instant joined,
    String risk) {

  public CustomerSummaryDto {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(kyc, "kyc");
    Objects.requireNonNull(balance, "balance");
    Objects.requireNonNull(totalSent, "totalSent");
    Objects.requireNonNull(joined, "joined");
    Objects.requireNonNull(risk, "risk");
  }
}
