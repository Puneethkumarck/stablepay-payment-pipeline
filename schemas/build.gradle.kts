plugins {
    id("stablepay.java-conventions")
    alias(libs.plugins.avro)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(libs.avro.core)
}

avro {
    fieldVisibility.set("PRIVATE")
    isCreateSetters = false
    isGettersReturnOptional = true
    outputCharacterEncoding.set("UTF-8")
}
