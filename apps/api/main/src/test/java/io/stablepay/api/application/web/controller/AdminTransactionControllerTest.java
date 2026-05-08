package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
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
import io.stablepay.api.domain.port.TransactionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminTransactionControllerTest {

  private static final AuthenticatedUser SOME_ADMIN_USER = someAdminUser();

  @Mock private TransactionRepository transactionRepository;

  @Spy private TransactionWebMapper mapper = new TransactionWebMapperImpl(new AmountMapperImpl());

  @InjectMocks private AdminTransactionController controller;

  @Nested
  class FindByReference {

    @Test
    void shouldReturnTransactionByReferenceForAdmin() {
      // given
      given(transactionRepository.findByReferenceAdmin(SOME_REFERENCE))
          .willReturn(Optional.of(SOME_TRANSACTION));

      // when
      var actual = controller.findByReference(SOME_REFERENCE, SOME_ADMIN_USER);

      // then
      var expected = ResponseEntity.ok(mapper.toDto(SOME_TRANSACTION));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowNotFoundWhenTransactionMissingForAdmin() {
      // given
      given(transactionRepository.findByReferenceAdmin(SOME_REFERENCE))
          .willReturn(Optional.empty());

      // when/then
      assertThatThrownBy(() -> controller.findByReference(SOME_REFERENCE, SOME_ADMIN_USER))
          .isInstanceOf(NotFoundException.class);
    }
  }
}
