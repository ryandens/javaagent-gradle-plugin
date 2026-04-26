import org.gradle.kotlin.dsl.assign
import org.gradle.plugin.compatibility.compatibility

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
    dependsOn(":jib-common:jar")
    dependsOn(":plugin:jar")
}

dependencies {
    plugin("com.google.cloud.tools:jib-gradle-plugin-extension-api:0.4.0")
    plugin("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.5.3")
    implementation(project(":jib-common"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.apache.commons:commons-compress:1.28.0")
}

gradlePlugin {
    plugins {
        create("javaagentJibPlugin") {
            id = "com.ryandens.javaagent-jib"
            displayName = "Javaagent Jib Plugin"
            description = "Automatically includes javaagents in OCI images created by Jib"
            implementationClass = "com.ryandens.javaagent.jib.JavaagentJibExtension"
            tags.set(listOf("javaagent", "instrumentation", "docker", "jib"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
