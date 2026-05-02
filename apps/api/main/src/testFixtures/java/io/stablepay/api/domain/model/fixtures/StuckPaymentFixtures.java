package io.stablepay.api.domain.model.fixtures;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.StuckPayment;
import io.stablepay.api.domain.model.TransactionId;
import java.time.Instant;
import java.util.UUID;

public final class StuckPaymentFixtures {

  public static final TransactionId SOME_STUCK_TRANSACTION_ID =
      TransactionId.of(UUID.fromString("00000000-0000-0000-0000-000000000030"));

  public static final CustomerId SOME_STUCK_CUSTOMER_ID =
      CustomerId.of(UUID.fromString("00000000-0000-0000-0000-000000000031"));

  public static final Instant SOME_STUCK_LAST_EVENT_AT = Instant.parse("2026-05-01T10:00:00Z");

  public static final StuckPayment SOME_STUCK_PAYMENT =
      StuckPayment.builder()
          .id(SOME_STUCK_TRANSACTION_ID)
          .reference("TXN-REF-STUCK-0001")
          .flowType("FIAT_PAYOUT")
          .internalStatus("AWAITING_CONFIRMATION")
          .customerId(SOME_STUCK_CUSTOMER_ID)
          .amount(MoneyFixtures.SOME_MONEY)
          .lastEventAt(SOME_STUCK_LAST_EVENT_AT)
          .stuckMillis(900_000L)
          .build();

  private StuckPaymentFixtures() {}
}
