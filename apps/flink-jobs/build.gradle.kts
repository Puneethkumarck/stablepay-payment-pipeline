plugins {
    id("stablepay.java-conventions")
    alias(libs.plugins.shadow)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":schemas"))
    implementation(libs.flink.streaming)
    implementation(libs.flink.clients)
    implementation(libs.flink.rocksdb)
    implementation(libs.flink.connector.kafka)
    implementation(libs.flink.avro)
    implementation(libs.flink.avro.confluent.registry)
    implementation(libs.flink.table.common)
    implementation(libs.flink.table.api.bridge)
    compileOnly(libs.flink.table.planner)
    implementation(libs.iceberg.flink.runtime)
    implementation(libs.opensearch.java)
    implementation(libs.httpclient5)
    implementation(libs.hadoop.common) {
        exclude(group = "org.slf4j")
    }
    implementation(libs.avro.core)
    implementation(libs.slf4j.api)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
}

tasks.shadowJar {
    archiveBaseName.set("stablepay-flink-jobs")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
    isZip64 = true
}
