package io.stablepay.api.infrastructure.web.mapper;

import io.stablepay.api.domain.model.DashboardStats;
import io.stablepay.api.infrastructure.web.dto.DashboardStatsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DashboardStatsWebMapper {

  @Mapping(target = "currencyCode", expression = "java(stats.currencyCode().name())")
  DashboardStatsDto toDto(DashboardStats stats);
}
