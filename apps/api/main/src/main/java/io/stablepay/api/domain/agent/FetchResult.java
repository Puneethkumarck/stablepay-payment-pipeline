package io.stablepay.api.domain.agent;

import java.util.Objects;
import lombok.Builder;

public sealed interface FetchResult permits FetchResult.Found, FetchResult.NotFound {

  @Builder(toBuilder = true)
  record Found(Timeline timeline) implements FetchResult {
    public Found {
      Objects.requireNonNull(timeline);
    }
  }

  record NotFound() implements FetchResult {}
}
