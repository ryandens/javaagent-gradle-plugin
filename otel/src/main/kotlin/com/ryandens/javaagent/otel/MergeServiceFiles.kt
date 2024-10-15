package com.ryandens.javaagent.otel

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.stream.Collectors
import javax.inject.Inject

/**
 * Internal task to merge service files between custom instrumentation JArs and the OpenTelemetry Javaagent.
 *
 * Inspired by
 * [Micronaut](https://github.com/micronaut-projects/micronaut-gradle-plugin/blob/eb9d2b3ab4b3fc71379fc3c1c6df30de761576be/aot-plugin/src/main/java/io/micronaut/gradle/aot/MergeServiceFiles.java#L42)
 * in lieu of a built-in solution in Gradle as referenced [here](https://github.com/gradle/gradle/issues/18751).
 */
@CacheableTask
abstract class MergeServiceFiles
    @Inject
    constructor(private val fileSystemOperations: FileSystemOperations) :
    DefaultTask() {
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val inputFiles: ConfigurableFileCollection

        @get:OutputDirectory
        abstract val outputDirectory: DirectoryProperty

        @TaskAction
        fun execute() {
            val serviceFiles: Set<File> =
                inputFiles.asFileTree.matching { f -> f.include("META-INF/services/**").include("inst/META-INF/services/**") }
                    .files
            val outputDir: File = outputDirectory.get().dir("META-INF/services").asFile
            fileSystemOperations.delete {
                it.delete(outputDir)
            }
            outputDir.mkdirs()
            val perService: Map<String, List<File>> =
                serviceFiles.stream()
                    .collect(Collectors.groupingBy(File::getName))
            for ((serviceType, files) in perService) {
                val mergedServiceFile = File(outputDir, serviceType)
                try {
                    PrintWriter(
                        OutputStreamWriter(
                            FileOutputStream(mergedServiceFile),
                            StandardCharsets.UTF_8,
                        ),
                    ).use { wrt ->
                        for (file in files) {
                            Files.readAllLines(file.toPath()).forEach(wrt::println)
                        }
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }
    }
