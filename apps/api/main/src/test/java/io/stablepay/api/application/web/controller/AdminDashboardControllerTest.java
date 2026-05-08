package io.stablepay.api.application.web.controller;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.someAdminUser;
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
class AdminDashboardControllerTest {

  private static final AuthenticatedUser SOME_ADMIN_USER = someAdminUser();

  @Mock private DashboardStatsRepository dashboardStatsRepository;

  @Spy private DashboardStatsWebMapper mapper = new DashboardStatsWebMapperImpl();

  @InjectMocks private AdminDashboardController controller;

  @Nested
  class Stats {

    @Test
    void shouldReturnDashboardStatsForAdmin() {
      // given
      given(dashboardStatsRepository.getStatsAdmin()).willReturn(SOME_DASHBOARD_STATS);

      // when
      var actual = controller.stats(SOME_ADMIN_USER);

      // then
      var expected = mapper.toDto(SOME_DASHBOARD_STATS);
      assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
  }
}
