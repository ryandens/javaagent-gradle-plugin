pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("com.gradle.enterprise") version "3.15.1"
}

rootProject.name = "javaagent-plugin"
include("plugin", "simple-agent", "otel")

val isCI = System.getenv("CI") != null

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        isUploadInBackground = !isCI

        if (isCI) {
            publishAlways()
        }

        capture {
            isTaskInputFiles = true
        }
    }
}
