package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
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
class AdminFlowControllerTest {

  private static final AuthenticatedUser SOME_ADMIN_USER = someAdminUser();

  @Mock private FlowRepository flowRepository;

  @Spy private FlowWebMapper mapper = new FlowWebMapperImpl(new AmountMapperImpl());

  @InjectMocks private AdminFlowController controller;

  @Nested
  class FindById {

    @Test
    void shouldReturnFlowByIdForAdmin() {
      // given
      given(flowRepository.findByIdAdmin(SOME_FLOW_ID)).willReturn(Optional.of(SOME_FLOW));

      // when
      var actual = controller.findById(SOME_FLOW_ID.value().toString(), SOME_ADMIN_USER);

      // then
      var expected = ResponseEntity.ok(mapper.toDto(SOME_FLOW));
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowNotFoundWhenFlowMissingForAdmin() {
      // given
      given(flowRepository.findByIdAdmin(SOME_FLOW_ID)).willReturn(Optional.empty());

      // when/then
      assertThatThrownBy(
              () -> controller.findById(SOME_FLOW_ID.value().toString(), SOME_ADMIN_USER))
          .isInstanceOf(NotFoundException.class);
    }
  }
}
