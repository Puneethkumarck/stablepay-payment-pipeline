package io.stablepay.api.domain.port;

import io.stablepay.api.domain.model.TransactionEvent;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Flux;

public interface TransactionEventSource {

  Flux<TransactionEvent> subscribeAdmin();

  List<TransactionEvent> snapshotSinceAdmin(Optional<String> since, int limit);
}
