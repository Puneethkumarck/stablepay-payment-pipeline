package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.DlqEvent;
import io.stablepay.api.domain.model.DlqId;
import io.stablepay.api.domain.model.PaginatedResult;
import java.util.Optional;

/**
 * Customer-scope rule (CONTEXT.md D-B1): every read method takes CustomerId or has the …Admin
 * suffix.
 */
public interface DlqRepository {

  Optional<DlqEvent> findByIdAdmin(DlqId id);

  PaginatedResult<DlqEvent> searchAdmin(int pageSize, Optional<String> cursor);
}
