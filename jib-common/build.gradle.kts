import org.gradle.plugin.compatibility.compatibility

plugins {
    id("com.ryandens.plugin-conventions")
}

dependencies {
    implementation(project(":plugin"))
    compileOnly("com.google.cloud.tools:jib-gradle-plugin-extension-api:0.4.0")
    compileOnly("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.5.3")
}

gradlePlugin {
    plugins {
        create("javaagentJibCommonPlugin") {
            id = "com.ryandens.javaagent-jib-common"
            displayName = "Javaagent Jib Common Plugin"
            description = "Come plugin to help automatically attach javaagents in OCI images created by jib-like plugins"
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
