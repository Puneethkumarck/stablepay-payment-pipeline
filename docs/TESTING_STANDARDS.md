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
var result = mapper.toDomain(apiEvent);
var expected = PayoutStatusEvent.builder()
        .payoutReference(UUID.fromString(apiEvent.payoutReference()))
        .status(PARTNER_ACCEPTED).build();
assertThat(result).usingRecursiveComparison().isEqualTo(expected);
```

**Service/handler test (interaction verification):**
```java
var expectedMerchant = merchant.toBuilder().build();
expectedMerchant.activate(approver, scopes);
// ... when ...
then(merchantRepository).should().save(eqIgnoringTimestamps(expectedMerchant));
```

**Integration test (controller response):**
```java
var response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), PayoutView.class);
var expected = PayoutView.builder().reference(ref).status("CREATED").amount(SOME_MONEY).build();
assertThat(response).usingRecursiveComparison()
        .ignoringFields("createdAt", "updatedAt").isEqualTo(expected);
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
void shouldHandleApprovedMerchantApprovalEvent()
void shouldThrowIfPayoutNotFound()
void shouldReturnAllBindings()
void shouldMapTransferSuccessEventToDomain()
void shouldDiscardWhenInvalidReturnFalse()
```

### Secondary Convention: `given_When_Then` (older controller tests only)

```java
void givenAValidRequestWithAuthenticatedUser_WhenAttemptToRetrieveThePdf_ThenSuccess()
```

**Rule**: Standardize on `should*` camelCase for all new code. The `given_When_Then` variant is legacy.

---

## 3. Test Structure: Given / When / Then

**Every test** follows the Given/When/Then pattern with explicit comment markers:

```java
@Test
void shouldHandleApprovedMerchantApprovalEvent() {
    // given
    var payout = MERCHANT_4_EYES_APPROVAL_REQUESTED_PAYOUT;
    var event = SOME_APPROVED_MERCHANT_APPROVAL_EVENT.toBuilder()
        .payoutReference(payout.transactionReference())
        .build();
    given(payoutRepository.findByTransactionReference(event.payoutReference()))
        .willReturn(Optional.of(payout));

    // when
    handler.handle(event);

    // then
    then(payoutRepository).should().save(eqIgnoringTimestamps(expected.payout()));
    then(payoutStatusEventPublisher).should().publish(eqIgnoringTimestamps(expected.event()));
}
```

**For exception tests**, `// when` and `// then` are combined:

```java
@Test
void shouldThrowIfPayoutNotFound() {
    // given
    var event = SOME_APPROVED_MERCHANT_APPROVAL_EVENT;
    given(payoutRepository.findByTransactionReference(event.payoutReference()))
        .willReturn(Optional.empty());

    // when/then
    assertThatThrownBy(() -> handler.handle(event))
        .isInstanceOf(PayoutNotFoundException.class)
        .hasMessageContaining("Could not find payout with transactionReference");
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
class MerchantApprovalEventHandlerTest {
    @Mock private PayoutRepository payoutRepository;
    @Mock private EventPublisher<StateChangedEvent> payoutStatusEventPublisher;
    @Mock private TransferService transferService;
    @InjectMocks private MerchantApprovalEventHandler handler;
}
```

### No Generic Argument Matchers (MANDATORY)

**Never use `any()`, `anyString()`, `anyLong()`, `eq()`, or similar generic Mockito matchers.** Always pass actual values in both stubs (`given()`) and verifications (`then().should()`).

```java
// BAD: generic matchers give zero confidence in what was actually called
given(repo.findById(any())).willReturn(Optional.of(merchant));                 // FORBIDDEN
given(repo.findByTransactionReference(any())).willReturn(Optional.empty());    // FORBIDDEN
given(repo.save(any(Merchant.class))).willAnswer(inv -> inv.getArgument(0));   // FORBIDDEN
then(repo).should().save(eq(expectedMerchant));                                // FORBIDDEN

// GOOD: actual values - test fails if arguments don't match
given(repo.findById(merchantId)).willReturn(Optional.of(merchant));            // CORRECT
given(repo.findByTransactionReference(txRef)).willReturn(Optional.empty());    // CORRECT
then(repo).should().save(eqIgnoringTimestamps(expectedMerchant));              // CORRECT
```

**Allowed custom matchers** (these verify actual content, not bypass matching):
- `eqIgnoringTimestamps(expected)` — recursive comparison ignoring timestamp types
- `eqIgnoring(expected, "field1", "field2")` — recursive comparison ignoring specific fields

**Stubbing `save()` with return value**: when the handler needs the saved object back:
```java
given(repo.save(eqIgnoringTimestamps(expectedMerchant)))
        .willAnswer(inv -> inv.getArgument(0));
```

### `@Spy` for Real Mappers/Validators

```java
@Spy private final BatchPayoutRequestMapper mapper = getMapper(BatchPayoutRequestMapper.class);
@Spy private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
```

MapStruct mappers and Jakarta validators are spied rather than mocked, so their real logic executes.

### Integration Test Mocking

```java
@ServiceTest
@AutoConfigureMockMvc
class ProofOfPaymentControllerTest extends RestControllerAbstractTest {
    @MockBean private ProofOfPaymentCommandHandler handler;
    @SpyBean private SomeRealBean spiedBean;
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
src/testFixtures/java/com/stablepay/payments/payout/test/
    FullContextIntegrationTest.java       // Integration test base
    RestControllerAbstractTest.java        // Controller test base
    TestUtils.java                         // Comparison utilities (MANDATORY)
    commons/
        AuthenticationGenerator.java       // Auth test helpers
    fixtures/
        PayoutFixtures.java                // Payout at every lifecycle state (30+ constants)
        CommonFixtures.java                // Shared: SOME_MONEY, SOME_SENDER, etc.
        FeeFixtures.java                   // Fee domain objects
        BatchPayoutFixtures.java           // Batch payout data
        BeneficiaryFixtures.java           // Beneficiary test data
        BridgeFixtures.java                // Bridge service responses
        TransferEventFixtures.java         // Transfer events
        MerchantApprovalFixtures.java      // Approval events
        PayoutStatusFixtures.java          // Status-related fixtures
        CustomerDetailsFixtures.java       // Customer details
        WalletMappingFixtures.java         // Wallet mappings
        ApiErrorFixtures.java              // Error responses
        ProofOfPaymentFixtures.java        // POP objects
        ProcessingProfileFixtures.java     // Processing profiles
    stubs/
        fee/FeeServiceStubs.java
        auth/AuthServiceStubs.java
        banking/BankingApiStubs.java
        bridge/BridgeServiceStubs.java
        beneficiary/BeneficiaryManagementStubs.java
        wallet/WalletServiceStubs.java
        capability/CapabilitiesManagementServiceStubs.java
        transfer/TransferServiceStubs.java
        mfa/MfaStub.java
    request/
        InvalidCreatePayoutRequestWithWalletId.java
        InvalidUnifiedPayoutRequest.java
```

### Fixture Design Patterns

**Pattern 1: Immutable static constants with descriptive names**

```java
@NoArgsConstructor(access = PRIVATE)
public final class PayoutFixtures {
    public static final Payout NEW_PAYOUT = createPayoutWithStatus(CREATED);
    public static final Payout FINCRIME_TRIGGERED_PAYOUT = createPayoutWithStatus(FINCRIME_TRIGGERED);
    public static final Payout PARTNER_COMPLETED_PAYOUT = createPayoutWithStatus(PARTNER_COMPLETED);
    // ... 30+ payout constants covering every state
}
```

**Pattern 2: Builder factory methods**

```java
public static PayoutRequestBuilder payoutRequestBuilder() {
    return PayoutRequest.builder()
        .merchantId("some-merchant-id")
        .accountReference("some-account-reference")
        .beneficiaryDetails(payoutBeneficiaryBuilder().build())
        .amount(SOME_MONEY);
}
```

**Pattern 3: State-machine-based fixture generation**

`PayoutFixtures.createPayoutWithStatus(Status)` walks the payout through its state machine from `CREATED` to the desired state, producing realistic test objects with proper status history.

**Pattern 4: `SOME_*` prefix convention**

```java
public static final Money SOME_MONEY = Money.builder().amount(...).currency(...).build();
public static final Sender SOME_SENDER = Sender.builder()...build();
public static final MerchantApprovalEvent SOME_APPROVED_MERCHANT_APPROVAL_EVENT = ...;
```

**Pattern 5: Static import for clean test code**

```java
import static com.stablepay.payments.payout.test.fixtures.PayoutFixtures.MERCHANT_4_EYES_APPROVAL_REQUESTED_PAYOUT;
import static com.stablepay.payments.payout.test.fixtures.CommonFixtures.SOME_MONEY;
```

**Pattern 6: All fixture factories MUST live in testFixtures (MANDATORY)**

Test helper methods that create domain objects must be extracted into dedicated `*Fixtures` classes in `src/testFixtures/java/.../fixtures/`. They must NOT remain as private methods in test classes.

Rules:
- One fixture class per aggregate/entity: `ComplianceCheckFixtures`, `FxQuoteFixtures`, etc.
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
public static class PayoutWithMultipleStatesProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext ctx) {
        return Stream.of(
            of(NEW_PAYOUT),
            of(FINCRIME_TRIGGERED_PAYOUT),
            // ... all payout states
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
assertThat(payoutElements)
    .usingRecursiveComparison()
    .isEqualTo(expectedPayoutElement);

assertThat(apiError)
    .usingRecursiveComparison()
    .ignoringFields("details.documentLink")
    .ignoringCollectionOrder()
    .isEqualTo(expected);
```

### Exception Assertions

```java
assertThatThrownBy(() -> handler.handle(event))
    .isInstanceOf(PayoutNotFoundException.class)
    .hasMessageContaining("Could not find payout with transactionReference");

assertThatThrownBy(() -> builder.withTransition(STATE1, STATE2, null))
    .isExactlyInstanceOf(IllegalStateException.class)
    .hasMessage("Cannot define the same transition twice for: STATE1 -> STATE2");
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

**State transition (aggregate):**
```java
@Test
void shouldTransitionToLockedStatus() {
    // given
    var quote = FxQuoteFixtures.activeQuote();

    // when
    var result = quote.lock();

    // then
    var expected = quote.toBuilder()
            .status(FxQuoteStatus.LOCKED)
            .build();

    assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("lockedAt")
            .isEqualTo(expected);
}
```

**Factory method (new entity with generated ID):**
```java
@Test
void shouldCreateComplianceCheck() {
    // given
    var paymentId = UUID.randomUUID();
    var corridor = new Corridor("US", "DE", "USD", "EUR");
    var amount = Money.of(new BigDecimal("1000.00"), "USD");

    // when
    var result = ComplianceCheck.initiate(paymentId, corridor, amount);

    // then
    var expected = ComplianceCheck.builder()
            .paymentId(paymentId)
            .corridor(corridor)
            .amount(amount)
            .status(ComplianceCheckStatus.PENDING)
            .build();

    assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("checkId", "createdAt")
            .isEqualTo(expected);
}
```

**Value object creation:**
```java
@Test
void shouldCreateMoney() {
    // when
    var result = Money.of(new BigDecimal("100.00"), "USD");

    // then
    var expected = new Money(new BigDecimal("100.00"), "USD");

    assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected);
}
```

**Mapper (input to output):**
```java
@Test
void shouldMapApiEventToDomain() {
    // given
    var apiEvent = SOME_PAYOUT_STATUS_API_ACCEPTED_EVENT;

    // when
    var result = mapper.toDomain(apiEvent);

    // then
    var expected = PayoutStatusEvent.builder()
            .payoutReference(UUID.fromString(apiEvent.payoutReference()))
            .status(PARTNER_ACCEPTED)
            .eventDateTime(apiEvent.eventDateTime())
            .build();

    assertThat(result)
            .usingRecursiveComparison()
            .isEqualTo(expected);
}
```

**Domain service returning a result:**
```java
@Test
void shouldLockRateAndReservePool() {
    // given
    var quote = FxQuoteFixtures.activeQuote();
    var pool = LiquidityPoolFixtures.poolWithBalance("10000.00");
    given(quoteRepository.findById(quote.quoteId())).willReturn(Optional.of(quote));
    given(poolRepository.findByCorridor(quote.corridor())).willReturn(Optional.of(pool));

    // when
    var result = lockService.lockRate(quote.quoteId(), paymentId);

    // then
    var expectedLock = FxRateLock.fromQuote(quote, paymentId);
    assertThat(result.lock())
            .usingRecursiveComparison()
            .ignoringFields("lockId", "createdAt")
            .isEqualTo(expectedLock);
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

#### Example: handler that mutates an existing entity

```java
@Test
void shouldActivateMerchant() {
    // given
    var merchant = MerchantFixtures.pendingApprovalMerchant();
    var merchantId = merchant.getMerchantId();
    var approver = MerchantFixtures.anApprover();
    var scopes = List.of("payments:read", "payments:write");
    given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));

    // Build expected: clone fixture, apply same domain operation
    var expectedMerchant = merchant.toBuilder().build();
    expectedMerchant.activate(approver, scopes);

    var expectedEvent = MerchantActivatedEvent.builder()
            .eventType(MerchantActivatedEvent.EVENT_TYPE)
            .merchantId(merchantId)
            .legalName(merchant.getLegalName())
            .build();

    given(merchantRepository.save(eqIgnoringTimestamps(expectedMerchant)))
            .willAnswer(inv -> inv.getArgument(0));

    // when
    handler.activate(merchantId, approver, scopes);

    // then - verify interactions with expected objects
    then(activationPolicy).should().validate(merchant);
    then(merchantRepository).should().save(eqIgnoringTimestamps(expectedMerchant));
    then(eventPublisher).should().publish(
            eqIgnoring(expectedEvent, "eventId", "correlationId"));
}
```

#### Example: handler that creates a new entity (random ID)

```java
@Test
void shouldApplyMerchant() {
    // given
    var command = new ApplyMerchantCommand("Acme Ltd", ...);

    // Build expected from same factory method - merchantId will differ
    var expectedMerchant = Merchant.createNew("Acme Ltd", ...);

    given(merchantRepository.save(eqIgnoring(expectedMerchant, "merchantId")))
            .willAnswer(inv -> inv.getArgument(0));

    // when
    handler.apply(command);

    // then - ignore merchantId since it's randomly generated inside the handler
    then(merchantRepository).should().save(
            eqIgnoring(expectedMerchant, "merchantId"));
}
```

#### Anti-patterns

```java
// BAD: mixing assertThat on return value with mock verification
var result = handler.activate(merchantId, approver, scopes);
assertThat(result).isNotNull();                    // FORBIDDEN - redundant
assertThat(result.isActive()).isTrue();             // FORBIDDEN - covered by save verification
then(eventPublisher).should().publish(any(...));    // FORBIDDEN - doesn't verify event content

// BAD: generic matchers
then(merchantRepository).should().save(any(Merchant.class));               // FORBIDDEN
then(eventPublisher).should().publish(any(MerchantSuspendedEvent.class));  // FORBIDDEN

// BAD: generic matchers in stubs
given(repo.findById(any())).willReturn(Optional.of(merchant));             // FORBIDDEN
given(repo.save(any(Merchant.class))).willAnswer(inv -> inv.getArgument(0)); // FORBIDDEN

// GOOD: actual values in stubs
given(repo.findById(merchantId)).willReturn(Optional.of(merchant));        // CORRECT

// GOOD: verify with expected objects
then(merchantRepository).should().save(eqIgnoringTimestamps(expectedMerchant));
then(eventPublisher).should().publish(
        eqIgnoring(expectedEvent, "eventId", "correlationId"));
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
@ArgumentsSource(PayoutWithMultipleStatesProvider.class)
void shouldRejectPayoutInInvalidState(Payout payout) { ... }

@ParameterizedTest
@MethodSource("provideInvalidRequests")
void shouldRejectInvalidRequest(PayoutRequest request) { ... }

@ParameterizedTest
@CsvSource({"EUR,100", "GBP,120", "USD,120"})
void shouldAutoApproveUnderThreshold(String currency, BigDecimal threshold) { ... }

@ParameterizedTest
@EnumSource(TransferType.class)
void shouldMapAllTransferTypes(TransferType type) { ... }

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
  |-- @MySQLTest (TestContainers)
  |-- @ServiceTest (Spring context)
  |-- @Import(TestChannelBinderConfiguration.class)
  |-- ObjectMapper, InputDestination, TestStreamSupport
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
        |-- FiatPayoutProcessorClient
        |
        +-- (Business flow tests extend this)
```

### Database: MySQL TestContainers

```java
@MySQLTest  // from foundation library - handles TestContainers lifecycle
```

JPA repository tests use `@Transactional` for automatic rollback.

### External HTTP Services: WireMock

Stub definitions follow a typed pattern:

```
stubs/
|-- FeeServiceStubs / RetrieveFeesStubDefinition
|-- BridgeServiceStubs / BridgeServiceGetAllWalletDetailsStubDefinition
|-- AuthServiceStubs / AuthServiceStubDefinition
|-- BeneficiaryManagementStubs / GetBeneficiaryStubsDefinition
|-- BankingApiStubs / GetWalletMappingStubDefinition
|-- WalletServiceStubs / WalletServiceStubDefinition
|-- CapabilitiesManagementServiceStubs / EvaluateCapabilityStubDefinition
|-- TransferServiceStubs / TransferServiceGetWalletSnapshotSubDefinition
+-- MfaStub / MfaStubDefinition
```

Each extends `StubDefinitionWithRequestData<Request, Response, Error>` from foundation library.

### Messaging: Spring Cloud Stream Test Binder

```java
@Import(TestChannelBinderConfiguration.class)
// ...
@Autowired protected InputDestination input;
@Autowired protected TestStreamSupport testStreamSupport;
```

### Test Application Config

```yaml
# integration-test: application-test.yml
# All external services -> localhost:4444 (WireMock)
# Fast scheduler intervals (PT1S, PT0S)
# RDS IAM disabled, dummy HAWK credentials
```

---

## 9. Business Test Setup

Full E2E tests that start the real server and verify complete payout lifecycle flows.

```java
@SpringBootTest(webEnvironment = DEFINED_PORT)
@WireMockTest(httpPort = 4444)
@DirtiesContext
@AutoConfigureMockMvc
class PayoutFlowBusinessTest extends BusinessTest {

    @BeforeEach
    void setUp() {
        // Set up all WireMock stubs for external services
    }

    @Test
    void shouldCompleteFullPayoutFlow() {
        // Create payout via API
        // Verify state transitions through the entire lifecycle
        // Assert final state
    }
}
```

**Supporting Infrastructure**:
- `TestContext` — builder-based test context holding all state
- `BusinessTestHelper` — static helper methods for common setup
- `BusinessTestMapper` — MapStruct mapper for assertions
- `BatchPayoutBusinessTestHelper` — batch-specific helpers

---

## 10. Architecture Test (MANDATORY for new projects)

Every new service must include **five ArchUnit rules**. This must be the **first test** written.

```java
class ArchitectureTest {
    private static final String BASE = "com.stablecoin.payments.<service>";

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
class PayoutStatusEventListenerTest {

    @Nested
    class WhenEventIsValid {
        @Test
        void shouldProcessEvent() { ... }

        @Test
        void shouldPublishStateChange() { ... }
    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    class LoggingTests {
        @Test
        void shouldLogWarning(CapturedOutput output) { ... }
    }
}
```

**Conventions**:
- Use `@Nested` to group tests by scenario or concern (e.g., valid inputs, error cases, logging)
- Inner class names should be descriptive: `WhenEventIsValid`, `WhenPayoutNotFound`, `LoggingTests`
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
| `eqIgnoring(expected, "merchantId")` | New entities created with `UUID.randomUUID()` inside the handler |

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
| JPA repository queries | Integration | `@ServiceTest @MySQLTest @Transactional` | `FullContextIntegrationTest` |
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
