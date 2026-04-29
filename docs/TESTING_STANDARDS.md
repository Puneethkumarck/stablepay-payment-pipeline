# Testing Standards

> Mandatory testing rules for Java/Spring Boot fintech microservices.
> Coding agents must follow these rules exactly. Do not deviate unless explicitly instructed.

## Table of Contents

- [GOLDEN RULE: Build Expected Object + Single Recursive Comparison](#golden-rule-build-expected-object--single-recursive-comparison-mandatory)
- [1. Test Strategy Overview](#1-test-strategy-overview)
- [2. Test Naming Conventions](#2-test-naming-conventions)
- [3. Test Structure: Given / When / Then](#3-test-structure-given--when--then)
- [4. Mocking Approach](#4-mocking-approach)
  - [4.1 Framework: Mockito with BDD Style](#framework-mockito-with-bdd-style)
  - [4.2 Unit Test Setup](#unit-test-setup)
  - [4.3 No Generic Argument Matchers (MANDATORY)](#no-generic-argument-matchers-mandatory)
  - [4.4 @Spy for Real Mappers/Validators](#spy-for-real-mappersvalidators)
  - [4.5 Integration Test Mocking](#integration-test-mocking)
  - [4.6 Log Capture](#log-capture)
- [5. Test Fixtures & Builders](#5-test-fixtures--builders)
- [6. Assertions](#6-assertions)
  - [6.1 Domain Model & Mapper Assertion Style](#domain-model--mapper-assertion-style-mandatory)
  - [6.2 Handler / Command Handler Test Assertion Style](#handler--command-handler-test-assertion-style-mandatory)
- [7. Parameterized Tests](#7-parameterized-tests)
- [8. Integration Test Setup](#8-integration-test-setup)
- [9. Business Test Setup](#9-business-test-setup)
- [10. Architecture Test (MANDATORY for new projects)](#10-architecture-test-mandatory-for-new-projects)
- [11. @Nested Test Classes](#11-nested-test-classes)
- [12. Test Data Isolation Strategies](#12-test-data-isolation-strategies)
- [13. Test Utilities](#13-test-utilities)
- [14. Coverage & Quality Gates](#14-coverage--quality-gates)
- [Quick Reference: Test Cheat Sheet](#quick-reference-test-cheat-sheet)
- [Anti-Patterns Summary](#anti-patterns-summary)

---

## GOLDEN RULE: Build Expected Object + Single Recursive Comparison (MANDATORY)

**Every test that verifies an object result MUST construct an expected object and compare with a single `assertThat(...).usingRecursiveComparison()`.** Multiple `assertThat` calls on individual fields are FORBIDDEN.

This applies to ALL test types: domain model tests, mapper tests, service tests, handler tests, integration tests.

### The Pattern

```java
// 1. Build expected object using toBuilder(), factory, or constructor
var expected = input.toBuilder()
        .status(NEW_STATUS)
        .build();

// 2. Single assertion with recursive comparison
assertThat(result)
        .usingRecursiveComparison()
        .ignoringFields("generatedId", "createdAt")   // only non-deterministic fields
        .isEqualTo(expected);
```

### Why

- Multiple scattered asserts create **incomplete verification** — if the method changes a field you didn't assert on, the test still passes silently
- A single recursive comparison catches **every field change**, making tests fail-fast on regressions
- Building the expected object makes the test **self-documenting** — the reader sees the full expected state

### FORBIDDEN vs REQUIRED

```java
// FORBIDDEN: multiple asserts on individual fields
var result = quote.lock();
assertThat(result.status()).isEqualTo(LOCKED);        // NEVER DO THIS
assertThat(result.quoteId()).isEqualTo(quoteId);      // NEVER DO THIS
assertThat(result.amount()).isEqualTo(amount);         // NEVER DO THIS
assertThat(result.currency()).isEqualTo("USD");        // NEVER DO THIS

// REQUIRED: build expected object + single recursive comparison
var result = quote.lock();
var expected = quote.toBuilder().status(LOCKED).build();
assertThat(result)
        .usingRecursiveComparison()
        .ignoringFields("lockedAt")
        .isEqualTo(expected);
```

### How to Build the Expected Object

| Scenario | Technique |
|---|---|
| State transition on existing object | `input.toBuilder().status(NEW_STATUS).build()` |
| Factory method creates new object | `Entity.builder().field1(val1).field2(val2).status(INITIAL).build()` |
| Mapper transforms input to output | `ExpectedType.builder().mappedField1(source.field1()).build()` |
| Handler saves mutated entity | Clone fixture via `toBuilder()`, apply same domain operation |

### Applies to Every Layer

**Domain model test:**
```java
var result = ComplianceCheck.initiate(paymentId, corridor, amount);
var expected = ComplianceCheck.builder()
        .paymentId(paymentId).corridor(corridor).amount(amount)
        .status(PENDING).build();
assertThat(result).usingRecursiveComparison()
        .ignoringFields("checkId", "createdAt").isEqualTo(expected);
```

**Mapper test:**
```java
var result = mapper.toView(domainTransaction);
var expected = TransactionView.builder()
        .reference(domainTransaction.id().value().toString())
        .customerStatus("COMPLETED")
        .amount(SOME_MONEY)
        .eventTime(SOME_INSTANT)
        .build();
assertThat(result).usingRecursiveComparison().isEqualTo(expected);
```

**Service/handler test (interaction verification):**
```java
var expectedKey = IdempotencyKey.builder()
        .key(SOME_IDEMPOTENCY_KEY)
        .scope(SOME_ADMIN_USER.id().toString())
        .result(ReplayResult.accepted(SOME_DLQ_ID))
        .build();
// ... when ...
then(idempotencyKeyRepository).should().save(eqIgnoringTimestamps(expectedKey));
```

**Integration test (controller response):**
```java
var response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), TransactionView.class);
var expected = TransactionView.builder()
        .reference(ref.value().toString())
        .customerStatus("PROCESSING")
        .amount(SOME_MONEY)
        .build();
assertThat(response).usingRecursiveComparison()
        .ignoringFields("eventTime", "ingestTime").isEqualTo(expected);
```

### Narrow Exceptions (the ONLY cases where individual asserts are allowed)

| Case | Why allowed | Example |
|---|---|---|
| Exception assertions | No object to compare | `assertThatThrownBy(...).isInstanceOf(...).hasMessage(...)` |
| Single boolean/primitive returns | Trivial comparison | `assertThat(result.isValid()).isTrue()` |
| Collection size + containment | AssertJ collection API is the right tool | `assertThat(list).hasSize(3).containsOnly(a, b, c)` |
| Single enum/string mapping | One-to-one mapping test | `assertThat(mapper.map(INPUT)).isEqualTo(OUTPUT)` |
| Optional presence checks | Wrapper, not domain object | `assertThat(result).isPresent().hasValue(expected)` |

**If in doubt, build an expected object.** The only time you skip it is when there is literally no object to build.

---

## 1. Test Strategy Overview

The project uses **four physically separated test source sets** with distinct scopes:

| Source Set | Directory | Scope | Speed |
|---|---|---|---|
| **Unit** | `src/test/java/` | Single class, mocked deps | Fast |
| **Test Fixtures** | `src/testFixtures/java/` | Shared data, stubs, utilities | N/A |
| **Integration** | `src/integration-test/java/` | Spring context, DB, WireMock | Medium |
| **Business** | `src/business-test/java/` | Full E2E flows, real server | Slow |

### Test Pyramid

```
         /\
        /  \       Business Tests
       /----\      Full server + WireMock + DB
      /      \
     / Integ  \    Integration Tests
    /----------\   Spring context + TestContainers + MockMvc
   /            \
  / Unit Tests   \ Unit Tests
 /----------------\ Pure Mockito, no Spring context
```

---

## 2. Test Naming Conventions

### Primary Convention: `should*` in camelCase

```java
void shouldReturnTransactionByReference()
void shouldThrowIfTransactionNotFound()
void shouldRejectAgentSqlOutsideAllowlist()
void shouldEmitDlqReplayCommandWhenIdempotencyKeyIsNew()
void shouldReturnExistingResultWhenIdempotencyKeyAlreadyUsed()
```

### Secondary Convention: `given_When_Then` (older legacy code only)

```java
void givenAValidRequestWithAuthenticatedUser_WhenAttemptToRetrieveTheTransaction_ThenSuccess()
```

**Rule**: Standardize on `should*` camelCase for all new code. The `given_When_Then` variant is legacy.

---

## 3. Test Structure: Given / When / Then

**Every test** follows the Given/When/Then pattern with explicit comment markers:

```java
@Test
void shouldEmitDlqReplayCommandWhenIdempotencyKeyIsNew() {
    // given
    var command = new ReplayDlqMessageCommand(SOME_DLQ_ID, "dlq.processing-failed.v1", SOME_IDEMPOTENCY_KEY);
    var user = SOME_ADMIN_USER;
    given(idempotencyKeyRepository.findByKey(command.idempotencyKey(), user.id().toString()))
        .willReturn(Optional.empty());

    // when
    var result = handler.handle(command, user);

    // then
    var expectedResult = ReplayResult.accepted(SOME_DLQ_ID);
    assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
    var expectedEvent = new DlqReplayCommandEvent(SOME_DLQ_ID, "dlq.processing-failed.v1", user.id(), SOME_INSTANT);
    then(eventPublisher).should().publish(eqIgnoring(expectedEvent, "requestedAt"));
    then(idempotencyKeyRepository).should().save(eqIgnoringTimestamps(IdempotencyKey.builder()
        .key(command.idempotencyKey()).scope(user.id().toString()).result(expectedResult).build()));
}
```

**For exception tests**, `// when` and `// then` are combined:

```java
@Test
void shouldThrowIfTransactionNotFound() {
    // given
    var ref = new TransactionId(UUID.randomUUID());
    given(transactionRepository.findByReference(ref, SOME_CUSTOMER_ID))
        .willReturn(Optional.empty());

    // when/then
    assertThatThrownBy(() -> handler.findByReference(ref, SOME_CUSTOMER_ID).orElseThrow(
        () -> new TransactionNotFoundException(ref)))
        .isInstanceOf(TransactionNotFoundException.class)
        .hasMessageContaining("Transaction not found:");
}
```

Rules:
- `// given` sets up inputs and expectations. Omit only if zero setup is needed.
- `// when` contains exactly one action (the method under test).
- `// then` contains only assertions or mock verifications.
- Use `var` for local variable declarations.

---

## 4. Mocking Approach

### Framework: Mockito with BDD Style

The project uses **BDDMockito exclusively**:

| BDD Style (REQUIRED) | Standard Style (FORBIDDEN) |
|---|---|
| `given(...).willReturn(...)` | `when(...).thenReturn(...)` |
| `then(...).should()` | `verify(...)` |
| `then(...).should(never())` | `verify(..., never())` |
| `then(...).shouldHaveNoInteractions()` | `verifyNoInteractions(...)` |

### Unit Test Setup

```java
@ExtendWith(MockitoExtension.class)
class DlqReplayCommandHandlerTest {
    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private EventPublisher<DlqReplayCommandEvent> eventPublisher;
    @InjectMocks private DlqReplayCommandHandler handler;
}
```

### No Generic Argument Matchers (MANDATORY)

**Never use `any()`, `anyString()`, `anyLong()`, `eq()`, or similar generic Mockito matchers.** Always pass actual values in both stubs (`given()`) and verifications (`then().should()`).

```java
// BAD: generic matchers give zero confidence in what was actually called
given(repo.findById(any())).willReturn(Optional.of(transaction));                 // FORBIDDEN
given(repo.findByReference(any(), any())).willReturn(Optional.empty());           // FORBIDDEN
given(repo.save(any(IdempotencyKey.class))).willAnswer(inv -> inv.getArgument(0));// FORBIDDEN
then(repo).should().save(eq(expectedKey));                                        // FORBIDDEN

// GOOD: actual values - test fails if arguments don't match
given(repo.findById(transactionId)).willReturn(Optional.of(transaction));         // CORRECT
given(repo.findByReference(transactionId, customerId)).willReturn(Optional.empty()); // CORRECT
then(repo).should().save(eqIgnoringTimestamps(expectedKey));                      // CORRECT
```

**Allowed custom matchers** (these verify actual content, not bypass matching):
- `eqIgnoringTimestamps(expected)` — recursive comparison ignoring timestamp types
- `eqIgnoring(expected, "field1", "field2")` — recursive comparison ignoring specific fields

**Stubbing `save()` with return value**: when the handler needs the saved object back:
```java
given(repo.save(eqIgnoringTimestamps(expectedKey)))
        .willAnswer(inv -> inv.getArgument(0));
```

### `@Spy` for Real Mappers/Validators

```java
@Spy private final TransactionResponseMapper mapper = getMapper(TransactionResponseMapper.class);
@Spy private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
```

MapStruct mappers and Jakarta validators are spied rather than mocked, so their real logic executes.

### Integration Test Mocking

```java
@ServiceTest
@AutoConfigureMockMvc
class TransactionControllerTest extends RestControllerAbstractTest {
    @MockBean private TransactionQueryHandler handler;
    @SpyBean private TransactionResponseMapper spiedMapper;
}
```

### Log Capture

```java
@Nested
@ExtendWith(OutputCaptureExtension.class)
class LoggingTests {
    @Test
    void shouldLogWarning(CapturedOutput output) {
        // ... trigger action ...
        assertThat(output).contains("expected log message");
    }
}
```

---

## 5. Test Fixtures & Builders

### Fixture Location

All fixtures live in `src/testFixtures/java/`. Never define shared fixtures in `src/test/`.

### Fixture Structure

```
src/testFixtures/java/com/stablepay/payments/test/
    FullContextIntegrationTest.java       // Integration test base (Testcontainers + Spring)
    RestControllerAbstractTest.java        // Controller test base (MockMvc + JWT)
    TestUtils.java                         // Comparison utilities (MANDATORY)
    commons/
        JwtTokenGenerator.java             // Issues test JWTs with role + customer_id claims
        TraceContextGenerator.java         // Generates W3C trace IDs for trace-propagation tests
    fixtures/
        TransactionFixtures.java           // SOME_TRANSACTION + builders for each lifecycle state
        FlowFixtures.java                  // SOME_FLOW + builders for on_ramp / off_ramp / crypto_to_crypto
        DlqFixtures.java                   // SOME_DLQ_MESSAGE + per-error-class fixtures
        CustomerFixtures.java              // SOME_CUSTOMER + 5 seeded test customers
        IdempotencyKeyFixtures.java
        ApiErrorFixtures.java              // Error response fixtures
        CommonFixtures.java                // SOME_MONEY, SOME_INSTANT, SOME_TRACE_ID
    stubs/
        opensearch/
            OpenSearchTransactionStubs.java   // WireMock OS responses for transaction queries
            OpenSearchFlowStubs.java
            OpenSearchDlqStubs.java
        trino/
            TrinoAnalyticsStubs.java          // JdbcTemplate result-set stubs
            TrinoAgentSqlStubs.java
        auth/
            AuthServiceJwksStubs.java         // JWK set responses for JWT decoder tests
        kafka/
            KafkaOutboxStubs.java             // Asserts outbox messages emitted with correct partition key
    request/
        InvalidTransactionFilterRequestProvider.java
        InvalidReplayDlqMessageRequestProvider.java
        InvalidAgentSqlRequestProvider.java
```

### Fixture Design Patterns

**Pattern 1: Immutable static constants with descriptive names**

```java
@NoArgsConstructor(access = PRIVATE)
public final class TransactionFixtures {
    public static final Transaction NEW_TRANSACTION = createTransactionWithStatus(CREATED);
    public static final Transaction COMPLIANCE_HELD_TRANSACTION = createTransactionWithStatus(COMPLIANCE_HELD);
    public static final Transaction PROVIDER_SETTLED_TRANSACTION = createTransactionWithStatus(PROVIDER_SETTLED);
    // ... constants covering every customer-visible status, plus a sample of internal statuses
}
```

**Pattern 2: Builder factory methods**

```java
public static TransactionFilterRequestBuilder transactionFilterRequestBuilder() {
    return TransactionFilterRequest.builder()
        .from(SOME_INSTANT.minus(7, DAYS))
        .to(SOME_INSTANT)
        .currency("USD")
        .status(CustomerStatus.COMPLETED)
        .pageSize(50);
}
```

**Pattern 3: State-machine-based fixture generation**

`TransactionFixtures.createTransactionWithStatus(InternalStatus)` walks a transaction through its lifecycle from `CREATED` to the desired state, producing realistic test objects with proper event timeline. Used heavily in business tests that need a transaction in a specific intermediate state.

**Pattern 4: `SOME_*` prefix convention**

```java
public static final Money SOME_MONEY = new Money(new BigDecimal("100.00"), CurrencyCode.USD);
public static final CustomerId SOME_CUSTOMER_ID = new CustomerId(UUID.fromString("..."));
public static final TransactionId SOME_TRANSACTION_ID = new TransactionId(UUID.fromString("..."));
public static final FlowId SOME_FLOW_ID = new FlowId(UUID.fromString("..."));
public static final Instant SOME_INSTANT = Instant.parse("2026-04-29T12:00:00Z");
public static final String SOME_IDEMPOTENCY_KEY = "stablepay-test-key-001";
public static final DlqReplayCommandEvent SOME_DLQ_REPLAY_COMMAND_EVENT = ...;
```

**Pattern 5: Static import for clean test code**

```java
import static com.stablepay.payments.test.fixtures.TransactionFixtures.PROVIDER_SETTLED_TRANSACTION;
import static com.stablepay.payments.test.fixtures.CommonFixtures.SOME_MONEY;
import static com.stablepay.payments.test.fixtures.CommonFixtures.SOME_CUSTOMER_ID;
```

**Pattern 6: All fixture factories MUST live in testFixtures (MANDATORY)**

Test helper methods that create domain objects must be extracted into dedicated `*Fixtures` classes in `src/testFixtures/java/.../fixtures/`. They must NOT remain as private methods in test classes.

Rules:
- One fixture class per aggregate/entity: `TransactionFixtures`, `FlowFixtures`, `DlqFixtures`, etc.
- Fixture classes are `public final` with a `private` constructor (utility class pattern)
- All factory methods are `public static` — callers use static imports
- Shared constants (e.g., `SOURCE_AMOUNT`, `BASE_TIME`) also live in fixture classes
- Methods that depend on Spring beans (e.g., `saveQuote()` which calls a repository) stay in the test class — only pure factory methods go to testFixtures
- Both unit tests (`src/test/`) and integration tests (`src/integration-test/`) should use testFixtures

Example:
```java
// src/testFixtures/java/.../fixtures/ComplianceCheckFixtures.java
public final class ComplianceCheckFixtures {
    public static final Money SOURCE_AMOUNT = new Money(new BigDecimal("1000.00"), "USD");
    private ComplianceCheckFixtures() {}

    public static ComplianceCheck aPendingCheck() {
        return ComplianceCheck.initiate(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), SOURCE_AMOUNT, "US", "DE", "EUR");
    }

    public static KycResult aKycResult(UUID checkId) { ... }
    public static SanctionsResult aSanctionsClearResult(UUID checkId) { ... }
}
```

**Pattern 7: `ArgumentsProvider` for parameterized tests**

```java
public static class TransactionWithMultipleStatesProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext ctx) {
        return Stream.of(
            of(NEW_TRANSACTION),
            of(COMPLIANCE_HELD_TRANSACTION),
            of(PROVIDER_SETTLED_TRANSACTION),
            // ... representative states
        );
    }
}
```

---

## 6. Assertions

### Library: AssertJ (exclusive)

No JUnit `assertEquals`/`assertTrue` anywhere. Only `assertThat` from AssertJ.

### Fluent Assertions

```java
assertThat(allBindings).containsOnly(destination1, destination2);
assertThat(shouldDiscardWhenInvalid).isFalse();
assertThat(result).isPresent().hasValue(saveEntity);
assertThat(result).isEmpty();
```

### Recursive Comparison (standard approach for complex objects)

```java
assertThat(transactionView)
    .usingRecursiveComparison()
    .isEqualTo(expectedTransactionView);

assertThat(apiError)
    .usingRecursiveComparison()
    .ignoringFields("details.documentLink")
    .ignoringCollectionOrder()
    .isEqualTo(expected);
```

### Exception Assertions

```java
assertThatThrownBy(() -> handler.handle(command, user))
    .isInstanceOf(TransactionNotFoundException.class)
    .hasMessageContaining("Transaction not found");

assertThatThrownBy(() -> sqlValidator.validate(disallowedSql))
    .isExactlyInstanceOf(AgentSqlNotAllowedException.class)
    .hasMessageContaining("Table not in allowlist:");
```

### Custom Timestamp-Ignoring Comparison

```java
then(payoutRepository).should().save(eqIgnoringTimestamps(expected.payout()));
```

### Domain Model & Mapper Assertion Style (MANDATORY)

> See **GOLDEN RULE** at the top of this document. This section provides additional domain-specific guidance.

**All tests that produce an object result** — domain models, mappers, services, value objects — must build an expected object and use a single `usingRecursiveComparison()`. This is not optional.

#### Decision Tree: How to Assert

```
Is the result an object (entity, VO, DTO, event)?
  YES -> Build expected object + single usingRecursiveComparison()
  NO  -> Is it a primitive, boolean, enum, or Optional wrapper?
    YES -> Single assertThat is acceptable
    NO  -> Is it an exception?
      YES -> assertThatThrownBy().isInstanceOf().hasMessage()
      NO  -> Is it a collection?
        YES -> assertThat(list).hasSize(n).containsOnly(...)
```

#### Complete Examples by Layer

**Domain method on a record (returning new instance):**
```java
@Test
void shouldDeriveCustomerStatusFromInternalStatus() {
    // given
    var transaction = NEW_TRANSACTION.toBuilder().internalStatus("PROVIDER_SETTLED").build();

    // when
    var result = transaction.withDerivedCustomerStatus();

    // then
    var expected = transaction.toBuilder()
            .customerStatus(CustomerStatus.COMPLETED.name())
            .build();

    assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected);
}
```

**Factory method (new entity with generated ID):**
```java
@Test
void shouldInitiateIdempotencyKey() {
    // given
    var key = "stablepay-test-key-001";
    var scope = SOME_ADMIN_USER.id().toString();
    var result = ReplayResult.accepted(SOME_DLQ_ID);

    // when
    var idempotencyKey = IdempotencyKey.create(key, scope, result);

    // then
    var expected = IdempotencyKey.builder()
            .key(key)
            .scope(scope)
            .result(result)
            .build();

    assertThat(idempotencyKey)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt")
            .isEqualTo(expected);
}
```

**Value object creation:**
```java
@Test
void shouldCreateMoney() {
    // when
    var result = Money.of(new BigDecimal("100.00"), CurrencyCode.USD);

    // then
    var expected = new Money(new BigDecimal("100.00"), CurrencyCode.USD);

    assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected);
}
```

**Mapper (input to output):**
```java
@Test
void shouldMapDomainTransactionToView() {
    // given
    var domain = COMPLIANCE_HELD_TRANSACTION;

    // when
    var result = mapper.toView(domain);

    // then
    var expected = TransactionView.builder()
            .reference(domain.id().value().toString())
            .customerStatus(CustomerStatus.PROCESSING.name())
            .internalStatus(domain.internalStatus())
            .amount(domain.amount())
            .eventTime(domain.eventTime())
            .build();

    assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected);
}
```

**Domain service returning a result:**
```java
@Test
void shouldComputeCustomerSummaryForDateRange() {
    // given
    var customerId = SOME_CUSTOMER_ID;
    var range = new DateRange(SOME_INSTANT.minus(30, DAYS), SOME_INSTANT);
    var aggRows = List.of(
        new VolumeRow("USD", 12, new BigDecimal("12500.00")),
        new VolumeRow("EUR", 5, new BigDecimal("4200.00"))
    );
    given(summaryRepository.queryVolumes(customerId, range)).willReturn(aggRows);

    // when
    var result = summaryHandler.load(customerId, range);

    // then
    var expected = CustomerSummary.builder()
            .customerId(customerId)
            .range(range)
            .totalVolumeByCurrency(Map.of(
                CurrencyCode.USD, new Money(new BigDecimal("12500.00"), CurrencyCode.USD),
                CurrencyCode.EUR, new Money(new BigDecimal("4200.00"), CurrencyCode.EUR)))
            .transactionCount(17)
            .build();
    assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("computedAt")
            .isEqualTo(expected);
}
```

#### FORBIDDEN (will be rejected in code review)

```java
// FORBIDDEN: multiple scattered asserts on individual fields
var result = check.startKyc();
assertThat(result.status()).isEqualTo(KYC_IN_PROGRESS);
assertThat(result.paymentId()).isEqualTo(paymentId);
assertThat(result.corridor()).isEqualTo(corridor);
assertThat(result.amount()).isEqualTo(amount);

// FORBIDDEN: asserting only some fields (misses regressions on other fields)
var result = mapper.toDomain(event);
assertThat(result.reference()).isEqualTo(event.reference());
assertThat(result.status()).isEqualTo(ACCEPTED);
// What about the 8 other fields? Silent pass if they break.

// REQUIRED: single recursive comparison catches ALL field changes
var expected = check.toBuilder().status(KYC_IN_PROGRESS).build();
assertThat(result)
        .usingRecursiveComparison()
        .ignoringFields("updatedAt")
        .isEqualTo(expected);
```

---

### Handler / Command Handler Test Assertion Style (MANDATORY)

**All handler and command handler unit tests** must follow interaction-verification style. Do NOT mix `assertThat` on return values with mock verifications.

#### Principles

1. **Verify interactions only** — use `then(repo).should().save(expected)` and `then(publisher).should().publish(expected)`
2. **Build expected objects from domain operations** — clone the fixture, apply the same domain method the handler will call
3. **Use `eqIgnoringTimestamps`** — for saved/published objects where timestamps differ
4. **Use `eqIgnoring`** — when additional fields must be ignored (e.g., `eventId`, `correlationId`)
5. **No scattered `assertThat` on return values** — the `save()`/`publish()` verification covers correctness

#### Example: handler that records a new entity (DLQ replay command)

```java
@Test
void shouldEmitDlqReplayCommandAndPersistIdempotencyKey() {
    // given
    var command = new ReplayDlqMessageCommand(SOME_DLQ_ID, "dlq.processing-failed.v1", SOME_IDEMPOTENCY_KEY);
    var user = SOME_ADMIN_USER;
    given(idempotencyKeyRepository.findByKey(SOME_IDEMPOTENCY_KEY, user.id().toString()))
            .willReturn(Optional.empty());

    // Build expected: same factory + downstream operation
    var expectedResult = ReplayResult.accepted(SOME_DLQ_ID);
    var expectedKey = IdempotencyKey.builder()
            .key(SOME_IDEMPOTENCY_KEY)
            .scope(user.id().toString())
            .result(expectedResult)
            .build();
    var expectedEvent = new DlqReplayCommandEvent(SOME_DLQ_ID, "dlq.processing-failed.v1", user.id(), SOME_INSTANT);

    given(idempotencyKeyRepository.save(eqIgnoringTimestamps(expectedKey)))
            .willAnswer(inv -> inv.getArgument(0));

    // when
    handler.handle(command, user);

    // then — verify interactions with expected objects
    then(idempotencyKeyRepository).should().save(eqIgnoringTimestamps(expectedKey));
    then(eventPublisher).should().publish(eqIgnoring(expectedEvent, "requestedAt"));
}
```

#### Example: handler that returns existing result on repeat (idempotency)

```java
@Test
void shouldReturnExistingResultWhenIdempotencyKeyAlreadyUsed() {
    // given
    var command = new ReplayDlqMessageCommand(SOME_DLQ_ID, "dlq.processing-failed.v1", SOME_IDEMPOTENCY_KEY);
    var user = SOME_ADMIN_USER;
    var existing = IdempotencyKey.builder()
            .key(SOME_IDEMPOTENCY_KEY).scope(user.id().toString())
            .result(ReplayResult.accepted(SOME_DLQ_ID)).build();
    given(idempotencyKeyRepository.findByKey(SOME_IDEMPOTENCY_KEY, user.id().toString()))
            .willReturn(Optional.of(existing));

    // when
    var result = handler.handle(command, user);

    // then
    assertThat(result).usingRecursiveComparison().isEqualTo(existing.result());
    then(eventPublisher).shouldHaveNoInteractions();
    then(idempotencyKeyRepository).should(never()).save(any());
}
```

#### Anti-patterns

```java
// BAD: mixing assertThat on return value with mock verification
var result = handler.handle(command, user);
assertThat(result).isNotNull();                                            // FORBIDDEN - redundant
assertThat(result.dlqId()).isEqualTo(SOME_DLQ_ID);                          // FORBIDDEN - covered by recursive-comparison expected
then(eventPublisher).should().publish(any(...));                            // FORBIDDEN - doesn't verify event content

// BAD: generic matchers
then(idempotencyKeyRepository).should().save(any(IdempotencyKey.class));    // FORBIDDEN
then(eventPublisher).should().publish(any(DlqReplayCommandEvent.class));    // FORBIDDEN

// BAD: generic matchers in stubs
given(repo.findByKey(any(), any())).willReturn(Optional.of(existing));      // FORBIDDEN
given(repo.save(any(IdempotencyKey.class))).willAnswer(inv -> inv.getArgument(0)); // FORBIDDEN

// GOOD: actual values in stubs
given(repo.findByKey(SOME_IDEMPOTENCY_KEY, user.id().toString())).willReturn(Optional.of(existing)); // CORRECT

// GOOD: verify with expected objects
then(idempotencyKeyRepository).should().save(eqIgnoringTimestamps(expectedKey));
then(eventPublisher).should().publish(eqIgnoring(expectedEvent, "requestedAt"));
```

#### When this pattern does NOT apply

- **Exception tests** — keep `assertThatThrownBy` + `then(...).should(never()).publish(any())` as-is
- **Query/read-only tests** — `assertThat` on returned values is appropriate (no side effects to verify)
- **Mapper tests** — pure input/output, use `assertThat` directly
- **Controller integration tests** — use MockMvc assertions

---

## 7. Parameterized Tests

```java
@ParameterizedTest
@ArgumentsSource(TransactionWithMultipleStatesProvider.class)
void shouldComputeCustomerStatusForEachInternalState(Transaction transaction) { ... }

@ParameterizedTest
@MethodSource("provideInvalidRequests")
void shouldRejectInvalidTransactionFilterRequest(TransactionFilterRequest request) { ... }

@ParameterizedTest
@CsvSource({"EUR,100", "GBP,120", "USD,120"})
void shouldRespectPerCurrencyMinimumAmount(String currency, BigDecimal minimum) { ... }

@ParameterizedTest
@EnumSource(FlowVariant.class)
void shouldRenderTimelineForEachFlowVariant(FlowVariant variant) { ... }

@ParameterizedTest
@ValueSource(booleans = {true, false})
void shouldHandleBothApprovalStates(boolean approved) { ... }

@ParameterizedTest
@NullSource
void shouldHandleNullInput(String input) { ... }
```

---

## 8. Integration Test Setup

### Base Class Hierarchy

```
FullContextIntegrationTest (abstract)
  |-- @PgTest, @OpenSearchTest, @TrinoTest, @KafkaTest (Testcontainers)
  |-- @ServiceTest (Spring context)
  |-- ObjectMapper, JwtTokenGenerator
  |
  |-- RestControllerAbstractTest (abstract)
  |     |-- @AutoConfigureMockMvc
  |     |-- MockMvc, MvcResponseReader
  |     |-- assertApiError(), withRole()
  |     |
  |     +-- (Controller integration tests extend this)
  |
  +-- BusinessTest (abstract)
        |-- @SpringBootTest(webEnvironment = DEFINED_PORT)
        |-- @DirtiesContext
        |-- StablepayApiClient
        |
        +-- (Business flow tests extend this)
```

### Containers: Testcontainers

```java
@PgTest          // PostgreSQL 17 — for auth + idempotency JPA tests
@OpenSearchTest  // OpenSearch 2.18 — for search-adapter integration tests
@TrinoTest       // Trino 470 — for analytics-adapter and agent-SQL tests
@KafkaTest       // Apache Kafka 4.0 KRaft — for outbox publish verification
```

JPA repository tests use `@Transactional` for automatic rollback.

### External Systems: WireMock + Stubs

Stub definitions follow a typed pattern under `src/testFixtures/java/.../stubs/`:

```
stubs/
|-- opensearch/
|     |-- OpenSearchTransactionStubs / TransactionSearchStubDefinition
|     |-- OpenSearchFlowStubs / FlowFetchStubDefinition
|     +-- OpenSearchDlqStubs / DlqSearchStubDefinition
|-- trino/
|     |-- TrinoAnalyticsStubs / AggQueryStubDefinition
|     +-- TrinoAgentSqlStubs / ConstrainedSqlStubDefinition
|-- auth/
|     +-- AuthServiceJwksStubs / JwksResponseStubDefinition
+-- kafka/
      +-- KafkaOutboxStubs / OutboxPublishStubDefinition
```

Each extends `StubDefinitionWithRequestData<Request, Response, Error>`.

### Messaging Verification

```java
// Outbox publish verification — assert the outbox emits exactly one event with the expected partition key
@Autowired protected OutboxAssertions outboxAssertions;

outboxAssertions.assertEmitted(DlqReplayCommandEvent.class)
    .withTopic("dlq.replay.command.v1")
    .withPartitionKey(SOME_DLQ_ID.toString())
    .matching(eqIgnoring(expectedEvent, "requestedAt"));
```

### Test Application Config

```yaml
# integration-test: application-test.yml
# OpenSearch / Trino / Kafka URLs point at Testcontainer-provided ports
# Fast scheduler intervals (PT1S, PT0S)
# Dummy JWT signing key + JWK set served by AuthServiceJwksStubs
```

---

## 9. Business Test Setup

Full E2E tests that start the real server and verify complete API flows end-to-end through OpenSearch + Trino + Postgres + Kafka containers.

```java
@SpringBootTest(webEnvironment = DEFINED_PORT)
@PgTest @OpenSearchTest @TrinoTest @KafkaTest
@DirtiesContext
@AutoConfigureMockMvc
class TransactionSearchFlowBusinessTest extends BusinessTest {

    @BeforeEach
    void setUp() {
        // Seed Iceberg fact tables via Trino INSERT (IcebergSeeder)
        // Seed OpenSearch transactions index with fixtures
        // Issue test JWT for the customer scope
    }

    @Test
    void shouldRetrieveTransactionByReferenceForOwningCustomer() {
        // Call GET /api/v1/transactions/{ref} with customer JWT
        // Verify response shape, customer scoping, status mapping
    }

    @Test
    void shouldReturn404WhenTransactionBelongsToDifferentCustomer() {
        // Same call with another customer's JWT
        // Assert 404 (not 403) — avoid enumeration leak
    }
}
```

**Supporting Infrastructure**:
- `TestContext` — builder-based test context holding all state
- `BusinessTestHelper` — static helper methods for common setup
- `BusinessTestMapper` — MapStruct mapper for assertions
- `IcebergSeeder` — seeds `fact_*` and `agg_*` tables via Trino INSERT for deterministic queries

---

## 10. Architecture Test (MANDATORY for new projects)

Every new service must include **five ArchUnit rules**. This must be the **first test** written.

```java
class ArchitectureTest {
    private static final String BASE = "com.stablepay.payments";

    // Rule 1: domain must NOT depend on infrastructure
    noClasses().that().resideInAPackage(BASE + ".domain..")
        .should().dependOnClassesThat().resideInAPackage(BASE + ".infrastructure..");

    // Rule 2: domain must NOT depend on application
    noClasses().that().resideInAPackage(BASE + ".domain..")
        .should().dependOnClassesThat().resideInAPackage(BASE + ".application..");

    // Rule 3: domain must NOT import Spring except stereotype + transaction
    noClasses().that().resideInAPackage(BASE + ".domain..")
        .should().dependOnClassesThat(
            resideInAPackage("org.springframework..")
                .and(resideOutsideOfPackage("org.springframework.stereotype.."))
                .and(resideOutsideOfPackage("org.springframework.transaction.."))
        );

    // Rule 4: domain must NOT import JPA
    noClasses().that().resideInAPackage(BASE + ".domain..")
        .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    // Rule 5: infrastructure must NOT depend on application.controller
    noClasses().that().resideInAPackage(BASE + ".infrastructure..")
        .should().dependOnClassesThat().resideInAPackage(BASE + ".application.controller..");
}
```

**Why allow `@Transactional` in domain?** Domain command handlers own transaction boundaries. This keeps business orchestration in the domain layer and controllers as thin DTO-mapping shells.

---

## 11. `@Nested` Test Classes

Use JUnit 5 `@Nested` to group related tests within a single test class:

```java
class DlqReplayCommandHandlerTest {

    @Nested
    class WhenIdempotencyKeyIsNew {
        @Test
        void shouldEmitReplayCommandEvent() { ... }

        @Test
        void shouldPersistIdempotencyKey() { ... }
    }

    @Nested
    class WhenIdempotencyKeyAlreadyUsed {
        @Test
        void shouldReturnExistingResult() { ... }

        @Test
        void shouldNotEmitEvent() { ... }
    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    class LoggingTests {
        @Test
        void shouldLogReplayAttempt(CapturedOutput output) { ... }
    }
}
```

**Conventions**:
- Use `@Nested` to group tests by scenario or concern (e.g., valid inputs, error cases, logging)
- Inner class names should be descriptive: `WhenIdempotencyKeyIsNew`, `WhenTransactionNotFound`, `LoggingTests`
- Each `@Nested` class can have its own `@BeforeEach` setup
- `@Nested` classes can carry their own extensions (e.g., `OutputCaptureExtension` for log capture only)
- Avoid nesting more than one level deep

---

## 12. Test Data Isolation Strategies

| Strategy | Where Used | Trade-off |
|---|---|---|
| `@Transactional` rollback | Integration tests (JPA) | Fast — each test rolls back; no DB cleanup needed. But tests don't see committed state. |
| `@DirtiesContext` | Business tests | Slow — Spring context rebuilt after each test class. Guarantees full isolation. |
| WireMock reset | Integration + business | `@BeforeEach` resets WireMock stubs to prevent cross-test contamination. |
| Test Channel Binder | Integration tests | In-memory messaging binder; messages don't persist between tests. |

**When to use which**:
- **Unit tests**: No isolation concerns — pure mocks, no shared state
- **Integration tests**: Use `@Transactional` on JPA tests for automatic rollback. Use `@MockBean`/`@SpyBean` for service-level isolation
- **Business tests**: Use `@DirtiesContext` because they start a real server with real DB writes that cannot be rolled back

**Rule**: Prefer `@Transactional` rollback wherever possible for speed. Reserve `@DirtiesContext` for full E2E tests only.

---

## 13. Test Utilities

### `TestUtils` (MUST be in every service's testFixtures)

Custom utility for timestamp-agnostic comparisons. Every service must have this in `src/testFixtures/java/.../fixtures/TestUtils.java`.

**Required `build.gradle.kts` dependencies for testFixtures:**
```kotlin
testFixturesImplementation("org.assertj:assertj-core")
testFixturesImplementation("org.mockito:mockito-core")
```

**Full implementation:**
```java
public final class TestUtils {

    private TestUtils() {}

    public static <T> T eqIgnoringTimestamps(T expected) {
        return eqIgnoring(expected);
    }

    public static <T> T eqIgnoring(T expected, String... fieldsToIgnore) {
        return argThat(it -> isEqualIgnoringTimestampsAnd(it, expected, fieldsToIgnore));
    }

    private static <T> boolean isEqualIgnoringTimestampsAnd(T original, T expected, String... fieldsToIgnore) {
        try {
            assertThat(original)
                    .usingRecursiveComparison()
                    .ignoringFieldsOfTypes(ZonedDateTime.class, LocalDateTime.class, LocalDate.class, Instant.class)
                    .ignoringFields(fieldsToIgnore)
                    .isEqualTo(expected);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
```

**Usage patterns:**

| Method | When to use |
|---|---|
| `eqIgnoringTimestamps(expected)` | Saved/published objects where only timestamps differ (e.g., `createdAt`, `updatedAt`) |
| `eqIgnoring(expected, "eventId", "correlationId")` | Events with random IDs generated inside the handler |
| `eqIgnoring(expected, "dlqId")` | New entities created with `UUID.randomUUID()` inside the handler |

### `AuthenticationGenerator`

```java
public static AuthenticatedUser dummyAuthenticatedUserForGivenRole(String role) { ... }
```

---

## 14. Coverage & Quality Gates

**Recommendation**: Configure Jacoco with:
- Line coverage >= 80%
- Branch coverage >= 70%
- Enforce via CI pipeline
- Exclude generated code (MapStruct mappers, Lombok builders)

---

## Quick Reference: Test Cheat Sheet

| What to Test | Test Type | Annotation | Base Class |
|---|---|---|---|
| Domain logic, handlers, services | Unit | `@ExtendWith(MockitoExtension.class)` | None |
| MapStruct mappers | Unit | None (plain JUnit) | None |
| Controller HTTP behavior | Integration | `@ServiceTest @AutoConfigureMockMvc` | `RestControllerAbstractTest` |
| JPA repository queries | Integration | `@ServiceTest @PgTest @Transactional` | `FullContextIntegrationTest` |
| Event listener processing | Integration | `@ServiceTest @Import(TestChannelBinderConfiguration)` | `FullContextIntegrationTest` |
| Architecture layer rules | Integration | `@AnalyzeClasses` | `DefaultArchitectureTest` |
| Full payout lifecycle | Business | `@SpringBootTest(DEFINED_PORT) @WireMockTest` | `BusinessTest` |

---

## Anti-Patterns Summary

| FORBIDDEN | REQUIRED |
|---|---|
| Multiple `assertThat` on individual fields of a result object | Build expected object + single `usingRecursiveComparison()` (see GOLDEN RULE) |
| Asserting only 2-3 fields out of 10 (silent pass on untested fields) | Recursive comparison catches ALL field changes automatically |
| `assertThat(result.status()).isEqualTo(X)` + `assertThat(result.amount()).isEqualTo(Y)` | `assertThat(result).usingRecursiveComparison().ignoringFields("generatedId").isEqualTo(expected)` |
| `when().thenReturn()` | `given().willReturn()` |
| `verify()` | `then().should()` |
| `any()`, `anyString()`, `eq()` | Actual values or `eqIgnoringTimestamps`/`eqIgnoring` |
| JUnit `assertEquals`/`assertTrue` | AssertJ `assertThat()` |
| `assertThat` on handler return values | `then().should().save(expected)` interaction verification |
| `@SpringBootTest` directly | Extend provided base test classes |
| Fixtures as private methods in test classes | Fixture classes in `src/testFixtures/` |
| `any(Entity.class)` in save verifications | `eqIgnoringTimestamps(expected)` or `eqIgnoring(expected, ...)` |
