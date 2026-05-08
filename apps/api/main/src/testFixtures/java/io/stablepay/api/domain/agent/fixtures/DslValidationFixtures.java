package io.stablepay.api.domain.agent.fixtures;

import io.stablepay.api.domain.agent.AgentSearchRequest;
import io.stablepay.api.domain.agent.AggShape;
import io.stablepay.api.domain.agent.QueryShape;
import java.util.List;
import java.util.Optional;

public final class DslValidationFixtures {

  public static final String VALID_INDEX_TRANSACTIONS = "transactions";
  public static final String VALID_INDEX_DLQ = "dlq-events";
  public static final String INVALID_INDEX = "secret-data";

  public static final QueryShape.TermShape VALID_TERM_QUERY =
      new QueryShape.TermShape("customer_id", "cust-001");

  public static final QueryShape.RangeShape VALID_RANGE_QUERY =
      new QueryShape.RangeShape(
          "event_time", Optional.of(1700000000000L), Optional.of(1700100000000L));

  public static final QueryShape.BoolShape VALID_BOOL_TERM_QUERY =
      new QueryShape.BoolShape(
          List.of(new QueryShape.TermShape("customer_id", "cust-001")),
          List.of(new QueryShape.TermShape("flow_type", "ON_RAMP")),
          List.of());

  public static final QueryShape.TermShape INVALID_FIELD_QUERY =
      new QueryShape.TermShape("password_hash", "secret");

  public static final QueryShape.BoolShape NESTED_INVALID_FIELD_QUERY =
      new QueryShape.BoolShape(
          List.of(new QueryShape.TermShape("password_hash", "secret")), List.of(), List.of());

  public static final AggShape.TermsAggShape VALID_TERMS_AGG =
      new AggShape.TermsAggShape("by_status", "internal_status", 10);

  public static final AggShape.DateHistogramAggShape VALID_DATE_HISTOGRAM_AGG =
      new AggShape.DateHistogramAggShape("over_time", "event_time", "day");

  public static final AggShape.SumAggShape VALID_SUM_AGG =
      new AggShape.SumAggShape("total_amount", "amount_micros");

  public static final AggShape.AvgAggShape VALID_AVG_AGG =
      new AggShape.AvgAggShape("avg_amount", "amount_micros");

  public static final AggShape.SumAggShape INVALID_AGG_FIELD =
      new AggShape.SumAggShape("bad_agg", "password_hash");

  public static AgentSearchRequest validBoolTermRequest() {
    return AgentSearchRequest.builder()
        .index(VALID_INDEX_TRANSACTIONS)
        .query(VALID_BOOL_TERM_QUERY)
        .aggs(Optional.empty())
        .size(Optional.of(50))
        .sort(Optional.empty())
        .build();
  }

  public static AgentSearchRequest validRangeRequest() {
    return AgentSearchRequest.builder()
        .index(VALID_INDEX_TRANSACTIONS)
        .query(VALID_RANGE_QUERY)
        .aggs(Optional.empty())
        .size(Optional.of(100))
        .sort(Optional.empty())
        .build();
  }

  public static AgentSearchRequest invalidIndexRequest() {
    return AgentSearchRequest.builder()
        .index(INVALID_INDEX)
        .query(VALID_TERM_QUERY)
        .aggs(Optional.empty())
        .size(Optional.empty())
        .sort(Optional.empty())
        .build();
  }

  public static AgentSearchRequest invalidFieldRequest() {
    return AgentSearchRequest.builder()
        .index(VALID_INDEX_TRANSACTIONS)
        .query(INVALID_FIELD_QUERY)
        .aggs(Optional.empty())
        .size(Optional.empty())
        .sort(Optional.empty())
        .build();
  }

  public static AgentSearchRequest oversizedRequest() {
    return AgentSearchRequest.builder()
        .index(VALID_INDEX_TRANSACTIONS)
        .query(VALID_TERM_QUERY)
        .aggs(Optional.empty())
        .size(Optional.of(5000))
        .sort(Optional.empty())
        .build();
  }

  public static AgentSearchRequest validSortRequest() {
    return AgentSearchRequest.builder()
        .index(VALID_INDEX_TRANSACTIONS)
        .query(VALID_TERM_QUERY)
        .aggs(Optional.empty())
        .size(Optional.empty())
        .sort(Optional.of(List.of("event_time")))
        .build();
  }

  public static AgentSearchRequest invalidSortFieldRequest() {
    return AgentSearchRequest.builder()
        .index(VALID_INDEX_TRANSACTIONS)
        .query(VALID_TERM_QUERY)
        .aggs(Optional.empty())
        .size(Optional.empty())
        .sort(Optional.of(List.of("password_hash")))
        .build();
  }

  public static AgentSearchRequest validAggsRequest() {
    return AgentSearchRequest.builder()
        .index(VALID_INDEX_TRANSACTIONS)
        .query(VALID_TERM_QUERY)
        .aggs(Optional.of(List.of(VALID_TERMS_AGG, VALID_SUM_AGG)))
        .size(Optional.empty())
        .sort(Optional.empty())
        .build();
  }

  public static AgentSearchRequest invalidAggFieldRequest() {
    return AgentSearchRequest.builder()
        .index(VALID_INDEX_TRANSACTIONS)
        .query(VALID_TERM_QUERY)
        .aggs(Optional.of(List.of(INVALID_AGG_FIELD)))
        .size(Optional.empty())
        .sort(Optional.empty())
        .build();
  }

  private DslValidationFixtures() {}
}
