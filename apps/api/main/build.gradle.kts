plugins {
    id("stablepay.java-conventions")
}

dependencies {
    implementation(project(":apps:api:api"))
    implementation(project(":apps:api:client"))
    implementation(project(":schemas"))
}
