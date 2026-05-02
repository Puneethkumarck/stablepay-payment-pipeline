package io.stablepay.api.domain.service;

import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_EVENT;
import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.DlqId;
import io.stablepay.api.domain.model.UserId;
import io.stablepay.api.domain.port.DlqRepository;
import io.stablepay.api.domain.port.OutboxRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DlqReplayServiceTest {

  private static final UserId SOME_USER_ID =
      UserId.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

  @Mock private DlqRepository dlqRepository;

  @Mock private OutboxRepository outboxRepository;

  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private DlqReplayService service;

  @Test
  void shouldReplayDlqEvent() {
    // given
    given(dlqRepository.findByIdAdmin(SOME_DLQ_ID)).willReturn(Optional.of(SOME_DLQ_EVENT));

    // when
    var actual = service.replay(SOME_DLQ_ID, SOME_USER_ID);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(SOME_DLQ_EVENT);
    var keyCaptor = ArgumentCaptor.forClass(String.class);
    var topicCaptor = ArgumentCaptor.forClass(String.class);
    var payloadCaptor = ArgumentCaptor.forClass(byte[].class);
    then(outboxRepository)
        .should()
        .publishIdempotent(keyCaptor.capture(), topicCaptor.capture(), payloadCaptor.capture());
    assertThat(keyCaptor.getValue()).isEqualTo("dlq-replay:" + SOME_DLQ_ID.value());
    assertThat(topicCaptor.getValue()).isEqualTo(DlqReplayService.REPLAY_TOPIC);
    assertThat(payloadCaptor.getValue()).isNotEmpty();
  }

  @Test
  void shouldThrowNotFoundWhenDlqEventMissing() {
    // given
    var missingId = DlqId.of(UUID.fromString("00000000-0000-0000-0000-999999999999"));
    given(dlqRepository.findByIdAdmin(missingId)).willReturn(Optional.empty());

    // when/then
    assertThatThrownBy(() -> service.replay(missingId, SOME_USER_ID))
        .isInstanceOf(NotFoundException.class);
  }
}
