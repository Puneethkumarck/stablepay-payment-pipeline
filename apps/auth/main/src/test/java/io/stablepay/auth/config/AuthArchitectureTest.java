package io.stablepay.auth.config;

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
  static final ArchRule domainHasNoSpringDependencies =
      noClasses()
          .that()
          .resideInAPackage("io.stablepay.auth.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.springframework..");

  @ArchTest
  static final ArchRule noFieldInjection =
      noFields().should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

  @ArchTest
  static final ArchRule noSystemOut = noClasses().should().callMethod(System.class, "out");
}
