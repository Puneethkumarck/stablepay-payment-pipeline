package io.stablepay.api.domain.model.fixtures;

import static io.stablepay.api.domain.model.fixtures.MoneyFixtures.SOME_MONEY;
import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_EVENT_TIME;
import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_TRANSACTION;
import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_TRANSACTION_CUSTOMER_ID;

import io.stablepay.api.domain.model.TransactionEvent;
import java.util.UUID;

public final class TransactionEventFixtures {

  public static final String SOME_OTHER_CUSTOMER_ID =
      UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee").toString();

  public static final TransactionEvent SOME_TRANSACTION_EVENT =
      TransactionEvent.builder()
          .eventId(SOME_TRANSACTION.eventId())
          .customerId(SOME_TRANSACTION_CUSTOMER_ID.value().toString())
          .status(SOME_TRANSACTION.customerStatus())
          .eventTime(SOME_EVENT_TIME)
          .amountMicros(SOME_MONEY.toMicros())
          .currencyCode(SOME_MONEY.currency().name())
          .flowType(SOME_TRANSACTION.flowType())
          .sortKey("c29ydC1rZXk")
          .build();

  public static final TransactionEvent SOME_OTHER_CUSTOMER_EVENT =
      SOME_TRANSACTION_EVENT.toBuilder()
          .eventId("evt-other-0001")
          .customerId(SOME_OTHER_CUSTOMER_ID)
          .sortKey("b3RoZXIta2V5")
          .build();

  private TransactionEventFixtures() {}

  public static TransactionEvent.TransactionEventBuilder someTransactionEvent() {
    return SOME_TRANSACTION_EVENT.toBuilder();
  }
}
