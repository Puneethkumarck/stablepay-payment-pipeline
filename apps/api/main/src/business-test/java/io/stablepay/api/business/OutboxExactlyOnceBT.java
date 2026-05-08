package io.stablepay.api.business;

import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_ID;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.config.BusinessTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestConstructor;

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OutboxExactlyOnceBT extends BusinessTestBase {

  private static final String COUNT_OUTBOX_SQL =
      "SELECT COUNT(*) FROM outbox_record WHERE record_key = :recordKey";

  private final NamedParameterJdbcTemplate jdbc;

  OutboxExactlyOnceBT(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Test
  void replayShouldCreateExactlyOneOutboxRow() {
    // given
    var client = authenticatedClient(adminJwt());
    var idempotencyKey = UUID.randomUUID().toString();
    var expectedRecordKey = "dlq-replay:" + SOME_DLQ_ID.value();
    var params = new MapSqlParameterSource("recordKey", expectedRecordKey);
    var countBefore = jdbc.queryForObject(COUNT_OUTBOX_SQL, params, Long.class);

    // when
    client
        .post()
        .uri("/api/v1/admin/dlq/{id}/replay", SOME_DLQ_ID.value())
        .header("X-Idempotency-Key", idempotencyKey)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(String.class);

    // then
    var countAfter = jdbc.queryForObject(COUNT_OUTBOX_SQL, params, Long.class);
    assertThat(countAfter - countBefore).isEqualTo(1L);
  }

  @Test
  void duplicateReplayShouldNotCreateAdditionalOutboxRow() {
    // given
    var client = authenticatedClient(adminJwt());
    var idempotencyKey = UUID.randomUUID().toString();
    var expectedRecordKey = "dlq-replay:" + SOME_DLQ_ID.value();
    client
        .post()
        .uri("/api/v1/admin/dlq/{id}/replay", SOME_DLQ_ID.value())
        .header("X-Idempotency-Key", idempotencyKey)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(String.class);
    var paramsBeforeDuplicate = new MapSqlParameterSource("recordKey", expectedRecordKey);
    var countBefore = jdbc.queryForObject(COUNT_OUTBOX_SQL, paramsBeforeDuplicate, Long.class);

    // when
    client
        .post()
        .uri("/api/v1/admin/dlq/{id}/replay", SOME_DLQ_ID.value())
        .header("X-Idempotency-Key", idempotencyKey)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(String.class);

    // then
    var paramsAfterDuplicate = new MapSqlParameterSource("recordKey", expectedRecordKey);
    var countAfter = jdbc.queryForObject(COUNT_OUTBOX_SQL, paramsAfterDuplicate, Long.class);
    assertThat(countAfter).isEqualTo(countBefore);
  }
}
