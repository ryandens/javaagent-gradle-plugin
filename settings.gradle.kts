pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("com.gradle.develocity") version "4.6.1"
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        exclusiveContent {
            forRepository { gradlePluginPortal() }
            filter {
                includeGroup("com.google.cloud.tools.jib")
                includeModule("com.google.cloud.tools", "jib-gradle-plugin")
            }
        }
        exclusiveContent {
            forRepository { google() }
            filter {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx\\..*")
                includeGroup("com.google.testing.platform")
            }
        }
    }
}

rootProject.name = "javaagent-plugin"
include("android", "jib-common", "jib", "plugin", "simple-agent", "otel")

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
