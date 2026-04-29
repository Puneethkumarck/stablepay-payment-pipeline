# ADR: Architecture Decision Record

**Status:** Accepted
**Date:** 2026-03-26
**Context:** Reference architecture for coding agents building new payment processing microservices.
**Stack:** Java 25 (LTS), Spring Boot 4.0.x, Gradle, PostgreSQL, Kafka, SQS/SNS
**Architecture:** Hexagonal (Ports & Adapters) + DDD + CQRS + Event-Driven

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
11. [Persistence: JPA + PostgreSQL](#11-persistence-jpa--mysql)
12. [External Service Integration: Feign Clients](#12-external-service-integration-feign-clients)
13. [Observability](#13-observability)
14. [Feature Flags](#14-feature-flags)
15. [Distributed Job Locking: ShedLock](#15-distributed-job-locking-shedlock)
16. [Testing Strategy: Three-Tier Pyramid](#16-testing-strategy-three-tier-pyramid)
17. [Batch Processing](#17-batch-processing)
18. [Authentication & Security](#18-authentication--security)
19. [Configuration & Environment](#19-configuration--environment)
20. [Key Libraries](#20-key-libraries)
21. [Coding Conventions for Agents](#21-coding-conventions-for-agents)

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
- Domain defines ports as interfaces (e.g., `PayoutRepository`, `FeeProvider`, `TransferExecutor`, `EventPublisher<T>`). Infrastructure implements them.
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
public record WalletId(UUID value) {}
public record TransactionId(UUID value) {}
```

Use UUID v7 (time-based sortable) via `com.github.f4b6a3:uuid-creator` for entities that benefit from chronological ordering.

### Example

```java
@Builder(toBuilder = true)
public record Payout(
    UUID id,
    UUID transactionReference,
    Money amount,
    Money fee,
    Status status,
    List<StatusHistory> statusHistory
) {
    public Payout {
        Objects.requireNonNull(id);
        Objects.requireNonNull(transactionReference);
    }

    public Payout withStatus(Status newStatus, UserDetails modifiedBy) {
        return toBuilder()
            .status(newStatus)
            .statusHistory(appendStatus(statusHistory, newStatus, modifiedBy))
            .build();
    }

    public void assertNotInFinalState() {
        if (status.isFinalState()) {
            throw new PayoutInFinalStateException(transactionReference);
        }
    }
}
```

---

## 4. CQRS: Command/Query Separation

### Decision

Separate command handlers (write) and query services (read) in the domain layer.

### Command Handlers

Command handlers live in the domain layer, co-located with their aggregates. They are Spring-managed beans.

```java
@Component
@RequiredArgsConstructor
public class PayoutCommandHandler implements EventHandler<CreatePayoutCommand> {

    private final PayoutRepository payoutRepository;

    @Transactional
    public void handle(CreatePayoutCommand command) {
        var payout = Payout.create(command);
        payoutRepository.save(payout);
    }
}
```

### Query Services

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayoutQueryHandler {

    private final PayoutReadOnlyRepository payoutRepository;

    public Page<PayoutView> search(PayoutFilter filter, Pageable pageable) {
        return payoutRepository.findByFilter(filter, pageable);
    }
}
```

### Repository Port Separation

```java
// Read-only port
public interface PayoutReadOnlyRepository {
    Optional<Payout> findByTransactionReference(UUID ref);
    Page<PayoutView> findByFilter(PayoutFilter filter, Pageable pageable);
}

// Full port extends read-only
public interface PayoutRepository extends PayoutReadOnlyRepository {
    Payout save(Payout payout);
    Payout getAndLock(UUID transactionReference);
}
```

### Command Routing — List-Based Polymorphic Pattern

Spring auto-collects all `@Component` processors implementing a common interface. Each processor decides via `shouldProcess()` whether it handles the change.

```java
@Component
@RequiredArgsConstructor
public class AccountEventHandler {
    private final List<AccountChangeProcessor> accountChangeProcessors;

    private void processAccountChange(AccountChange change) {
        accountChangeProcessors.forEach(p -> p.process(change));
    }
}
```

---

## 5. State Machine: Generic, Table-Driven

### Decision

State transitions are enforced by a generic `StateMachine<S, T>` framework embedded in the aggregate.

### Rules

- Define all valid transitions statically using `withTransition(from, to, action)` and `withTransitionsFrom(from, to1, to2, ...)`.
- Each transition either emits a `StateChangedEvent` (triggering downstream side effects) or performs `noAction()`.
- Invalid transitions throw `StateMachineException` (retryable).
- Customer-facing status is derived from internal status via a mapping function. Never store customer status directly.

### Status Lifecycle

```
CREATED
  -> TRANSFER_LIABILITY_TO_SUSPENSE_TRIGGERED -> _COMPLETED / _FAILED
  -> MERCHANT_4_EYES_APPROVAL_REQUESTED -> _APPROVED / _COMPLETED / _DECLINED / _EXPIRED
  -> FINCRIME_TRIGGERED -> _ON_HOLD / _RFI / _COMPLETED / _REJECTED / _FUNDS_CONFISCATED
  -> PAYOPS_APPROVAL_REQUESTED -> _COMPLETED / _DECLINED
  -> BANKING_API_TRIGGERED -> _ROUTED
  -> PARTNER_ACCEPTED -> PARTNER_COMPLETED / PARTNER_FAILED / PARTNER_REVERTED
  -> TRANSFER_SUSPENSE_TO_ASSET_TRIGGERED -> _COMPLETED -> PROCESSING_COMPLETED
  -> TRANSFER_SUSPENSE_TO_LIABILITY_TRIGGERED -> _COMPLETED -> PROCESSING_FAILED (refund)
```

### Customer Status Mapping


| Internal Phase                                 | Customer Status    |
| ---------------------------------------------- | ------------------ |
| CREATED through FINCRIME_COMPLETED             | `PROCESSING`       |
| MERCHANT_4_EYES_APPROVAL_REQUESTED             | `PENDING_APPROVAL` |
| PARTNER_COMPLETED through PROCESSING_COMPLETED | `COMPLETED`        |
| DECLINED / EXPIRED / CANCELLED                 | `CANCELLED`        |
| PARTNER_FAILED / PROCESSING_FAILED             | `FAILED`           |
| PARTNER_REVERTED                               | `REVERTED`         |


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
public class PayoutStatusEventOutboxPublisher implements EventPublisher<PayoutStatusEvent> {

    private final OutboxEventPublisher outboxEventPublisher;
    private final EventMapper eventMapper;

    @Override
    public void publish(PayoutStatusEvent event) {
        var outboxEvent = eventMapper.toOutboxEvent(event);
        outboxEventPublisher.publish(outboxEvent);  // Written in same TX as domain change
    }
}

// OutboxHandler routes events to Kafka topics
@Component
public class PayoutOutboxHandler extends NamastackOutboxHandler {
    // Maps event types to destination topics
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
| SQS Queues   | AWS SQS    | Inbound   | Commands and targeted events     |
| SNS Topics   | AWS SNS    | Outbound  | Fan-out status notifications     |
| Kafka Topics | AWS MSK    | Both      | Transfer status events (ordered) |


### Inbound Listeners

All listeners extend `ExceptionHandlingConsumer` or `PayloadValidatingConsumer`:

```java
@Component
public class PayoutCommandListener extends PayloadValidatingConsumer<LegacyCreatePayoutCommand> {
    @Override
    protected void processValid(LegacyCreatePayoutCommand request) {
        var domainEvent = mapper.toDomain(request);
        eventHandler.handle(domainEvent);
    }
}
```

- Re-throw retryable exceptions; persist non-retryable as failure events.
- Override `processValid()` or `process()` method.

### Event Routing

Define all queue/topic bindings in a single `EventRouting` configuration class:

```java
@Configuration
public class EventRouting {
    @Value("${messaging.bindings.payout-commands}") String payoutCommands;
    @Value("${messaging.bindings.payout-status-events}") String statusEvents;
}
```

---

## 7. Pessimistic Locking for Financial Operations

### Decision

Use `@Lock(LockModeType.PESSIMISTIC_WRITE)` for all balance-affecting operations. Use `@Version` (optimistic) for everything else.

### Implementation

```java
// JPA repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM WalletEntity w WHERE w.id = :id")
Optional<WalletEntity> findByIdForUpdate(@Param("id") UUID id);

// Domain repository port
public interface WalletRepository {
    Wallet getAndLock(WalletId id);  // Delegates to findByIdForUpdate
}

// Command handler
var wallet = walletRepository.getAndLock(command.getWalletId());
wallet.assertNotBlocked();
var adjusted = wallet.withBalanceAdjusted(command.getAmount());
if (adjusted.overdrawn()) throw new InsufficientFundsException(...);
walletRepository.save(adjusted);
```

### Why

- One writer at a time per wallet — deterministic, no retries needed.
- Acceptable latency for financial operations (lock contention is low per-wallet).
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
@RequestMapping("/api/v2/fiat/pay")
public class PayoutController {

    private final PayoutCommandHandler commandHandler;
    private final PayoutQueryHandler queryService;
    private final PayoutRequestResponseMapper mapper;

    @PostMapping
    public ResponseEntity<CreatePayoutResponse> create(
            @RequestBody @Valid CreatePayoutRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        var command = mapper.toCommand(request);
        var result = commandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }
}
```

### Rules

- Controllers are **thin** — only mapping and delegation, no business logic.
- **Idempotency:** All create operations require an `X-Idempotency-Key` header (3-36 chars). Enforced via DB uniqueness constraint on `(idempotencyKey, accountReference)`.
- **Request Metadata:** GeoLocation headers extracted via custom `HeaderRequestMetadataArgumentResolver` with `@ExtractedRequestMetadata` annotation.
- **Error Codes:** Use structured error codes: `{ORG}-FIAT-XXXX` (4-digit zero-padded).
- **Pagination:** Use Spring `Pageable` for list endpoints. Return `Page<T>` responses.

### API Versioning

```
/api/v2/fiat/pay/           <- Public customer/merchant API
/api/v2/fiat/pay/unified/   <- Newer unified API (preferred for new integrations)
/api/banking/internal/       <- Internal operations API (PayOps, compliance)
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

```java
@Repository
@RequiredArgsConstructor
public class PayoutRepositoryAdapter implements PayoutRepository {
    private final PayoutJpaRepository jpaRepository;
    private final PayoutEntityMapper mapper;

    @Override
    public Payout save(Payout payout) {
        var entity = mapper.toEntity(payout);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
```

### Database Patterns


| Decision           | Choice                                    | Reason                                    |
| ------------------ | ----------------------------------------- | ----------------------------------------- |
| Entity inheritance | `@MappedSuperclass`                       | Avoids JOIN overhead                      |
| Dynamic queries    | `JpaSpecificationExecutor<T>`             | Specification pattern is cleaner          |
| Pagination         | Offset-based (`Page<Pageable>`)           | Standard for REST APIs                    |
| JSON columns       | `@JdbcTypeCode(SqlTypes.JSON)`            | Simpler for metadata than separate tables |
| Flyway             | Enabled, `V{N}__{TICKET}_description.sql` | Versioned schema evolution                |


---

## 12. External Service Integration: Feign Clients

### Decision

All HTTP service-to-service calls use Spring Cloud OpenFeign.

### External Services


| Service                 | Purpose                              | Adapter                        |
| ----------------------- | ------------------------------------ | ------------------------------ |
| Banking API             | Partner routing                      | `BankingApiAdapter`            |
| Wallet Service          | Wallet balances, processing profiles | `WalletServiceAdapter`         |
| Beneficiary Management  | Beneficiary validation/creation      | `BeneficiaryManagementAdapter` |
| Fee Service             | Fee calculation                      | `FeeServiceAdapter`            |
| Transfer Service        | Ledger transfers                     | `TransferServiceClient`        |
| Capabilities Management | Auto-approval thresholds             | `CapabilitiesServiceAdapter`   |
| Accounts Service        | Customer/sender details              | `AccountServiceAdapter`        |
| Bridge Service          | Wallet/merchant mapping              | `BridgeAdapter`                |


### Rules

- Each external service has a dedicated adapter implementing a domain port.
- Client URLs via: `application.clients.{service-name}.url`.
- Automatic distributed tracing via `MicrometerObservationCapability`.

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

- **Purpose:** End-to-end flow verification from API call through all state transitions.
- **Framework:** Spring Boot Test + MockMvc + WireMock + TestContext builder.
- **Pattern:** Create payout -> verify external calls -> verify DB state -> verify events published.

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


| Annotation   | Container     | Image              |
| ------------ | ------------- | ------------------ |
| `@PgTest`    | PostgreSQL    | `postgres:16`      |
| `@KafkaTest` | Kafka         | `cp-kafka:7.6.0`   |
| `@S3Test`    | LocalStack S3 | `localstack:2.1.0` |
| `@RedisTest` | Redis         | `redis:7.2`        |


### Test Fixtures

- Shared via Gradle's `testFixtures` source set.
- Naming: `SOME_ENTITY_NAME` for constants, `someEntityBuilder()` for builders.
- WireMock stubs organized by external service (e.g., `FeeServiceStubs`, `BridgeServiceStubs`).

---

## 17. Batch Processing

### Decision

Batch payouts via CSV upload with pre-processing, validation, and MFA-triggered execution.

### Flow

```
1. Upload CSV -> Parse into PreProcessedPayouts -> Persist batch (CREATED)
2. Validate each payout (beneficiary, amount, limits) -> Update status (VALIDATED)
3. MFA trigger -> Create individual payouts from batch -> Each follows standard lifecycle
4. Batch completion tracked via BatchCompletionService
```

### Rules

- Batch and payout references are UUIDs.
- Pre-processed payouts can be updated or deleted before triggering.
- Batch status: `CREATED -> VALIDATED -> PROCESSING -> COMPLETED`.
- Validation errors stored per payout as `Map<String, String>`.

---

## 18. Authentication & Security

### Decision

Spring Security with role-based access for API endpoints.

### Rules

- **Spring Security:** Use `@Secured({"ROLE_*"})` on every endpoint for explicit role requirements.
- **User Extraction:** `@AuthenticationPrincipal AuthenticatedUser user` — provides `userId`, `email`, `fullName`, `accountReference`, and roles.
- **Audit Trail:** Every state change records `UserDetails` (userId, fullName, email) of who triggered it. System-initiated changes use `SYSTEM_USER_DETAILS`.

---

## 19. Configuration & Environment

### Profiles


| Profile   | Purpose                                |
| --------- | -------------------------------------- |
| `default` | Base configuration (`application.yml`) |
| `local`   | Local development with LocalStack      |
| `test`    | Integration/business test config       |


### Key Properties

```yaml
# Messaging
messaging.bindings:
  payout-commands: <queue-name>
  payout-status-events: <topic-name>
  transfer-status-events: <kafka-topic>

# External clients
application.clients:
  banking-api.url: <url>
  wallet-service.url: <url>

# Outbox
outbox:
  batch-size: 500
  interval: 1000
  strategy: keep_order

# Database
spring:
  datasource.url: jdbc:postgresql://<host>/<db>
  flyway.enabled: true
  jpa.open-in-view: false
```

---

## 20. Key Libraries


| Library | Purpose |
|---------|---------|
| `namastack-outbox-starter-jdbc` | Transactional outbox with JDBC (polling, batching, retry, dead-letter) |
| MapStruct | Compile-time object mapping |
| Lombok | Boilerplate reduction (@Builder, @RequiredArgsConstructor) |
| nv-i18n | ISO CurrencyCode and CountryCode |
| uuid-creator | Time-based UUID v7 generation |
| Flying Saucer | PDF generation |
| OpenCSV | CSV parsing |
| ShedLock | Distributed job locking |
| ArchUnit | Architecture rule enforcement |


---

## 21. Coding Conventions for Agents

### Package Naming

```
com.{org}.banking.{domain}/
  application/
    config/          <- Spring configuration classes
    controller/      <- REST controllers
    stream/          <- Message listeners
    stream/mapper/   <- Wire-to-domain mappers
    job/             <- Scheduled jobs
  domain/
    {subdomain}/     <- Grouped by business capability
    {subdomain}/model/ <- Domain models
    common/model/    <- Shared value objects
    statemachine/    <- Generic state machine framework
    command/         <- Command handler interfaces
  infrastructure/
    db/              <- JPA entities, repositories, adapters
    db/{subdomain}/  <- Grouped by aggregate
    client/          <- Feign clients and adapters
    client/{service}/ <- One package per external service
    stream/          <- Event publishers (outbox)
    stream/mapper/   <- Domain-to-wire mappers
    common/          <- Shared infrastructure utilities
```

### Naming Conventions


| Type            | Pattern                                                | Example                                                |
| --------------- | ------------------------------------------------------ | ------------------------------------------------------ |
| Domain port     | `{Noun}Repository`, `{Noun}Provider`, `{Noun}Executor` | `PayoutRepository`, `FeeProvider`                      |
| Infra adapter   | `{Noun}RepositoryAdapter`, `{Service}Adapter`          | `PayoutRepositoryAdapter`, `BankingApiAdapter`         |
| Event handler   | `{Event}Handler` implementing `EventHandler<T>`        | `TransferEventHandler`                                 |
| Command handler | `{Action}CommandHandler`                               | `BalanceAdjustmentCommandHandler`                      |
| Mapper          | `{Source}Mapper` or `{Target}Mapper`                   | `PayoutEntityMapper`, `TransferEventMapper`            |
| Test fixtures   | `{Entity}Fixtures` with `SOME_*` constants             | `PayoutFixtures.SOME_PAYOUT`                           |
| Wire events     | `{Domain}{Action}Event`                                | `PayoutStatusEvent`, `TransferEvent`                   |
| Commands        | `{Action}{Entity}Command`                              | `CreatePayoutCommand`, `TriggerBatchCommand`           |
| Exception       | `{Noun}{Problem}Exception`                             | `InsufficientFundsException`, `WalletBlockedException` |
| Error code      | `{SERVICE_PREFIX}-{NUMBER}`                            | `FIAT-0101`, `TRANS-2004`                              |


### Code Style

- Use `var` for local variables (Java 25).
- Use Lombok `@Builder`, `@RequiredArgsConstructor`, `@Jacksonized`, `@Slf4j`.
- Records for DTOs and value objects. Classes only when mutable state is required.
- BDD test style: `// given`, `// when`, `// then`.
- AssertJ for all assertions. `usingRecursiveComparison()` for deep equality on records.
- Parameterized tests with `@ArgumentsSource` or `@MethodSource` for multi-case validation.
- BDD-style Mockito: `given(...).willReturn(...)`, `then(...).should()`.
- Constructor injection only — never field injection.

### What NOT to Do

- Do NOT put JPA annotations in the domain layer.
- Do NOT expose JPA entities outside the infrastructure layer.
- Do NOT publish events directly to a message broker (always use outbox).
- Do NOT use mutable state in domain models.
- Do NOT hardcode queue/topic names (use `EventRouting` configuration).
- Do NOT bypass the state machine for status transitions.
- Do NOT store customer-facing status in the database (derive from internal status).
- Do NOT use `@Transactional` on controllers — place it on domain services and command handlers.
- Do NOT use `@Autowired` on fields (use constructor injection via `@RequiredArgsConstructor`).
- Do NOT use `System.out` / `System.err` (use `@Slf4j`).
- Do NOT throw generic `Exception` / `RuntimeException` (use domain-specific exceptions with error codes).
- Do NOT use `spring.jpa.open-in-view: true`.
- Do NOT use `@Enumerated(ORDINAL)` (always `STRING`).
- Do NOT create `@SpringBootTest` for unit tests (use `@ExtendWith(MockitoExtension.class)`).

