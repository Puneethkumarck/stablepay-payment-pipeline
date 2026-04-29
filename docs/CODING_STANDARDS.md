# Coding Standards for Fintech Backend Services

Instructions for coding agents developing new Java/Spring Boot microservices in this codebase.

## Table of Contents

- [1. Architecture: Hexagonal (Ports and Adapters)](#1-architecture-hexagonal-ports-and-adapters)
- [2. Multi-Module Gradle Structure](#2-multi-module-gradle-structure)
- [3. Domain Layer](#3-domain-layer)
  - [3.1 Domain Models](#31-domain-models)
  - [3.2 Repository Ports](#32-repository-ports)
  - [3.3 Domain Services and Handlers](#33-domain-services-and-handlers)
  - [3.4 Domain Interfaces](#34-domain-interfaces)
  - [3.5 State Machine](#35-state-machine)
  - [3.6 Error Handling](#36-error-handling)
- [4. Application Layer](#4-application-layer)
  - [4.1 REST Controllers](#41-rest-controllers)
  - [4.2 Event Listeners](#42-event-listeners)
  - [4.3 Scheduled Jobs](#43-scheduled-jobs)
- [5. Infrastructure Layer](#5-infrastructure-layer)
  - [5.1 Database (JPA)](#51-database-jpa)
  - [5.2 Messaging (Namastack Outbox Pattern)](#52-messaging-namastack-outbox-pattern)
  - [5.3 External HTTP Clients](#53-external-http-clients)
- [6. Object Mapping](#6-object-mapping)
- [7. Testing](#7-testing)
- [8. Java Conventions](#8-java-conventions)
  - [8.1 Language Level](#81-language-level)
  - [8.2 Lombok Usage](#82-lombok-usage)
  - [8.3 Dependency Injection](#83-dependency-injection)
  - [8.4 Null Handling](#84-null-handling)
  - [8.5 Naming](#85-naming)
  - [8.6 Import Order](#86-import-order)
  - [8.7 Functional Over Imperative](#87-functional-over-imperative)
- [9. API Design](#9-api-design)
  - [9.1 Request Validation](#91-request-validation)
  - [9.2 Money Representation](#92-money-representation)
  - [9.3 Idempotency](#93-idempotency)
- [10. Event-Driven Patterns](#10-event-driven-patterns)
  - [10.1 Namastack Outbox](#101-namastack-outbox)
  - [10.2 Event Naming](#102-event-naming)
- [11. Security](#11-security)
- [12. Quick Reference Checklist](#12-quick-reference-checklist)

---

## 1. Architecture: Hexagonal (Ports and Adapters)

Every service follows a strict three-layer package structure under `com.<org>.<domain>.<service>`:

```
com.<org>.banking.payout
  ├── domain/           # Core business logic, services, models, ports
  ├── application/      # Input adapters: REST controllers, event listeners, scheduled jobs
  └── infrastructure/   # Output adapters: database, messaging, external HTTP clients
```

**Rules:**
- `domain` MUST NOT import from `application` or `infrastructure`.
- `domain` services and handlers use Spring for DI (`@Component`, `@Service`) and transactions (`@Transactional`). This is an accepted pragmatic choice.
- `domain` models (entities, value objects, enums) MUST NOT import Spring. Only Lombok is allowed on models.
- `application` depends on `domain`. It maps API models to domain models and delegates.
- `infrastructure` depends on `domain`. It implements domain repository ports and external integrations.
- Dependencies always point inward: `application` -> `domain` <- `infrastructure`.

---

## 2. Multi-Module Gradle Structure

Each service is a multi-module Gradle project:

| Module | Purpose | Plugin |
|--------|---------|--------|
| `<service>-api` | Shared contracts: API DTOs, request/response models, event schemas, validation | `java-library` |
| `<service>-client` | Feign client + auto-configuration for consumers of this service | `java-library` |
| `<service>` (core) | Main application: domain, application, infrastructure | `org.springframework.boot` |

**Dependency rules:**
- The `-api` module has no dependency on the core module.
- The `-client` module depends on `-api`.
- The core module depends on `-api` via `implementation`.
- Cross-service communication uses the other service's `-client` module.
- Internal foundation libraries use the shared microservice foundation artifacts.

---

## 3. Domain Layer

### 3.1 Domain Models

Use **Java records** for immutable value objects and domain events. Use **classes with Lombok `@Builder`** when the model requires complex construction or the state machine pattern.

```java
// Value object (record)
public record Money(BigDecimal value, CurrencyCode currency) {}

// Domain event (record)
public record PayoutStateChangedEvent(Payout payout) implements StateChangedEvent {}

// Change result pairs the updated entity with its emitted event
public record PayoutChangeResult(Payout payout, StateChangedEvent event) {}
```

Domain models use Lombok but NOT Spring:

```java
// Domain entity with behavior (rich model) - Lombok only, NO Spring
@Slf4j
@Builder(toBuilder = true, access = PACKAGE)
public class Payout implements StateProvider<Status> {

    // Domain behavior lives ON the entity - returns new instances
    public PayoutChangeResult transferLiabilityToSuspenseCompleted() { ... }
    public PayoutChangeResult processingFailed() { ... }
    public PayoutChangeResult merchant4EyesApprovalApproved(UserDetails approval) { ... }
}
```

**Rules:**
- Domain models are immutable. State transitions return new instances (via `PayoutChangeResult`).
- Use `@Builder(toBuilder = true)` when you need copy-and-modify semantics.
- Enums for statuses, types, and fixed classifications. Use `@Getter` and `@RequiredArgsConstructor` on enums with fields.
- No JPA annotations in domain models. Entity mapping belongs in `infrastructure`.
- No Spring annotations on domain models. Only Lombok (`@Builder`, `@Getter`, `@RequiredArgsConstructor`, `@Slf4j`, `@SneakyThrows`).

### 3.2 Repository Ports

Define repository interfaces in the domain layer. Implementation is in `infrastructure`.

```java
// domain/payout/PayoutRepository.java - plain interface, no annotations
public interface PayoutRepository {
    Optional<Payout> findByTransactionReference(UUID transactionReference);
    Payout save(Payout payout);
}
```

### 3.3 Domain Services and Handlers

Services orchestrate domain logic. They use Spring for DI and transactions, and Lombok for boilerplate.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutCreationService {
    private final PayoutRepository payoutRepository;
    private final PayoutDataEnricher payoutDataEnricher;
    private final PayoutTransactionalProxy payoutTransactionalProxy;
    // ...
}

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventHandler implements EventHandler<TransferEvent> {
    private final PayoutRepository payoutRepository;
    private final EventPublisher<StateChangedEvent> eventPublisher;

    @Transactional
    public void handle(TransferEvent event) { ... }
}
```

**Allowed Spring annotations in domain handlers/services:**
- `@Component`, `@Service` - for DI registration
- `@Transactional`, `@Transactional(propagation = REQUIRES_NEW)` - for transaction management

**NOT allowed in domain:**
- `@RestController`, `@GetMapping`, etc. (application layer only)
- `@Entity`, `@Table`, `@Column` (infrastructure layer only)
- `@Autowired` (use `@RequiredArgsConstructor` instead)

### 3.4 Domain Interfaces

```java
public interface EventPublisher<T> {
    void publish(T event);
}

public interface EventHandler<T> {
    void handle(T event);
}
```

### 3.5 State Machine

Use a type-safe `StateMachine<Status, Entity>` for modeling state transitions. The state machine lives in the domain layer and uses only Lombok (no Spring).

```java
@Builder
@RequiredArgsConstructor
public class StateMachine<S, T extends StateProvider<S>> { ... }

// Define transitions declaratively
StateMachine.<Status, Order>builder()
    .withExceptionProvider(OrderStateMachineException::new)
    .withTransition(CREATED, PENDING, action)
    .withTransitionsFrom(PENDING, Set.of(APPROVED, REJECTED), action)
    .build();
```

- The entity must implement `StateProvider<Status>` to expose its current state.
- Invalid transitions throw a domain-specific `StateMachineException`.
- Transition actions return `StateChangedEvent` (or `null` for silent transitions).
- State transitions on the domain model return new instances (immutable), not mutate in place.

### 3.6 Error Handling

- Define domain-specific exceptions extending `RuntimeException`.
- Use structured error codes: `<ORG>-<DOMAIN>-XXXX` (4-digit, zero-padded). Example: `ACME-FIAT-0001`.
- Each exception type maps to a specific HTTP status in the application layer.

```java
public class PayoutNotFoundException extends RuntimeException { ... }
public class PayoutStateMachineException extends StateMachineException { ... }
public class InsufficientWalletBalanceException extends RuntimeException { ... }
```

---

## 4. Application Layer

### 4.1 REST Controllers

- Delegate all business logic to domain services. Controllers are thin.
- Use Spring MVC annotations (`@RestController`, `@GetMapping`, etc.).
- Use role-based access control annotations.
- Use `@Valid` for request validation. Validation rules live on the API model.
- Custom argument resolvers for cross-cutting concerns (e.g., `@ExtractedRequestMetadata`).

### 4.2 Event Listeners

- Extend framework listener base classes.
- Map external event schemas to domain models, then delegate to `EventHandler<T>`.
- Use a dedicated mapper for each event type.

### 4.3 Scheduled Jobs

- Use `@Scheduled` for cron-based processing.
- Jobs query for entities in a specific state and delegate to domain handlers.
- Keep job classes minimal; business logic stays in the domain.

---

## 5. Infrastructure Layer

### 5.1 Database (JPA)

- Package: `infrastructure.db.<feature>`.
- JPA entities are **separate** from domain models. Use `@Entity` classes with `@Table`.
- Mappers convert between JPA entities and domain models.
- Use Spring Data JPA repositories (`JpaRepository`) wrapped by an adapter that implements the domain port.

```java
// infrastructure/db/payout/PayoutJpaRepository.java (Spring Data)
interface PayoutJpaRepository extends JpaRepository<PayoutEntity, Long> { ... }

// infrastructure/db/payout/PayoutRepositoryAdapter.java (implements domain port)
class PayoutRepositoryAdapter implements PayoutRepository { ... }
```

### 5.2 Messaging (Namastack Outbox Pattern)

Use **Namastack Outbox** (`io.namastack:namastack-outbox-starter-jdbc`) for reliable transactional event publishing via Kafka.

**Event model:** Each domain event is a record with a static `TOPIC` field:

```java
public record PaymentCompleted(
        UUID paymentId,
        UUID correlationId,
        Money sourceAmount,
        Instant completedAt
) {
    public static final String TOPIC = "payment.completed";
}
```

**Publisher:** Extend `AbstractOutboxEventPublisher` to schedule events within a transaction:

```java
@Component
public class OutboxEventPublisher extends AbstractOutboxEventPublisher
        implements PaymentEventPublisher {

    public OutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId")); // key field for partitioning
    }
}
```

The base class calls `outbox.schedule(event, key)` inside `@Transactional(propagation = MANDATORY)`, ensuring events are only scheduled within an existing transaction.

**Handler:** Extend `AbstractOutboxHandler` to dispatch events to Kafka:

```java
@Component
public class ServiceOutboxHandler extends AbstractOutboxHandler {

    public ServiceOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
```

The handler is annotated with `@OutboxHandler` (`io.namastack.outbox.annotation`) and resolves the topic from the event's static `TOPIC` field.

**Rules:**
- Events MUST have a `public static final String TOPIC` field.
- Publishing MUST happen within a transaction (`propagation = MANDATORY`).
- One `OutboxEventPublisher` and one `OutboxHandler` per service module.
- Key fields (for Kafka partitioning) are resolved by accessor name (e.g., `paymentId`).

### 5.3 External HTTP Clients

- Package: `infrastructure.client.<service>`.
- Use Feign clients for inter-service communication.
- Wrap Feign clients in adapter classes that map between API models and domain models.
- Handle `FeignException` subtypes (e.g., `NotFound` -> `Optional.empty()`).
- Infrastructure records (RPC response DTOs, client config records) MUST use `@Builder(toBuilder = true)`.

---

## 6. Object Mapping

Use **MapStruct** for all layer-boundary mapping. No manual field-by-field mapping.

**Naming conventions:**
| Direction | Method name |
|-----------|-------------|
| API -> Domain | `toDomain(apiModel)` |
| Domain -> API | `toApi(domainModel)` |
| Domain -> Event | `toEvent(domainModel)` |
| Domain -> Contract | `toContract(domainModel)` |
| External -> Domain | `toDomain(externalModel)` |

**Rules:**
- One mapper interface per concern (e.g., `PayoutRequestMapper`, `TransferEventMapper`).
- Mappers are interfaces annotated with `@Mapper(componentModel = "spring")`.
- In unit tests, instantiate via `Mappers.getMapper(XxxMapper.class)` or the generated `XxxMapperImpl`.
- Use `@Spy` for mapper injection in tests when you need to verify mapper calls.

---

## 7. Testing

All testing rules, patterns, fixtures, assertions, and mocking conventions are defined in **[TESTING_STANDARDS.md](TESTING_STANDARDS.md)**. Refer to that document when writing any test code.

---

## 8. Java Conventions

### 8.1 Language Level

- Java 25 with Spring Boot 4.x / Spring Cloud 2025.x.
- Use modern Java features: `var`, records, sealed interfaces, pattern matching `switch`, text blocks, unnamed variables (`_`), string templates where appropriate.
- Use `var` for local variables when the type is obvious from context.

### 8.2 Lombok Usage

| Annotation | Where | Purpose |
|------------|-------|---------|
| `@RequiredArgsConstructor` | Domain services, handlers, infrastructure adapters | Constructor injection via `private final` fields |
| `@Slf4j` | Domain services, handlers | Logging via `log.info(...)` |
| `@Builder` | All records and value objects across all layers | Object construction |
| `@Builder(toBuilder = true)` | All records that need copy-and-modify or test fixtures | Immutable updates, test fixture readability |
| `@Getter` | Enums with fields, JPA entities | Field access |
| `@SneakyThrows` | Test methods, domain methods with checked exceptions | Avoid boilerplate |
| `@Singular` | Builder collections (e.g., StateMachine transitions) | Add-one-at-a-time builder |
| `@Data` | Test utility classes only | Never in production code |

**Rules:**
- Every `record` MUST have `@Builder(toBuilder = true)`. Positional record constructors are not acceptable — callers must use named builder fields. The only exception is trivial 1-2 field records (e.g., `record Money(BigDecimal value, String currency)`).

**Never use:**
- `@Autowired` - always use `@RequiredArgsConstructor` instead
- `@AllArgsConstructor` in domain models - use records or `@Builder`
- `@Data` in production code
- Positional record constructors for records with 3+ fields — always use `@Builder`

### 8.3 Dependency Injection

- Use **constructor injection** via `@RequiredArgsConstructor` with `private final` fields.
- No `@Autowired` annotation anywhere.

```java
@Component
@RequiredArgsConstructor
public class CustomerAwareSenderProvider implements SenderProvider {
    private final SenderAccountProvider senderAccountProvider;
    private final SenderCustomerProvider senderCustomerProvider;
}
```

### 8.4 Null Handling

- Use `Optional` for return types that may not have a value.
- Never return `null` from repository lookups; return `Optional`.
- Use `ofNullable()` when bridging nullable external data.

### 8.5 Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Package | lowercase, singular | `domain.payout.model.core` |
| Class | PascalCase | `PayoutCreationService` |
| Interface | PascalCase, no `I` prefix | `PayoutRepository` |
| Method | camelCase, verb-first | `findByTransactionReference` |
| Constant | SCREAMING_SNAKE_CASE | `FIAT_PAYOUT_FLOW_TYPE` |
| Test method | `should<Action><Condition>` | `shouldThrowExceptionIfPayoutNotFound` |
| Fixture constant | `SOME_*` or descriptive | `SOME_PAYOUT_REQUEST`, `NEW_PAYOUT` |
| Fixture builder | `<concept>Builder()` | `payoutBuilder()`, `senderBuilder()` |

### 8.6 Import Order

```java
import static ...;         // Static imports first

import com.<org>...;       // Internal imports
import com.other...;       // Third-party imports
import java...;            // Java standard library
import jakarta...;         // Jakarta imports
import org...;             // Framework imports (Spring, JUnit, etc.)
```

### 8.7 Functional Over Imperative

Prefer functional, declarative style over imperative loops and mutable state.

**Stream API over loops:**

```java
// BAD — imperative
var result = new ArrayList<String>();
for (var item : items) {
    if (item.isActive()) {
        result.add(item.name());
    }
}

// GOOD — functional
var result = items.stream()
        .filter(Item::isActive)
        .map(Item::name)
        .toList();
```

**Optional chaining over null checks:**

```java
// BAD — imperative null checks
var account = repository.findById(id);
if (account == null) {
    throw new AccountNotFoundException(id);
}
return account.balance();

// GOOD — Optional pipeline
return repository.findById(id)
        .map(Account::balance)
        .orElseThrow(() -> new AccountNotFoundException(id));
```

**Map/compute over get-then-put:**

```java
// BAD — imperative
var count = map.get(key);
if (count == null) {
    map.put(key, 1);
} else {
    map.put(key, count + 1);
}

// GOOD — functional
map.merge(key, 1, Integer::sum);
```

**Rules:**
- Use `Stream` operations (`map`, `filter`, `flatMap`, `reduce`, `collect`) instead of `for`/`while` loops for transformations and filtering.
- Use `Optional` pipelines (`map`, `flatMap`, `filter`, `orElseThrow`) instead of `if (x != null)` / `if (x.isPresent())` checks.
- Use `Map.computeIfAbsent`, `Map.merge`, `Map.getOrDefault` instead of get-check-put patterns.
- Use method references (`Item::name`) over lambdas (`item -> item.name()`) when the reference is clear.
- Prefer `toList()`, `toSet()`, `toMap()` terminal collectors.
- Prefer immutable return types: `List.of()`, `Map.of()`, `Stream.toList()` (returns unmodifiable list).
- **Exception:** Use imperative style when a loop has side effects (logging each iteration, accumulating errors), early exits, or complex multi-step mutations that would be less readable as a stream chain.

---

## 9. API Design

### 9.1 Request Validation

- Use Jakarta Bean Validation on API models (`@NotNull`, `@NotBlank`, `@Valid`, `@Pattern`).
- Custom validators implement `ConstraintValidator`.
- Centralize error message constants in `ErrorMessages` class.

### 9.2 Money Representation

```java
public record Money(
    @NotNull BigDecimal value,
    @NotNull CurrencyCode currency
) {}
```

- Always use `BigDecimal` for monetary values. Never `double` or `float`.
- Use the `nv-i18n` library for `CurrencyCode` and `CountryCode`.

### 9.3 Idempotency

- Support idempotency keys for create operations via request headers.
- Map idempotency keys through the full stack to the domain layer.

---

## 10. Event-Driven Patterns

### 10.1 Namastack Outbox

All events are published through the Namastack transactional outbox (see Section 5.2 for implementation details):

```java
// Domain port (in domain layer)
public interface PaymentEventPublisher {
    void publish(Object event);
}

// Infrastructure adapter (in infrastructure layer)
@Component
public class OutboxEventPublisher extends AbstractOutboxEventPublisher
        implements PaymentEventPublisher {
    public OutboxEventPublisher(Outbox outbox) {
        super(outbox, List.of("paymentId"));
    }
}
```

### 10.2 Event Naming

- Commands: `Create*Command`, `Trigger*Command`
- Events: `*StatusEvent`, `*ChangedEvent`, `*ResultEvent`
- Requests: `*ScreeningRequest`, `*TransferRequest`

---

## 11. Security

- Role-based access via Spring Security (`@RolesAllowed`, custom role annotations).
- Service-to-service authentication via shared credential mechanisms (e.g., HMAC-based auth).
- Auto-configuration classes for client authentication setup.

---

## 12. Quick Reference Checklist

Before submitting code, verify:

- [ ] Domain models have zero Spring imports (Lombok only)
- [ ] Domain services/handlers use only `@Component`/`@Service`/`@Transactional` from Spring
- [ ] Domain layer does not import from `application` or `infrastructure`
- [ ] All mapping uses MapStruct, not manual field copying
- [ ] Repository interfaces are in `domain`, implementations in `infrastructure.db`
- [ ] Events use Namastack outbox pattern, not direct Kafka publishing
- [ ] Domain events have a `public static final String TOPIC` field
- [ ] Money values use `BigDecimal` with `CurrencyCode`
- [ ] Constructor injection via `@RequiredArgsConstructor`, no `@Autowired`
- [ ] State transitions use the `StateMachine` pattern
- [ ] Custom exceptions extend `RuntimeException` with structured error codes
- [ ] Functional style: streams over loops, Optional pipelines over null checks
- [ ] Tests follow [TESTING_STANDARDS.md](TESTING_STANDARDS.md)
