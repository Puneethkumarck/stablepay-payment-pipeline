import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("stablepay.java-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    `java-test-fixtures`
}

val integrationTest: SourceSet by sourceSets.creating {
    java.srcDir("src/integration-test/java")
    resources.srcDir("src/integration-test/resources")
    compileClasspath += sourceSets.main.get().output + sourceSets["testFixtures"].output
    runtimeClasspath += output + compileClasspath
}

val businessTest: SourceSet by sourceSets.creating {
    java.srcDir("src/business-test/java")
    resources.srcDir("src/business-test/resources")
    compileClasspath += sourceSets.main.get().output + sourceSets["testFixtures"].output
    runtimeClasspath += output + compileClasspath
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(
        configurations.testImplementation.get(),
        configurations["testFixturesApi"],
    )
}
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

val businessTestImplementation: Configuration by configurations.getting {
    extendsFrom(
        configurations.testImplementation.get(),
        configurations["testFixturesApi"],
    )
}
configurations["businessTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(project(":apps:auth:auth"))
    implementation(project(":apps:auth:client"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql.driver)

    implementation(libs.nimbus.jose.jwt)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.bucket4j.core)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testFixturesApi(libs.spring.boot.starter.test)
    testFixturesApi(libs.spring.security.test)
    testFixturesApi(libs.testcontainers.postgres)
    testFixturesCompileOnly(libs.lombok)
    testFixturesAnnotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    integrationTestImplementation(libs.spring.boot.starter.test)
    integrationTestImplementation(libs.testcontainers.postgres)
    integrationTestImplementation(libs.testcontainers.junit)
    integrationTestImplementation(testFixtures(project(":apps:auth:auth")))
    "integrationTestCompileOnly"(libs.lombok)
    "integrationTestAnnotationProcessor"(libs.lombok)

    businessTestImplementation(libs.spring.boot.starter.test)
    businessTestImplementation(libs.testcontainers.postgres)
    businessTestImplementation(libs.testcontainers.junit)
    "businessTestCompileOnly"(libs.lombok)
    "businessTestAnnotationProcessor"(libs.lombok)
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter("test")
}

val businessTestTask = tasks.register<Test>("businessTest") {
    description = "Runs business (E2E) tests."
    group = "verification"
    testClassesDirs = businessTest.output.classesDirs
    classpath = businessTest.runtimeClasspath
    shouldRunAfter("integrationTest")
}

tasks.named("check") {
    dependsOn(integrationTestTask, businessTestTask)
}

tasks.named<BootJar>("bootJar") {
    archiveBaseName.set("stablepay-auth")
}
