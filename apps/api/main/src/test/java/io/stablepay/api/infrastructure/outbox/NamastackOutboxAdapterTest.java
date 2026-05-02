package io.stablepay.api.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import io.namastack.outbox.Outbox;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NamastackOutboxAdapterTest {

  @Mock Outbox outbox;

  @Test
  void shouldDelegateToOutboxScheduleWithEncodedEnvelopeAndKey() {
    // given
    var adapter = new NamastackOutboxAdapter(outbox);
    var topic = "stablepay.test.events";
    var idempotencyKey = "test-key";
    var payload = "hello".getBytes(StandardCharsets.UTF_8);
    var expectedEnvelope =
        OutboxEnvelope.builder()
            .topic(topic)
            .payloadBase64(Base64.getEncoder().encodeToString(payload))
            .build();

    // when
    adapter.publishIdempotent(idempotencyKey, topic, payload);

    // then
    var envelopeCaptor = ArgumentCaptor.forClass(Object.class);
    var keyCaptor = ArgumentCaptor.forClass(String.class);
    then(outbox).should().schedule(envelopeCaptor.capture(), keyCaptor.capture());
    assertThat(envelopeCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedEnvelope);
    assertThat(keyCaptor.getValue()).isEqualTo(idempotencyKey);
  }

  @Test
  void shouldThrowNpeWhenIdempotencyKeyIsNull() {
    // given
    var adapter = new NamastackOutboxAdapter(outbox);

    // when / then
    assertThatThrownBy(() -> adapter.publishIdempotent(null, "topic", new byte[] {1}))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("idempotencyKey");
  }

  @Test
  void shouldThrowNpeWhenTopicIsNull() {
    // given
    var adapter = new NamastackOutboxAdapter(outbox);

    // when / then
    assertThatThrownBy(() -> adapter.publishIdempotent("key", null, new byte[] {1}))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("topic");
  }

  @Test
  void shouldThrowNpeWhenPayloadIsNull() {
    // given
    var adapter = new NamastackOutboxAdapter(outbox);

    // when / then
    assertThatThrownBy(() -> adapter.publishIdempotent("key", "topic", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("payload");
  }
}
