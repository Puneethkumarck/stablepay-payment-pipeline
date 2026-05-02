package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.CustomerSummary;
import java.util.Optional;

public interface CustomerRepository {

  Optional<CustomerSummary> findById(CustomerId customerId);

  Optional<CustomerSummary> findByIdAdmin(CustomerId id);
}
