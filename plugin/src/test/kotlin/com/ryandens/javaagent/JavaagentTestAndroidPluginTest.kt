package com.ryandens.javaagent

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A simple unit test for the 'com.ryandens.javaagent-test' plugin.
 */
class JavaagentTestAndroidPluginTest {
    private lateinit var project: Project

    @BeforeTest fun beforeEach() {
        project = ProjectBuilder.builder().build()
    }

    @Test fun `plugin adds jvm argument for Android App unit tests`() {
        project.plugins.apply("com.android.application")
        project.plugins.apply("com.ryandens.javaagent-test-android")
        project.dependencies.add("testJavaagent", "io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")

        project.afterEvaluate {
            assertEquals(
                1,
                project.tasks
                    .named("testDebugUnitTest", org.gradle.api.tasks.testing.Test::class.java)
                    .get()
                    .jvmArgumentProviders.size,
                "Failed to attach to task testDebugUnitTest"
            )
            assertEquals(
                1,
                project.tasks
                    .named("testReleaseUnitTest", org.gradle.api.tasks.testing.Test::class.java)
                    .get()
                    .jvmArgumentProviders.size,
                "Failed to attach to task testReleaseUnitTest"
            )

            assertEquals(
                0,
                project.tasks
                    .named("connectedDebugAndroidTest", org.gradle.api.tasks.testing.Test::class.java)
                    .get()
                    .jvmArgumentProviders.size,
                "Unexpectedly attached to task connectedDebugAndroidTest"
            )
            assertEquals(
                0,
                project.tasks
                    .named("connectedAndroidTest", org.gradle.api.tasks.testing.Test::class.java)
                    .get()
                    .jvmArgumentProviders.size,
                "Unexpectedly attached to task connectedAndroidTest"
            )
        }
    }

    @Test fun `plugin adds jvm argument for Android Library unit tests`() {
        project.plugins.apply("com.android.library")
        project.plugins.apply("com.ryandens.javaagent-test-android")
        project.dependencies.add("testJavaagent", "io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")

        project.afterEvaluate {
            assertEquals(
                1,
                project.tasks
                    .named("testDebugUnitTest", org.gradle.api.tasks.testing.Test::class.java)
                    .get()
                    .jvmArgumentProviders.size,
                "Failed to attach to task testDebugUnitTest"
            )
            assertEquals(
                1,
                project.tasks
                    .named("testReleaseUnitTest", org.gradle.api.tasks.testing.Test::class.java)
                    .get()
                    .jvmArgumentProviders.size,
                "Failed to attach to task testReleaseUnitTest"
            )

            assertEquals(
                0,
                project.tasks
                    .named("connectedDebugAndroidTest", org.gradle.api.tasks.testing.Test::class.java)
                    .get()
                    .jvmArgumentProviders.size,
                "Unexpectedly attached to task connectedDebugAndroidTest"
            )
            assertEquals(
                0,
                project.tasks
                    .named("connectedAndroidTest", org.gradle.api.tasks.testing.Test::class.java)
                    .get()
                    .jvmArgumentProviders.size,
                "Unexpectedly attached to task connectedAndroidTest"
            )
        }
    }
}
