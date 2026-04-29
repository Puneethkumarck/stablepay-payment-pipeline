# Project Structure

> Physical layout of a fintech microservice. For coding standards see [CODING_STANDARDS.md](CODING_STANDARDS.md), for testing rules see [TESTING_STANDARDS.md](TESTING_STANDARDS.md), for architectural decisions see [ADR.md](ADR.md).

---

## 1. Gradle Multi-Module Layout

```
{service-name}/
├── {service-name}/                    # Main Spring Boot application
├── {service-name}-api/                # Shared API contracts (java-library)
├── {service-name}-client/             # Feign client SDK (java-library)
├── build.gradle                       # Root build file
└── settings.gradle                    # Module includes
```

### Module build.gradle Templates

**Root `build.gradle`** - shared config:
```groovy
plugins {
    id 'org.springframework.boot' apply false
    id 'java'
}

subprojects {
    group = 'com.{org}.{domain}'
    sourceCompatibility = JavaVersion.VERSION_17
}
```

**`{service-name}/build.gradle`** (main app):
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
    implementation project(":{service-name}-api")

    // Foundation modules
    implementation "com.{org}.microservice.foundation:web-app:$foundationVersion"
    implementation "com.{org}.microservice.foundation:security:$foundationVersion"
    implementation "com.{org}.microservice.foundation:messaging-aws-sqs:$foundationVersion"
    implementation "com.{org}.microservice.foundation:messaging-aws-sns:$foundationVersion"
    implementation "com.{org}.microservice.foundation:messaging-kafka:$foundationVersion"
    implementation "com.{org}.microservice.foundation:outbox-starter-jpa:$foundationVersion"
    implementation "com.{org}.microservice.foundation:outbox-starter-messaging:$foundationVersion"
    implementation "com.{org}.microservice.foundation:storage-mysql-aws:$foundationVersion"
    implementation "com.{org}.microservice.foundation:rest-client:$foundationVersion"

    // External service clients
    implementation "com.{org}.{other-service}:{other-service}-client:$version"

    // Test fixtures from api and external services
    testFixturesApi testFixtures(project(":{service-name}-api"))
    testFixturesApi testFixtures("com.{org}.{other-service}:{other-service}-api:$version")
    testFixturesApi "com.{org}.microservice.foundation:messaging-aws-tests:$foundationVersion"
    testFixturesApi "com.{org}.microservice.foundation:storage-mysql-tests:$foundationVersion"
    testFixturesApi "com.{org}.microservice.foundation:security-tests:$foundationVersion"

    // Business tests use the client module
    businessTestImplementation project(":{service-name}-client")

    developmentOnly "org.springframework.boot:spring-boot-devtools"
}
```

**`{service-name}-api/build.gradle`** (shared contracts):
```groovy
plugins {
    id 'java-library'
}

dependencies {
    api "com.{org}.microservice.foundation:outbox-api:$foundationVersion"
    api "com.{org}.microservice.foundation:commons:$foundationVersion"
    implementation "org.springframework.boot:spring-boot-starter-validation"
    api "com.neovisionaries:nv-i18n:$neovisionariesVersion"
    api "com.github.f4b6a3:uuid-creator:$uuidCreatorVersion"

    // Cross-service API dependencies
    api "com.{org}.{other-service}:{other-service}-api:$version"

    testImplementation "org.springframework.boot:spring-boot-starter-test"
}
```

**`{service-name}-client/build.gradle`** (Feign client):
```groovy
plugins {
    id 'java-library'
}

dependencies {
    api project(":{service-name}-api")
    api "com.{org}.microservice.foundation:rest-client:$foundationVersion"
    implementation "com.{org}.microservice.foundation:security:$foundationVersion"

    testFixturesApi testFixtures(project(":{service-name}-api"))
}
```

---

## 2. Source Set Layout

The main module uses custom Gradle source sets for test tiers:

```
{service-name}/src/
├── main/
│   ├── java/                          # Production code
│   └── resources/
│       ├── application.yml            # Base config
│       ├── application-test.yml       # Test profile (WireMock URLs, fast intervals)
│       ├── application-sandbox.yml    # Sandbox profile
│       ├── db/migration/              # Flyway migrations: V{N}__{TICKET}_{desc}.sql
│       └── templates/                 # Thymeleaf templates (if needed)
├── test/
│   ├── java/                          # Unit tests
│   └── resources/
├── testFixtures/
│   └── java/                          # Shared fixtures, stubs, base classes
├── integration-test/
│   └── java/                          # Integration tests
└── business-test/
    ├── java/                          # End-to-end business tests
    └── resources/
        └── batch/                     # Test data files (CSV, etc.)
```

---

## 3. Package Tree

**Root:** `com.{org}.banking.{domain}`

```
com.{org}.banking.{domain}/
│
├── {DomainName}Application.java
│
├── application/
│   ├── config/
│   │   ├── EventRouting.java
│   │   └── WebConfig.java
│   ├── security/
│   │   └── Roles.java
│   ├── controller/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ErrorCodes.java
│   │   ├── {resource}/
│   │   │   ├── {Resource}Controller.java
│   │   │   ├── Internal{Resource}Controller.java
│   │   │   ├── handler/
│   │   │   └── mapper/
│   │   └── {other-resource}/
│   ├── stream/
│   │   ├── {Event}Listener.java
│   │   └── mapper/
│   └── job/
│       └── {Scheduled}Job.java
│
├── domain/
│   ├── EventHandler.java
│   ├── EventPublisher.java
│   ├── {aggregate}/
│   │   ├── {Aggregate}CommandHandler.java
│   │   ├── {Aggregate}CreationService.java
│   │   ├── {Aggregate}Repository.java          # Port interface
│   │   ├── {Aggregate}Validator.java
│   │   ├── {Aggregate}StateUpdater.java
│   │   ├── {Aggregate}QueryHandler.java
│   │   ├── {Aggregate}TransactionalProxy.java
│   │   ├── mapper/
│   │   └── model/
│   │       ├── core/                            # Aggregate root + value objects
│   │       ├── events/                          # Commands + domain events
│   │       └── query/                           # Query parameter objects
│   ├── common/
│   │   ├── {Capability}Provider.java            # Shared port interfaces
│   │   └── model/                               # Shared value objects (Money, Address, etc.)
│   ├── exceptions/
│   ├── statemachine/
│   └── {subdomain}/
│       ├── {Subdomain}Service.java
│       └── model/
│
└── infrastructure/
    ├── client/
    │   ├── {service}/
    │   │   ├── {Service}Adapter.java            # Implements domain port
    │   │   ├── {Service}FeignClient.java
    │   │   └── {ResponseDto}.java
    │   └── mapper/
    ├── db/
    │   ├── {aggregate}/
    │   │   ├── {Aggregate}Entity.java
    │   │   ├── {Aggregate}JpaRepository.java
    │   │   ├── {Aggregate}RepositoryAdapter.java
    │   │   └── mapper/
    │   └── common/
    ├── stream/
    │   ├── {Event}Publisher.java                 # Implements EventPublisher<T>
    │   └── mapper/
    └── common/
```

---

## 4. API Module Package Tree

```
com.{org}.banking.{domain}.api.model/
├── Create{Resource}Request.java
├── {Resource}Response.java
├── {Resource}View.java
├── {Resource}StatusEvent.java
├── EventEnvelope.java
├── common/
│   ├── Money.java
│   ├── {Status}Enum.java
│   └── {Type}Enum.java
└── validator/
    ├── ErrorMessages.java
    ├── {Custom}Validator.java
    └── @Valid{Annotation}.java
```

**Test fixtures source set** (`src/testFixtures/java`):

```
com.{org}.banking.{domain}.api.model/
├── {Resource}Fixtures.java
├── {Resource}RequestFixtures.java
├── Create{Resource}ResponseFixtures.java
└── CommonFixtures.java
```

---

## 5. Test Fixtures Package Tree

Located in `{service-name}/src/testFixtures/java`:

```
com.{org}.banking.{domain}.test/
├── FullContextIntegrationTest.java        # Integration test base class
├── RestControllerAbstractTest.java        # Controller test base class
├── TestUtils.java                         # eqIgnoringTimestamps, eqIgnoring
├── commons/
│   └── AuthenticationGenerator.java
├── fixtures/
│   ├── PayoutFixtures.java                # One per aggregate (30+ constants per state)
│   ├── CommonFixtures.java                # Shared: SOME_MONEY, SOME_SENDER, etc.
│   ├── {Subdomain}Fixtures.java
│   └── ...
├── stubs/
│   ├── fee/FeeServiceStubs.java           # One per external service
│   ├── auth/AuthServiceStubs.java
│   ├── banking/BankingApiStubs.java
│   ├── bridge/BridgeServiceStubs.java
│   ├── beneficiary/BeneficiaryManagementStubs.java
│   ├── wallet/WalletServiceStubs.java
│   ├── capability/CapabilitiesManagementServiceStubs.java
│   ├── transfer/TransferServiceStubs.java
│   └── mfa/MfaStub.java
└── request/
    └── Invalid{Request}Provider.java      # ArgumentsProvider for validation tests
```

---

## 6. Business Test Package Tree

Located in `{service-name}/src/business-test/java`:

```
com.{org}.banking.{domain}/
├── BusinessTest.java                      # Base: @SpringBootTest(DEFINED_PORT) + @DirtiesContext
├── {Feature}FlowBusinessTest.java
├── {Resource}ControllerBusinessTest.java
└── common/
    ├── TestContext.java                    # Builder-based test state holder
    ├── BusinessTestHelper.java
    ├── BusinessTestMapper.java            # MapStruct mapper for assertions
    └── {Feature}BusinessTestHelper.java
```

---

## 7. File Placement Decision Tree

```
Where does this new file go?

REST endpoint?
  -> application/controller/{resource}/

Event listener?
  -> application/stream/

Scheduled job?
  -> application/job/

Spring config bean?
  -> application/config/

Business logic (no infra deps)?
  -> domain/{aggregate}/

Domain port interface?
  -> domain/{aggregate}/ or domain/common/

Domain model / value object?
  -> domain/{aggregate}/model/core/

Domain command or event?
  -> domain/{aggregate}/model/events/

Query parameter object?
  -> domain/{aggregate}/model/query/

Domain exception?
  -> domain/exceptions/

JPA entity or Spring Data repo?
  -> infrastructure/db/{aggregate}/

Entity <-> domain mapper?
  -> infrastructure/db/{aggregate}/mapper/

External HTTP client adapter?
  -> infrastructure/client/{service}/

External response DTO mapper?
  -> infrastructure/client/mapper/

Message publisher?
  -> infrastructure/stream/

Domain event -> wire event mapper?
  -> infrastructure/stream/mapper/

Request/response DTO shared with consumers?
  -> {service-name}-api module

Test fixture constants/builders?
  -> src/testFixtures/java/.../fixtures/

WireMock stub for external service?
  -> src/testFixtures/java/.../stubs/{service}/

Unit test?
  -> src/test/java/ (mirrors production package)

Integration test?
  -> src/integration-test/java/

Business flow test?
  -> src/business-test/java/

Flyway migration?
  -> src/main/resources/db/migration/V{N}__{TICKET}_{desc}.sql
```
