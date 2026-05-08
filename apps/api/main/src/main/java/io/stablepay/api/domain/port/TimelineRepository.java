package io.stablepay.api.domain.port;

import io.stablepay.api.domain.agent.TimelineEntry;
import java.util.List;

public interface TimelineRepository {

  List<TimelineEntry> findByReferenceAdmin(String reference);
}
