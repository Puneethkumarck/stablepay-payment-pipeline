package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_EVENT;
import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_ID;
import static io.stablepay.api.domain.model.fixtures.DlqSummaryFixtures.SOME_DLQ_SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.dto.DlqReplayResponse;
import io.stablepay.api.application.web.mapper.DlqEventWebMapper;
import io.stablepay.api.application.web.mapper.DlqSummaryWebMapper;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.port.DlqRepository;
import io.stablepay.api.domain.service.DlqReplayService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminDlqControllerTest {

  private static final AuthenticatedUser SOME_ADMIN_USER = someAdminUser();
  private static final Instant FIXED_NOW = Instant.parse("2026-05-08T10:00:00Z");

  @Mock private DlqRepository dlqRepository;
  @Mock private DlqReplayService dlqReplayService;

  @Spy private DlqEventWebMapper mapper = Mappers.getMapper(DlqEventWebMapper.class);
  @Spy private DlqSummaryWebMapper dlqSummaryMapper = Mappers.getMapper(DlqSummaryWebMapper.class);
  @Spy private Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  @InjectMocks private AdminDlqController controller;

  @Nested
  class ListDlqEvents {

    @Test
    void shouldReturnPaginatedDlqList() {
      // given
      var paginatedResult = new PaginatedResult<>(List.of(SOME_DLQ_EVENT), Optional.of("cursor-1"));
      given(dlqRepository.searchAdmin(20, Optional.empty())).willReturn(paginatedResult);

      // when
      var actual = controller.list(Optional.empty(), 20, SOME_ADMIN_USER);

      // then
      var expected = mapper.toResponse(paginatedResult);
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class Summary {

    @Test
    void shouldReturnDlqSummary() {
      // given
      given(dlqRepository.summaryAdmin()).willReturn(SOME_DLQ_SUMMARY);

      // when
      var actual = controller.summary(SOME_ADMIN_USER);

      // then
      var expected = dlqSummaryMapper.toDto(SOME_DLQ_SUMMARY);
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }

  @Nested
  class FindById {

    @Test
    void shouldReturnDlqEventById() {
      // given
      given(dlqRepository.findByIdAdmin(SOME_DLQ_ID)).willReturn(Optional.of(SOME_DLQ_EVENT));

      // when
      var actual = controller.findById(SOME_DLQ_ID.value().toString(), SOME_ADMIN_USER);

      // then
      assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
      var expected = mapper.toDto(SOME_DLQ_EVENT);
      assertThat(actual.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowNotFoundWhenDlqEventMissing() {
      // given
      given(dlqRepository.findByIdAdmin(SOME_DLQ_ID)).willReturn(Optional.empty());

      // when/then
      assertThatThrownBy(() -> controller.findById(SOME_DLQ_ID.value().toString(), SOME_ADMIN_USER))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  class Replay {

    @Test
    void shouldReplayDlqEvent() {
      // given
      given(dlqReplayService.replay(SOME_DLQ_ID, SOME_ADMIN_USER.userId()))
          .willReturn(SOME_DLQ_EVENT);

      // when
      var actual = controller.replay(SOME_DLQ_ID.value().toString(), SOME_ADMIN_USER);

      // then
      var expected =
          DlqReplayResponse.builder()
              .dlqId(SOME_DLQ_ID.value().toString())
              .status("ACCEPTED")
              .timestamp(FIXED_NOW)
              .build();
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
      then(dlqReplayService).should().replay(SOME_DLQ_ID, SOME_ADMIN_USER.userId());
    }

    @Test
    void shouldThrowNotFoundWhenDlqReplayEventMissing() {
      // given
      given(dlqReplayService.replay(SOME_DLQ_ID, SOME_ADMIN_USER.userId()))
          .willThrow(new NotFoundException("DLQ event", SOME_DLQ_ID.value().toString()));

      // when/then
      assertThatThrownBy(() -> controller.replay(SOME_DLQ_ID.value().toString(), SOME_ADMIN_USER))
          .isInstanceOf(NotFoundException.class);
    }
  }
}
