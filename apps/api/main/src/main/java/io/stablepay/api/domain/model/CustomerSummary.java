package io.stablepay.api.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record CustomerSummary(
    CustomerId id,
    String name,
    String email,
    String kyc,
    Money balance,
    Money totalSent,
    int txnCount,
    Instant joined,
    String risk) {

  public CustomerSummary {
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
