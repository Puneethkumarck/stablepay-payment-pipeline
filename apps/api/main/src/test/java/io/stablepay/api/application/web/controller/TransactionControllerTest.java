package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_REFERENCE;
import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_TRANSACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.mapper.AmountMapperImpl;
import io.stablepay.api.application.web.mapper.TransactionWebMapper;
import io.stablepay.api.application.web.mapper.TransactionWebMapperImpl;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.TransactionSearch;
import io.stablepay.api.domain.port.TransactionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

  private static final AuthenticatedUser SOME_CUSTOMER_USER = someCustomerUser();

  @Mock private TransactionRepository transactionRepository;

  @Spy private TransactionWebMapper mapper = new TransactionWebMapperImpl(new AmountMapperImpl());

  @InjectMocks private TransactionController controller;

  @Nested
  class FindByReference {

    @Test
    void shouldReturnTransactionByReference() {
      // given
      given(
              transactionRepository.findByReference(
                  SOME_REFERENCE, SOME_CUSTOMER_USER.requireCustomerId()))
          .willReturn(Optional.of(SOME_TRANSACTION));

      // when
      var actual = controller.findByReference(SOME_REFERENCE, SOME_CUSTOMER_USER);

      // then
      assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
      var expected = mapper.toDto(SOME_TRANSACTION);
      assertThat(actual.getBody()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowNotFoundWhenTransactionMissing() {
      // given
      given(
              transactionRepository.findByReference(
                  SOME_REFERENCE, SOME_CUSTOMER_USER.requireCustomerId()))
          .willReturn(Optional.empty());

      // when/then
      assertThatThrownBy(() -> controller.findByReference(SOME_REFERENCE, SOME_CUSTOMER_USER))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  class ListTransactions {

    @Test
    void shouldReturnPaginatedTransactionList() {
      // given
      var paginatedResult =
          new PaginatedResult<>(List.of(SOME_TRANSACTION), Optional.of("cursor-1"));
      var search =
          TransactionSearch.builder()
              .customerStatus(Optional.empty())
              .reference(Optional.empty())
              .flowType(Optional.empty())
              .internalStatus(Optional.empty())
              .from(Optional.empty())
              .to(Optional.empty())
              .pageSize(20)
              .cursor(Optional.empty())
              .build();
      given(transactionRepository.search(search, SOME_CUSTOMER_USER.requireCustomerId()))
          .willReturn(paginatedResult);

      // when
      var actual = controller.list(Optional.empty(), Optional.empty(), 20, SOME_CUSTOMER_USER);

      // then
      var expected = mapper.toResponse(paginatedResult);
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnPaginatedTransactionListWithStatusFilter() {
      // given
      var paginatedResult = new PaginatedResult<>(List.of(SOME_TRANSACTION), Optional.empty());
      var search =
          TransactionSearch.builder()
              .customerStatus(Optional.of("COMPLETED"))
              .reference(Optional.empty())
              .flowType(Optional.empty())
              .internalStatus(Optional.empty())
              .from(Optional.empty())
              .to(Optional.empty())
              .pageSize(20)
              .cursor(Optional.empty())
              .build();
      given(transactionRepository.search(search, SOME_CUSTOMER_USER.requireCustomerId()))
          .willReturn(paginatedResult);

      // when
      var actual =
          controller.list(Optional.of("COMPLETED"), Optional.empty(), 20, SOME_CUSTOMER_USER);

      // then
      var expected = mapper.toResponse(paginatedResult);
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
