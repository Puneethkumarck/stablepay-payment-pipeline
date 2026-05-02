package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionSearch;
import java.util.Optional;
import java.util.stream.Stream;

public interface TransactionRepository {

  Optional<Transaction> findByReference(String reference, CustomerId customerId);

  Optional<Transaction> findByReferenceAdmin(String reference);

  PaginatedResult<Transaction> search(TransactionSearch criteria, CustomerId customerId);

  PaginatedResult<Transaction> searchAdmin(TransactionSearch criteria);

  Stream<Transaction> tailSinceSortValueAdmin(Optional<String> sortValue, int batchSize);
}
