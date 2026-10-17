package com.ryandens.javaagent.android

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import javax.tools.ToolProvider
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaagentTestAndroidPluginFunctionalTest {
    private val directory = Files.createTempDirectory("android-javaagent-").toFile()
    private val module = directory.resolve("android-module")

    @AfterTest fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test fun `application variants infer their own versions and reuse the configuration cache`() {
        verifyVariants("com.android.application", agpVersion, agentFirst = false)
    }

    @Test fun `library variants support either plugin order`() {
        verifyVariants("com.android.library", agpVersion, agentFirst = true)
    }

    @Test fun `library variants support isolated projects`() {
        verifyVariants("com.android.library", agpVersion, agentFirst = true, isolated = true)
    }

    @Test fun `help neither realizes test tasks nor resolves agents`() {
        createProject("com.android.application", agpVersion, agentFirst = true)
        module.resolve("build.gradle").appendText(
            """

            tasks.withType(Test).configureEach { throw new GradleException('Test task realized by help') }
            configurations.matching { it.name.endsWith('Javaagent') || it.name == 'javaagent' }.configureEach {
                incoming.beforeResolve { throw new GradleException('Agent resolved by help') }
            }
            """,
        )
        runBuild("help")
        assertTrue(runBuild("help").output.contains("Reusing configuration cache."))
    }

    @Test fun `plugin does not apply Android or Java plugins implicitly`() {
        directory.resolve("settings.gradle").writeText("rootProject.name = 'no-android'\n")
        directory.resolve("build.gradle").writeText(
            """
            plugins { id('com.ryandens.javaagent-test-android') }
            assert !plugins.hasPlugin('java')
            assert !plugins.hasPlugin('com.android.application')
            assert !plugins.hasPlugin('com.android.library')
            """,
        )
        runBuild("help")
    }

    @Test fun `extension can disable agents without resolving them`() {
        verifyDisabled("javaagentTest { enabled = false }", emptyArray())
    }

    @Test fun `tests can run without any agents`() {
        createProject("com.android.library", agpVersion, agentFirst = false)
        writeTest(null)
        runBuild(":android-module:testDemoDebugUnitTest")
    }

    @Test fun `project property can disable agents without resolving them`() {
        verifyDisabled("javaagentTest { enabled = true }", arrayOf("-PjavaagentTestEnabled=false"))
    }

    @Test fun `project agent jar is built before Android tests`() {
        createProject("com.android.library", agpVersion, agentFirst = false)
        directory.resolve("settings.gradle").appendText("\ninclude(':agent')\n")
        val agent = directory.resolve("agent")
        agent.mkdirs()
        agent.resolve("build.gradle").writeText(
            """
            plugins { id('java') }
            tasks.jar {
                manifest.attributes('Premain-Class': 'fixture.Agent', 'Implementation-Version': 'project')
            }
            """,
        )
        val source = agent.resolve("src/main/java/fixture/Agent.java")
        source.parentFile.mkdirs()
        source.writeText(agentSource)
        module.resolve("build.gradle").appendText(
            """

            dependencies {
                javaagent(project(':agent')) {
                    attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, 'android-options') }
                }
            }
            """,
        )
        writeTest("android-options")
        val result = runBuild(":android-module:testDemoStagingUnitTest", "--parallel", "-Dorg.gradle.unsafe.isolated-projects=true")
        assertEquals(TaskOutcome.SUCCESS, result.task(":agent:jar")?.outcome)
        assertTrue(result.output.contains("Fixture agent: project=android-options"))
    }

    private fun verifyVariants(
        androidPlugin: String,
        agpVersion: String,
        agentFirst: Boolean,
        isolated: Boolean = false,
    ) {
        createProject(androidPlugin, agpVersion, agentFirst)
        publishAgent("1.0")
        publishAgent("2.0")
        module.resolve("build.gradle").appendText(
            """

            dependencies {
                testImplementation(platform('fixture:agent-bom:1.0'))
                testImplementation('fixture:test-agent')
                javaagent('fixture:test-agent') {
                    attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, 'shared-options') }
                }
                testJavaagent('fixture:test-agent') {
                    attributes { attribute(com.ryandens.javaagent.AgentOptions.OPTIONS, 'android-options') }
                }
            }
            androidComponents.onVariants(androidComponents.selector().all()) { variant ->
                variant.hostTests.values().each { host ->
                    if (variant.buildType == 'staging') {
                        host.runtimeConfiguration.resolutionStrategy.force('fixture:test-agent:2.0')
                    }
                }
            }
            """,
        )
        writeTest("android-options")
        val arguments =
            listOf(":android-module:testDemoDebugUnitTest", ":android-module:testDemoStagingUnitTest", "--parallel", "--build-cache") +
                if (isolated) listOf("-Dorg.gradle.unsafe.isolated-projects=true") else emptyList()
        val result = runBuild(*arguments.toTypedArray())
        assertTrue(result.output.contains("Fixture agent: 1.0=android-options"))
        assertTrue(result.output.contains("Fixture agent: 2.0=android-options"))
        assertFalse(result.output.contains("Fixture agent: 1.0=shared-options"))
        val cached = runBuild(*arguments.toTypedArray())
        assertTrue(cached.output.contains("Reusing configuration cache."), cached.output)
        assertEquals(TaskOutcome.UP_TO_DATE, cached.task(":android-module:testDemoDebugUnitTest")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, cached.task(":android-module:testDemoStagingUnitTest")?.outcome)
        module.resolve("build").deleteRecursively()
        val restored = runBuild(*arguments.toTypedArray())
        assertEquals(TaskOutcome.FROM_CACHE, restored.task(":android-module:testDemoDebugUnitTest")?.outcome)
        assertEquals(TaskOutcome.FROM_CACHE, restored.task(":android-module:testDemoStagingUnitTest")?.outcome)
    }

    private fun verifyDisabled(
        extension: String,
        arguments: Array<String>,
    ) {
        createProject("com.android.library", agpVersion, agentFirst = true)
        module.resolve("build.gradle").appendText(
            """

            $extension
            dependencies { testJavaagent('missing:must-not-resolve:1.0') }
            configurations.matching { it.name.endsWith('Javaagent') }.configureEach {
                incoming.beforeResolve { throw new GradleException('Disabled agent was resolved') }
            }
            """,
        )
        writeTest(null)
        runBuild(":android-module:testDemoDebugUnitTest", *arguments)
    }

    private fun createProject(
        androidPlugin: String,
        agpVersion: String,
        agentFirst: Boolean,
    ) {
        val sdk = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        require(sdk != null && File(sdk, "platforms/android-36/android.jar").isFile) {
            "Android functional tests require ANDROID_HOME with platforms;android-36 and build-tools;36.0.0 installed"
        }
        directory.resolve("settings.gradle").writeText(
            """
            pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
            dependencyResolutionManagement {
                repositories { maven { url = uri('repo') }; google(); mavenCentral() }
            }
            rootProject.name = 'android-agent-tests'
            include(':android-module')
            """,
        )
        module.mkdirs()
        val android = "id('$androidPlugin') version '$agpVersion'"
        val javaagent = "id('com.ryandens.javaagent-test-android')"
        module.resolve("build.gradle").writeText(
            """
            import com.android.build.api.variant.HostTestBuilder
            plugins { ${if (agentFirst) "$javaagent; $android" else "$android; $javaagent"} }
            android {
                namespace = 'fixture.android'
                compileSdk = 36
                buildToolsVersion = '36.0.0'
                defaultConfig { minSdk = 23 }
                flavorDimensions += 'tier'
                productFlavors { demo { dimension = 'tier' } }
                buildTypes { staging { initWith(debug) } }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                testOptions { unitTests.all { testLogging.showStandardStreams = true } }
            }
            androidComponents.beforeVariants(androidComponents.selector().all()) { builder ->
                builder.hostTests[HostTestBuilder.UNIT_TEST_TYPE].enable = true
                if (builder.buildType == 'release') { builder.enable = false }
            }
            dependencies { testImplementation('junit:junit:4.13.2') }
            assert !plugins.hasPlugin('java')
            """,
        )
        val manifest = module.resolve("src/main/AndroidManifest.xml")
        manifest.parentFile.mkdirs()
        manifest.writeText("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" />")
    }

    private fun writeTest(options: String?) {
        val test = module.resolve("src/test/java/fixture/AgentTest.java")
        test.parentFile.mkdirs()
        val expected = options?.let { "\"$it\"" } ?: "null"
        test.writeText(
            """
            package fixture;
            public class AgentTest {
                @org.junit.Test public void agentIsConfigured() {
                    org.junit.Assert.assertEquals($expected, System.getProperty("fixture.agent.options"));
                }
            }
            """,
        )
    }

    private fun publishAgent(version: String) {
        val source = directory.resolve("agent-source/fixture/Agent.java")
        source.parentFile.mkdirs()
        source.writeText(agentSource)
        val classes = directory.resolve("agent-classes").apply { mkdirs() }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-d", classes.path, source.path))
        val artifact = directory.resolve("repo/fixture/test-agent/$version/test-agent-$version")
        artifact.parentFile.mkdirs()
        val manifest = Manifest()
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        manifest.mainAttributes[Attributes.Name("Premain-Class")] = "fixture.Agent"
        manifest.mainAttributes[Attributes.Name.IMPLEMENTATION_VERSION] = version
        JarOutputStream(File("${artifact.path}.jar").outputStream(), manifest).use { jar ->
            jar.putNextEntry(JarEntry("fixture/Agent.class"))
            jar.write(classes.resolve("fixture/Agent.class").readBytes())
            jar.closeEntry()
        }
        File("${artifact.path}.pom").writeText(
            "<project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId><artifactId>test-agent</artifactId><version>$version</version></project>",
        )
        val bom = directory.resolve("repo/fixture/agent-bom/1.0/agent-bom-1.0.pom")
        bom.parentFile.mkdirs()
        bom.writeText(
            """
            <project><modelVersion>4.0.0</modelVersion>
              <groupId>fixture</groupId><artifactId>agent-bom</artifactId><version>1.0</version><packaging>pom</packaging>
              <dependencyManagement><dependencies><dependency>
                <groupId>fixture</groupId><artifactId>test-agent</artifactId><version>1.0</version>
              </dependency></dependencies></dependencyManagement>
            </project>
            """.trimIndent(),
        )
    }

    private val agpVersion: String
        get() =
            GradleRunner
                .create()
                .withPluginClasspath()
                .pluginClasspath
                .single { it.name.startsWith("gradle-api-") && it.path.contains("com.android.tools.build") }
                .name
                .removePrefix("gradle-api-")
                .removeSuffix(".jar")

    private fun runBuild(vararg arguments: String): BuildResult {
        val runner =
            GradleRunner
                .create()
                .withProjectDir(directory)
                .withPluginClasspath()
                .withArguments(*arguments, "--configuration-cache", "--stacktrace")
                .forwardOutput()
        // AGP 8 uses a Gradle internal API removed in 9.6; exercise its supported Gradle line.
        if (agpVersion.startsWith("8.")) {
            runner.withGradleVersion("9.5.0")
        }
        return runner.build()
    }

    private val agentSource =
        """
        package fixture;
        public class Agent {
            public static void premain(String options, java.lang.instrument.Instrumentation instrumentation) {
                System.setProperty("fixture.agent.options", options == null ? "<none>" : options);
                System.out.println("Fixture agent: " + Agent.class.getPackage().getImplementationVersion() + "=" + options);
            }
        }
        """
}
