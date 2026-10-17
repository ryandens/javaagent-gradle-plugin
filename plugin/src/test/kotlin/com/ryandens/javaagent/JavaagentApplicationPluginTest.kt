package com.ryandens.javaagent

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        assertNotNull(project.configurations.findByName("applicationRunJavaagent"))
        assertNotNull(project.configurations.findByName("applicationDistributionJavaagent"))
        assertNull(project.extensions.findByName("javaagent"))
    }

    @Test fun `individual plugins create only their own agent configuration`() {
        val plugins =
            mapOf(
                "com.ryandens.javaagent-application-run" to "applicationRunJavaagent",
                "com.ryandens.javaagent-application-distribution" to "applicationDistributionJavaagent",
                "com.ryandens.javaagent-test" to "testJavaagent",
            )
        for ((pluginId, configuration) in plugins) {
            val project = ProjectBuilder.builder().build()
            project.plugins.apply("application")
            project.plugins.apply(pluginId)
            assertNotNull(project.configurations.findByName(configuration))
            for (other in plugins.values.filter { it != configuration }) {
                assertNull(project.configurations.findByName(other))
            }
            assertNull(project.extensions.findByName("javaagent"))
        }
    }
}
