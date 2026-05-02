package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.Flow;
import io.stablepay.api.domain.model.FlowId;
import io.stablepay.api.domain.model.PaginatedResult;
import java.util.Optional;

public interface FlowRepository {

  Optional<Flow> findById(FlowId id, CustomerId customerId);

  Optional<Flow> findByIdAdmin(FlowId id);

  PaginatedResult<Flow> searchByCustomer(
      CustomerId customerId, int pageSize, Optional<String> cursor);

  PaginatedResult<Flow> searchAdmin(int pageSize, Optional<String> cursor);
}
