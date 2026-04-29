# ADR: Architecture Decision Record — stablepay-payment-pipeline (Java service)

**Status:** Accepted
**Last revised:** 2026-04-29
**Scope:** Architecture decisions governing `apps/api/` (Spring Boot 4.x service) and Java code in `apps/flink-jobs/`. Adapted from a sibling reference project; project-specific values applied throughout.
**Stack:** Java 25 LTS, Spring Boot 4.0.x, Spring Cloud 2025.x, Gradle 9 Kotlin DSL, PostgreSQL 17, Kafka 4.0+, OpenSearch 2.18+, Trino 470+, Apache Iceberg (read-side via Trino)
**Architecture:** Hexagonal (Ports & Adapters) + read-mostly CQRS + Event-Driven (DLQ replay only) + ArchUnit-enforced layering

---

## Table of Contents

1. [Build System: Gradle Multi-Module](#1-build-system-gradle-multi-module)
2. [Hexagonal Architecture (Ports & Adapters)](#2-hexagonal-architecture-ports--adapters)
3. [Domain Model: Immutable Records](#3-domain-model-immutable-records)
4. [CQRS: Command/Query Separation](#4-cqrs-commandquery-separation)
5. [State Machine: Generic, Table-Driven](#5-state-machine-generic-table-driven)
6. [Event-Driven Architecture: Outbox Pattern](#6-event-driven-architecture-outbox-pattern)
7. [Pessimistic Locking for Financial Operations](#7-pessimistic-locking-for-financial-operations)
8. [API Design](#8-api-design)
9. [Error Handling: Dual Exception Hierarchies](#9-error-handling-dual-exception-hierarchies)
10. [Object Mapping: MapStruct](#10-object-mapping-mapstruct)
11. [Persistence: JPA + PostgreSQL](#11-persistence-jpa--postgresql)
12. [External Integration](#12-external-integration)
13. [Observability](#13-observability)
14. [Feature Flags](#14-feature-flags)
15. [Distributed Job Locking: ShedLock](#15-distributed-job-locking-shedlock)
16. [Testing Strategy: Three-Tier Pyramid](#16-testing-strategy-three-tier-pyramid)
17. [Authentication & Security](#17-authentication--security)
18. [Configuration & Environment](#18-configuration--environment)
19. [Key Libraries](#19-key-libraries)
20. [Coding Conventions for Agents](#20-coding-conventions-for-agents)

---

## 1. Build System: Gradle Multi-Module

### Decision

Use Gradle with Java 25 LTS toolchain, Spring Boot 4.0.x plugin, and centralized dependency management.

### Implementation

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.4'
    id 'io.spring.dependency-management' version '1.1.7'
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

### Module Structure

```
project-root/
├── project-api/       <- Shared contract library (DTOs, enums, events, validation)
├── project-client/    <- Feign client library for other services to consume
├── project-app/       <- Main application (application + domain + infrastructure)
├── build.gradle       <- Root build with dependency versions
└── settings.gradle
```

### Rules

- `project-api` is published as a library. Other services depend on it for type-safe integration.
- `project-client` wraps a Feign client + Spring Boot auto-configuration.
- `project-app` contains all three hexagonal layers and the Spring Boot main class.
- Test fixtures shared via Gradle's `testFixtures` source set.

---

## 2. Hexagonal Architecture (Ports & Adapters)

### Decision

All services follow strict hexagonal architecture with three layers.

### Layer Structure

```
application/     <- Inbound adapters (REST controllers, message listeners)
domain/          <- Core business logic
infrastructure/  <- Outbound adapters (DB, HTTP clients, message publishers)
```

### Dependency Direction

```
application → domain ← infrastructure
```

### Annotation Split Across Layers


| Layer              | Records (Aggregates, VOs, Commands, Events)        | Classes (Services, Handlers)                                                                   |
| ------------------ | -------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| **Domain**         | `@Builder(toBuilder = true)` on all records         | `@Component`, `@Service`, `@RequiredArgsConstructor`, `@Slf4j`, `@Transactional`               |
| **Application**    | `@Builder(toBuilder = true)` on DTOs, Jakarta Validation annotations | `@RestController`, `@Validated`, `@RestControllerAdvice`, `@RequiredArgsConstructor`, `@Slf4j` |
| **Infrastructure** | `@Builder(toBuilder = true)` on all records/DTOs    | `@Repository`, `@Component`, `@Entity`, `@Table`, `@Column`, `@RequiredArgsConstructor`        |


### Critical Rules

- **Domain records** (aggregates, value objects, commands, events) are framework-free — only `@Builder` from Lombok.
- **Domain classes** (services, command handlers) ARE Spring-managed beans — `@Component`/`@Service`, `@RequiredArgsConstructor`, `@Slf4j`.
- **No JPA annotations** (`@Entity`, `@Table`, `@Column`) ever appear in the domain layer.
- Domain defines ports as interfaces (e.g., `TransactionReadOnlyRepository`, `FlowReadOnlyRepository`, `IdempotencyKeyRepository`, `AgentSqlRepository`, `EventPublisher<T>`). Infrastructure implements them.
- Application layer orchestrates inbound requests into domain calls.

---

## 3. Domain Model: Immutable Records

### Decision

Domain aggregates and value objects are Java `record` types with Lombok `@Builder(toBuilder = true)`.

### Rules

- State changes produce **new instances** via `toBuilder()`. Never mutate in place.
- Use compact constructors with `Objects.requireNonNull()` for mandatory field validation.
- Use `with*()` methods that return new record instances.
- Business invariants enforced via `assert*()` methods that throw domain exceptions.
- Each aggregate has a `StatusHistory` list tracking every state transition with timestamp, user, and reason.
- `Money` is always `(BigDecimal value, CurrencyCode currency)` using `nv-i18n` for ISO currency codes.

### Type-Safe IDs

Wrap `UUID` in domain-specific records:

```java
public record TransactionId(UUID value) {}
public record FlowId(UUID value) {}
public record CustomerId(UUID value) {}
public record DlqId(UUID value) {}
```

Use UUID v7 (time-based sortable) via `com.github.f4b6a3:uuid-creator` for entities that benefit from chronological ordering.

### Example

```java
@Builder(toBuilder = true)
public record Transaction(
    TransactionId id,
    FlowId flowId,
    CustomerId customerId,
    Money amount,
    String internalStatus,
    String customerStatus,
    Instant eventTime,
    List<TransactionEvent> timeline
) {
    public Transaction {
        Objects.requireNonNull(id);
        Objects.requireNonNull(customerId);
    }

    public Transaction withDerivedCustomerStatus() {
        return toBuilder()
            .customerStatus(CustomerStatus.from(internalStatus).name())
            .build();
    }

    public void assertVisibleTo(CustomerId scope) {
        if (!customerId.equals(scope)) {
            throw new CustomerScopeMismatchException(id, scope);
        }
    }
}
```

---

## 4. CQRS: Command/Query Separation

### Decision

Separate command handlers (write) and query services (read) in the domain layer.

### Command Handlers

Command handlers live in the domain layer, co-located with their aggregates. They are Spring-managed beans. In v1 the API has one command handler — DLQ replay.

```java
@Component
@RequiredArgsConstructor
public class DlqReplayCommandHandler {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final EventPublisher<DlqReplayCommandEvent> eventPublisher;

    @Transactional
    public ReplayResult handle(ReplayDlqMessageCommand command, AuthenticatedUser user) {
        var existing = idempotencyKeyRepository.findByKey(command.idempotencyKey(), user.id().toString());
        if (existing.isPresent()) return existing.get().result();

        var event = new DlqReplayCommandEvent(command.dlqId(), command.sourceTopic(),
                                                user.id(), Instant.now());
        eventPublisher.publish(event);                   // Namastack outbox — same TX as save
        var key = IdempotencyKey.create(command.idempotencyKey(), user.id().toString(),
                                          ReplayResult.accepted(command.dlqId()));
        idempotencyKeyRepository.save(key);
        return key.result();
    }
}
```

### Query Services

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryHandler {

    private final TransactionReadOnlyRepository transactionRepository;

    public Optional<Transaction> findByReference(TransactionId id, CustomerId scope) {
        return transactionRepository.findByReference(id, scope);
    }

    public SearchResult<Transaction> search(TransactionSearchQuery query, Pageable page) {
        return transactionRepository.search(query, page);
    }
}
```

### Repository Port Separation

```java
// Read-only port — most of v1 uses this style since the API is read-mostly
public interface TransactionReadOnlyRepository {
    Optional<Transaction> findByReference(TransactionId reference, CustomerId scope);
    Page<Transaction> findByFilter(TransactionFilter filter, Pageable pageable);
    SearchResult<Transaction> search(TransactionSearchQuery query, Pageable pageable);
}

// Idempotency-key port (the only write-side port in v1)
public interface IdempotencyKeyRepository {
    Optional<IdempotencyKey> findByKey(String key, String scope);
    IdempotencyKey save(IdempotencyKey key);
}
```

### Command Routing — List-Based Polymorphic Pattern

Spring auto-collects all `@Component` processors implementing a common interface. Each processor decides via `shouldProcess()` whether it handles the change. v1 doesn't use this pattern (single-handler per command), but it's documented as the canonical approach for when more handlers are added later.

```java
@Component
@RequiredArgsConstructor
public class DlqEventHandler {
    private final List<DlqEventProcessor> dlqEventProcessors;

    private void process(DlqEvent event) {
        dlqEventProcessors.stream()
            .filter(p -> p.shouldProcess(event))
            .forEach(p -> p.process(event));
    }
}
```

---

## 5. State Machine: Generic, Table-Driven

### Decision

State transitions are enforced by a generic `StateMachine<S, T>` framework. The framework lives in the domain layer and is available for any aggregate that has a state lifecycle.

### Applicability to v1

**The Spring Boot API in v1 does not own a write-side state machine** — the simulator (`apps/simulator/`) and the Flink correlator (`apps/flink-jobs/`) drive all transaction and flow state transitions. The API is read-mostly.

The generic `StateMachine<S, T>` framework is documented here because:
1. It is the canonical pattern when write-side aggregates are added in a future phase
2. Any inferred / derived status (e.g., customer-visible status from internal status) uses lookup tables rather than ad-hoc if/else, and the framework's `getValidPredecessors()` style is the right pattern to validate that the inferred mapping covers every possible internal status

### Rules (when used)

- Define all valid transitions statically using `withTransition(from, to, action)` and `withTransitionsFrom(from, to1, to2, ...)`.
- Each transition either emits a `StateChangedEvent` (triggering downstream side effects) or performs `noAction()`.
- Invalid transitions throw `StateMachineException` (retryable).
- Customer-facing status is **derived** from internal status via a mapping function — never stored.

### Customer Status Mapping (canonical mapping that the API computes from `internalStatus` returned by Trino/OpenSearch)


| Internal phase                                                              | Customer Status    |
| --------------------------------------------------------------------------- | ------------------ |
| CREATED through COMPLIANCE_PASSED                                            | `PROCESSING`       |
| CUSTOMER_APPROVAL_REQUIRED                                                   | `NEEDS_APPROVAL`   |
| OPS_APPROVAL_REQUIRED                                                        | `PROCESSING`       |
| PROVIDER_SUBMITTED through PROVIDER_SETTLED → LEDGER_RELEASED → COMPLETED   | `COMPLETED`        |
| REJECTED / CANCELLED / DECLINED / EXPIRED                                    | `CANCELLED`        |
| PROVIDER_REJECTED → LEDGER_REFUNDED → FAILED                                 | `FAILED`           |
| PROVIDER_REVERSED → REVERSED                                                 | `REVERSED`         |
| FUNDS_SEIZED                                                                 | `CANCELLED`        |


---

## 6. Event-Driven Architecture: Outbox Pattern

### Decision

All inter-service communication uses asynchronous messaging with the **transactional outbox pattern** via **Namastack Outbox Starter JDBC** (`io.namastack:namastack-outbox-starter-jdbc`).

### Implementation

```gradle
implementation "io.namastack:namastack-outbox-starter-jdbc:1.1.0"
```

```java
// Infrastructure adapter implements domain port
@Component
@RequiredArgsConstructor
public class DlqReplayOutboxPublisher extends AbstractOutboxEventPublisher
        implements EventPublisher<DlqReplayCommandEvent> {

    public DlqReplayOutboxPublisher(Outbox outbox) {
        super(outbox, List.of("dlqId"));   // partition key
    }
}

// OutboxHandler routes events to Kafka topics
@Component
public class StablepayOutboxHandler extends AbstractOutboxHandler {
    public StablepayOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
```

Create the outbox table via Flyway migration in Namastack's expected schema.

### Configuration

```yaml
namastack:
  outbox:
    poll-interval: 1000
    batch-size: 500
```

### Rules

- **Never publish events directly to a broker.** Always persist events in the outbox table within the same DB transaction as the aggregate change.
- Event publishers live in the infrastructure layer, implementing domain-defined `EventPublisher<T>` ports.
- Call the outbox publisher from within `@Transactional` service methods.
- At-least-once delivery — **consumers must be idempotent**.
- Event ordering preserved within a transaction.

### Messaging Infrastructure


| Channel      | Technology | Direction | Purpose                          |
| ------------ | ---------- | --------- | -------------------------------- |
| Kafka Topics | Apache Kafka | Outbound | DLQ replay commands + future event publishing via Namastack outbox |

In v1 the Spring Boot API service is read-mostly — it does not consume inbound message streams. The Flink jobs (separate Java codebase under `apps/flink-jobs/`) handle all stream consumption from Kafka. The API publishes one event type via the Namastack outbox: `dlq.replay.command.v1` from `POST /admin/dlq/{id}/replay`.

### Event Routing

Define all topic bindings in a single `EventRouting` configuration class:

```java
@Configuration
public class EventRouting {
    @Value("${messaging.bindings.dlq-replay-commands}") String dlqReplayCommands;
}
```

---

## 7. Pessimistic Locking for Financial Operations

### Decision

Use `@Lock(LockModeType.PESSIMISTIC_WRITE)` for all balance-affecting operations. Use `@Version` (optimistic) for everything else.

### Applicability to v1

**v1 of `stablepay-api` is read-mostly** — there are no balance-affecting writes. The only API write is `POST /admin/dlq/{id}/replay`, which is non-financial and uses the idempotency-key constraint plus the Namastack outbox transaction for safety. Pessimistic locking is documented here as the correct pattern when balance writes are added in a future phase (e.g., a settle-internal-transfer admin endpoint).

### Pattern (for future write paths)

```java
// JPA repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM AccountEntity a WHERE a.id = :id")
Optional<AccountEntity> findByIdForUpdate(@Param("id") UUID id);

// Domain repository port
public interface AccountRepository {
    Account getAndLock(AccountId id);  // Delegates to findByIdForUpdate
}

// Command handler
var account = accountRepository.getAndLock(command.accountId());
account.assertNotBlocked();
var adjusted = account.withBalanceAdjusted(command.amount());
if (adjusted.overdrawn()) throw new InsufficientFundsException(...);
accountRepository.save(adjusted);
```

### Why

- One writer at a time per account — deterministic, no retries needed.
- Acceptable latency for financial operations (lock contention is low per-account).
- Deadlock risk mitigated by consistent lock ordering.

---

## 8. API Design

### Decision

RESTful APIs with role-based access control, idempotency, and request metadata extraction.

### Controller Structure

```java
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionQueryHandler queryHandler;
    private final TransactionResponseMapper mapper;

    @GetMapping("/{reference}")
    @Secured("ROLE_CUSTOMER")
    public ResponseEntity<TransactionView> findByReference(
            @PathVariable String reference,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return queryHandler.findByReference(new TransactionId(UUID.fromString(reference)), user.customerId())
            .map(mapper::toView)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

### Rules

- Controllers are **thin** — only mapping and delegation, no business logic.
- **Idempotency:** Non-idempotent operations (DLQ replay, future state-changing ops) require an `X-Idempotency-Key` header (3-36 chars). Enforced via DB uniqueness constraint on `(idempotency_key, customer_id_or_admin_user_id)`.
- **Request Metadata:** GeoLocation headers extracted via custom `HeaderRequestMetadataArgumentResolver` with `@ExtractedRequestMetadata` annotation when needed for audit trails.
- **Error Codes:** Use structured error codes: `STBLPAY-XXXX` (4-digit zero-padded).
- **Pagination:** Use Spring `Pageable` for list endpoints. Return `Page<T>` responses.

### API Versioning

```
/api/v1/transactions/           <- Public customer-facing transaction queries (account-scoped via JWT customer_id)
/api/v1/flows/                  <- Public customer-facing payment-flow drill-down
/api/v1/customers/              <- Public customer summary
/api/v1/admin/                  <- Admin (cross-customer search, DLQ inspection + replay, stuck-tx, aggregates)
/api/v1/agent/                  <- LLM agent service-to-service (constrained SQL, search, timeline)
```

---

## 9. Error Handling: Dual Exception Hierarchies

### Decision

Two coexisting exception patterns. Each service defines its own `ErrorCode` enum with a service-specific prefix.

### Pattern 1 — HTTP-Oriented

```java
public abstract class ClientException extends RuntimeException {
    private final String errorCode;      // "FIAT-0101"
    private final HttpStatus httpStatus; // NOT_FOUND
}
```

### Pattern 2 — Retryability-Oriented

```java
public abstract class NonRetryableRuntimeException extends RuntimeException
    implements NonRetryableException {
    public abstract ErrorCode getErrorCode(); // TRANS-2004
}
// Some exceptions implement OutboxEventMappable — publishable as failure events
```

### Standard ApiError Response

```java
@Builder @Jacksonized
public record ApiError(
    @NotNull String code,      // Machine-readable error code
    @NotNull String status,    // HTTP status reason phrase
    @NotNull String message,   // Human-readable description
    Detail details             // Optional structured details
) {
    @Builder @Jacksonized
    public record Detail(String documentLink, Map<String, List<String>> errors) {}
}
```

### Global Exception Handler

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Log warn for 4xx, error for 5xx
    // Never expose stack traces — return message only
    // One @RestControllerAdvice per service
}
```

---

## 10. Object Mapping: MapStruct

### Decision

All layer-boundary mapping uses MapStruct (compile-time code generation).

### Rules

- Define mapper interfaces annotated with `@Mapper(componentModel = "spring")`.
- Each architectural boundary has its own mapper:
  - `application/controller/mapper/` — Request DTO -> Domain command
  - `application/stream/mapper/` — Wire event -> Domain event
  - `infrastructure/db/mapper/` — Domain entity <-> JPA entity
  - `infrastructure/stream/mapper/` — Domain event -> Wire event
  - `infrastructure/client/mapper/` — External API response -> Domain model
- Mappers are unit tested with `Mappers.getMapper(XMapper.class)`.
- Use `unmappedTargetPolicy = IGNORE` to suppress noise for intentionally unmapped fields.
- Mapper composition via `uses = {...}` for nested object mapping.

### Gradle Compiler Config

```gradle
compileJava {
    options.compilerArgs += [
        '-Amapstruct.defaultComponentModel=spring',
        '-Amapstruct.defaultInjectionStrategy=constructor',
        '-Amapstruct.unmappedTargetPolicy=ERROR'
    ]
}
```

---

## 11. Persistence: JPA + PostgreSQL

### Decision

Spring Data JPA with PostgreSQL and Flyway migrations.

### Rules

- JPA entities live in `infrastructure/db/` and are **never exposed to the domain layer**.
- Use `@JdbcTypeCode(VARCHAR)` for UUID columns.
- Use `@JdbcTypeCode(JSON)` for complex nested objects stored as JSON strings.
- `@Enumerated(STRING)` always — never `ORDINAL`.
- `@CreationTimestamp` / `@UpdateTimestamp` for audit timestamps.
- `spring.jpa.open-in-view: false` (always).
- DDL mode: `validate` (production), `create-drop` (tests).

### Repository Adapter Pattern

The Spring Boot API has only two JPA-backed tables in v1: `auth_users` (seeded users for the local JWT issuer) and `idempotency_keys`. Domain reads land on OpenSearch and Trino — not JPA.

```java
@Repository
@RequiredArgsConstructor
public class IdempotencyKeyRepositoryAdapter implements IdempotencyKeyRepository {
    private final IdempotencyKeyJpaRepository jpaRepository;
    private final IdempotencyKeyEntityMapper mapper;

    @Override
    public Optional<IdempotencyKey> findByKey(String idempotencyKey, String scope) {
        return jpaRepository.findByKeyAndScope(idempotencyKey, scope)
            .map(mapper::toDomain);
    }

    @Override
    public IdempotencyKey save(IdempotencyKey key) {
        var entity = mapper.toEntity(key);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
```

### Database Patterns


| Decision           | Choice                                    | Reason                                    |
| ------------------ | ----------------------------------------- | ----------------------------------------- |
| Pagination         | Cursor-based (opaque cursor + limit)      | Avoids deep-offset pain at scale; OpenSearch + Trino handle this efficiently |
| JSON columns       | `@JdbcTypeCode(SqlTypes.JSON)`            | Used sparingly — only for idempotency-key replay-result snapshots |
| Flyway             | Enabled, `V{N}__{TICKET}_description.sql` | Versioned schema evolution for `auth_users` and `idempotency_keys` |
| UUID columns       | `@JdbcTypeCode(VARCHAR)`                  | Postgres native UUID storage with explicit typing |
| Enum columns       | `@Enumerated(STRING)` always              | Never `ORDINAL` — readable + reorder-safe |


---

## 12. External Integration

### Decision

The Spring Boot API integrates with three downstream systems, none of which are peer microservices in v1. Adapter classes implement domain ports per the hexagonal pattern; the underlying transports differ.

### Integrations


| System         | Purpose                                                      | Adapter                                | Transport                                |
| -------------- | ------------------------------------------------------------ | -------------------------------------- | ---------------------------------------- |
| OpenSearch     | Transaction search, flow drill-down, DLQ inspection          | `OpenSearchTransactionRepositoryAdapter`, `OpenSearchFlowRepositoryAdapter`, `OpenSearchDlqRepositoryAdapter` | `spring-data-opensearch` over HTTPS      |
| Trino          | Analytics aggregations and constrained agent SQL execution   | `TrinoAnalyticsRepositoryAdapter`, `AgentSqlExecutorAdapter`                                                  | Trino JDBC driver via Spring JdbcTemplate |
| MCP tools server | (Reverse direction) — the Spring Boot API hosts the agent endpoints that the MCP server proxies to. There is no outbound MCP call from the API. | n/a                                                                                                          | n/a (the MCP server in `apps/agent-tools-mcp/` calls the API)                                              |


### Rules

- Each downstream system has a dedicated adapter implementing a domain port (`TransactionReadOnlyRepository`, `FlowReadOnlyRepository`, `CustomerSummaryRepository`, `AgentSqlRepository`, etc.).
- Connection URLs via: `application.opensearch.url`, `application.trino.url`.
- Automatic distributed tracing via `MicrometerObservationCapability` for the Trino JDBC driver and the OpenSearch HTTP client.
- All outbound calls carry the W3C TraceContext headers from the inbound request so the trace continues unbroken into the downstream system.

---

## 13. Observability

### Decision

Full observability stack with metrics, tracing, error tracking, and logging.


| Concern        | Technology                         | Implementation                                                  |
| -------------- | ---------------------------------- | --------------------------------------------------------------- |
| Metrics        | Micrometer + Prometheus            | `@Timed`, `@Counted` via AOP. `/actuator/prometheus`            |
| Tracing        | Micrometer Tracing + OpenTelemetry | `micrometer-tracing-bridge-otel`. Feign auto-instrumented       |
| Error tracking | Sentry                             | `sentry-spring-boot-starter-jakarta`. `/actuator/sentry`        |
| Logging        | SLF4J + Logback                    | `@Slf4j` on all classes. Structured with parameterized messages |
| Health probes  | Spring Boot Actuator               | Liveness + Readiness probes                                     |


### Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, loggers, metrics, prometheus, sentry, dlqs, outbox
  metrics:
    distribution:
      percentiles:
        http.server.requests: 0.5, 0.9, 0.95, 0.99
```

---

## 14. Feature Flags

### Decision

Define a `FeatureFlagClient` port interface in the domain. Implement via vendor-specific adapter (e.g., LaunchDarkly).

```java
public interface FeatureFlagClient {
    String getFlagValue(String featureFlagKey, String defaultValue);
    Boolean getFlagValue(String featureFlagKey, Boolean defaultValue);
}
```

### Why

- Domain code depends on abstraction, not vendor SDK.
- Tests mock the interface — no vendor dependency in test classpath.
- Default values always required — service works if flag service is unavailable.

---

## 15. Distributed Job Locking: ShedLock

### Decision

Use ShedLock with JDBC provider for background jobs that must not execute concurrently across replicas.

```gradle
implementation 'net.javacrumbs.shedlock:shedlock-spring'
implementation 'net.javacrumbs.shedlock:shedlock-provider-jdbc-template'
```

- Lock state stored in the database — no additional infrastructure required.
- Prevents duplicate outbox event publishing across replicas.

---

## 16. Testing Strategy: Three-Tier Pyramid

### Decision

Three levels of automated tests with distinct purposes.

### Unit Tests (`src/test/java`)

- **Purpose:** Test domain logic, mappers, and individual components in isolation.
- **Framework:** JUnit 5 + Mockito + AssertJ.
- **Pattern:** BDD style with `// given`, `// when`, `// then` comments.
- **No Spring context.** Use `@ExtendWith(MockitoExtension.class)` and `@InjectMocks`.
- MapStruct mappers tested with `Mappers.getMapper(XMapper.class)`.
- Validation tested via `ConstraintValidationTest` base class using `jakarta.validation.Validator` directly.

### Integration Tests (`src/integration-test/java`)

- **Purpose:** Test infrastructure adapters with real dependencies.
- **Framework:** Spring Boot Test + Testcontainers (PostgreSQL) + WireMock.
- **Tests:** JPA repositories, Feign clients, controller endpoints, message listeners.

### Business Tests (`src/business-test/java`)

- **Purpose:** End-to-end flow verification from API call through downstream systems.
- **Framework:** Spring Boot Test + MockMvc + Testcontainers (Postgres, OpenSearch, Trino, Kafka) + TestContext builder.
- **Pattern:** Seed Iceberg + OpenSearch fixtures → call API → verify response shape + role-scoping + idempotency + DLQ replay command emitted via outbox.

### Architecture Tests (ArchUnit)

Extend `DefaultArchitectureTest` and enforce:


| Rule                                                      | Description                               |
| --------------------------------------------------------- | ----------------------------------------- |
| `testClassesShouldResideInTheSamePackageAsImplementation` | Tests mirror production package structure |
| `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS`               | No `System.out` / `System.err`            |
| `NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING`                 | Use SLF4J only                            |
| `NO_CLASSES_SHOULD_USE_FIELD_INJECTION`                   | Constructor injection only                |
| `NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS`              | Use specific exception types              |


### Testcontainers


| Annotation         | Container     | Image              |
| ------------------ | ------------- | ------------------ |
| `@PgTest`          | PostgreSQL    | `postgres:17`      |
| `@KafkaTest`       | Kafka         | `apache/kafka:4.0` (KRaft) |
| `@OpenSearchTest`  | OpenSearch    | `opensearchproject/opensearch:2.18.0` |
| `@TrinoTest`       | Trino         | `trinodb/trino:470` |
| `@RedisTest`       | Redis         | `redis:7.4`        |
| `@MinioTest`       | MinIO         | `minio/minio:latest` (used by Trino-Iceberg integration tests) |


### Test Fixtures

- Shared via Gradle's `testFixtures` source set.
- Naming: `SOME_ENTITY_NAME` for constants, `someEntityBuilder()` for builders.
- Stubs organized per downstream system (e.g., `OpenSearchResponseStubs`, `TrinoQueryResultStubs`).

---

## 17. Authentication & Security

### Decision

Spring Security with role-based access for API endpoints.

### Rules

- **Spring Security:** Use `@Secured({"ROLE_*"})` on every endpoint for explicit role requirements. Three roles in v1: `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_AGENT`.
- **User Extraction:** `@AuthenticationPrincipal AuthenticatedUser user` — provides `userId`, `email`, `customerId`, and roles. JWT issued by the in-repo `apps/auth/` service (RS256, JWK set served at `/.well-known/jwks.json`).
- **Customer Scoping:** Every read operation on customer-scoped data filters by `customer_id` from the principal at the repository-adapter layer. Admin role bypasses the filter explicitly.
- **Audit Trail:** Mutations (DLQ replay) record `UserDetails` (userId, email) of who triggered them. System-initiated changes use `SYSTEM_USER`.

---

## 18. Configuration & Environment

### Profiles


| Profile     | Purpose                                                |
| ----------- | ------------------------------------------------------ |
| `default`   | Base configuration (`application.yml`)                 |
| `local`     | Local Docker Compose development                       |
| `test`      | Integration/business test config (Testcontainers)      |


### Key Properties

```yaml
# Messaging
messaging:
  bindings:
    dlq-replay-commands: dlq.replay.command.v1

# External systems
application:
  opensearch:
    url: ${OPENSEARCH_URL:http://opensearch:9200}
  trino:
    url: ${TRINO_URL:jdbc:trino://trino:8080/iceberg}
  agent-mcp:
    url: ${AGENT_MCP_URL:http://agent-tools-mcp:8000}

# Outbox
outbox:
  batch-size: 500
  interval: 1000
  strategy: keep_order

# Database (auth + idempotency only)
spring:
  datasource.url: ${POSTGRES_URL:jdbc:postgresql://catalog-db:5432/stablepay_api}
  flyway.enabled: true
  jpa.open-in-view: false

# Rate limiting
ratelimit:
  customer: 100/min
  admin: 500/min
  agent: 1000/min

# JWT
auth:
  jwks-url: ${JWKS_URL:http://auth:8080/.well-known/jwks.json}
  access-token-ttl: PT15M
  refresh-token-ttl: P7D
```

---

## 19. Key Libraries


| Library | Purpose |
|---------|---------|
| `io.namastack:namastack-outbox-starter-jdbc` | Transactional outbox with JDBC (polling, batching, retry, dead-letter) |
| MapStruct | Compile-time object mapping |
| Lombok | Boilerplate reduction (@Builder, @RequiredArgsConstructor) |
| nv-i18n | ISO CurrencyCode and CountryCode |
| uuid-creator (`com.github.f4b6a3:uuid-creator`) | Time-based UUID v7 generation |
| Spring Data OpenSearch | OpenSearch repository abstraction |
| Trino JDBC driver | Trino client for analytics + agent SQL execution |
| `io.trino:trino-parser` | SQL parsing for agent-SQL allowlist validation |
| ShedLock JDBC | Distributed job locking (background maintenance jobs if added) |
| ArchUnit | Architecture rule enforcement |
| Bucket4j | Token-bucket rate limiting |
| Sentry | Error tracking (`sentry-spring-boot-starter-jakarta`) |
| `micrometer-tracing-bridge-otel` | OTEL trace context propagation |


---

## 20. Coding Conventions for Agents

### Package Naming

```
com.stablepay.payments/
  application/
    config/          <- Spring configuration classes (EventRouting, WebConfig, SecurityConfig)
    controller/      <- REST controllers (transaction, flow, customer, admin, agent)
    security/        <- Roles, AuthenticatedUser
  domain/
    transaction/     <- TransactionQueryHandler + ports
    flow/            <- FlowQueryHandler + ports
    customer/        <- CustomerSummaryHandler + ports
    dlq/             <- DlqQueryHandler + ReplayCommandHandler
    agent/           <- AgentSqlHandler + AgentSearchHandler + AgentTimelineHandler
    common/          <- Money, type-safe IDs (TransactionId, FlowId, CustomerId)
    exceptions/
  infrastructure/
    opensearch/      <- OpenSearch repository adapters + mappers
    trino/           <- Trino repository adapters + mappers
    db/              <- JPA entities (auth_users, idempotency_keys) + adapters
    kafka/           <- Outbox event publisher (Namastack)
    common/          <- Shared infrastructure utilities (e.g., TraceContextHelper)
```

### Naming Conventions


| Type            | Pattern                                                | Example                                                |
| --------------- | ------------------------------------------------------ | ------------------------------------------------------ |
| Domain port     | `{Noun}Repository`, `{Noun}Provider`, `{Noun}Executor` | `TransactionReadOnlyRepository`, `AgentSqlRepository`  |
| Infra adapter   | `{Noun}RepositoryAdapter`, `{System}Adapter`           | `OpenSearchTransactionRepositoryAdapter`, `TrinoAnalyticsRepositoryAdapter` |
| Query handler   | `{Aggregate}QueryHandler`                              | `TransactionQueryHandler`, `FlowQueryHandler`          |
| Command handler | `{Action}CommandHandler`                               | `DlqReplayCommandHandler`                              |
| Mapper          | `{Source}Mapper` or `{Target}Mapper`                   | `TransactionEntityMapper`, `MoneyEntityMapper`         |
| Test fixtures   | `{Entity}Fixtures` with `SOME_*` constants             | `TransactionFixtures.SOME_TRANSACTION`                 |
| Wire events     | `{Domain}{Action}Event`                                | `DlqReplayCommandEvent`                                |
| Commands        | `{Action}{Entity}Command`                              | `ReplayDlqMessageCommand`                              |
| Exception       | `{Noun}{Problem}Exception`                             | `TransactionNotFoundException`, `IdempotencyKeyConflictException` |
| Error code      | `STBLPAY-{NUMBER}`                                     | `STBLPAY-0101`, `STBLPAY-2004`                         |


### Code Style

- Use `var` for local variables (Java 25).
- Use Lombok `@Builder`, `@RequiredArgsConstructor`, `@Jacksonized`, `@Slf4j`.
- Records for DTOs and value objects. Classes only when mutable state is required.
- BDD test style: `// given`, `// when`, `// then`.
- AssertJ for all assertions. **Single `usingRecursiveComparison()` per result assertion** — see TESTING_STANDARDS.md golden rule.
- Parameterized tests with `@ArgumentsSource` or `@MethodSource` for multi-case validation.
- BDD-style Mockito: `given(...).willReturn(...)`, `then(...).should()`.
- Constructor injection only — never field injection.

### What NOT to Do

- Do NOT put JPA annotations in the domain layer.
- Do NOT expose JPA entities outside the infrastructure layer.
- Do NOT publish events directly to a message broker (always use Namastack outbox).
- Do NOT use mutable state in domain models.
- Do NOT hardcode queue/topic names (use `EventRouting` configuration).
- Do NOT bypass the state machine for status transitions.
- Do NOT store customer-facing status in the database (derive from internal status).
- Do NOT use `@Transactional` on controllers — place it on domain services and command handlers.
- Do NOT use `@Autowired` on fields (use constructor injection via `@RequiredArgsConstructor`).
- Do NOT use `System.out` / `System.err` (use `@Slf4j`).
- Do NOT throw generic `Exception` / `RuntimeException` (use domain-specific exceptions with `STBLPAY-XXXX` error codes).
- Do NOT use `spring.jpa.open-in-view: true`.
- Do NOT use `@Enumerated(ORDINAL)` (always `STRING`).
- Do NOT create `@SpringBootTest` for unit tests (use `@ExtendWith(MockitoExtension.class)`).
- Do NOT use `float`/`double` for money — always `BigDecimal + CurrencyCode` in the domain layer; convert to/from `long` micros at the infrastructure-layer mapper.
- Do NOT skip the customer-scope filter on read paths — always pass `CustomerId scope` from the principal into the repository call.

