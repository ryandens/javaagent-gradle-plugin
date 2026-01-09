plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenLocal()
}
dependencies {
    /* uncomment when doing local testing on this project, leave commented out for functional test to resolve latest plugin
    implementation("com.ryandens:otel:0.9.1")
    implementation("com.ryandens:plugin:0.9.1")
    */
    implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.3.1")
    implementation("io.opentelemetry.instrumentation.muzzle-generation:io.opentelemetry.instrumentation.muzzle-generation.gradle.plugin:2.20.1-alpha")
    implementation("io.opentelemetry.instrumentation.muzzle-check:io.opentelemetry.instrumentation.muzzle-check.gradle.plugin:2.20.1-alpha")
}
