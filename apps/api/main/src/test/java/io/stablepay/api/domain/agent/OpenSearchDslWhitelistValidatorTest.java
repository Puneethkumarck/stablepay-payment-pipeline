package io.stablepay.api.domain.agent;

import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidAggFieldRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidCalendarIntervalRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidFieldRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidIndexRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.invalidSortFieldRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.negativeSizeRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.oversizedRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validAggsRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validAllAggTypesRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validBoolTermRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validBoolWithMustNotRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validExistsRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validMatchPhraseRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validRangeRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.validTermsRequest;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.zeroSizeRequest;
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
      // given
      var request = validBoolTermRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 50);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptValidRangeQuery() {
      // given
      var request = validRangeRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 100);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptValidMatchPhraseQuery() {
      // given
      var request = validMatchPhraseRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 100);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptValidExistsQuery() {
      // given
      var request = validExistsRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 100);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptValidTermsQuery() {
      // given
      var request = validTermsRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 100);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptBoolWithMustNotClauses() {
      // given
      var request = validBoolWithMustNotRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 100);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
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
      // given
      var request = oversizedRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 1000);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldClampNegativeSizeTo1() {
      // given
      var request = negativeSizeRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 1);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldClampZeroSizeTo1() {
      // given
      var request = zeroSizeRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 1);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class AggregationValidation {

    @Test
    void shouldAcceptValidAggregations() {
      // given
      var request = validAggsRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 100);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldAcceptAllAggregationTypes() {
      // given
      var request = validAllAggTypesRequest();

      // when
      var result = validator.validate(request);

      // then
      var expected = new DslValidationResult.Valid(request, 100);
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
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

    @Test
    void shouldRejectInvalidCalendarInterval() {
      // when
      var result = validator.validate(invalidCalendarIntervalRequest());

      // then
      var expected =
          new DslValidationResult.Invalid(
              "Calendar interval not allowed: fortnight", "STBLPAY-6003");
      assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
