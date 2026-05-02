package io.stablepay.api.infrastructure.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PostgresOutboxRepositoryTest {

  @Mock NamedParameterJdbcTemplate jdbc;

  @Test
  void publishIdempotent_throwsNpe_whenIdempotencyKeyIsNull() {
    // given
    var repository = new PostgresOutboxRepository(jdbc);

    // when / then
    assertThatThrownBy(() -> repository.publishIdempotent(null, "topic", new byte[] {1}))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("idempotencyKey");
  }

  @Test
  void publishIdempotent_throwsNpe_whenTopicIsNull() {
    // given
    var repository = new PostgresOutboxRepository(jdbc);

    // when / then
    assertThatThrownBy(() -> repository.publishIdempotent("key", null, new byte[] {1}))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("topic");
  }

  @Test
  void publishIdempotent_throwsNpe_whenPayloadIsNull() {
    // given
    var repository = new PostgresOutboxRepository(jdbc);

    // when / then
    assertThatThrownBy(() -> repository.publishIdempotent("key", "topic", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("payload");
  }

  @Test
  void sqlConstant_matchesPartialUniqueIndexShape() {
    // given
    var expected =
        "INSERT INTO namastack_outbox (idempotency_key, event_topic, payload, created_at)"
            + " VALUES (:key, :topic, :payload, :now)"
            + " ON CONFLICT (idempotency_key, event_topic)"
            + " WHERE idempotency_key IS NOT NULL DO NOTHING";

    // when
    var actual = PostgresOutboxRepository.SQL_PUBLISH_IDEMPOTENT;

    // then
    assertThat(actual).isEqualTo(expected);
  }
}
