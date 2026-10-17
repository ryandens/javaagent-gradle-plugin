package com.ryandens.javaagent

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaagentMultiplatformFunctionalTest {
    private val projectDir = createTempDirectory("javaagent-multiplatform").toFile()

    @AfterTest
    fun cleanup() {
        projectDir.deleteRecursively()
    }

    @Test
    fun `attaches agents to default JVM target and reuses configuration cache`() {
        createProject("jvm()")
        val arguments = listOf("jvmTest", "--configuration-cache")
        val result = runBuild(arguments)
        assertEquals(TaskOutcome.SUCCESS, result.task(":jvmTest")?.outcome)
        assertTrue(result.output.contains("Simple agent args: test-options"))
        assertEquals(1, result.output.lineSequence().count { it == "Hello from my simple agent!" })
        assertTrue(result.output.contains("Configuration cache entry stored."))
        val cached = runBuild(arguments + "--rerun-tasks")
        assertTrue(cached.output.contains("Reusing configuration cache."))
        assertTrue(cached.output.contains("Simple agent args: test-options"))
    }

    @Test
    fun `infers agent version for a named JVM target with javaagent plugin applied first`() {
        createProject("jvm(\"desktop\")", agentPluginFirst = true)
        publishAgent()
        projectDir.resolve("build.gradle.kts").appendText(
            """

            dependencies {
                add("desktopTestRuntimeOnly", "com.ryandens.fixture:published-agent:1.0")
                add("testJavaagent", "com.ryandens.fixture:published-agent") {
                    (this as org.gradle.api.artifacts.ModuleDependency).attributes {
                        attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, "inferred-options")
                    }
                }
            }
            """,
        )
        val arguments = listOf("desktopTest", "--configuration-cache")
        val result = runBuild(arguments)
        assertEquals(TaskOutcome.SUCCESS, result.task(":desktopTest")?.outcome)
        assertTrue(result.output.contains("Simple agent args: inferred-options"))
        assertTrue(result.output.contains("Simple agent args: test-options"))
        val cached = runBuild(arguments + "--rerun-tasks")
        assertTrue(cached.output.contains("Reusing configuration cache."))
        assertTrue(cached.output.contains("Simple agent args: inferred-options"))
    }

    @Test
    fun `can disable agents for multiplatform tests`() {
        createProject("jvm()")
        val result = runBuild(listOf("jvmTest", "-PjavaagentTestEnabled=false"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":jvmTest")?.outcome)
        assertFalse(result.output.contains("Hello from my simple agent!"))
    }

    @Test
    fun `can apply without a JVM target`() {
        createProject("js { nodejs() }")
        projectDir.resolve("build.gradle.kts").appendText(
            """

            check(tasks.findByName("test") == null)
            check(!plugins.hasPlugin("java"))
            """,
        )
        runBuild(listOf("help", "--configuration-cache"))
    }

    private fun createProject(
        target: String,
        agentPluginFirst: Boolean = false,
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
            rootProject.name = "multiplatform-test"
            include(":simple-agent")
            """,
        )
        val plugins =
            listOf(
                "kotlin(\"multiplatform\") version \"2.4.21\"",
                "id(\"com.ryandens.javaagent-test\")",
            ).let { if (agentPluginFirst) it.reversed() else it }
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import com.ryandens.javaagent.agentOptions

            plugins {
                ${plugins.joinToString("\n")}
            }
            repositories {
                mavenCentral()
                maven { url = uri(rootProject.file("repo")) }
            }
            kotlin {
                $target
                sourceSets.commonTest.dependencies { implementation(kotlin("test")) }
            }
            dependencies {
                add("javaagent", project(":simple-agent")) { agentOptions = "shared-options" }
                add("testJavaagent", project(":simple-agent")) { agentOptions = "test-options" }
            }
            """,
        )
        val agentDir = projectDir.resolve("simple-agent").apply { mkdirs() }
        File("../simple-agent/src").copyRecursively(agentDir.resolve("src"))
        agentDir.resolve("build.gradle.kts").writeText(
            """
            plugins { `java-library`; `maven-publish` }
            group = "com.ryandens.fixture"
            version = "1.0"
            tasks.jar { manifest { attributes("Premain-Class" to "com.ryandens.SimpleAgent") } }
            publishing {
                publications {
                    create<MavenPublication>("agent") {
                        artifactId = "published-agent"
                        from(components["java"])
                    }
                }
                repositories { maven { url = uri(rootProject.file("repo")) } }
            }
            """,
        )
        projectDir.resolve("src/commonTest/kotlin/ExampleTest.kt").apply {
            parentFile.mkdirs()
            writeText(
                """
                import kotlin.test.Test
                import kotlin.test.assertEquals

                class ExampleTest {
                    @Test fun example() { assertEquals(4, 2 + 2) }
                }
                """,
            )
        }
    }

    private fun publishAgent() {
        runBuild(listOf(":simple-agent:publish"))
    }

    private fun runBuild(arguments: List<String>) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(arguments + "--stacktrace")
            .forwardOutput()
            .build()
}
