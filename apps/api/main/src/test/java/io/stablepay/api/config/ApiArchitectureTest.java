package io.stablepay.api.config;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

@AnalyzeClasses(packages = "io.stablepay.api")
class ApiArchitectureTest {

  private static final String CUSTOMER_ID_FQN = "io.stablepay.api.domain.model.CustomerId";
  private static final String IDEMPOTENCY_REPOSITORY_FQN =
      "io.stablepay.api.domain.port.IdempotencyRepository";
  private static final String OUTBOX_REPOSITORY_FQN =
      "io.stablepay.api.domain.port.OutboxRepository";
  private static final String TRANSACTION_REPOSITORY_FQN =
      "io.stablepay.api.domain.port.TransactionRepository";
  private static final String SSE_UNSCOPED_TAIL_METHOD = "tailSinceSortValue";

  @ArchTest
  static final ArchRule layered =
      layeredArchitecture()
          .consideringAllDependencies()
          .layer("Domain")
          .definedBy("io.stablepay.api.domain..")
          .layer("Application")
          .definedBy("io.stablepay.api.application..")
          .layer("Infrastructure")
          .definedBy("io.stablepay.api.infrastructure..")
          .whereLayer("Infrastructure")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Application")
          .mayOnlyBeAccessedByLayers("Infrastructure")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers("Application", "Infrastructure")
          .withOptionalLayers(true);

  @ArchTest
  static final ArchRule domainHasNoSpringDependenciesExceptStereotypeAndTransaction =
      noClasses()
          .that()
          .resideInAPackage("io.stablepay.api.domain..")
          .should()
          .dependOnClassesThat(
              resideInAPackage("org.springframework..")
                  .and(resideOutsideOfPackage("org.springframework.stereotype.."))
                  .and(resideOutsideOfPackage("org.springframework.transaction..")));

  @ArchTest
  static final ArchRule domainHasNoJpaDependencies =
      noClasses()
          .that()
          .resideInAPackage("io.stablepay.api.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("jakarta.persistence..");

  @ArchTest
  static final ArchRule noFieldInjection =
      noFields().should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

  @ArchTest
  static final ArchRule noSystemOut =
      noClasses()
          .should()
          .accessField(System.class, "out")
          .orShould()
          .accessField(System.class, "err");

  @ArchTest
  static final ArchRule portMethodsRequireCustomerScope =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("io.stablepay.api.domain.port..")
          .and()
          .areDeclaredInClassesThat()
          .areInterfaces()
          .and()
          .areDeclaredInClassesThat()
          .doNotHaveFullyQualifiedName(IDEMPOTENCY_REPOSITORY_FQN)
          .and()
          .areDeclaredInClassesThat()
          .doNotHaveFullyQualifiedName(OUTBOX_REPOSITORY_FQN)
          .should(
              new ArchCondition<JavaMethod>(
                  "take a CustomerId parameter or have a name ending in 'Admin'") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                  if (isUnscopedSseSource(method)) {
                    return;
                  }
                  var hasCustomerId =
                      method.getRawParameterTypes().stream()
                          .anyMatch(p -> p.getName().equals(CUSTOMER_ID_FQN));
                  var isAdmin = method.getName().endsWith("Admin");
                  if (!hasCustomerId && !isAdmin) {
                    events.add(
                        SimpleConditionEvent.violated(
                            method,
                            "Port method "
                                + method.getFullName()
                                + " must take CustomerId or have a name ending in 'Admin'"));
                  }
                }

                private boolean isUnscopedSseSource(JavaMethod method) {
                  return method.getOwner().getFullName().equals(TRANSACTION_REPOSITORY_FQN)
                      && method.getName().equals(SSE_UNSCOPED_TAIL_METHOD);
                }
              });
}
