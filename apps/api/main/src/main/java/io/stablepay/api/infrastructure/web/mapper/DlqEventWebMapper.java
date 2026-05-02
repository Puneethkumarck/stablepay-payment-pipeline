package io.stablepay.api.infrastructure.web.mapper;

import io.stablepay.api.domain.model.DlqEvent;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.infrastructure.web.dto.DlqEventDto;
import io.stablepay.api.infrastructure.web.dto.PaginatedResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DlqEventWebMapper {

  @Mapping(target = "id", expression = "java(event.id().value().toString())")
  DlqEventDto toDto(DlqEvent event);

  default PaginatedResponse<DlqEventDto> toResponse(PaginatedResult<DlqEvent> result) {
    return PaginatedResponse.<DlqEventDto>builder()
        .items(result.items().stream().map(this::toDto).toList())
        .nextCursor(result.nextCursor())
        .hasMore(result.nextCursor().isPresent())
        .build();
  }
}
