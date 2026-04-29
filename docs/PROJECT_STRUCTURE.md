# Project Structure — stablepay-api

> Physical layout of the Spring Boot 4.x service in `apps/api/`. For coding standards see [CODING_STANDARDS.md](CODING_STANDARDS.md), for testing rules see [TESTING_STANDARDS.md](TESTING_STANDARDS.md), for architectural decisions see [ADR.md](ADR.md).

---

## 1. Gradle Multi-Module Layout

The service lives at `apps/api/` in the project monorepo and is itself a 3-module Gradle build:

```
apps/api/
├── stablepay-api/                    # Main Spring Boot application (application + domain + infrastructure)
├── stablepay-api-api/                # Shared API contracts (java-library) — DTOs, error codes, event envelope
├── stablepay-api-client/             # Feign client SDK (java-library) — for service-to-service consumers
├── build.gradle.kts                  # Root build file (Gradle Kotlin DSL)
└── settings.gradle.kts               # Module includes
```

### Module build.gradle Templates

> Examples below use Groovy syntax for readability; the actual build files in this repo use **Kotlin DSL** (`.gradle.kts`). The Kotlin DSL forms are shown for `-api` and `-client` modules below.

**Root `build.gradle.kts`** — shared config:
```kotlin
plugins {
    id("org.springframework.boot") version "4.0.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    java
}

subprojects {
    group = "com.stablepay.payments"
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }
}
```

**`stablepay-api/build.gradle`** (main app, Groovy form for readability):
```groovy
plugins {
    id 'org.springframework.boot'
}

springBoot {
    buildInfo {
        properties {
            version = rootProject.version
            artifact = rootProject.name
        }
    }
}

dependencies {
    implementation project(":stablepay-api-api")

    // Spring Boot starters
    implementation "org.springframework.boot:spring-boot-starter-web"
    implementation "org.springframework.boot:spring-boot-starter-validation"
    implementation "org.springframework.boot:spring-boot-starter-actuator"
    implementation "org.springframework.boot:spring-boot-starter-security"
    implementation "org.springframework.boot:spring-boot-starter-oauth2-resource-server"
    implementation "org.springframework.boot:spring-boot-starter-data-jpa"
    implementation "org.springframework.kafka:spring-kafka"
    implementation "org.opensearch.client:spring-data-opensearch-starter:$springDataOpenSearchVersion"

    // Outbox + observability
    implementation "io.namastack:namastack-outbox-starter-jdbc:$namastackOutboxVersion"
    implementation "io.micrometer:micrometer-tracing-bridge-otel"
    implementation "io.opentelemetry:opentelemetry-exporter-otlp"
    implementation "io.sentry:sentry-spring-boot-starter-jakarta:$sentryVersion"

    // External system clients
    implementation "io.trino:trino-jdbc:$trinoVersion"
    implementation "io.trino:trino-parser:$trinoVersion"  // SQL allowlist validator

    // Persistence
    runtimeOnly "org.postgresql:postgresql"
    implementation "org.flywaydb:flyway-core"
    implementation "org.flywaydb:flyway-database-postgresql"

    // Locking + rate limiting
    implementation "net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion"
    implementation "net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlockVersion"
    implementation "com.bucket4j:bucket4j-redis:$bucket4jVersion"
    implementation "org.springframework.boot:spring-boot-starter-data-redis"

    // Lombok + MapStruct
    compileOnly "org.projectlombok:lombok"
    annotationProcessor "org.projectlombok:lombok"
    implementation "org.mapstruct:mapstruct:$mapstructVersion"
    annotationProcessor "org.mapstruct:mapstruct-processor:$mapstructVersion"

    // Test fixtures
    testFixturesApi testFixtures(project(":stablepay-api-api"))
    testFixturesApi "org.springframework.boot:spring-boot-starter-test"
    testFixturesApi "org.springframework.security:spring-security-test"
    testFixturesApi "org.testcontainers:postgresql:$testcontainersVersion"
    testFixturesApi "org.testcontainers:kafka:$testcontainersVersion"
    testFixturesApi "org.opensearch:opensearch-testcontainers:$opensearchTcVersion"
    testFixturesApi "com.github.tomakehurst:wiremock-jre8-standalone:$wiremockVersion"

    // Business tests use the client module
    businessTestImplementation project(":stablepay-api-client")

    developmentOnly "org.springframework.boot:spring-boot-devtools"
}
```

**`stablepay-api-api/build.gradle.kts`** (shared contracts — DTOs + envelope + error codes consumed by the Next.js client codegen and the Flink-side type registry):
```kotlin
plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-validation")
    api("com.neovisionaries:nv-i18n:$neovisionariesVersion")
    api("com.github.f4b6a3:uuid-creator:$uuidCreatorVersion")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

**`stablepay-api-client/build.gradle.kts`** (Feign-style client SDK for service-to-service callers — the LLM agent and any future internal consumers):
```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":stablepay-api-api"))
    api("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-security")
    testFixturesApi(testFixtures(project(":stablepay-api-api")))
}
```

---

## 2. Source Set Layout

The main module uses custom Gradle source sets for test tiers:

```
stablepay-api/src/
├── main/
│   ├── java/                          # Production code
│   └── resources/
│       ├── application.yml            # Base config
│       ├── application-local.yml      # Local Compose config
│       ├── application-test.yml       # Testcontainers + WireMock + fast intervals
│       └── db/migration/              # Flyway migrations: V{N}__{TICKET}_{desc}.sql
├── test/
│   ├── java/                          # Unit tests (JUnit 5 + Mockito + AssertJ)
│   └── resources/
├── testFixtures/
│   └── java/                          # Shared fixtures, stubs, base classes
├── integration-test/
│   └── java/                          # Integration tests (Testcontainers + Spring + WireMock)
└── business-test/
    ├── java/                          # End-to-end business tests (full server + real containers)
    └── resources/
```

---

## 3. Package Tree

**Root:** `com.stablepay.payments`

```
com.stablepay.payments/
│
├── PaymentsApplication.java
│
├── application/
│   ├── config/
│   │   ├── EventRouting.java                    # Topic name bindings
│   │   ├── SecurityConfig.java                  # Spring Security with JWT decoder
│   │   ├── RateLimitConfig.java                 # Bucket4j + Redis configuration
│   │   ├── ObservabilityConfig.java             # Micrometer + OTEL + Sentry
│   │   └── WebConfig.java                       # @ExtractedRequestMetadata argument resolver
│   ├── security/
│   │   ├── Roles.java                           # ROLE_CUSTOMER, ROLE_ADMIN, ROLE_AGENT
│   │   └── AuthenticatedUser.java               # Principal record
│   └── controller/
│       ├── GlobalExceptionHandler.java
│       ├── ErrorCodes.java                      # STBLPAY-XXXX enum
│       ├── transaction/
│       │   ├── TransactionController.java       # GET /api/v1/transactions/{ref}, list, search
│       │   ├── TransactionInternalController.java  # /api/v1/admin/transactions/search
│       │   └── mapper/                          # Request/Response ↔ Domain
│       ├── flow/
│       │   ├── FlowController.java              # GET /api/v1/flows/{id}
│       │   └── mapper/
│       ├── customer/
│       │   ├── CustomerSummaryController.java   # GET /api/v1/customers/{id}/summary
│       │   └── mapper/
│       ├── admin/
│       │   ├── DlqController.java               # GET /admin/dlq, /admin/dlq/{id}, POST /admin/dlq/{id}/replay
│       │   ├── StuckController.java             # GET /admin/stuck
│       │   ├── AggregatesController.java        # GET /admin/aggregates/{name}
│       │   └── mapper/
│       └── agent/
│           ├── AgentSqlController.java          # POST /api/v1/agent/sql (allowlist-validated)
│           ├── AgentSearchController.java       # POST /api/v1/agent/search
│           ├── AgentTimelineController.java     # GET /api/v1/agent/timeline/{ref}
│           └── mapper/
│
├── domain/
│   ├── EventPublisher.java                      # Generic port interface
│   ├── transaction/
│   │   ├── TransactionQueryHandler.java
│   │   ├── TransactionReadOnlyRepository.java   # Port — implemented by OpenSearch adapter
│   │   ├── mapper/
│   │   └── model/
│   │       ├── core/                            # Transaction, TransactionEvent
│   │       └── query/                           # TransactionFilter, TransactionSearchQuery, SearchResult
│   ├── flow/
│   │   ├── FlowQueryHandler.java
│   │   ├── FlowReadOnlyRepository.java
│   │   ├── mapper/
│   │   └── model/
│   │       ├── core/                            # Flow, FlowTimeline
│   │       └── query/
│   ├── customer/
│   │   ├── CustomerSummaryHandler.java
│   │   ├── CustomerSummaryRepository.java
│   │   └── model/
│   │       └── core/                            # CustomerSummary
│   ├── dlq/
│   │   ├── DlqQueryHandler.java
│   │   ├── DlqRepository.java
│   │   ├── DlqReplayCommandHandler.java
│   │   └── model/
│   │       ├── core/                            # DlqMessage, ReplayResult
│   │       └── events/                          # ReplayDlqMessageCommand, DlqReplayCommandEvent
│   ├── agent/
│   │   ├── AgentSqlHandler.java
│   │   ├── AgentSqlRepository.java              # Port — implemented by Trino adapter with SQL parser allowlist
│   │   ├── AgentSearchHandler.java
│   │   ├── AgentTimelineHandler.java
│   │   ├── ConstrainedSql.java                  # Validated SQL value object
│   │   └── model/
│   ├── idempotency/
│   │   ├── IdempotencyKey.java                  # record
│   │   └── IdempotencyKeyRepository.java
│   ├── common/
│   │   ├── Money.java                           # BigDecimal + CurrencyCode
│   │   ├── ids/
│   │   │   ├── TransactionId.java
│   │   │   ├── FlowId.java
│   │   │   ├── CustomerId.java
│   │   │   └── DlqId.java
│   │   └── pagination/                          # Pageable, SearchResult
│   ├── exceptions/                              # TransactionNotFound, FlowNotFound, IdempotencyKeyConflict, AgentSqlNotAllowed, CustomerScopeMismatch
│   └── validator/                               # Eight-rule final validator for agent responses
│
└── infrastructure/
    ├── opensearch/
    │   ├── transaction/
    │   │   ├── OpenSearchTransactionRepositoryAdapter.java
    │   │   ├── TransactionDocument.java          # @Document(indexName = "transactions")
    │   │   └── mapper/
    │   ├── flow/
    │   │   ├── OpenSearchFlowRepositoryAdapter.java
    │   │   └── FlowDocument.java
    │   └── dlq/
    │       ├── OpenSearchDlqRepositoryAdapter.java
    │       └── DlqEventDocument.java
    ├── trino/
    │   ├── TrinoAnalyticsRepositoryAdapter.java
    │   ├── TrinoCustomerSummaryRepositoryAdapter.java
    │   ├── AgentSqlExecutorAdapter.java          # Uses io.trino:trino-parser to validate SQL allowlist
    │   └── mapper/
    ├── kafka/
    │   ├── DlqReplayOutboxPublisher.java         # Implements EventPublisher<DlqReplayCommandEvent>
    │   └── StablepayOutboxHandler.java           # Routes outbox events to Kafka
    ├── db/
    │   ├── auth/
    │   │   ├── AuthUserEntity.java
    │   │   ├── AuthUserJpaRepository.java
    │   │   └── AuthUserRepositoryAdapter.java
    │   └── idempotency/
    │       ├── IdempotencyKeyEntity.java
    │       ├── IdempotencyKeyJpaRepository.java
    │       └── IdempotencyKeyRepositoryAdapter.java
    └── common/
        ├── TraceContextHelper.java               # Extracts/propagates W3C TraceContext
        └── PiiMaskingFilter.java                 # Logback masking pattern enforcement
```

---

## 4. API Module Package Tree (`stablepay-api-api`)

The shared-contract module exposes DTOs, view models, error responses, and the event envelope to clients (Next.js codegen, LLM agent service-to-service caller).

```
com.stablepay.payments.api.model/
├── transaction/
│   ├── TransactionView.java
│   ├── TransactionFilterRequest.java
│   ├── TransactionSearchRequest.java
│   └── TransactionTimelineView.java
├── flow/
│   ├── FlowView.java
│   └── FlowTimelineView.java
├── customer/
│   └── CustomerSummaryView.java
├── dlq/
│   ├── DlqMessageView.java
│   ├── ReplayDlqMessageRequest.java
│   ├── ReplayResultView.java
│   └── DlqReplayCommandEvent.java               # Carries TOPIC = "dlq.replay.command.v1"
├── agent/
│   ├── AgentSqlRequest.java
│   ├── AgentSearchRequest.java
│   └── AgentTimelineView.java
├── error/
│   ├── ApiError.java                            # @Builder @Jacksonized record
│   └── ErrorCodes.java
├── envelope/
│   └── EventEnvelope.java                       # Shared envelope: event_id, event_time, ingest_time, schema_version, flow_id, correlation_id, trace_id
├── common/
│   ├── Money.java                               # BigDecimal + CurrencyCode
│   ├── CustomerStatus.java                      # PROCESSING / NEEDS_APPROVAL / COMPLETED / FAILED / CANCELLED / REVERSED
│   └── FlowVariant.java                         # ON_RAMP / OFF_RAMP / CRYPTO_TO_CRYPTO
└── validator/
    ├── ErrorMessages.java
    └── ValidIdempotencyKey.java
```

**Test fixtures source set** (`src/testFixtures/java`):

```
com.stablepay.payments.api.model/
├── TransactionViewFixtures.java
├── FlowViewFixtures.java
├── DlqMessageFixtures.java
├── ReplayResultViewFixtures.java
├── AgentSqlRequestFixtures.java
└── CommonFixtures.java                          # SOME_MONEY, SOME_CUSTOMER_ID, SOME_FLOW_ID
```

---

## 5. Test Fixtures Package Tree

Located in `stablepay-api/src/testFixtures/java`:

```
com.stablepay.payments.test/
├── FullContextIntegrationTest.java        # Integration test base class (Testcontainers + Spring)
├── RestControllerAbstractTest.java        # Controller test base class (MockMvc + JWT)
├── TestUtils.java                         # eqIgnoringTimestamps, eqIgnoring
├── commons/
│   ├── JwtTokenGenerator.java             # Issues test JWTs with role + customer_id claims
│   └── TraceContextGenerator.java         # Generates W3C trace IDs for trace-propagation tests
├── fixtures/
│   ├── TransactionFixtures.java           # SOME_TRANSACTION + builders for each lifecycle state
│   ├── FlowFixtures.java                  # SOME_FLOW + builders for on_ramp / off_ramp / crypto_to_crypto
│   ├── DlqFixtures.java                   # SOME_DLQ_MESSAGE + per-error-class fixtures
│   ├── CustomerFixtures.java              # SOME_CUSTOMER + 5 seeded test customers
│   ├── IdempotencyKeyFixtures.java
│   └── CommonFixtures.java                # SOME_MONEY, SOME_INSTANT, SOME_TRACE_ID
├── stubs/
│   ├── opensearch/
│   │   ├── OpenSearchTransactionStubs.java       # WireMock OS responses for transaction queries
│   │   ├── OpenSearchFlowStubs.java
│   │   └── OpenSearchDlqStubs.java
│   ├── trino/
│   │   ├── TrinoAnalyticsStubs.java              # JdbcTemplate result-set stubs
│   │   └── TrinoAgentSqlStubs.java
│   ├── auth/
│   │   └── AuthServiceJwksStubs.java             # JWK set responses for JWT decoder tests
│   └── kafka/
│       └── KafkaOutboxStubs.java                 # Asserts outbox messages emitted with correct partition key
└── request/
    ├── InvalidTransactionFilterRequestProvider.java
    ├── InvalidReplayDlqMessageRequestProvider.java
    └── InvalidAgentSqlRequestProvider.java
```

---

## 6. Business Test Package Tree

Located in `stablepay-api/src/business-test/java`:

```
com.stablepay.payments/
├── BusinessTest.java                                # Base: @SpringBootTest(DEFINED_PORT) + Testcontainers
├── transaction/
│   └── TransactionSearchFlowBusinessTest.java       # Login → search → drill-down → assert response shape + role scoping
├── flow/
│   └── FlowDrilldownBusinessTest.java               # Multi-leg flow timeline assembly
├── dlq/
│   └── DlqReplayBusinessTest.java                   # POST /admin/dlq/{id}/replay → outbox → Kafka → idempotency on retry
├── agent/
│   └── AgentSqlAllowlistBusinessTest.java           # Disallowed SQL rejected; allowed SQL forwarded; cross-customer leak attempts refused
├── customer/
│   └── CustomerScopeEnforcementBusinessTest.java    # Customer A cannot see B's data; admin can
└── common/
    ├── TestContext.java                             # Builder-based test state holder
    ├── BusinessTestHelper.java
    ├── BusinessTestMapper.java                     # MapStruct mapper for response-vs-expected comparison
    └── IcebergSeeder.java                           # Seeds fact_* and agg_* tables via Trino INSERT for deterministic queries
```

---

## 7. File Placement Decision Tree

```
Where does this new file go?

REST endpoint?
  -> application/controller/{transaction|flow|customer|admin|agent}/

Spring config bean?
  -> application/config/

Security/role/principal class?
  -> application/security/

Domain query handler / command handler?
  -> domain/{transaction|flow|customer|dlq|agent|idempotency}/

Domain port interface (repository, executor)?
  -> domain/{transaction|flow|customer|dlq|agent|idempotency}/

Domain model / value object?
  -> domain/{aggregate}/model/core/

Domain command or event?
  -> domain/{aggregate}/model/events/

Query parameter object?
  -> domain/{aggregate}/model/query/

Type-safe ID record (TransactionId, FlowId, etc.)?
  -> domain/common/ids/

Money / shared value object?
  -> domain/common/

Domain exception?
  -> domain/exceptions/

OpenSearch document + adapter?
  -> infrastructure/opensearch/{aggregate}/

OpenSearch ↔ domain mapper?
  -> infrastructure/opensearch/{aggregate}/mapper/

Trino repository adapter?
  -> infrastructure/trino/

JPA entity or Spring Data repo (auth, idempotency only)?
  -> infrastructure/db/{auth|idempotency}/

JPA entity ↔ domain mapper?
  -> infrastructure/db/{aggregate}/mapper/

Outbox event publisher?
  -> infrastructure/kafka/

Request/response DTO shared with consumers (Next.js codegen, internal)?
  -> stablepay-api-api module

Custom validator (jakarta.validation.ConstraintValidator)?
  -> stablepay-api-api/src/main/java/.../validator/

Test fixture constants/builders?
  -> src/testFixtures/java/.../fixtures/

Stub responses for an external system?
  -> src/testFixtures/java/.../stubs/{opensearch|trino|auth|kafka}/

Unit test?
  -> src/test/java/ (mirrors production package)

Integration test (Spring + Testcontainers)?
  -> src/integration-test/java/

Business flow test (full server + real containers)?
  -> src/business-test/java/

Flyway migration?
  -> src/main/resources/db/migration/V{N}__{TICKET}_{desc}.sql
```
