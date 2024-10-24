plugins {
    `kotlin-dsl`
    alias(buildlibs.plugins.reproducible.builds)
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
    implementation(buildlibs.plugin.publish)
    implementation(buildlibs.kotlin)
    implementation(buildlibs.nebula.apache.license)
    implementation(buildlibs.reproducible.builds)
    implementation(buildlibs.spotless)
    implementation(buildlibs.sigstore)
}
