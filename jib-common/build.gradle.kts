import org.gradle.plugin.compatibility.compatibility

plugins {
    id("com.ryandens.plugin-conventions")
}

dependencies {
    implementation(project(":plugin"))
    compileOnly(libs.jib.extension.api)
    compileOnly(libs.jib.plugin)
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
