package io.stablepay.api.application.web.controller;

import static io.stablepay.api.domain.model.fixtures.CustomerSummaryFixtures.SOME_CUSTOMER_ID;
import static io.stablepay.api.domain.model.fixtures.CustomerSummaryFixtures.SOME_CUSTOMER_SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.security.Role;
import io.stablepay.api.application.web.mapper.AmountMapperImpl;
import io.stablepay.api.application.web.mapper.CustomerSummaryWebMapper;
import io.stablepay.api.application.web.mapper.CustomerSummaryWebMapperImpl;
import io.stablepay.api.domain.exception.NotFoundException;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.UserId;
import io.stablepay.api.domain.port.CustomerRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

  private static final AuthenticatedUser SOME_CUSTOMER_USER =
      AuthenticatedUser.builder()
          .userId(UserId.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")))
          .customerId(Optional.of(SOME_CUSTOMER_ID))
          .roles(Set.of(Role.CUSTOMER))
          .email("test@example.com")
          .build();

  private static final CustomerId OTHER_CUSTOMER_ID =
      CustomerId.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

  @Mock private CustomerRepository customerRepository;

  @Spy
  private CustomerSummaryWebMapper mapper =
      new CustomerSummaryWebMapperImpl(new AmountMapperImpl());

  @InjectMocks private CustomerController controller;

  @Nested
  class Summary {

    @Test
    void shouldReturnSummaryForOwnCustomerId() {
      // given
      given(customerRepository.findById(SOME_CUSTOMER_ID))
          .willReturn(Optional.of(SOME_CUSTOMER_SUMMARY));

      // when
      var actual = controller.summary(SOME_CUSTOMER_ID.value().toString(), SOME_CUSTOMER_USER);

      // then
      assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(actual.getBody()).isNotNull();
      assertThat(actual.getBody().id()).isEqualTo(SOME_CUSTOMER_ID.value().toString());
    }

    @Test
    void shouldThrowNotFoundForCrossCustomerAccess() {
      // given — requesting another customer's data

      // when/then
      assertThatThrownBy(
              () -> controller.summary(OTHER_CUSTOMER_ID.value().toString(), SOME_CUSTOMER_USER))
          .isInstanceOf(NotFoundException.class);
      then(customerRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowNotFoundWhenCustomerDoesNotExist() {
      // given
      given(customerRepository.findById(SOME_CUSTOMER_ID)).willReturn(Optional.empty());

      // when/then
      assertThatThrownBy(
              () -> controller.summary(SOME_CUSTOMER_ID.value().toString(), SOME_CUSTOMER_USER))
          .isInstanceOf(NotFoundException.class);
    }
  }
}
