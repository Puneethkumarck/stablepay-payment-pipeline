package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
import static io.stablepay.api.domain.model.fixtures.StuckPaymentFixtures.SOME_STUCK_PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.mapper.AmountMapperImpl;
import io.stablepay.api.application.web.mapper.StuckPaymentWebMapper;
import io.stablepay.api.application.web.mapper.StuckPaymentWebMapperImpl;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.port.StuckRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminStuckControllerTest {

  private static final AuthenticatedUser SOME_ADMIN_USER = someAdminUser();

  @Mock private StuckRepository stuckRepository;

  @Spy private StuckPaymentWebMapper mapper = new StuckPaymentWebMapperImpl(new AmountMapperImpl());

  @InjectMocks private AdminStuckController controller;

  @Nested
  class ListStuckPayments {

    @Test
    void shouldReturnPaginatedStuckPaymentList() {
      // given
      var paginatedResult = new PaginatedResult<>(List.of(SOME_STUCK_PAYMENT), Optional.empty());
      given(stuckRepository.searchAdmin(20, Optional.empty())).willReturn(paginatedResult);

      // when
      var actual = controller.list(Optional.empty(), 20, SOME_ADMIN_USER);

      // then
      var expected = mapper.toResponse(paginatedResult);
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
