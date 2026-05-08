package io.stablepay.api.domain.agent.fixtures;

public final class SqlValidationFixtures {

  public static final String VALID_VIEW_QUERY =
      "SELECT * FROM iceberg.analytics.v_payment_summary WHERE currency = 'USD'";

  public static final String VALID_AGG_QUERY =
      "SELECT currency, total FROM iceberg.agg.agg_daily_volume LIMIT 50";

  public static final String VALID_FACT_QUERY =
      "SELECT transaction_id, amount FROM iceberg.facts.fact_transactions LIMIT 100";

  public static final String INSERT_SQL =
      "INSERT INTO iceberg.analytics.v_payment_summary (currency) VALUES ('USD')";

  public static final String DELETE_SQL = "DELETE FROM iceberg.analytics.v_payment_summary";

  public static final String WITH_RECURSIVE_SQL =
      "WITH RECURSIVE cte AS (SELECT 1 UNION ALL SELECT 1) SELECT * FROM cte";

  public static final String UNKNOWN_TABLE_SQL = "SELECT * FROM postgres.public.users";

  public static final String MISSING_LIMIT_SQL =
      "SELECT * FROM iceberg.analytics.v_payment_summary";

  public static final String OVERSIZED_LIMIT_SQL =
      "SELECT * FROM iceberg.analytics.v_payment_summary LIMIT 50000";

  public static final String DLQ_TABLE_SQL = "SELECT * FROM iceberg.dlq.dlq_events";

  private SqlValidationFixtures() {}
}
