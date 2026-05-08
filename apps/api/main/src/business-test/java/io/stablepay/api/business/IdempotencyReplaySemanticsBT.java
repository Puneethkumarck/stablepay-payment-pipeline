package io.stablepay.api.business;

import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_ID;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.application.web.dto.DlqReplayResponse;
import io.stablepay.api.config.BusinessTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class IdempotencyReplaySemanticsBT extends BusinessTestBase {

  @Test
  void firstReplayShouldReturn200() {
    // given
    var client = authenticatedClient(adminJwt());
    var idempotencyKey = UUID.randomUUID().toString();

    // when
    var response =
        client
            .post()
            .uri("/api/v1/admin/dlq/{id}/replay", SOME_DLQ_ID.value())
            .header("X-Idempotency-Key", idempotencyKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(DlqReplayResponse.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().dlqId()).isEqualTo(SOME_DLQ_ID.value().toString());
    assertThat(response.getBody().status()).isEqualTo("ACCEPTED");
  }

  @Test
  void duplicateReplayShouldReturn200WithReplayedHeader() {
    // given
    var client = authenticatedClient(adminJwt());
    var idempotencyKey = UUID.randomUUID().toString();
    client
        .post()
        .uri("/api/v1/admin/dlq/{id}/replay", SOME_DLQ_ID.value())
        .header("X-Idempotency-Key", idempotencyKey)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(DlqReplayResponse.class);

    // when
    var replayResponse =
        client
            .post()
            .uri("/api/v1/admin/dlq/{id}/replay", SOME_DLQ_ID.value())
            .header("X-Idempotency-Key", idempotencyKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(DlqReplayResponse.class);

    // then
    assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(replayResponse.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("true");
    assertThat(replayResponse.getBody()).isNotNull();
    assertThat(replayResponse.getBody().dlqId()).isEqualTo(SOME_DLQ_ID.value().toString());
  }
}
