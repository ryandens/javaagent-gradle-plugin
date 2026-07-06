package com.ryandens.javaagent

import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
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

    private val isWindows = OperatingSystem.current().isWindows

    private val scriptExtension = if (isWindows) ".bat" else ""

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

    @Test fun `run task depends on the tasks that build the javaagent artifacts`() {
        val dependencies = """
            javaagent project(':simple-agent')
        """

        createJavaagentProject(dependencies)

        // The run task adds the agent jars via a CommandLineArgumentProvider that is not a tracked input, so
        // only an explicit task dependency makes Gradle schedule :simple-agent:jar before :hello-world:run.
        // Without it the run task can launch with -javaagent:<jar> before that jar has been produced, failing
        // with "Error opening zip file or JAR manifest missing" (the `can attach to application run task`
        // test above reproduces that intermittently under parallel scheduling). --dry-run prints the task
        // execution graph in order without running anything, so asserting the ordering here catches the
        // regression deterministically, independent of scheduling timing.
        val result = runBuild(listOf("--dry-run", ":hello-world:run"))

        val agentJarIndex = result.output.indexOf(":simple-agent:jar")
        val runIndex = result.output.indexOf(":hello-world:run")
        assertTrue(agentJarIndex >= 0, "expected :simple-agent:jar to be scheduled ahead of :hello-world:run")
        assertTrue(runIndex >= 0, "expected :hello-world:run to be scheduled")
        assertTrue(agentJarIndex < runIndex, "expected :simple-agent:jar to be scheduled before :hello-world:run")
    }

    @Test fun `can attach to test task`() {
        // A real third-party javaagent used to verify the plugin attaches it and it logs its version banner.
        val otelVersion = "2.29.0"
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
        // A real third-party javaagent used to verify the plugin attaches it and it logs its version banner.
        val otelVersion = "2.29.0"
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
        val applicationDistributionScript =
            File(functionalTestDir, "hello-world${File.separator}build${File.separator}scripts${File.separator}hello-world$scriptExtension")
        if (isWindows) {
            val expectedWindowsDefaultJvmOpts = """set DEFAULT_JVM_OPTS="-javaagent:%APP_HOME%\agent-libs\simple-agent.jar" "-Xmx256m""""
            assertTrue(applicationDistributionScript.readText().contains(expectedWindowsDefaultJvmOpts))
        } else {
            val expectedDefaultJavaOpts = """
DEFAULT_JVM_OPTS="-javaagent:${"$"}APP_HOME/agent-libs/simple-agent.jar -Xmx256m"
"""
            assertTrue(applicationDistributionScript.readText().contains(expectedDefaultJavaOpts))
        }

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
            javaagent 'io.opentelemetry.javaagent:opentelemetry-javaagent:2.28.0'
            """.trimIndent(),
        )

        val firstBuild = runBuild(listOf("--build-cache", "build", "installDist", "execStartScript"))

        assertTrue(
            firstBuild.output.contains("io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.28.0"),
        )

        val buildScript = helloWorldDir.resolve("build.gradle")

        // replace the agent version with a newer version
        buildScript.writeText(buildScript.readText().replace("2.28.0", "2.29.0"))
        val secondBuild = runBuild(listOf("--build-cache", "build", "installDist", "execStartScript"))
        assertTrue(
            secondBuild.output.contains("io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.29.0"),
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
            File(functionalTestDir, "hello-world${File.separator}build${File.separator}scripts${File.separator}hello-world$scriptExtension")
        if (isWindows) {
            assertTrue(applicationDistributionScript.readText().contains("""set DEFAULT_JVM_OPTS="-Xmx256m""""))
        } else {
            assertTrue(applicationDistributionScript.readText().contains("""DEFAULT_JVM_OPTS="-Xmx256m"""))
        }

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
                "id(\"com.ryandens.java-conventions\")",
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

        // On Windows a `.bat` file is not a directly executable image, so it cannot be launched via
        // ProcessBuilder/CreateProcess (which is what Gradle's Exec task uses). It must be run through the
        // command interpreter instead, e.g. `cmd /c hello-world.bat`.
        val execCommandLine =
            if (isWindows) {
                "'cmd', '/c', 'hello-world.bat'"
            } else {
                "'./hello-world'"
            }
        // Escape backslashes and quotes so a Windows JAVA_HOME embeds cleanly in the generated Groovy string literal.
        val javaHome =
            Jvm
                .current()
                .getJavaHome()
                .toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
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
                    commandLine $execCommandLine
                    environment JAVA_HOME: "$javaHome"
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
        if (isWindows) {
            // Keep the TestKit Gradle home short. Its default lives under the deeply nested
            // build/tmp/functionalTest/work/.gradle-test-kit, which pushes cached agent jars -- e.g. the
            // OpenTelemetry javaagent at caches/modules-2/files-2.1/.../<sha1>/opentelemetry-javaagent-<ver>.jar
            // -- past Windows' 260-character MAX_PATH. The JVM's native appendToBootstrapClassLoaderSearch
            // then rejects the path with IllegalArgumentException and the agent fails to install. Rooting the
            // TestKit home at the drive root keeps the full agent jar path comfortably within the limit.
            val driveRoot =
                functionalTestDir.absoluteFile
                    .toPath()
                    .root
                    .toFile()
            runner.withTestKitDir(File(driveRoot, "gtk"))
        }
        return runner.build()
    }
}
