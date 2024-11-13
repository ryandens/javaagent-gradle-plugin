pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("com.gradle.develocity") version "3.18.2"
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
