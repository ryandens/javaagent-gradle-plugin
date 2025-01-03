package com.ryandens.javaagent.otel

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OTelJavaagentPluginFunctionalTest {
    @Test
    fun `can run task`(
        @TempDir projectDir: File,
    ) {
        File("../example-projects/").copyRecursively(projectDir)

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("mergeInstrumentationServiceFiles", "extendedAgent", "run")
        runner.withProjectDir(projectDir)
        runner.withDebug(true)
        val result = runner.build()

        // Verify the result
        // TODO use testcontainers to start an otel fake backend and retrieve spans from it here to verify instrumentation worked
        assertTrue(result.output.contains("AgentInstaller - Installed 1 extension(s)"))
        assertTrue(
            result.output.contains(
                "Loading instrumentation jdbc [class io.opentelemetry.javaagent.instrumentation.jdbc.JdbcInstrumentationModule]",
            ),
        )
        assertTrue(
            result.output.contains(
                "Applying instrumentation: sample [class com.ryandens.example.SampleInstrumentationModule]",
            ),
        )
        assertTrue(result.output.contains("LoggingSpanExporter - 'iterative'")) // span name
        assertTrue(result.output.contains(" [tracer: FibonacciTracer:]")) // tracer name
        val agent = File(projectDir, "app/build/agents/extended-opentelemetry-javaagent.jar")
        assertTrue(agent.exists())
    }
}
