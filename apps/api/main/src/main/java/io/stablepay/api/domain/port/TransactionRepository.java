package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionSearch;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Customer-scope rule (CONTEXT.md D-B1): every read method takes CustomerId or has the …Admin
 * suffix.
 */
public interface TransactionRepository {

  Optional<Transaction> findByReference(String reference, CustomerId customerId);

  Optional<Transaction> findByReferenceAdmin(String reference);

  PaginatedResult<Transaction> search(TransactionSearch criteria, CustomerId customerId);

  PaginatedResult<Transaction> searchAdmin(TransactionSearch criteria);

  /**
   * SSE source — no scope here; filter applied at the SSE handler per CONTEXT.md D-C3. Method name
   * ends in "Admin" because it's an unscoped tail (admin-style access pattern).
   */
  Stream<Transaction> tailSinceSortValueAdmin(Optional<String> sortValue, int batchSize);
}
