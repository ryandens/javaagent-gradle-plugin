package com.ryandens.javaagent

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

/**
 * A simple functional test for plugins with IDs matching 'com.ryandens.javaagent-*'. Currently tested plugins are:
 *
 * `com.ryandens.javaagent-application-run` (via `com.ryandens.javaagent-application`)
 * `com.ryandens.javaagent-application-distribution` (via `com.ryandens.javaagent-application`)
 */
class JavaagentPluginFunctionalTest {

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

    @Test fun `can attach to application run task`() {
        val dependencies = """
            javaagent project(':simple-agent')
        """

        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject(dependencies, listOf("build", "run"))

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))
    }

    private fun createAndBuildJavaagentProject(dependencies: String, buildArgs: List<String>): BuildResult {

        val helloWorldDir = File(functionalTestDir, "hello-world")
        File("src/functionalTest/resources/hello-world-project/").copyRecursively(helloWorldDir)
        File("../simple-agent/").copyRecursively(File(functionalTestDir, "simple-agent"))

        functionalTestDir.resolve("settings.gradle").writeText(
            """
            rootProject.name = 'example'
            include('hello-world')
            include('simple-agent')
            """
        )
        helloWorldDir.resolve("build.gradle").writeText(
            """
                plugins {
                    id('application')
                    id('com.ryandens.javaagent-application')
                }
                
                repositories {
                    mavenCentral()
                }
                
                application {
                    mainClass = 'com.ryandens.HelloWorld'
                    applicationDefaultJvmArgs = ['-Xmx256m']
                }
                
                run {
                    jvmArgs = ['-Xms100m']
                }
                
                dependencies {
                    $dependencies
                }
            """
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
