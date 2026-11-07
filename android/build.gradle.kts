import org.gradle.plugin.compatibility.compatibility

plugins {
    id("com.ryandens.kotlin-test-conventions")
}

// TestKit injects plugins through a separate classloader: include the matching AGP only for tests.
val androidTestPluginDependencies = configurations.dependencyScope("androidTestPluginDependencies").get()
val androidTestPlugin =
    configurations.resolvable("androidTestPlugin") {
        extendsFrom(androidTestPluginDependencies)
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
    }

dependencies {
    implementation(project(":plugin"))
    compileOnly(libs.android.gradle.api)
    addProvider(
        androidTestPluginDependencies.name,
        providers
            .gradleProperty(
                "androidTestAgpVersion",
            ).orElse(libs.versions.android.test.gradle)
            .map { "com.android.tools.build:gradle:$it" },
    )
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(androidTestPlugin)
}

gradlePlugin {
    plugins {
        create("javaagentTestAndroidPlugin") {
            id = "com.ryandens.javaagent-test-android"
            displayName = "Javaagent Android Test Plugin"
            description = "Automatically attaches javaagents to Android host-side JVM tests"
            implementationClass = "com.ryandens.javaagent.android.JavaagentTestAndroidPlugin"
            tags.set(listOf("javaagent", "instrumentation", "android", "test"))
            compatibility {
                features { configurationCache = true }
            }
        }
    }
}

tasks.named<Test>("functionalTest") {
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    inputs
        .property(
            "androidSdkHome",
            providers.environmentVariable("ANDROID_HOME").orElse(providers.environmentVariable("ANDROID_SDK_ROOT")),
        ).optional(true)
}
