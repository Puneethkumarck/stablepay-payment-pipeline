package io.stablepay.api.business;

import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_REFERENCE;
import static org.assertj.core.api.Assertions.assertThat;

import io.stablepay.api.application.web.dto.TransactionDto;
import io.stablepay.api.config.BusinessTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

class CrossCustomerEnforcementBT extends BusinessTestBase {

  @Test
  void aliceShouldSeeHerOwnTransaction() {
    // given
    var client = authenticatedClient(aliceJwt());

    // when
    var response =
        client
            .get()
            .uri("/api/v1/transactions/{ref}", SOME_REFERENCE)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(TransactionDto.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().reference()).isEqualTo(SOME_REFERENCE);
  }

  @Test
  void bobShouldGet404ForAlicesTransaction() {
    // given
    var client = authenticatedClient(bobJwt());

    // when
    var exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            HttpClientErrorException.class,
            () ->
                client
                    .get()
                    .uri("/api/v1/transactions/{ref}", SOME_REFERENCE)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class));

    // then
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    var expected = "STBLPAY-3001";
    assertThat(exception.getResponseBodyAsString()).contains(expected);
  }

  @Test
  void adminShouldSeeAnyTransaction() {
    // given
    var client = authenticatedClient(adminJwt());

    // when
    var response =
        client
            .get()
            .uri("/api/v1/admin/transactions/{ref}", SOME_REFERENCE)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(TransactionDto.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().reference()).isEqualTo(SOME_REFERENCE);
  }
}
