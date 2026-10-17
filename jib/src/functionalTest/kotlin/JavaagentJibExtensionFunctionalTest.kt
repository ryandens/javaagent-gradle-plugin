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
        assertTrue(result.output.contains("Running extension: com.ryandens.javaagent.jib.JavaagentJibExtension"))

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

    @Test fun `can pass agent options to jib entrypoint`() {
        val dependencies = """
            jibJavaagent(project(':simple-agent')) {
                attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, '12345:config.yaml') }
            }
            runtimeOnly 'commons-lang:commons-lang:2.6'
        """

        val result =
            createAndBuildJavaagentProject(
                dependencies,
                listOf("jibBuildTar"),
            )

        assertTrue(result.output.contains("Running extension: com.ryandens.javaagent.jib.JavaagentJibExtension"))
        assertTrue(File(functionalTestDir, JIB_IMAGE).exists())

        FileInputStream(File(functionalTestDir, JIB_IMAGE)).use { fis ->
            ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<TarArchiveEntry>>(ArchiveStreamFactory.TAR, fis).use { ais ->
                var entry = ais.nextEntry
                while (entry != null) {
                    if ("config.json" == entry.name) {
                        val json = ais.readBytes().toString(Charsets.UTF_8)
                        assertTrue(json.contains("-javaagent:/opt/jib-agents/simple-agent.jar=12345:config.yaml"))
                    }
                    entry = ais.nextEntry
                }
            }
        }
    }

    @Test fun `can pass dependency scoped options to jib entrypoint`() {
        createAndBuildJavaagentProject(
            "",
            listOf("jibBuildTar"),
            kotlinBuildScript =
                """
                import com.ryandens.javaagent.agentOptions

                plugins {
                    application
                    id("com.google.cloud.tools.jib")
                    id("com.ryandens.javaagent-jib")
                    id("com.ryandens.javaagent-application")
                }
                application { mainClass.set("com.ryandens.HelloWorld") }
                dependencies {
                    runJavaagent(project(":simple-agent")) { agentOptions = "local-only-options" }
                    distributionJavaagent(project(":simple-agent")) { agentOptions = "distribution-only-options" }
                    jibJavaagent(project(":simple-agent")) {
                        agentOptions = "12345:dependency-config.yaml"
                    }
                }
                jib {
                    from { image = "scratch" }
                    to { image = "javaagent-hello-world" }
                    container { mainClass = "com.ryandens.HelloWorld" }
                }
                """,
        )

        var foundConfiguration = false
        FileInputStream(File(functionalTestDir, JIB_IMAGE)).use { fis ->
            ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<TarArchiveEntry>>(ArchiveStreamFactory.TAR, fis).use { ais ->
                var entry = ais.nextEntry
                while (entry != null) {
                    if (entry.name == "config.json") {
                        foundConfiguration = true
                        val json = ais.readBytes().toString(Charsets.UTF_8)
                        assertTrue(json.contains("-javaagent:/opt/jib-agents/simple-agent.jar=12345:dependency-config.yaml"))
                        assertFalse(json.contains("local-only-options"))
                        assertFalse(json.contains("distribution-only-options"))
                    }
                    entry = ais.nextEntry
                }
            }
        }
        assertTrue(foundConfiguration, "The image must contain the configuration with the agent entrypoint")
    }

    @Test fun `works even without any javaagent dependencies`() {
        val dependencies = """
            runtimeOnly 'commons-lang:commons-lang:2.6'
        """

        // create the test project and run the tasks
        val result = createAndBuildJavaagentProject(dependencies, listOf("jibBuildTar"))

        // Verify the result
        assertTrue(result.output.contains("Running extension: com.ryandens.javaagent.jib.JavaagentJibExtension"))

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
        kotlinBuildScript: String? = null,
    ): BuildResult {
        val helloWorldDir = File(functionalTestDir, "hello-world")
        File(
            "src${File.separator}functionalTest${File.separator}resources${File.separator}hello-world-project",
        ).copyRecursively(helloWorldDir)
        val simpleAgentTestDir = File(functionalTestDir, "simple-agent")
        val simpleAgentBuildScript = simpleAgentTestDir.resolve("build.gradle.kts")
        File("..${File.separator}simple-agent").copyRecursively(simpleAgentTestDir)
        // remove a constant from the above file
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

        helloWorldDir.resolve(if (kotlinBuildScript == null) "build.gradle" else "build.gradle.kts").writeText(
            kotlinBuildScript ?: """
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
