pluginManagement {
    includeBuild("build-logic")
}

plugins {
    val enableDevelocity =
        providers.gradleProperty("develocityEnabled")
            .map(String::toBoolean)
            .orElse(providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) })
            .orElse(false)
            .get()
    val enableFoojayResolver =
        providers.gradleProperty("foojayResolverEnabled")
            .map(String::toBoolean)
            .orElse(providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) })
            .orElse(false)
            .get()

    if (enableDevelocity) {
        id("com.gradle.develocity") version "4.2.2"
    }
    if (enableFoojayResolver) {
        id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
    }
}

rootProject.name = "javaagent-plugin"
include("plugin", "simple-agent", "otel")

val enableDevelocity =
    providers.gradleProperty("develocityEnabled")
        .map(String::toBoolean)
        .orElse(providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) })
        .orElse(false)
        .get()

if (enableDevelocity) {
    apply(from = "gradle/develocity.settings.gradle.kts")
}
