package io.stablepay.api.domain.model.fixtures;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.CustomerSummary;
import java.time.Instant;
import java.util.UUID;

public final class CustomerSummaryFixtures {

  public static final CustomerId SOME_CUSTOMER_ID =
      CustomerId.of(UUID.fromString("00000000-0000-0000-0000-000000000040"));

  public static final Instant SOME_CUSTOMER_JOINED = Instant.parse("2026-05-01T10:00:00Z");

  public static final CustomerSummary SOME_CUSTOMER_SUMMARY =
      CustomerSummary.builder()
          .id(SOME_CUSTOMER_ID)
          .name("Acme Corp")
          .email("masked-customer@example.com")
          .kyc("VERIFIED")
          .balance(MoneyFixtures.SOME_MONEY)
          .totalSent(MoneyFixtures.SOME_MONEY)
          .txnCount(42)
          .joined(SOME_CUSTOMER_JOINED)
          .risk("LOW")
          .build();

  private CustomerSummaryFixtures() {}
}
