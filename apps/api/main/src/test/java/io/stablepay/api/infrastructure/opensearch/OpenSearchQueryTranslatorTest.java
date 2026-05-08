package io.stablepay.api.infrastructure.opensearch;

import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_AVG_AGG;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_BOOL_TERM_QUERY;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_BOOL_WITH_MUST_NOT_QUERY;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_DATE_HISTOGRAM_AGG;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_EXISTS_QUERY;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_MATCH_PHRASE_QUERY;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_RANGE_QUERY;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_SUM_AGG;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_TERMS_AGG;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_TERMS_QUERY;
import static io.stablepay.api.domain.agent.fixtures.DslValidationFixtures.VALID_TERM_QUERY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenSearchQueryTranslatorTest {

  private final OpenSearchQueryTranslator translator = new OpenSearchQueryTranslator();

  @Nested
  class QueryTranslation {

    @Test
    void shouldTranslateTermQuery() {
      // when
      var result = translator.translateQuery(VALID_TERM_QUERY);

      // then
      assertThat(result.isTerm()).isTrue();
      assertThat(result.term().field()).isEqualTo("customer_id");
      assertThat(result.term().value().stringValue()).isEqualTo("cust-001");
    }

    @Test
    void shouldTranslateRangeQuery() {
      // when
      var result = translator.translateQuery(VALID_RANGE_QUERY);

      // then
      assertThat(result.isRange()).isTrue();
      assertThat(result.range().field()).isEqualTo("event_time");
    }

    @Test
    void shouldTranslateMatchPhraseQuery() {
      // when
      var result = translator.translateQuery(VALID_MATCH_PHRASE_QUERY);

      // then
      assertThat(result.isMatchPhrase()).isTrue();
      assertThat(result.matchPhrase().field()).isEqualTo("flow_type");
      assertThat(result.matchPhrase().query()).isEqualTo("ON_RAMP");
    }

    @Test
    void shouldTranslateExistsQuery() {
      // when
      var result = translator.translateQuery(VALID_EXISTS_QUERY);

      // then
      assertThat(result.isExists()).isTrue();
      assertThat(result.exists().field()).isEqualTo("error_class");
    }

    @Test
    void shouldTranslateTermsQuery() {
      // when
      var result = translator.translateQuery(VALID_TERMS_QUERY);

      // then
      assertThat(result.isTerms()).isTrue();
      assertThat(result.terms().field()).isEqualTo("internal_status");
    }

    @Test
    void shouldTranslateBoolQuery() {
      // when
      var result = translator.translateQuery(VALID_BOOL_TERM_QUERY);

      // then
      assertThat(result.isBool()).isTrue();
      assertThat(result.bool().must()).hasSize(1);
      assertThat(result.bool().filter()).hasSize(1);
      assertThat(result.bool().mustNot()).isEmpty();
    }

    @Test
    void shouldTranslateBoolQueryWithMustNot() {
      // when
      var result = translator.translateQuery(VALID_BOOL_WITH_MUST_NOT_QUERY);

      // then
      assertThat(result.isBool()).isTrue();
      assertThat(result.bool().must()).hasSize(1);
      assertThat(result.bool().filter()).isEmpty();
      assertThat(result.bool().mustNot()).hasSize(1);
    }
  }

  @Nested
  class AggregationTranslation {

    @Test
    void shouldTranslateTermsAgg() {
      // when
      var result = translator.translateAggs(List.of(VALID_TERMS_AGG));

      // then
      assertThat(result).containsKey("by_status");
      assertThat(result.get("by_status").isTerms()).isTrue();
    }

    @Test
    void shouldTranslateDateHistogramAgg() {
      // when
      var result = translator.translateAggs(List.of(VALID_DATE_HISTOGRAM_AGG));

      // then
      assertThat(result).containsKey("over_time");
      assertThat(result.get("over_time").isDateHistogram()).isTrue();
    }

    @Test
    void shouldTranslateSumAgg() {
      // when
      var result = translator.translateAggs(List.of(VALID_SUM_AGG));

      // then
      assertThat(result).containsKey("total_amount");
      assertThat(result.get("total_amount").isSum()).isTrue();
    }

    @Test
    void shouldTranslateAvgAgg() {
      // when
      var result = translator.translateAggs(List.of(VALID_AVG_AGG));

      // then
      assertThat(result).containsKey("avg_amount");
      assertThat(result.get("avg_amount").isAvg()).isTrue();
    }

    @Test
    void shouldTranslateMultipleAggs() {
      // when
      var result =
          translator.translateAggs(
              List.of(VALID_TERMS_AGG, VALID_DATE_HISTOGRAM_AGG, VALID_SUM_AGG, VALID_AVG_AGG));

      // then
      assertThat(result).hasSize(4);
      assertThat(result).containsKeys("by_status", "over_time", "total_amount", "avg_amount");
    }
  }
}
