package com.ryandens.javaagent

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

/**
 * A simple functional test for the 'com.ryandens.javaagent.attach' plugin.
 */
class JavaagentPluginPluginFunctionalTest {
    @Test fun `can attach to application run task`() {
        // Setup the test build
        val projectDir = File("build/functionalTest")
        projectDir.mkdirs()
        val helloWorldDir = File(projectDir, "hello-world")
        File("src/functionalTest/resources/hello-world-project/").copyRecursively(helloWorldDir)
        File("../simple-agent/").copyRecursively(File(projectDir, "simple-agent"))

        projectDir.resolve("settings.gradle").writeText("""
        rootProject.name = 'example'
        include('hello-world')
        include('simple-agent')
        """)
        helloWorldDir.resolve("build.gradle").writeText("""
            plugins {
                id('application')
                id('com.ryandens.javaagent-application')
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
            
            dependencies {
                javaagent project(':simple-agent')
            }
        """)

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("build", "run")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        // Verify the result
        assertTrue(result.output.contains("Hello World!"))
        assertTrue(result.output.contains("Hello from my simple agent!"))
    }
}
