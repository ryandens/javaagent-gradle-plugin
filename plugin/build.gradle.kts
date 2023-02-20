plugins {
    id("com.ryandens.plugin-conventions")
}

val plugin: Configuration by configurations.creating

configurations {
    compileOnly {
        extendsFrom(plugin)
    }
    testImplementation {
        extendsFrom(plugin)
    }
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    // adds dependencies with the plugin configuration to the plugin classpath
    pluginClasspath.setFrom(pluginClasspath.plus(plugin.files))
    // avoid warnings
    dependsOn(tasks.compileKotlin)
    dependsOn(tasks.compileJava)
    dependsOn(tasks.processResources)
}

dependencies {
    plugin("com.google.cloud.tools:jib-gradle-plugin-extension-api:0.4.0")
    plugin("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.3.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.apache.commons:commons-compress:1.22")
}

gradlePlugin {
    plugins {
        create("javaagentApplicationPlugin") {
            id = "com.ryandens.javaagent-application"
            displayName = "Javaagent Application Plugin"
            description = "Automatically attaches javaagents to the Application Plugin run tasks and the main application distributions"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationPlugin"
            tags.set(listOf("javaagent", "instrumentation", "application"))
        }
        create("javaagentApplicationDistributionPlugin") {
            id = "com.ryandens.javaagent-application-distribution"
            displayName = "Javaagent Application Distribution Plugin"
            description = "Automatically attaches javaagents to the Application Plugin distributions"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationDistributionPlugin"
            tags.set(listOf("javaagent", "instrumentation", "application"))
        }
        create("javaagentApplicationRunPlugin") {
            id = "com.ryandens.javaagent-application-run"
            displayName = "Javaagent Application Run Plugin"
            description = "Automatically attaches javaagents to the Application Plugin run tasks"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationRunPlugin"
            tags.set(listOf("javaagent", "instrumentation", "application"))
        }
        create("javaagentJibPlugin") {
            id = "com.ryandens.javaagent-jib"
            displayName = "Javaagent Jib Plugin"
            description = "Automatically includes javaagents in OCI images created by Jib"
            implementationClass = "com.ryandens.javaagent.JavaagentJibExtension"
            tags.set(listOf("javaagent", "instrumentation", "docker", "jib"))
        }
        create("javaagentTestPlugin") {
            id = "com.ryandens.javaagent-test"
            displayName = "Javaagent Test Plugin"
            description = "Automatically attaches javaagents to the Java Plugin Test tasks"
            implementationClass = "com.ryandens.javaagent.JavaagentTestPlugin"
            tags.set(listOf("javaagent", "instrumentation", "test"))
        }
    }
}
