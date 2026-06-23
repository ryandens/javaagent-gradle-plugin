import org.gradle.plugin.compatibility.compatibility

plugins {
    id("com.ryandens.plugin-conventions")
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.apache.commons:commons-compress:1.28.0")
    testImplementation("org.apache.commons:commons-text:1.15.0")
}

gradlePlugin {
    plugins {
        create("javaagentApplicationPlugin") {
            id = "com.ryandens.javaagent-application"
            displayName = "Javaagent Application Plugin"
            description = "Automatically attaches javaagents to the Application Plugin run tasks and the main application distributions"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationPlugin"
            tags.set(listOf("javaagent", "instrumentation", "application"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("javaagentApplicationDistributionPlugin") {
            id = "com.ryandens.javaagent-application-distribution"
            displayName = "Javaagent Application Distribution Plugin"
            description = "Automatically attaches javaagents to the Application Plugin distributions"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationDistributionPlugin"
            tags.set(listOf("javaagent", "instrumentation", "application"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("javaagentApplicationRunPlugin") {
            id = "com.ryandens.javaagent-application-run"
            displayName = "Javaagent Application Run Plugin"
            description = "Automatically attaches javaagents to the Application Plugin run tasks"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationRunPlugin"
            tags.set(listOf("javaagent", "instrumentation", "application"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
        create("javaagentTestPlugin") {
            id = "com.ryandens.javaagent-test"
            displayName = "Javaagent Test Plugin"
            description = "Automatically attaches javaagents to the Java Plugin Test tasks"
            implementationClass = "com.ryandens.javaagent.JavaagentTestPlugin"
            tags.set(listOf("javaagent", "instrumentation", "test"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

tasks.functionalTest {
    inputs.file(layout.projectDirectory.dir("../simple-agent/build.gradle.kts"))
    inputs.files(layout.projectDirectory.dir("../simple-agent/src/"))
}
