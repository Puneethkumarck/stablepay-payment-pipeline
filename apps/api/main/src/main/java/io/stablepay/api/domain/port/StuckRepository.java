package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.StuckPayment;
import java.util.Optional;

/**
 * Customer-scope rule (CONTEXT.md D-B1): every read method takes CustomerId or has the …Admin
 * suffix.
 */
public interface StuckRepository {

  PaginatedResult<StuckPayment> searchAdmin(int pageSize, Optional<String> cursor);
}
