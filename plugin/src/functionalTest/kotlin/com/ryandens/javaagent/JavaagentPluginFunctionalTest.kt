package com.ryandens.javaagent

import org.gradle.internal.jvm.Jvm
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test fun `can work without javaagent dependencies`() {
        val dependencies = """
        """

        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject(dependencies, listOf("assemble", "run"))

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
    }

    @Test fun `can attach to application run task`() {
        val dependencies = """
            javaagent project(':simple-agent')
        """

        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject(dependencies, listOf("assemble", "run"))

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))
    }

    @Test fun `can attach two agents to application run task`() {
        val otelVersion = "1.11.1"
        val dependencies = """
            javaagent project(':simple-agent')
            javaagent 'io.opentelemetry.javaagent:opentelemetry-javaagent:$otelVersion'
        """

        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject(dependencies, listOf("assemble", "run"))

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))
        assertTrue(result.output.contains("io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: $otelVersion"))
    }

    @Test fun `can attach to application distribution`() {
        val dependencies = """
            javaagent project(':simple-agent')
        """

        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject(dependencies, listOf("build", "installDist", "execStartScript"))

        // verify the distribution was created properly
        val applicationDistribution = File(functionalTestDir, "hello-world/build/distributions/hello-world.tar")
        assertTrue(applicationDistribution.exists())

        // verify the expected text was injected into the start script
        val expectedDefaultJavaOpts = """
DEFAULT_JVM_OPTS="-javaagent:${"$"}APP_HOME/agent-libs/simple-agent.jar -Xmx256m"
"""
        val applicationDistributionScript = File(functionalTestDir, "hello-world/build/scripts/hello-world")
        assertTrue(applicationDistributionScript.readText().contains(expectedDefaultJavaOpts))

        /*
         * TODO add support for windows
         * val expectedWindowsDefaultJvmOpts = """
DEFAULT_JVM_OPTS="-javaagent:${"$"}APP_HOME/lib/simple-agent.jar -Xmx256m"
"""
        val applicationDistributionScript = File(functionalTestDir, "hello-world/build/scripts/hello-world.bat")
        assertTrue(applicationDistributionScript.readText().contains(expectedWindowsDefaultJvmOpts))
         */

        // verify the agent was added to the /lib/ dir of the distribution
        assertTrue(File(functionalTestDir, "hello-world/build/install/hello-world/agent-libs/simple-agent.jar").exists())

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))
    }

    @Test fun `cat attach no agents to application distribution`() {
        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject("", listOf("build", "installDist", "execStartScript"))

        // verify the distribution was created properly
        val applicationDistribution = File(functionalTestDir, "hello-world/build/distributions/hello-world.tar")
        assertTrue(applicationDistribution.exists())

        val applicationDistributionScript = File(functionalTestDir, "hello-world/build/scripts/hello-world")
        assertTrue(applicationDistributionScript.readText().contains("""DEFAULT_JVM_OPTS="-Xmx256m"""))

        assertFalse(File(functionalTestDir, "hello-world/build/install/hello-world/agent-libs/").exists())
        assertTrue(result.output.contains("Hello World!"))
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
                
                task execStartScript(type: Exec) {
                    workingDir '${helloWorldDir.canonicalPath}/build/install/hello-world/bin/'
                    commandLine './hello-world'
                    environment JAVA_HOME: "${Jvm.current().getJavaHome()}"
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
