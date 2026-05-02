package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.CustomerSummary;
import java.util.Optional;

/**
 * Customer-scope rule (CONTEXT.md D-B1): every read method takes CustomerId or has the …Admin
 * suffix.
 */
public interface CustomerRepository {

  Optional<CustomerSummary> findById(CustomerId customerId);

  Optional<CustomerSummary> findByIdAdmin(CustomerId id);
}
