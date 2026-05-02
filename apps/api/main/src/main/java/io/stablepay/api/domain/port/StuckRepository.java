package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.StuckPayment;
import java.util.Optional;

public interface StuckRepository {

  PaginatedResult<StuckPayment> searchAdmin(int pageSize, Optional<String> cursor);
}
