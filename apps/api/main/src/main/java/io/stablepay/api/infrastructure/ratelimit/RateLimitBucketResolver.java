package io.stablepay.api.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import io.stablepay.api.application.security.Role;

@FunctionalInterface
public interface RateLimitBucketResolver {

  Bucket resolve(String key, Role role);
}
