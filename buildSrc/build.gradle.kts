plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Pin commons-compress 1.27.1: Spring Boot 4.0.6's BootZipCopyAction calls
    // putArchiveEntry(ZipArchiveEntry), removed in commons-compress 1.28+ shipped with Gradle 9.5.
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.4")
}
