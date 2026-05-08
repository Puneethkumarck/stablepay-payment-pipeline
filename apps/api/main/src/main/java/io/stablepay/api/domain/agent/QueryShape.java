package io.stablepay.api.domain.agent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;

public sealed interface QueryShape
    permits QueryShape.TermShape,
        QueryShape.RangeShape,
        QueryShape.MatchPhraseShape,
        QueryShape.BoolShape,
        QueryShape.ExistsShape,
        QueryShape.TermsShape {

  @Builder(toBuilder = true)
  record TermShape(String field, Object value) implements QueryShape {
    public TermShape {
      Objects.requireNonNull(field);
      Objects.requireNonNull(value);
    }
  }

  @Builder(toBuilder = true)
  record RangeShape(String field, Optional<Object> gte, Optional<Object> lte)
      implements QueryShape {
    public RangeShape {
      Objects.requireNonNull(field);
      Objects.requireNonNull(gte);
      Objects.requireNonNull(lte);
    }
  }

  @Builder(toBuilder = true)
  record MatchPhraseShape(String field, String phrase) implements QueryShape {
    public MatchPhraseShape {
      Objects.requireNonNull(field);
      Objects.requireNonNull(phrase);
    }
  }

  @Builder(toBuilder = true)
  record BoolShape(List<QueryShape> must, List<QueryShape> filter, List<QueryShape> mustNot)
      implements QueryShape {
    public BoolShape {
      Objects.requireNonNull(must);
      Objects.requireNonNull(filter);
      Objects.requireNonNull(mustNot);
    }
  }

  @Builder(toBuilder = true)
  record ExistsShape(String field) implements QueryShape {
    public ExistsShape {
      Objects.requireNonNull(field);
    }
  }

  @Builder(toBuilder = true)
  record TermsShape(String field, List<Object> values) implements QueryShape {
    public TermsShape {
      Objects.requireNonNull(field);
      Objects.requireNonNull(values);
    }
  }
}
