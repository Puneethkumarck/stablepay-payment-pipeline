package io.stablepay.api.domain.agent;

import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidAggFieldRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidFieldRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidIndexRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidSortFieldRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.oversizedRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validAggsRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validBoolTermRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validRangeRequest;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenSearchDslWhitelistValidatorTest {

  private final AllowedQueryShapeRegistry registry = new AllowedQueryShapeRegistry();
  private final OpenSearchDslWhitelistValidator validator =
      new OpenSearchDslWhitelistValidator(registry);

  @Nested
  class ValidQueries {

    @Test
    void shouldAcceptValidBoolWithTermQuery() {
      // when
      var result = validator.validate(validBoolTermRequest());

      // then
      assertThat(result).isInstanceOf(DslValidationResult.Valid.class);
      var valid = (DslValidationResult.Valid) result;
      assertThat(valid.size()).isEqualTo(50);
      assertThat(valid.translatedQuery()).isNotNull();
      assertThat(valid.translatedQuery().isBool()).isTrue();
    }

    @Test
    void shouldAcceptValidRangeQuery() {
      // when
      var result = validator.validate(validRangeRequest());

      // then
      assertThat(result).isInstanceOf(DslValidationResult.Valid.class);
      var valid = (DslValidationResult.Valid) result;
      assertThat(valid.size()).isEqualTo(100);
      assertThat(valid.translatedQuery()).isNotNull();
      assertThat(valid.translatedQuery().isRange()).isTrue();
    }
  }

  @Nested
  class RejectedQueries {

    @Test
    void shouldRejectInvalidIndex() {
      // when
      var result = validator.validate(invalidIndexRequest());

      // then
      var expected =
          new DslValidationResult.Invalid("Index not in allowlist: secret-data", "STBLPAY-6001");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectInvalidField() {
      // when
      var result = validator.validate(invalidFieldRequest());

      // then
      var expected =
          new DslValidationResult.Invalid("Field not in allowlist: password_hash", "STBLPAY-6002");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectInvalidSortField() {
      // when
      var result = validator.validate(invalidSortFieldRequest());

      // then
      var expected =
          new DslValidationResult.Invalid(
              "Sort field not in allowlist: password_hash", "STBLPAY-6005");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class SizeHandling {

    @Test
    void shouldCapSizeAbove1000To1000() {
      // when
      var result = validator.validate(oversizedRequest());

      // then
      assertThat(result).isInstanceOf(DslValidationResult.Valid.class);
      var valid = (DslValidationResult.Valid) result;
      assertThat(valid.size()).isEqualTo(1000);
    }
  }

  @Nested
  class AggregationValidation {

    @Test
    void shouldAcceptValidAggregations() {
      // when
      var result = validator.validate(validAggsRequest());

      // then
      assertThat(result).isInstanceOf(DslValidationResult.Valid.class);
      var valid = (DslValidationResult.Valid) result;
      assertThat(valid.translatedAggs()).containsKeys("by_status", "total_amount");
    }

    @Test
    void shouldRejectAggregationWithInvalidField() {
      // when
      var result = validator.validate(invalidAggFieldRequest());

      // then
      var expected =
          new DslValidationResult.Invalid("Field not in allowlist: password_hash", "STBLPAY-6002");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
