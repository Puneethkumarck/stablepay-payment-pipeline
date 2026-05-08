package io.stablepay.api.domain.agent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import lombok.Builder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AggShape.TermsAggShape.class, name = "terms"),
  @JsonSubTypes.Type(value = AggShape.DateHistogramAggShape.class, name = "date_histogram"),
  @JsonSubTypes.Type(value = AggShape.SumAggShape.class, name = "sum"),
  @JsonSubTypes.Type(value = AggShape.AvgAggShape.class, name = "avg")
})
public sealed interface AggShape
    permits AggShape.TermsAggShape,
        AggShape.DateHistogramAggShape,
        AggShape.SumAggShape,
        AggShape.AvgAggShape {

  @Builder(toBuilder = true)
  record TermsAggShape(String name, String field, int size) implements AggShape {
    public TermsAggShape {
      Objects.requireNonNull(name);
      Objects.requireNonNull(field);
      if (size <= 0) {
        throw new IllegalArgumentException("TermsAggShape size must be positive");
      }
    }
  }

  @Builder(toBuilder = true)
  record DateHistogramAggShape(String name, String field, String calendarInterval)
      implements AggShape {
    public DateHistogramAggShape {
      Objects.requireNonNull(name);
      Objects.requireNonNull(field);
      Objects.requireNonNull(calendarInterval);
    }
  }

  @Builder(toBuilder = true)
  record SumAggShape(String name, String field) implements AggShape {
    public SumAggShape {
      Objects.requireNonNull(name);
      Objects.requireNonNull(field);
    }
  }

  @Builder(toBuilder = true)
  record AvgAggShape(String name, String field) implements AggShape {
    public AvgAggShape {
      Objects.requireNonNull(name);
      Objects.requireNonNull(field);
    }
  }
}
