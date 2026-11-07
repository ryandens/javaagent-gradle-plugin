import org.gradle.plugin.compatibility.compatibility

plugins {
    id("com.ryandens.archive-test-conventions")
}

val pluginDependencies = configurations.dependencyScope("pluginDependencies").get()
val pluginClasspathConfiguration =
    configurations.resolvable("pluginClasspath") {
        extendsFrom(pluginDependencies)
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
    }

configurations {
    compileOnly { extendsFrom(pluginDependencies) }
    testImplementation { extendsFrom(pluginDependencies) }
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(pluginClasspathConfiguration)
}

dependencies {
    add(pluginDependencies.name, libs.jib.extension.api)
    add(pluginDependencies.name, libs.jib.plugin)
    implementation(project(":plugin"))
    implementation(project(":jib-common"))
}

gradlePlugin {
    plugins {
        create("javaagentJibPlugin") {
            id = "com.ryandens.javaagent-jib"
            displayName = "Javaagent Jib Plugin"
            description = "Automatically includes javaagents in OCI images created by Jib"
            implementationClass = "com.ryandens.javaagent.jib.JavaagentJibPlugin"
            tags.set(listOf("javaagent", "instrumentation", "docker", "jib"))
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}
