plugins {
    id("stablepay.java-conventions")
    `java-library`
    `java-test-fixtures`
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testFixturesCompileOnly(libs.lombok)
    testFixturesAnnotationProcessor(libs.lombok)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
}
