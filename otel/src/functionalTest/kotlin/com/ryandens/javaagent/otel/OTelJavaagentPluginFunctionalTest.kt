package com.ryandens.javaagent.otel

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.DefaultAsserter.assertTrue

class OTelJavaagentPluginFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun getProjectDir() = tempFolder.root
    private fun getBuildFile() = getProjectDir().resolve("build.gradle")
    private fun getSettingsFile() = getProjectDir().resolve("settings.gradle")

    @Test
    fun `can run task`() {
        // Setup the test build
        getSettingsFile().writeText("")
        getBuildFile().writeText(
            """
plugins {
    id('java')
    id('com.ryandens.javaagent-otel')
}

repositories {
  mavenCentral()
}

dependencies {
  otel("io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")
  otelInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jdbc:1.11.1-alpha")
  otelExtension("io.opentelemetry.contrib:opentelemetry-samplers:1.12.0-alpha")
}
"""
        )

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("extendedAgent")
        runner.withProjectDir(getProjectDir())
        val result = runner.build()

        assertTrue({ "" }, result.output.isNotEmpty())
        // Verify the result
//    assertTrue({""}, result.output.contains("Hello from plugin 'com.ryandens.otel.extension.plugin.greeting'"))
    }
}
