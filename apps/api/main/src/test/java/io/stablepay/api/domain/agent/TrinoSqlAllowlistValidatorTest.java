package io.stablepay.api.domain.agent;

import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.DELETE_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.DLQ_TABLE_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.INSERT_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.JOIN_MIXED_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.MISSING_LIMIT_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.NON_RECURSIVE_CTE_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.OVERSIZED_LIMIT_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.SUBQUERY_DISALLOWED_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.UNKNOWN_TABLE_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.UPDATE_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.UPPERCASE_TABLE_SQL;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.VALID_AGG_QUERY;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.VALID_FACT_QUERY;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.VALID_VIEW_QUERY;
import static io.stablepay.api.domain.agent.fixtures.SqlValidationFixtures.WITH_RECURSIVE_SQL;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TrinoSqlAllowlistValidatorTest {

  private final AllowedTableRegistry registry = new AllowedTableRegistry();
  private final TrinoSqlAllowlistValidator validator = new TrinoSqlAllowlistValidator(registry);

  @Nested
  class ValidQueries {

    @Test
    void shouldAcceptValidViewQuery() {
      // when
      var result = validator.validate(VALID_VIEW_QUERY);

      // then
      var expected = new SqlValidationResult.Valid(VALID_VIEW_QUERY + " LIMIT 1000", 1000);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptValidAggQuery() {
      // when
      var result = validator.validate(VALID_AGG_QUERY);

      // then
      var expected =
          new SqlValidationResult.Valid(
              "SELECT currency, total FROM iceberg.agg.agg_daily_volume LIMIT 50", 50);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptValidFactQuery() {
      // when
      var result = validator.validate(VALID_FACT_QUERY);

      // then
      var expected =
          new SqlValidationResult.Valid(
              "SELECT transaction_id, amount FROM iceberg.facts.fact_transactions LIMIT 100", 100);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptNonRecursiveCte() {
      // when
      var result = validator.validate(NON_RECURSIVE_CTE_SQL);

      // then
      var expected = new SqlValidationResult.Valid(NON_RECURSIVE_CTE_SQL + " LIMIT 1000", 1000);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptUppercaseTableNames() {
      // when
      var result = validator.validate(UPPERCASE_TABLE_SQL);

      // then
      var expected = new SqlValidationResult.Valid(UPPERCASE_TABLE_SQL + " LIMIT 1000", 1000);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class RejectedStatements {

    @Test
    void shouldRejectInsertStatement() {
      // when
      var result = validator.validate(INSERT_SQL);

      // then
      var expected = new SqlValidationResult.Invalid("Only SELECT queries allowed", "STBLPAY-5002");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectDeleteStatement() {
      // when
      var result = validator.validate(DELETE_SQL);

      // then
      var expected = new SqlValidationResult.Invalid("Only SELECT queries allowed", "STBLPAY-5002");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectWithRecursive() {
      // when
      var result = validator.validate(WITH_RECURSIVE_SQL);

      // then
      var expected = new SqlValidationResult.Invalid("WITH RECURSIVE not allowed", "STBLPAY-5003");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectUnknownTable() {
      // when
      var result = validator.validate(UNKNOWN_TABLE_SQL);

      // then
      var expected =
          new SqlValidationResult.Invalid(
              "Table postgres.public.users not in allowlist", "STBLPAY-5004");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectDlqTable() {
      // when
      var result = validator.validate(DLQ_TABLE_SQL);

      // then
      var expected =
          new SqlValidationResult.Invalid(
              "Table iceberg.dlq.dlq_events not in allowlist", "STBLPAY-5004");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectUpdateStatement() {
      // when
      var result = validator.validate(UPDATE_SQL);

      // then
      var expected = new SqlValidationResult.Invalid("Only SELECT queries allowed", "STBLPAY-5002");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectDisallowedTableInSubquery() {
      // when
      var result = validator.validate(SUBQUERY_DISALLOWED_SQL);

      // then
      var expected =
          new SqlValidationResult.Invalid(
              "Table postgres.public.users not in allowlist", "STBLPAY-5004");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectDisallowedTableInJoin() {
      // when
      var result = validator.validate(JOIN_MIXED_SQL);

      // then
      var expected =
          new SqlValidationResult.Invalid(
              "Table postgres.public.users not in allowlist", "STBLPAY-5004");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class LimitHandling {

    @Test
    void shouldInjectDefaultLimitWhenMissing() {
      // when
      var result = validator.validate(MISSING_LIMIT_SQL);

      // then
      var expected = new SqlValidationResult.Valid(MISSING_LIMIT_SQL + " LIMIT 1000", 1000);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldCapOversizedLimit() {
      // when
      var result = validator.validate(OVERSIZED_LIMIT_SQL);

      // then
      var expected =
          new SqlValidationResult.Valid(
              "SELECT * FROM iceberg.analytics.v_payment_summary LIMIT 10000", 10000);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class EdgeCases {

    @Test
    void shouldRejectEmptyInput() {
      // when
      var result = validator.validate("");

      // then
      var expected = new SqlValidationResult.Invalid("SQL is empty", "STBLPAY-5001");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectNullInput() {
      // when
      var result = validator.validate(null);

      // then
      var expected = new SqlValidationResult.Invalid("SQL is empty", "STBLPAY-5001");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectMalformedSql() {
      // when
      var result = validator.validate("NOT VALID SQL AT ALL !!!");

      // then
      var expected = new SqlValidationResult.Invalid("SQL parse error", "STBLPAY-5001");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
