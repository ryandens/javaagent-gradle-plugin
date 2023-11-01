package com.ryandens.javaagent

import org.gradle.internal.jvm.Jvm
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private lateinit var helloWorldDir: File

    @BeforeTest
    fun beforeEach() {
        functionalTestDir = File("build", "functionalTest")
        functionalTestDir.mkdirs()
        helloWorldDir = File(functionalTestDir, "hello-world")
    }

    @AfterTest
    fun afterEach() {
        functionalTestDir.deleteRecursively()
    }

    @Test fun `can work without javaagent dependencies`() {
        val dependencies = """
        """

        // create the test project and run the tasks
        createJavaagentProject(dependencies)
        val result = runBuild(listOf("assemble", "run"))

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
    }

    @Test fun `can attach to application run task`() {
        val dependencies = """
            javaagent project(':simple-agent')
        """

        // create the test project and run the tasks
        createJavaagentProject(dependencies)
        val result = runBuild(listOf("--configuration-cache", "assemble", "run"))

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))
        assertTrue(result.output.contains("Configuration cache entry stored."))

        // verify configuration cache
        val ccResult = runBuild(listOf("--configuration-cache", "assemble", "run"))
        assertTrue(ccResult.output.contains("Hello World!"))
        assertTrue(ccResult.output.contains("Hello from my simple agent!"))
        assertTrue(ccResult.output.contains("Reusing configuration cache."))
    }

    @Test fun `can attach to test task`() {
        val otelVersion = "1.11.1"
        val dependencies = """
            javaagent project(':simple-agent')
            testJavaagent 'io.opentelemetry.javaagent:opentelemetry-javaagent:$otelVersion'
        """

        // create the test project and run the tasks
        createJavaagentProject(dependencies)
        val result = runBuild(listOf("--configuration-cache", "assemble", "test"))

        // Verify the result
        assertTrue(result.output.contains("Hello from my simple agent!"))
        assertTrue(
            result.output.contains("io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: $otelVersion"),
        )

        // verify configuration cache
        val ccResult = runBuild(listOf("--configuration-cache", "assemble", "test"))
        assertEquals(TaskOutcome.UP_TO_DATE, ccResult.task(":hello-world:test")?.outcome)
        assertTrue(ccResult.output.contains("Reusing configuration cache."))
    }

    @Test fun `can attach two agents to application run task`() {
        val otelVersion = "1.11.1"
        val dependencies = """
            javaagent project(':simple-agent')
            javaagent 'io.opentelemetry.javaagent:opentelemetry-javaagent:$otelVersion'
        """

        // create the test project and run the tasks
        createJavaagentProject(dependencies)
        val result = runBuild(listOf("assemble", "run"))

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))
        assertTrue(
            result.output.contains("io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: $otelVersion"),
        )
    }

    @Test fun `can attach to application distribution`() {
        val dependencies = """
            javaagent project(':simple-agent')
        """

        // create the test project and run the tasks
        createJavaagentProject(dependencies)
        val result = runBuild(listOf("--configuration-cache", "build", "installDist", "execStartScript"))

        // verify the distribution was created properly
        val applicationDistribution =
            File(functionalTestDir, "hello-world${File.separator}build${File.separator}distributions${File.separator}hello-world.tar")
        assertTrue(applicationDistribution.exists())

        // verify the expected text was injected into the start script
        val expectedDefaultJavaOpts = """
DEFAULT_JVM_OPTS="-javaagent:${"$"}APP_HOME/agent-libs/simple-agent.jar -Xmx256m"
"""
        val applicationDistributionScript =
            File(functionalTestDir, "hello-world${File.separator}build${File.separator}scripts${File.separator}hello-world")
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
        assertTrue(
            File(
                functionalTestDir,
                "hello-world/build/install/hello-world/agent-libs/simple-agent.jar".replace("/", File.separator),
            ).exists(),
        )

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))

        // verify configuration cache
        val ccResult = runBuild(listOf("--configuration-cache", "build", "installDist", "execStartScript"))
        assertTrue(ccResult.output.contains("Reusing configuration cache."))
    }

    @Test fun `can handle upgrade of agent with build cache`() {
        createJavaagentProject(
            """
            javaagent 'io.opentelemetry.javaagent:opentelemetry-javaagent:1.30.0'
            """.trimIndent(),
        )

        val firstBuild = runBuild(listOf("--build-cache", "build", "installDist", "execStartScript"))

        assertTrue(
            firstBuild.output.contains("io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 1.30.0"),
        )

        val buildScript = helloWorldDir.resolve("build.gradle")

        // replace the agent version with a newer version
        buildScript.writeText(buildScript.readText().replace("1.30.0", "1.31.0"))
        val secondBuild = runBuild(listOf("--build-cache", "build", "installDist", "execStartScript"))
        assertTrue(
            secondBuild.output.contains("io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 1.31.0"),
        )
    }

    @Test fun `cat attach no agents to application distribution`() {
        // create the test project and run the tasks
        createJavaagentProject("")
        val result = runBuild(listOf("build", "installDist", "execStartScript"))

        // verify the distribution was created properly
        val applicationDistribution =
            File(functionalTestDir, "hello-world${File.separator}build${File.separator}distributions${File.separator}hello-world.tar")
        assertTrue(applicationDistribution.exists())

        val applicationDistributionScript =
            File(functionalTestDir, "hello-world${File.separator}build${File.separator}scripts${File.separator}hello-world")
        assertTrue(applicationDistributionScript.readText().contains("""DEFAULT_JVM_OPTS="-Xmx256m"""))

        assertFalse(
            File(
                functionalTestDir,
                "hello-world${File.separator}build${File.separator}install${File.separator}hello-world${File.separator}agent-libs/",
            ).exists(),
        )
        assertTrue(result.output.contains("Hello World!"))
    }

    private fun createJavaagentProject(dependencies: String) {
        val helloWorldDir = File(functionalTestDir, "hello-world")
        Paths.get("src", "functionalTest", "resources", "hello-world-project").toFile().copyRecursively(helloWorldDir)
        val simpleAgentTestDir = File(functionalTestDir, "simple-agent")
        val simpleAgentBuildScript = simpleAgentTestDir.resolve("build.gradle.kts")
        Paths.get("..", "simple-agent").toFile().copyRecursively(simpleAgentTestDir)
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
                    id('com.ryandens.javaagent-application')
                    id('com.ryandens.javaagent-test')
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
                    dependsOn('installDist')
                    inputs.files(layout.buildDirectory.dir('install'))
                    workingDir(layout.buildDirectory.dir('install').map { it.dir('hello-world').dir('bin') })
                    commandLine '.${File.separator}hello-world'
                    environment JAVA_HOME: "${Jvm.current().getJavaHome()}"
                }
                
                test {
                    useJUnitPlatform()
                }
                
                dependencies {
                    $dependencies
                     testImplementation(platform("org.junit:junit-bom:5.11.2"))
                     testImplementation("org.junit.jupiter:junit-jupiter")
                     testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
                     testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                }
            """,
        )
    }

    private fun runBuild(buildArgs: List<String>): BuildResult {
        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(buildArgs)
        runner.withProjectDir(functionalTestDir)
        return runner.build()
    }
}
