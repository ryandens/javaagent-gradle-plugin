package com.ryandens.javaagent.android

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTests
import com.ryandens.javaagent.AgentOptionsResolver
import com.ryandens.javaagent.JavaForkOptionsConfigurer
import com.ryandens.javaagent.JavaagentBasePlugin
import com.ryandens.javaagent.JavaagentPlugin
import com.ryandens.javaagent.JavaagentTestExtension
import com.ryandens.javaagent.JavaagentTestPlugin
import com.ryandens.javaagent.utils.JavaagentVersionUtil
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.ClasspathNormalizer

/** Attaches agents to Android JVM host tests without applying the incompatible Java plugin. */
class JavaagentTestAndroidPlugin : JavaagentPlugin {
    override fun applyAfterJavaagentSetup(
        project: Project,
        javaagentConfiguration: NamedDomainObjectProvider<Configuration>,
    ) {
        val testAgents =
            JavaagentBasePlugin.targetConfiguration(project, JavaagentTestPlugin.CONFIGURATION_NAME, javaagentConfiguration)
        val extension = project.extensions.create("javaagentTest", JavaagentTestExtension::class.java)
        val enabled =
            project.providers
                .gradleProperty("javaagentTestEnabled")
                .map(String::toBoolean)
                .orElse(extension.enabled)
        val emptyClasspath = project.objects.fileCollection()
        val noClasspath = project.providers.provider { emptyClasspath }
        val noOptions = project.providers.provider { emptyMap<String, String>() }

        for (pluginId in listOf("com.android.application", "com.android.library")) {
            project.pluginManager.withPlugin(pluginId) {
                val components = project.extensions.getByType(AndroidComponentsExtension::class.java)
                components.onVariants(components.selector().all()) { variant ->
                    for (hostTest in (variant as HasHostTests).hostTests.values) {
                        // Each host test resolves against its own runtime, including flavor/build-type versions.
                        val runtime = project.configurations.named(hostTest.runtimeConfiguration.name)
                        val agents =
                            JavaagentBasePlugin.targetConfiguration(project, "${hostTest.name}Javaagent", testAgents)
                        agents.configure { configuration ->
                            configuration.resolutionStrategy.eachDependency { dependency ->
                                if (dependency.requested.version.isNullOrBlank()) {
                                    dependency.useVersion(
                                        JavaagentVersionUtil.versionFromDependencyAndConfiguration(dependency.requested, runtime),
                                    )
                                }
                            }
                        }
                        val options = AgentOptionsResolver.optionsByFilePath(agents)
                        val enabledOptions = enabled.flatMap { if (it) options else noOptions }
                        // Preserve producing-task dependencies while excluding disabled configurations entirely.
                        val enabledClasspath = enabled.flatMap<FileCollection> { if (it) agents else noClasspath }
                        val enabledFiles = enabledClasspath.map { it.files }
                        // AGP invokes this lazily, only when the actual variant's test task is configured.
                        hostTest.configureTestTask { test ->
                            test.inputs
                                .files(enabledClasspath)
                                .withPropertyName("javaagentClasspath")
                                .withNormalizer(ClasspathNormalizer::class.java)
                            test.inputs.property("agentOptions", enabledOptions)
                            JavaForkOptionsConfigurer.configureJavaForkOptions(test, enabledFiles, enabledOptions)
                        }
                    }
                }
            }
        }
    }
}
