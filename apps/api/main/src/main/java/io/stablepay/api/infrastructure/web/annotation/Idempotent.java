package io.stablepay.api.infrastructure.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for idempotent endpoints. The AOP aspect that enforces idempotency via {@code
 * X-Idempotency-Key} header and a DB unique constraint is provided by SPP-92.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {}
