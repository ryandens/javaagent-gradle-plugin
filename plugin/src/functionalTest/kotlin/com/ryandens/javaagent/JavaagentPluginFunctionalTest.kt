package com.ryandens.javaagent

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Paths
import java.util.zip.ZipFile
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

        // The run task adds the agent jars via a CommandLineArgumentProvider that is not itself a tracked
        // input, so the plugin registers the javaagent configuration as a task input to make Gradle schedule
        // :simple-agent:jar before :hello-world:run. Without that, the run task can launch with
        // -javaagent:<jar> before that jar has been produced, failing with "Error opening zip file or JAR
        // manifest missing" (the `can attach to application run task` test above reproduces that
        // intermittently under parallel scheduling). --dry-run prints the task execution graph in order
        // without running anything, so asserting the ordering here catches the regression deterministically,
        // independent of scheduling timing.
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
            // Each option is its own backslash-escaped quoted token inside the outer-quoted value, so the
            // launcher never word-splits an option (including ones whose value has a space) -- see issue #95.
            val expectedDefaultJavaOpts = """
DEFAULT_JVM_OPTS="\"-javaagent:${"$"}APP_HOME/agent-libs/simple-agent.jar\" \"-Xmx256m\""
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

    @Test fun `preserves applicationDefaultJvmArgs containing spaces in application distribution`() {
        // Regression test for https://github.com/ryandens/javaagent-gradle-plugin/issues/95: applying the
        // plugin must not drop or mangle the JVM args a consumer configures via `applicationDefaultJvmArgs`.
        // A value containing a space (`-Dgreeting=hello world`) is the sharp edge: the generated start script
        // must keep each option as its own quoted token so the shell does not word-split "hello world" into
        // two arguments. HelloWorld echoes the `greeting` system property back so we can assert end-to-end
        // that the argument actually reaches the JVM intact.
        val dependencies = """
            javaagent project(':simple-agent')
        """

        createJavaagentProject(dependencies, applicationDefaultJvmArgs = "['-Dgreeting=hello world', '-Xmx256m']")
        val result = runBuild(listOf("build", "installDist", "execStartScript"))

        // the agent is still attached
        assertTrue(result.output.contains("Hello from my simple agent!"))
        // and the space-containing default JVM arg survived intact rather than being word-split
        assertTrue(
            result.output.contains("greeting=[hello world]"),
            "expected the space-containing applicationDefaultJvmArg to reach the JVM as a single argument, but the run output was:\n${result.output}",
        )
    }

    @Test fun `can pass agent options to application run task`() {
        val dependencies = """
            runJavaagent(project(':simple-agent')) {
                attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, '12345:config.yaml') }
            }
        """

        createJavaagentProject(dependencies)
        val result = runBuild(listOf("assemble", "run"))

        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))
        // the simple agent echoes the agentArgs it was launched with (the `=<options>` portion)
        assertTrue(result.output.contains("Simple agent args: 12345:config.yaml"))
    }

    @Test fun `can configure independent options in each plugin configuration`() {
        createJavaagentProject("")
        // Publish the fixture agent locally so the consumer uses the external module overload,
        // exactly as in the documented Kotlin DSL example.
        File(functionalTestDir, "simple-agent/build.gradle.kts").appendText(
            """

            apply(plugin = "maven-publish")
            group = "com.ryandens.fixture"
            version = "1.0"
            configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("agent") { from(components["java"]) }
                }
                repositories { maven { url = uri(rootProject.file("repo")) } }
            }
            """,
        )
        runBuild(listOf(":simple-agent:publish"))
        helloWorldDir.resolve("build.gradle").delete()
        helloWorldDir.resolve("build.gradle.kts").writeText(
            """
            import com.ryandens.javaagent.agentOptions

            plugins {
                application
                id("com.ryandens.javaagent-application")
                id("com.ryandens.javaagent-test")
            }
            repositories {
                maven { url = uri(rootProject.file("repo")) }
                mavenCentral()
            }
            application { mainClass.set("com.ryandens.HelloWorld") }
            dependencies {
                javaagent("com.ryandens.fixture:simple-agent:1.0") {
                    agentOptions = "shared-options"
                }
                runJavaagent("com.ryandens.fixture:simple-agent:1.0") {
                    agentOptions = "12345:config.yaml"
                }
                distributionJavaagent("com.ryandens.fixture:simple-agent:1.0") {
                    agentOptions = "distribution-options"
                }
                testJavaagent("com.ryandens.fixture:simple-agent:1.0") {
                    agentOptions = "test-options"
                }
                testImplementation(platform("org.junit:junit-bom:5.11.2"))
                testImplementation("org.junit.jupiter:junit-jupiter")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }
            tasks.test {
                useJUnitPlatform()
                testLogging.showStandardStreams = true
            }
            """,
        )
        val arguments =
            listOf("--configuration-cache", ":hello-world:run", ":hello-world:test", ":hello-world:distTar", ":hello-world:distZip")
        val result = runBuild(arguments)
        assertTrue(result.output.contains("Simple agent args: 12345:config.yaml"))
        assertTrue(result.output.contains("Simple agent args: test-options"))
        assertFalse(result.output.contains("Simple agent args: shared-options"))
        val script = helloWorldDir.resolve("build/scripts/hello-world$scriptExtension").readText()
        assertTrue(script.contains("distribution-options"))
        assertFalse(script.contains("12345:config.yaml"))
        assertFalse(script.contains("test-options"))
        assertFalse(script.contains("shared-options"))
        ZipFile(helloWorldDir.resolve("build/distributions/hello-world.zip")).use { zip ->
            val launcher = zip.getInputStream(zip.getEntry("hello-world/bin/hello-world$scriptExtension")).bufferedReader().readText()
            assertEquals(script, launcher)
        }
        var tarLauncher: String? = null
        TarArchiveInputStream(
            helloWorldDir.resolve("build/distributions/hello-world.tar").inputStream(),
        ).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                if (entry.name == "hello-world/bin/hello-world$scriptExtension") {
                    tarLauncher = tar.readBytes().toString(Charsets.UTF_8)
                }
                entry = tar.nextEntry
            }
        }
        assertEquals(script, tarLauncher)
        val cachedResult = runBuild(arguments)
        assertTrue(cachedResult.output.contains("Reusing configuration cache."))
        assertTrue(cachedResult.output.contains("Simple agent args: 12345:config.yaml"))
        assertEquals(TaskOutcome.UP_TO_DATE, cachedResult.task(":hello-world:test")?.outcome)
        val buildScript = helloWorldDir.resolve("build.gradle.kts")
        buildScript.writeText(buildScript.readText().replace("test-options", "changed-test-options"))
        val changedResult = runBuild(arguments)
        assertEquals(TaskOutcome.SUCCESS, changedResult.task(":hello-world:test")?.outcome)
        assertTrue(changedResult.output.contains("Simple agent args: changed-test-options"))
        assertTrue(changedResult.output.contains("Simple agent args: 12345:config.yaml"))
        assertEquals(TaskOutcome.UP_TO_DATE, changedResult.task(":hello-world:startScripts")?.outcome)

        assertEquals(TaskOutcome.UP_TO_DATE, changedResult.task(":hello-world:distTar")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, changedResult.task(":hello-world:distZip")?.outcome)
        buildScript.writeText(buildScript.readText().replace("distribution-options", "changed-distribution-options"))
        val changedDistribution = runBuild(arguments)
        assertEquals(TaskOutcome.SUCCESS, changedDistribution.task(":hello-world:startScripts")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, changedDistribution.task(":hello-world:distTar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, changedDistribution.task(":hello-world:distZip")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, changedDistribution.task(":hello-world:test")?.outcome)
        assertTrue(changedDistribution.output.contains("Simple agent args: 12345:config.yaml"))

        // Options follow the selected artifact even when a module is substituted with a project.
        buildScript.appendText(
            """

            configurations.all {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("com.ryandens.fixture:simple-agent")).using(project(":simple-agent"))
                }
            }
            """,
        )
        val substitutedResult = runBuild(arguments)
        assertTrue(substitutedResult.output.contains("Simple agent args: changed-test-options"))
        assertTrue(substitutedResult.output.contains("Simple agent args: 12345:config.yaml"))
    }

    @Test fun `run only agents are absent from tests and packaged distributions`() {
        createJavaagentProject(
            """
            runJavaagent(project(':simple-agent')) {
                attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, 'local-only') }
            }
            """,
        )
        val result = runBuild(listOf("--configuration-cache", "run", "test", "distZip", "distTar"))
        assertEquals(1, result.output.lineSequence().count { it == "Hello from my simple agent!" })
        assertTrue(result.output.contains("Simple agent args: local-only"))
        assertFalse(helloWorldDir.resolve("build/scripts/hello-world$scriptExtension").readText().contains("-javaagent:"))
        ZipFile(helloWorldDir.resolve("build/distributions/hello-world.zip")).use { zip ->
            assertFalse(zip.entries().asSequence().any { it.name.contains("agent-libs/") })
        }
    }

    @Test fun `can configure options on a project agent dependency`() {
        createJavaagentProject("")
        helloWorldDir.resolve("build.gradle").delete()
        helloWorldDir.resolve("build.gradle.kts").writeText(
            """
            import com.ryandens.javaagent.agentOptions

            plugins {
                application
                id("com.ryandens.javaagent-application")
            }
            application { mainClass.set("com.ryandens.HelloWorld") }
            dependencies {
                javaagent(project(":simple-agent")) {
                    agentOptions = "project-options"
                }
            }
            """,
        )
        val result = runBuild(listOf("--configuration-cache", ":hello-world:run"))
        assertTrue(result.output.contains("Simple agent args: project-options"))
        val cachedResult = runBuild(listOf("--configuration-cache", ":hello-world:run"))
        assertTrue(cachedResult.output.contains("Reusing configuration cache."))
        assertTrue(cachedResult.output.contains("Simple agent args: project-options"))

        val buildScript = helloWorldDir.resolve("build.gradle.kts")
        buildScript.writeText(buildScript.readText().replace("agentOptions = \"project-options\"", "agentOptions = \"\""))
        val emptyResult = runBuild(listOf(":hello-world:run"))
        assertTrue(emptyResult.output.lineSequence().any { it.trimEnd() == "Simple agent args:" })

        buildScript.appendText(
            """

            dependencies {
                javaagent(project(":simple-agent")) { agentOptions = "conflicting-options" }
            }
            """,
        )
        val failedResult = runBuild(listOf(":hello-world:run"), expectFailure = true)
        assertTrue(failedResult.output.contains("Conflicting javaagent options for"))
    }

    @Test fun `passes agent options only to their dependency`() {
        // Only the project agent has options; the other agent must not receive them.
        val otelVersion = "2.29.0"
        val dependencies = """
            javaagent(project(':simple-agent')) {
                attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, '12345:config.yaml') }
            }
            javaagent 'io.opentelemetry.javaagent:opentelemetry-javaagent:$otelVersion'
        """

        createJavaagentProject(dependencies)
        val result = runBuild(listOf("assemble", "run"))

        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Simple agent args: 12345:config.yaml"))
        // the OpenTelemetry agent still installs; it simply receives no options
        assertTrue(
            result.output.contains("io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: $otelVersion"),
        )
    }

    @Test fun `can pass agent options to application distribution start script`() {
        val dependencies = """
            distributionJavaagent(project(':simple-agent')) {
                attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, '12345:config.yaml') }
            }
        """

        createJavaagentProject(dependencies)
        val result = runBuild(listOf("--configuration-cache", "build", "installDist", "execStartScript"))

        val applicationDistributionScript =
            File(functionalTestDir, "hello-world${File.separator}build${File.separator}scripts${File.separator}hello-world$scriptExtension")
        if (isWindows) {
            val expectedWindowsDefaultJvmOpts =
                """set DEFAULT_JVM_OPTS="-javaagent:%APP_HOME%\agent-libs\simple-agent.jar=12345:config.yaml" "-Xmx256m""""
            assertTrue(applicationDistributionScript.readText().contains(expectedWindowsDefaultJvmOpts))
        } else {
            // The option string rides inside the agent's own backslash-escaped quoted token -- see issue #95.
            val expectedDefaultJavaOpts =
                """
DEFAULT_JVM_OPTS="\"-javaagent:${"$"}APP_HOME/agent-libs/simple-agent.jar=12345:config.yaml\" \"-Xmx256m\""
"""
            assertTrue(applicationDistributionScript.readText().contains(expectedDefaultJavaOpts))
        }

        // Verify the option was actually applied at runtime
        assertTrue(result.output.contains("Simple agent args: 12345:config.yaml"))
    }

    @Test fun `distribution options preserve shell special characters`() {
        val options =
            if (isWindows) {
                "hello world %PATH% ^ & |"
            } else {
                """hello world ${"$"}HOME ${"$"}(touch agent-options-expanded) `touch agent-options-backticks` "double" 'single' \ tail"""
            }
        val groovyOptions = options.replace("\\", "\\\\").replace("'", "\\'")
        createJavaagentProject(
            """
            distributionJavaagent(project(':simple-agent')) {
                attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, '$groovyOptions') }
            }
            """,
        )
        val result = runBuild(listOf("installDist", "execStartScript"))
        assertTrue(result.output.contains("Simple agent args: $options"), result.output)
        val bin = helloWorldDir.resolve("build/install/hello-world/bin")
        assertFalse(bin.resolve("agent-options-expanded").exists())
        assertFalse(bin.resolve("agent-options-backticks").exists())
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
            // With no agents, the placeholder token is stripped but the remaining option keeps its own
            // backslash-escaped quotes inside the outer-quoted value: DEFAULT_JVM_OPTS="\"-Xmx256m\""
            assertTrue(applicationDistributionScript.readText().contains("DEFAULT_JVM_OPTS=\"\\\"-Xmx256m\\\"\""))
        }

        assertFalse(
            File(
                functionalTestDir,
                "hello-world${File.separator}build${File.separator}install${File.separator}hello-world${File.separator}agent-libs/",
            ).exists(),
        )
        assertTrue(result.output.contains("Hello World!"))
    }

    private fun createJavaagentProject(
        dependencies: String,
        applicationDefaultJvmArgs: String = "['-Xmx256m']",
    ) {
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
                    applicationDefaultJvmArgs = $applicationDefaultJvmArgs
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

    private fun runBuild(
        buildArgs: List<String>,
        expectFailure: Boolean = false,
    ): BuildResult {
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
        return if (expectFailure) runner.buildAndFail() else runner.build()
    }
}
