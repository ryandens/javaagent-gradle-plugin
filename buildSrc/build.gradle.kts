plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradle.publish:plugin-publish-plugin:1.1.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0-Beta")
    implementation("com.netflix.nebula:nebula-publishing-plugin:19.0.0")
}
