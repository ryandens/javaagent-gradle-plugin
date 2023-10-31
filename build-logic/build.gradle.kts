plugins {
    `kotlin-dsl`
    alias(buildlibs.plugins.spotless)
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "src/main/kotlin/*.gradle.kts")
        ktlint()
    }
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradle.publish:plugin-publish-plugin:1.1.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
    implementation("com.netflix.nebula:nebula-publishing-plugin:20.0.0")
    implementation(buildlibs.spotless)
}
