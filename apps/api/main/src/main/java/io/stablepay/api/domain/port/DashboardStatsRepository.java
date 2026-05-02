package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.DashboardStats;

public interface DashboardStatsRepository {

  DashboardStats getStats(CustomerId customerId);

  DashboardStats getStatsAdmin();
}
