package com.ryandens.javaagent

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * A simple unit test for the 'com.ryandens.javaagent-test' plugin.
 */
class JavaagentTestPluginTest {
    private lateinit var project: Project

    /**
     * Sets up a project with a single javaagent dependency and the javaagent test plugin applied
     */
    @BeforeTest fun beforeEach() {
        project = ProjectBuilder.builder().build()
        project.plugins.apply("java")
        project.plugins.apply("com.ryandens.javaagent-test")
        project.dependencies.add("javaagent", "io.opentelemetry.javaagent:opentelemetry-javaagent:1.11.1")
    }

    @Test fun `plugin adds jvm argument provider for javaagent`() {
        assertEquals(
            1,
            project.tasks
                .named("test", org.gradle.api.tasks.testing.Test::class.java)
                .get()
                .jvmArgumentProviders.size,
        )
    }

    @Test fun `plugin javaagent test configuration can be disabled`() {
        project.extensions.configure(JavaagentTestExtension::class.java) {
            it.enabled.set(false)
        }

        assertEquals(
            0,
            project.tasks
                .named("test", org.gradle.api.tasks.testing.Test::class.java)
                .get()
                .jvmArgumentProviders.size,
        )
    }

    @Test fun `plugin can be applied before a JVM plugin`() {
        val pluginFirstProject = ProjectBuilder.builder().build()
        pluginFirstProject.plugins.apply("com.ryandens.javaagent-test")
        assertFalse(pluginFirstProject.plugins.hasPlugin("java"))
        pluginFirstProject.plugins.apply("java")
        assertEquals(
            1,
            pluginFirstProject.tasks
                .named("test", org.gradle.api.tasks.testing.Test::class.java)
                .get()
                .jvmArgumentProviders.size,
        )
    }
}
