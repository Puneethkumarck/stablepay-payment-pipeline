package io.stablepay.api.domain.agent;

import static io.stablepay.api.domain.agent.fixtures.AgentServiceFixtures.SOME_REFERENCE;
import static io.stablepay.api.domain.agent.fixtures.AgentServiceFixtures.SOME_TIMELINE_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.domain.port.TimelineRepository;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentTimelineServiceTest {

  @Mock private TimelineRepository timelineRepository;

  @InjectMocks private AgentTimelineService service;

  @Nested
  class Fetch {

    @Test
    void shouldReturnFoundWithTimelineWhenEntriesExist() {
      // given
      given(timelineRepository.findByReferenceAdmin(SOME_REFERENCE))
          .willReturn(SOME_TIMELINE_ENTRIES);

      // when
      var actual = service.fetch(SOME_REFERENCE);

      // then
      var found = (FetchResult.Found) actual;
      var expected =
          new FetchResult.Found(
              new Timeline(SOME_REFERENCE, found.timeline().markdown(), SOME_TIMELINE_ENTRIES));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnNotFoundWhenNoEntries() {
      // given
      given(timelineRepository.findByReferenceAdmin(SOME_REFERENCE)).willReturn(List.of());

      // when
      var actual = service.fetch(SOME_REFERENCE);

      // then
      var expected = new FetchResult.NotFound();
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldIncludeMarkdownWithTableHeaders() {
      // given
      given(timelineRepository.findByReferenceAdmin(SOME_REFERENCE))
          .willReturn(SOME_TIMELINE_ENTRIES);

      // when
      var actual = service.fetch(SOME_REFERENCE);

      // then
      var found = (FetchResult.Found) actual;
      assertThat(found.timeline().markdown()).contains("# Timeline for " + SOME_REFERENCE);
      assertThat(found.timeline().markdown()).contains("| # | Time (UTC) |");
    }
  }

  @Nested
  class SafeSubstring {

    @Test
    void shouldReturnFullStringWhenShorterThanMax() {
      // given
      var shortId = "abc";

      // when
      var actual = AgentTimelineService.safeSubstring(shortId, 8);

      // then
      assertThat(actual).isEqualTo("abc");
    }

    @Test
    void shouldTruncateWhenLongerThanMax() {
      // given
      var longId = "abcdefghijklmnop";

      // when
      var actual = AgentTimelineService.safeSubstring(longId, 8);

      // then
      assertThat(actual).isEqualTo("abcdefgh");
    }

    @Test
    void shouldReturnFullStringWhenExactlyMaxLength() {
      // given
      var exactId = "abcdefgh";

      // when
      var actual = AgentTimelineService.safeSubstring(exactId, 8);

      // then
      assertThat(actual).isEqualTo("abcdefgh");
    }
  }
}
