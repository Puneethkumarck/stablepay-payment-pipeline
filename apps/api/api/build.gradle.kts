plugins {
    id("stablepay.java-conventions")
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(project(":schemas"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testFixturesCompileOnly(libs.lombok)
    testFixturesAnnotationProcessor(libs.lombok)
    testFixturesImplementation(libs.assertj.core)
    testFixturesImplementation(libs.mockito.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
}
