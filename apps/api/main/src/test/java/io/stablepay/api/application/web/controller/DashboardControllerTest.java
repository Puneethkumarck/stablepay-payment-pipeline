package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someCustomerUser;
import static io.stablepay.api.domain.model.fixtures.DashboardStatsFixtures.SOME_DASHBOARD_STATS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import io.stablepay.api.application.security.AuthenticatedUser;
import io.stablepay.api.application.web.mapper.DashboardStatsWebMapper;
import io.stablepay.api.application.web.mapper.DashboardStatsWebMapperImpl;
import io.stablepay.api.domain.port.DashboardStatsRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

  private static final AuthenticatedUser SOME_CUSTOMER_USER = someCustomerUser();

  @Mock private DashboardStatsRepository dashboardStatsRepository;

  @Spy private DashboardStatsWebMapper mapper = new DashboardStatsWebMapperImpl();

  @InjectMocks private DashboardController controller;

  @Nested
  class Stats {

    @Test
    void shouldReturnDashboardStats() {
      // given
      given(dashboardStatsRepository.getStats(SOME_CUSTOMER_USER.requireCustomerId()))
          .willReturn(SOME_DASHBOARD_STATS);

      // when
      var actual = controller.stats(SOME_CUSTOMER_USER);

      // then
      var expected = mapper.toDto(SOME_DASHBOARD_STATS);
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
