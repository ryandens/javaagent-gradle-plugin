import org.gradle.plugin.compatibility.compatibility

plugins {
    id("com.ryandens.plugin-conventions")
}

repositories {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("androidx\\..*")
            includeGroup("com.google.testing.platform")
        }
    }
}

// TestKit injects plugins through a separate classloader: include the matching AGP only for tests.
val androidTestPluginDependencies = configurations.dependencyScope("androidTestPluginDependencies")
val androidTestPlugin =
    configurations.resolvable("androidTestPlugin") {
        extendsFrom(androidTestPluginDependencies.get())
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
    }

dependencies {
    implementation(project(":plugin"))
    compileOnly("com.android.tools.build:gradle-api:9.5.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    addProvider(
        androidTestPluginDependencies.name,
        providers.gradleProperty("androidTestAgpVersion").orElse("9.4.1").map { "com.android.tools.build:gradle:$it" },
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
