package io.stablepay.auth.infrastructure.db;

import io.stablepay.auth.domain.model.CustomerId;
import io.stablepay.auth.domain.model.Role;
import io.stablepay.auth.domain.model.User;
import io.stablepay.auth.domain.model.UserId;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserRowMapper {

  @Mapping(target = "id", source = "userId", qualifiedByName = "toUserId")
  @Mapping(target = "customerId", source = "customerId", qualifiedByName = "toCustomerId")
  @Mapping(target = "passwordHash", source = "password")
  @Mapping(target = "roles", source = "roles", qualifiedByName = "toRoles")
  User toDomain(UserRow row);

  @Named("toUserId")
  default UserId toUserId(UUID id) {
    return UserId.of(id);
  }

  @Named("toCustomerId")
  default Optional<CustomerId> toCustomerId(UUID id) {
    return Optional.ofNullable(id).map(CustomerId::of);
  }

  @Named("toRoles")
  default Set<Role> toRoles(String csv) {
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Role::valueOf)
        .collect(Collectors.toUnmodifiableSet());
  }
}
