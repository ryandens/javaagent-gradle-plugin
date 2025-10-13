package com.ryandens.javaagent.otel

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class MergeServiceFilesTest {

    @Test
    fun `merging is deterministic when inputs are shuffled`(@TempDir tempDir: Path) {
        val projectDir = tempDir.resolve("project").toFile().apply { mkdirs() }
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val task = project.tasks.create("merge", MergeServiceFiles::class.java)

        val outputBaseDir = tempDir.resolve("output").toFile()
        task.outputDirectory.set(outputBaseDir)

        val inputsRoot = tempDir.resolve("inputs").toFile().apply { mkdirs() }
        val firstInput = File(inputsRoot, "b-input").apply { mkdirs() }
        val secondInput = File(inputsRoot, "a-input").apply { mkdirs() }
        val thirdInput = File(inputsRoot, "c-input").apply { mkdirs() }

        firstInput.resolve("META-INF/services/example.Service").apply {
            parentFile.mkdirs()
            writeText("impl-from-b")
        }

        secondInput.resolve("META-INF/services/example.Service").apply {
            parentFile.mkdirs()
            writeText("impl-from-a")
        }

        thirdInput.resolve("inst/META-INF/services/example.SecondService").apply {
            parentFile.mkdirs()
            writeText("impl-from-c")
        }

        fun executeWithInputs(vararg directories: File): Map<String, List<String>> {
            task.inputFiles.setFrom(directories.toList())
            task.execute()

            val servicesDir = File(outputBaseDir, "META-INF/services")
            return servicesDir.listFiles()?.sortedBy { it.name }?.associate { it.name to it.readLines() } ?: emptyMap()
        }

        val firstResult = executeWithInputs(firstInput, secondInput, thirdInput)
        val secondResult = executeWithInputs(thirdInput, firstInput, secondInput)

        assertEquals(
            mapOf(
                "example.SecondService" to listOf("impl-from-c"),
                "example.Service" to listOf("impl-from-a", "impl-from-b"),
            ),
            firstResult,
        )
        assertEquals(firstResult, secondResult)
    }
}

