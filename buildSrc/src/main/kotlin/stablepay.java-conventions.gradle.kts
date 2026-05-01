plugins {
    java
    id("com.diffplug.spotless")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat("1.35.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}
