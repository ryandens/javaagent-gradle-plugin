package com.ryandens.javaagent

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.io.FileInputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val JIB_IMAGE = "hello-world/build/jib-image.tar"

/**
 * A simple functional test for Jib extension
 */
class JavaagentJibExtensionFunctionalTest {
    private lateinit var functionalTestDir: File

    @BeforeTest
    fun beforeEach() {
        functionalTestDir = File("build/functionalTest")
        functionalTestDir.mkdirs()
    }

    @AfterTest
    fun afterEach() {
        functionalTestDir.deleteRecursively()
    }

    @Test fun `can create docker image using jib`() {
        val dependencies = """
            javaagent project(':simple-agent')
            runtimeOnly 'commons-lang:commons-lang:2.6'
        """

        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject(dependencies, listOf("jibBuildTar"))

        // Verify the result
        assertTrue(result.output.contains("Running extension: com.ryandens.javaagent.JavaagentJibExtension"))

        // verify the agent was added to entrypoint
        assertTrue(File(functionalTestDir, JIB_IMAGE).exists())

        FileInputStream(File(functionalTestDir, JIB_IMAGE)).use { fis ->
            ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<TarArchiveEntry>>(ArchiveStreamFactory.TAR, fis).use { ais ->
                var entry = ais.nextEntry
                while (entry != null) {
                    if ("config.json" == entry.name) {
                        val json = ais.readBytes().toString(Charsets.UTF_8)
                        assertTrue(json.contains("-javaagent:/opt/jib-agents/simple-agent.jar"))
                    }
                    entry = ais.nextEntry
                }
            }
        }
    }

    @Test fun `works even without any javaagent dependencies`() {
        val dependencies = """
            runtimeOnly 'commons-lang:commons-lang:2.6'
        """

        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject(dependencies, listOf("jibBuildTar"))

        // Verify the result
        assertTrue(result.output.contains("Running extension: com.ryandens.javaagent.JavaagentJibExtension"))

        // verify the agent was added to entrypoint
        assertTrue(File(functionalTestDir, JIB_IMAGE).exists())
        FileInputStream(File(functionalTestDir, JIB_IMAGE)).use { fis ->
            ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<TarArchiveEntry>>(ArchiveStreamFactory.TAR, fis).use { ais ->
                var entry = ais.nextEntry
                while (entry != null) {
                    if ("config.json" == entry.name) {
                        val json = ais.readBytes().toString(Charsets.UTF_8)
                        assertFalse(json.contains("-javaagent:/opt/jib-agents"))
                    }
                    entry = ais.nextEntry
                }
            }
        }
    }

    private fun createAndBuildJavaagentProject(
        dependencies: String,
        buildArgs: List<String>,
    ): BuildResult {
        val helloWorldDir = File(functionalTestDir, "hello-world")
        File("src/functionalTest/resources/hello-world-project/").copyRecursively(helloWorldDir)
        val simpleAgentTestDir = File(functionalTestDir, "simple-agent")
        val simpleAgentBuildScript = simpleAgentTestDir.resolve("build.gradle.kts")
        File("../simple-agent/").copyRecursively(simpleAgentTestDir)
        // remove a constant from the above file
        simpleAgentBuildScript.writeText(
            simpleAgentBuildScript.readText().replace(
                "id(\"com.ryandens.java-conventions\")\n",
                "",
            ),
        )

        functionalTestDir.resolve("settings.gradle").writeText(
            """
            rootProject.name = 'example'
            include('hello-world')
            include('simple-agent')
            """,
        )

        helloWorldDir.resolve("build.gradle").writeText(
            """
                plugins {
                    id('application')
                    id('com.google.cloud.tools.jib') version '3.1.4'
                    id('com.ryandens.javaagent-jib')
                }
                
                repositories {
                    mavenCentral()
                }
                
                application {
                    mainClass = 'com.ryandens.HelloWorld'
                    applicationDefaultJvmArgs = ['-Xmx256m']
                }
                                
                dependencies {
                    $dependencies
                }

                java {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }

                jib {
                    from {
                        image = "scratch"
                    }
                    container {
                        mainClass = "com.ryandens.HelloWorld"
                    }
                    to {
                        image = "javaagent-hello-world"
                    }
                }
            """,
        )

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(buildArgs)
        runner.withProjectDir(functionalTestDir)
        return runner.build()
    }
}
