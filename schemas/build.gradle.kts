plugins {
    id("stablepay.java-conventions")
    alias(libs.plugins.avro)
}

dependencies {
    implementation(libs.avro.core)
}

avro {
    fieldVisibility.set("PRIVATE")
    isGettersReturnOptional = true
    outputCharacterEncoding.set("UTF-8")
}
