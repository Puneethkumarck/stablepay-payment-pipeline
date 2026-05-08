package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static io.stablepay.api.domain.model.fixtures.FlowFixtures.SOME_FLOW;
import static io.stablepay.api.domain.model.fixtures.FlowFixtures.SOME_FLOW_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.mapper.AmountMapperImpl;
import io.stablepay.api.application.web.mapper.FlowWebMapper;
import io.stablepay.api.application.web.mapper.FlowWebMapperImpl;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.port.FlowRepository;
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
class FlowControllerTest {

  private static final AuthenticatedUser SOME_CUSTOMER_USER = someCustomerUser();

  @Mock private FlowRepository flowRepository;

  @Spy private FlowWebMapper mapper = new FlowWebMapperImpl(new AmountMapperImpl());

  @InjectMocks private FlowController controller;

  @Nested
  class FindById {

    @Test
    void shouldReturnFlowById() {
      // given
      given(flowRepository.findById(SOME_FLOW_ID, SOME_CUSTOMER_USER.requireCustomerId()))
          .willReturn(Optional.of(SOME_FLOW));

      // when
      var actual = controller.findById(SOME_FLOW_ID.value().toString(), SOME_CUSTOMER_USER);

      // then
      var expected = ResponseEntity.ok(mapper.toDto(SOME_FLOW));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowNotFoundWhenFlowMissing() {
      // given
      given(flowRepository.findById(SOME_FLOW_ID, SOME_CUSTOMER_USER.requireCustomerId()))
          .willReturn(Optional.empty());

      // when/then
      assertThatThrownBy(
              () -> controller.findById(SOME_FLOW_ID.value().toString(), SOME_CUSTOMER_USER))
          .isInstanceOf(NotFoundException.class);
    }
  }
}
