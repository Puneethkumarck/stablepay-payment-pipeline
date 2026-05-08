package io.stablepay.api.domain.agent;

import io.stablepay.api.domain.port.TimelineRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentTimelineService {

  private static final DateTimeFormatter TS_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

  private final TimelineRepository timelineRepository;

  public FetchResult fetch(String reference) {
    var entries = timelineRepository.findByReferenceAdmin(reference);
    if (entries.isEmpty()) {
      return new FetchResult.NotFound();
    }
    var markdown = renderMarkdown(reference, entries);
    return new FetchResult.Found(new Timeline(reference, markdown, entries));
  }

  private String renderMarkdown(String reference, List<TimelineEntry> entries) {
    var sb = new StringBuilder();
    sb.append("# Timeline for ").append(reference).append("\n\n");
    sb.append("| # | Time (UTC) | Source | Status | Event ID | Detail |\n");
    sb.append("|---|------------|--------|--------|----------|--------|\n");
    for (var i = 0; i < entries.size(); i++) {
      var entry = entries.get(i);
      var shortId = safeSubstring(entry.eventId(), 8);
      sb.append("| ")
          .append(i + 1)
          .append(" | ")
          .append(TS_FORMAT.format(entry.timestamp()))
          .append(" | ")
          .append(entry.source())
          .append(" | ")
          .append(entry.status())
          .append(" | ")
          .append(shortId)
          .append(" | ")
          .append(entry.detail())
          .append(" |\n");
    }
    return sb.toString();
  }

  static String safeSubstring(String value, int maxLen) {
    return value.length() <= maxLen ? value : value.substring(0, maxLen);
  }
}
