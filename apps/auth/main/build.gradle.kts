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
    extendsFrom(configurations.testImplementation.get())
}
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

val businessTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
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
    implementation(libs.flyway.database.postgresql)
    runtimeOnly("org.postgresql:postgresql")

    implementation(libs.nimbus.jose.jwt)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.bucket4j.core)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testFixturesApi(libs.spring.boot.starter.test)
    testFixturesApi("org.springframework.security:spring-security-test")
    testFixturesApi("org.testcontainers:postgresql:${libs.versions.testcontainers.get()}")
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
    integrationTestImplementation("org.testcontainers:postgresql:${libs.versions.testcontainers.get()}")
    integrationTestImplementation(libs.testcontainers.junit)

    businessTestImplementation(libs.spring.boot.starter.test)
    businessTestImplementation("org.testcontainers:postgresql:${libs.versions.testcontainers.get()}")
    businessTestImplementation(libs.testcontainers.junit)
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

tasks.bootJar {
    archiveBaseName.set("stablepay-auth")
}
