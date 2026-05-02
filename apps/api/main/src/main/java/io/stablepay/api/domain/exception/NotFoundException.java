package io.stablepay.api.domain.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

  private final String resourceType;
  private final String resourceId;

  public NotFoundException(String resourceType, String resourceId) {
    super("%s not found: %s".formatted(resourceType, resourceId));
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }
}
