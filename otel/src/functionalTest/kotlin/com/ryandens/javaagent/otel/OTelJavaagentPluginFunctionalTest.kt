package com.ryandens.javaagent.otel

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.jar.JarArchiveEntry
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import kotlin.test.DefaultAsserter.assertTrue

class OTelJavaagentPluginFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun getProjectDir() = tempFolder.root

    @Test
    fun `can run task`() {
        File("../example-projects/").copyRecursively(getProjectDir())

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("extendedAgent")
        runner.withProjectDir(getProjectDir())
        val result = runner.build()

        // Verify the result
        assertTrue({ "" }, result.output.isNotEmpty())
        val agent = File(getProjectDir(), "app/build/agents/extended-opentelemetry-javaagent.jar")
        assertTrue({ "extended agent was created" }, agent.exists())
        var foundCustomInstrumentationClassData = false
        // the following must be asserted on in this manner because currently there are duplicate service files for InstrumentationModule
        var foundSampleInstrumentationModuleInServiceFile = false
        var foundServletInstrumentationModuleInServiceFile = false
        FileInputStream(agent).use { fis ->
            ArchiveStreamFactory().createArchiveInputStream("jar", fis).use { ais ->
                var entry = ais.nextEntry as JarArchiveEntry?
                while (entry != null) {
                    if ("inst/com/ryandens/javaagent/example/SampleInstrumentationModule.classdata" == entry.name) {
                        foundCustomInstrumentationClassData = true
                    }
                    if ("inst/META-INF/services/io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule" == entry.name) {
                        val instrumentationServices = ais.readBytes().toString(Charsets.UTF_8)
                        if (instrumentationServices.contains("com.ryandens.javaagent.example.SampleInstrumentationModule")) {
                            foundSampleInstrumentationModuleInServiceFile = true
                        }
                        if (instrumentationServices.contains("io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3InstrumentationModule")) {
                            foundServletInstrumentationModuleInServiceFile = true
                        }
                    }
                    entry = ais.nextEntry as JarArchiveEntry?
                }
            }
        }
        assertTrue({ "foundCustomInstrumentationClassData" }, foundCustomInstrumentationClassData)
        assertTrue({ "foundSampleInstrumentationModuleInServiceFile" }, foundSampleInstrumentationModuleInServiceFile)
        assertTrue({ "foundServletInstrumentationModuleInServiceFile" }, foundServletInstrumentationModuleInServiceFile)
    }
}
