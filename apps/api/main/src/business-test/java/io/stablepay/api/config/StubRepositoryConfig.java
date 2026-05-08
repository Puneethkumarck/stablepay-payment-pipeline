package io.stablepay.api.config;

import static io.stablepay.api.application.security.fixtures.AuthenticatedUserFixtures.SOME_CUSTOMER_ID;
import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_EVENT;
import static io.stablepay.api.domain.model.fixtures.DlqEventFixtures.SOME_DLQ_ID;
import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_REFERENCE;
import static io.stablepay.api.domain.model.fixtures.TransactionFixtures.SOME_TRANSACTION;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.i18n.CurrencyCode;
import io.github.bucket4j.BucketConfiguration;
import io.stablepay.api.application.security.Role;
import io.stablepay.api.domain.model.CustomerId;
import io.stablepay.api.domain.model.DashboardStats;
import io.stablepay.api.domain.model.DlqSummary;
import io.stablepay.api.domain.model.PaginatedResult;
import io.stablepay.api.domain.model.Transaction;
import io.stablepay.api.domain.model.TransactionEvent;
import io.stablepay.api.domain.port.CustomerRepository;
import io.stablepay.api.domain.port.DashboardStatsRepository;
import io.stablepay.api.domain.port.DlqRepository;
import io.stablepay.api.domain.port.FlowRepository;
import io.stablepay.api.domain.port.StuckRepository;
import io.stablepay.api.domain.port.TransactionEventSource;
import io.stablepay.api.domain.port.TransactionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@TestConfiguration
public class StubRepositoryConfig {

  static final Transaction ALICE_TRANSACTION =
      SOME_TRANSACTION.toBuilder().customerId(SOME_CUSTOMER_ID).build();

  private final CopyOnWriteArrayList<TransactionEvent> eventBuffer = new CopyOnWriteArrayList<>();
  private final Sinks.Many<TransactionEvent> eventSink =
      Sinks.many().multicast().onBackpressureBuffer(256);

  public void emitEvent(TransactionEvent event) {
    eventBuffer.add(event);
    eventSink.tryEmitNext(event);
  }

  public void clearEvents() {
    eventBuffer.clear();
  }

  @Bean
  @Primary
  public TransactionRepository stubTransactionRepository() {
    return new TransactionRepository() {
      @Override
      public Optional<Transaction> findByReference(String reference, CustomerId customerId) {
        if (SOME_REFERENCE.equals(reference) && SOME_CUSTOMER_ID.equals(customerId)) {
          return Optional.of(ALICE_TRANSACTION);
        }
        return Optional.empty();
      }

      @Override
      public Optional<Transaction> findByReferenceAdmin(String reference) {
        if (SOME_REFERENCE.equals(reference)) {
          return Optional.of(ALICE_TRANSACTION);
        }
        return Optional.empty();
      }

      @Override
      public PaginatedResult<Transaction> search(
          io.stablepay.api.domain.model.TransactionSearch criteria, CustomerId customerId) {
        return new PaginatedResult<>(List.of(), Optional.empty());
      }

      @Override
      public PaginatedResult<Transaction> searchAdmin(
          io.stablepay.api.domain.model.TransactionSearch criteria) {
        return new PaginatedResult<>(List.of(), Optional.empty());
      }

      @Override
      public Stream<Transaction> tailSinceSortValueAdmin(
          Optional<String> sortValue, int batchSize) {
        return Stream.empty();
      }
    };
  }

  @Bean
  @Primary
  public DlqRepository stubDlqRepository() {
    return new DlqRepository() {
      @Override
      public Optional<io.stablepay.api.domain.model.DlqEvent> findByIdAdmin(
          io.stablepay.api.domain.model.DlqId id) {
        if (SOME_DLQ_ID.equals(id)) {
          return Optional.of(SOME_DLQ_EVENT);
        }
        return Optional.empty();
      }

      @Override
      public PaginatedResult<io.stablepay.api.domain.model.DlqEvent> searchAdmin(
          int pageSize, Optional<String> cursor) {
        return new PaginatedResult<>(List.of(), Optional.empty());
      }

      @Override
      public DlqSummary summaryAdmin() {
        return DlqSummary.builder().countsByErrorClass(Map.of()).totalCount(0L).build();
      }
    };
  }

  @Bean
  @Primary
  public FlowRepository stubFlowRepository() {
    return new FlowRepository() {
      @Override
      public Optional<io.stablepay.api.domain.model.Flow> findById(
          io.stablepay.api.domain.model.FlowId id, CustomerId customerId) {
        return Optional.empty();
      }

      @Override
      public Optional<io.stablepay.api.domain.model.Flow> findByIdAdmin(
          io.stablepay.api.domain.model.FlowId id) {
        return Optional.empty();
      }

      @Override
      public PaginatedResult<io.stablepay.api.domain.model.Flow> searchByCustomer(
          CustomerId customerId, int pageSize, Optional<String> cursor) {
        return new PaginatedResult<>(List.of(), Optional.empty());
      }

      @Override
      public PaginatedResult<io.stablepay.api.domain.model.Flow> searchAdmin(
          int pageSize, Optional<String> cursor) {
        return new PaginatedResult<>(List.of(), Optional.empty());
      }
    };
  }

  @Bean
  @Primary
  public CustomerRepository stubCustomerRepository() {
    return new CustomerRepository() {
      @Override
      public Optional<io.stablepay.api.domain.model.CustomerSummary> findById(
          CustomerId customerId) {
        return Optional.empty();
      }

      @Override
      public Optional<io.stablepay.api.domain.model.CustomerSummary> findByIdAdmin(CustomerId id) {
        return Optional.empty();
      }
    };
  }

  @Bean
  @Primary
  public DashboardStatsRepository stubDashboardStatsRepository() {
    return new DashboardStatsRepository() {
      @Override
      public DashboardStats getStats(CustomerId customerId) {
        return emptyStats();
      }

      @Override
      public DashboardStats getStatsAdmin() {
        return emptyStats();
      }

      private DashboardStats emptyStats() {
        return DashboardStats.builder()
            .volume24hMicros(0L)
            .currencyCode(CurrencyCode.USD)
            .transactionCount(0L)
            .successRate(0.0)
            .dlqCount(0L)
            .dlqCriticalCount(0L)
            .stuckCount(0L)
            .stuckCriticalCount(0L)
            .periodStart(Instant.parse("2026-05-01T00:00:00Z"))
            .periodEnd(Instant.parse("2026-05-02T00:00:00Z"))
            .build();
      }
    };
  }

  @Bean
  @Primary
  public StuckRepository stubStuckRepository() {
    return (pageSize, cursor) -> new PaginatedResult<>(List.of(), Optional.empty());
  }

  @Bean
  @Primary
  public TransactionEventSource stubTransactionEventSource() {
    return new TransactionEventSource() {
      @Override
      public Flux<TransactionEvent> subscribeAdmin() {
        return eventSink.asFlux();
      }

      @Override
      public List<TransactionEvent> snapshotSinceAdmin(Optional<String> since, int limit) {
        return List.copyOf(eventBuffer);
      }
    };
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }

  public static final long BT_RATE_LIMIT_CAPACITY = 10L;

  @Bean
  @Primary
  public Map<Role, BucketConfiguration> btRoleBucketConfigurations() {
    var config =
        BucketConfiguration.builder()
            .addLimit(
                limit ->
                    limit
                        .capacity(BT_RATE_LIMIT_CAPACITY)
                        .refillGreedy(BT_RATE_LIMIT_CAPACITY, Duration.ofMinutes(1)))
            .build();
    var configurations = new EnumMap<Role, BucketConfiguration>(Role.class);
    configurations.put(Role.CUSTOMER, config);
    configurations.put(Role.ADMIN, config);
    configurations.put(Role.AGENT, config);
    return Map.copyOf(configurations);
  }
}
