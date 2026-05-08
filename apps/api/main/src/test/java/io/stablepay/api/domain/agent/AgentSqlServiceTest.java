package io.stablepay.api.domain.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentSqlServiceTest {

  private static final String VALID_SQL =
      "SELECT * FROM iceberg.analytics.v_payment_summary WHERE currency = 'USD'";
  private static final String SANITIZED_SQL = VALID_SQL + " LIMIT 1000";

  @Mock private TrinoSqlAllowlistValidator validator;
  @Mock private AgentSqlExecutor executor;

  @InjectMocks private AgentSqlService service;

  @Nested
  class Execute {

    @Test
    void shouldReturnRejectedWhenValidationFails() {
      // given
      given(validator.validate(VALID_SQL))
          .willReturn(new SqlValidationResult.Invalid("Table not in allowlist", "STBLPAY-5004"));

      // when
      var actual = service.execute(VALID_SQL, 1000);

      // then
      var expected = new SqlExecutionResult.Rejected("STBLPAY-5004", "Table not in allowlist");
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldDelegateToExecutorOnValid() {
      // given
      var success =
          new SqlExecutionResult.Success(
              List.of("currency", "total"), List.of(List.of("USD", 100L)), 1, "q-1", 42L);
      given(validator.validate(VALID_SQL))
          .willReturn(new SqlValidationResult.Valid(SANITIZED_SQL, 1000));
      given(executor.executeQuery(SANITIZED_SQL, 1000)).willReturn(success);

      // when
      var actual = service.execute(VALID_SQL, 1000);

      // then
      assertThat(actual).usingRecursiveComparison().isEqualTo(success);
    }

    @Test
    void shouldReturnFailedWhenExecutorFails() {
      // given
      var failed = new SqlExecutionResult.Failed("STBLPAY-5005", "Query execution failed");
      given(validator.validate(VALID_SQL))
          .willReturn(new SqlValidationResult.Valid(SANITIZED_SQL, 1000));
      given(executor.executeQuery(SANITIZED_SQL, 1000)).willReturn(failed);

      // when
      var actual = service.execute(VALID_SQL, 1000);

      // then
      assertThat(actual).usingRecursiveComparison().isEqualTo(failed);
    }
  }
}
