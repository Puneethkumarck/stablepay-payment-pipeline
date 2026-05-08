package io.stablepay.api.domain.agent;

import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AllowedTableRegistry {

  private static final Set<Pattern> ALLOWED =
      Set.of(
          Pattern.compile("^iceberg\\.analytics\\.v_[a-z_]+$"),
          Pattern.compile("^iceberg\\.facts\\.fact_[a-z_]+$"),
          Pattern.compile("^iceberg\\.agg\\.agg_[a-z_]+$"));

  public boolean isAllowed(String fullyQualifiedName) {
    var normalized = fullyQualifiedName.toLowerCase(java.util.Locale.ROOT);
    return ALLOWED.stream().anyMatch(p -> p.matcher(normalized).matches());
  }
}
