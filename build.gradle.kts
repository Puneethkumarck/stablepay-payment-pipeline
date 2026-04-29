plugins {
    java
    alias(libs.plugins.avro) apply false
}

allprojects {
    group = "io.stablepay"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
    }
}
