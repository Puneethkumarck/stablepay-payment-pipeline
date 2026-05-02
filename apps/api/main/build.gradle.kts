import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("stablepay.java-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    `java-test-fixtures`
    jacoco
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
        configurations.implementation.get(),
        configurations["testFixturesApi"],
    )
}
configurations["businessTestRuntimeOnly"].extendsFrom(
    configurations.testRuntimeOnly.get(),
    configurations.runtimeOnly.get(),
)

dependencies {
    implementation(project(":apps:api:api"))
    implementation(project(":apps:api:client"))
    implementation(project(":schemas"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.aspectj)

    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql.driver)

    implementation(libs.namastack.outbox.starter.jdbc)
    implementation(libs.opensearch.java)
    implementation(libs.httpclient5)
    implementation(libs.trino.jdbc)
    implementation(libs.nv.i18n)

    implementation(libs.bucket4j.core)
    implementation(libs.bucket4j.redis)
    implementation(libs.bucket4j.lettuce)
    implementation(libs.caffeine)

    implementation(libs.nimbus.jose.jwt)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testFixturesApi(libs.spring.boot.starter.test)
    testFixturesApi(libs.spring.security.test)
    testFixturesApi(libs.spring.boot.starter.oauth2.resource.server)
    testFixturesApi(libs.testcontainers.postgres)
    testFixturesApi(libs.nv.i18n)
    testFixturesApi(libs.bucket4j.core)
    testFixturesCompileOnly(libs.lombok)
    testFixturesAnnotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)
    testImplementation(libs.trino.parser)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    integrationTestImplementation(libs.spring.boot.starter.test)
    integrationTestImplementation(libs.spring.boot.starter.webmvc.test)
    integrationTestImplementation(libs.spring.security.test)
    integrationTestImplementation(libs.testcontainers.postgres)
    integrationTestImplementation(libs.opensearch.testcontainers)
    integrationTestImplementation(libs.testcontainers.junit)
    "integrationTestCompileOnly"(libs.lombok)
    "integrationTestAnnotationProcessor"(libs.lombok)

    businessTestImplementation(libs.spring.boot.starter.test)
    businessTestImplementation(libs.spring.boot.resttestclient)
    businessTestImplementation(libs.spring.boot.http.client)
    businessTestImplementation(libs.spring.boot.restclient)
    businessTestImplementation(libs.spring.boot.testcontainers)
    businessTestImplementation(libs.testcontainers.postgres)
    businessTestImplementation(libs.testcontainers.junit)
    businessTestImplementation(libs.nimbus.jose.jwt)
    "businessTestCompileOnly"(libs.lombok)
    "businessTestAnnotationProcessor"(libs.lombok)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            "-Amapstruct.defaultComponentModel=spring",
            "-Amapstruct.defaultInjectionStrategy=constructor",
            "-Amapstruct.unmappedTargetPolicy=ERROR",
        ),
    )
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

tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named("check") {
    dependsOn(integrationTestTask, businessTestTask, "jacocoTestReport")
}

tasks.named<BootJar>("bootJar") {
    archiveBaseName.set("stablepay-api")
}
