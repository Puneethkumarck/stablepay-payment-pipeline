package io.stablepay.api.domain.model.fixtures;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.Flow;
import io.stablepay.api.domain.model.FlowId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class FlowFixtures {

  public static final FlowId SOME_FLOW_ID =
      FlowId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));

  public static final CustomerId SOME_FLOW_CUSTOMER_ID =
      CustomerId.of(UUID.fromString("00000000-0000-0000-0000-000000000011"));

  public static final Instant SOME_FLOW_CREATED_AT = Instant.parse("2026-05-01T10:00:00Z");

  public static final Instant SOME_FLOW_UPDATED_AT = Instant.parse("2026-05-01T10:05:00Z");

  public static final Flow SOME_FLOW =
      Flow.builder()
          .id(SOME_FLOW_ID)
          .flowType("MULTI_LEG")
          .status("IN_PROGRESS")
          .customerId(SOME_FLOW_CUSTOMER_ID)
          .totalAmount(MoneyFixtures.SOME_MONEY)
          .legCount(3)
          .createdAt(SOME_FLOW_CREATED_AT)
          .updatedAt(SOME_FLOW_UPDATED_AT)
          .completedAt(Optional.empty())
          .build();

  private FlowFixtures() {}
}
