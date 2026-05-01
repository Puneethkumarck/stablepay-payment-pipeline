package io.stablepay.auth.infrastructure.db;

import io.stablepay.auth.domain.model.RefreshToken;
import io.stablepay.auth.domain.model.RefreshTokenId;
import io.stablepay.auth.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface RefreshTokenRowMapper {

  @Mapping(target = "id", source = "tokenId", qualifiedByName = "toRefreshTokenId")
  @Mapping(target = "userId", source = "userId", qualifiedByName = "toUserId")
  @Mapping(target = "revokedAt", source = "revokedAt", qualifiedByName = "toOptionalInstant")
  RefreshToken toDomain(RefreshTokenRow row);

  @Named("toRefreshTokenId")
  default RefreshTokenId toRefreshTokenId(UUID id) {
    return RefreshTokenId.of(id);
  }

  @Named("toUserId")
  default UserId toUserId(UUID id) {
    return UserId.of(id);
  }

  @Named("toOptionalInstant")
  default Optional<Instant> toOptionalInstant(Instant value) {
    return Optional.ofNullable(value);
  }
}
