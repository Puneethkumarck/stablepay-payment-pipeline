package io.stablepay.api.domain.agent;

import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validBoolTermRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentSearchServiceTest {

  @Mock private OpenSearchDslWhitelistValidator validator;
  @Mock private AgentSearchExecutor executor;

  @InjectMocks private AgentSearchService service;

  @Nested
  class Execute {

    @Test
    void shouldReturnRejectedWhenValidationFails() {
      // given
      var request = validBoolTermRequest();
      given(validator.validate(request))
          .willReturn(
              new DslValidationResult.Invalid("Index not in allowlist: secret", "STBLPAY-6001"));

      // when
      var actual = service.execute(request);

      // then
      var expected =
          new SearchExecutionResult.Rejected("STBLPAY-6001", "Index not in allowlist: secret");
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldDelegateToExecutorOnValid() {
      // given
      var request = validBoolTermRequest();
      var success =
          new SearchExecutionResult.Success(List.of(Map.of("event_id", "e1")), 1L, 50, 15L);
      given(validator.validate(request)).willReturn(new DslValidationResult.Valid(request, 50));
      given(executor.executeSearch(request, 50)).willReturn(success);

      // when
      var actual = service.execute(request);

      // then
      assertThat(actual).usingRecursiveComparison().isEqualTo(success);
    }

    @Test
    void shouldReturnFailedWhenExecutorFails() {
      // given
      var request = validBoolTermRequest();
      var failed = new SearchExecutionResult.Failed("STBLPAY-6006", "Search execution failed");
      given(validator.validate(request)).willReturn(new DslValidationResult.Valid(request, 50));
      given(executor.executeSearch(request, 50)).willReturn(failed);

      // when
      var actual = service.execute(request);

      // then
      assertThat(actual).usingRecursiveComparison().isEqualTo(failed);
    }
  }
}
