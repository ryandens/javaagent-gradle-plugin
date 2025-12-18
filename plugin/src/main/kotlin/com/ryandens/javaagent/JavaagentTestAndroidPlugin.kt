package com.ryandens.javaagent

import com.ryandens.javaagent.utils.JavaagentVersionUtil
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

class JavaagentTestAndroidPlugin :
    Plugin<Project>,
    JavaagentPlugin {
    companion object {
        const val CONFIGURATION_NAME = "testJavaagent"
    }

    override fun dependentProjectPlugins(): Collection<String> = setOf("com.android.application", "com.android.library")

    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        // Register configuration
        val javaagentTestConfiguration =
            project.configurations.register(CONFIGURATION_NAME) {
                // we expect javaagents to come as shaded JARs
                it.isTransitive = false
                it.extendsFrom(javaagentConfiguration.get())
                it.resolutionStrategy { strategy ->
                    strategy.eachDependency { dep ->
                        if (dep.requested.version.isNullOrBlank()) {
                            dep.useVersion(
                                JavaagentVersionUtil.versionFromDependencyAndConfiguration(
                                    dep.requested,
                                    project.configurations.named("testRuntimeClasspath"),
                                ),
                            )
                        }
                    }
                }
            }

        val extension = project.extensions.create("javaagentTest", JavaagentTestExtension::class.java)

        val enabled = calculateEnabled(project, extension)

        // configure the run task to use the `javaagent` flag pointing to the dependency stored in the local Maven repository
        project.afterEvaluate { project ->
            // TODO get the actual variants from the build
            val variants = listOf("debug", "release")
            variants.map { variant ->
                variant.replaceFirstChar(Char::titlecase)
            }.forEach { variantName ->
                project.tasks.named("test${variantName}UnitTest", Test::class.java) {
                    if (enabled.get()) {
                        JavaForkOptionsConfigurer.configureJavaForkOptions(
                            it,
                            javaagentTestConfiguration.map { configuration ->
                                configuration.files
                            },
                        )
                    }
                }
            }
        }
    }

    private fun calculateEnabled(
        project: Project,
        extension: JavaagentTestExtension,
    ): Provider<Boolean> {
        val javaagentTestEnabled = project.findProperty("javaagentTestEnabled")
        val enabled =
            extension.enabled.map {
                if (javaagentTestEnabled != null) {
                    (javaagentTestEnabled as String).toBoolean()
                } else {
                    it
                }
            }
        return enabled
    }
}
