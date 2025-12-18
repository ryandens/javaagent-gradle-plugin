rootProject.name = "example-projects"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("..")

    plugins {
        id("com.android.application").version("8.13.2")
        id("com.android.library").version("8.13.2")
        id("de.mannodermaus.android-junit5").version("1.14.0.0")
        id("com.ryandens.plugin-conventions")
        id("com.ryandens.javaagent-test-android")
        id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
    }
}

includeBuild("..")

include("app", "custom-instrumentation", "android-app", "android-library")
