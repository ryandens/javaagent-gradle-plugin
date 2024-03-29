package com.ryandens.javaagent

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'com.ryandens.javaagent.attach' plugin.
 */
class JavaagentApplicationPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("application")
        project.plugins.apply("com.ryandens.javaagent-application")

        // Verify the result
        assertNotNull(project.configurations.findByName("javaagent"))
    }
}
