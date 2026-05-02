package io.stablepay.api.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import io.stablepay.api.infrastructure.security.Role;

@FunctionalInterface
public interface RateLimitBucketResolver {

  Bucket resolve(String key, Role role);
}
