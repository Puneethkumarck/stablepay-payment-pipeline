rootProject.name = "stablepay-payment-pipeline"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
    }
}

include(
    ":schemas",
    ":apps:auth:auth",
    ":apps:auth:client",
    ":apps:auth:main",
    ":apps:api:api",
    ":apps:api:client",
    ":apps:api:main",
    ":apps:flink-jobs"
)
