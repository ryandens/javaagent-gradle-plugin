pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("com.gradle.develocity") version "4.3.2"
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

rootProject.name = "javaagent-plugin"
include("plugin", "simple-agent", "otel")

val isCI = providers.environmentVariable("CI").isPresent

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        uploadInBackground.set(isCI)
        if (isCI) {
            publishing {
                onlyIf { true }
            }
        }
    }
}
