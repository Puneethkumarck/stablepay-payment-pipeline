package io.stablepay.api.application.web.mapper;

import io.stablepay.api.application.web.dto.DashboardStatsDto;
import io.stablepay.api.domain.model.DashboardStats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DashboardStatsWebMapper {

  @Mapping(target = "currencyCode", expression = "java(stats.currencyCode().name())")
  DashboardStatsDto toDto(DashboardStats stats);
}
