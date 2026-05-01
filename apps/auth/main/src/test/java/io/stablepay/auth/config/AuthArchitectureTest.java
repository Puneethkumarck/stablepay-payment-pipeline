package io.stablepay.auth.config;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.stablepay.auth")
class AuthArchitectureTest {

  @ArchTest
  static final ArchRule layered =
      layeredArchitecture()
          .consideringAllDependencies()
          .layer("Domain")
          .definedBy("io.stablepay.auth.domain..")
          .layer("Application")
          .definedBy("io.stablepay.auth.application..")
          .layer("Infrastructure")
          .definedBy("io.stablepay.auth.infrastructure..")
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
          .resideInAPackage("io.stablepay.auth.domain..")
          .should()
          .dependOnClassesThat(
              resideInAPackage("org.springframework..")
                  .and(resideOutsideOfPackage("org.springframework.stereotype.."))
                  .and(resideOutsideOfPackage("org.springframework.transaction..")));

  @ArchTest
  static final ArchRule domainHasNoJpaDependencies =
      noClasses()
          .that()
          .resideInAPackage("io.stablepay.auth.domain..")
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
}
