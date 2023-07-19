package com.ryandens.javaagent

import org.gradle.api.Transformer
import org.gradle.api.internal.plugins.DefaultTemplateBasedStartScriptGenerator
import org.gradle.api.internal.plugins.StartScriptTemplateBindingFactory
import org.gradle.api.provider.Provider
import org.gradle.jvm.application.scripts.JavaAppStartScriptGenerationDetails
import org.gradle.jvm.application.scripts.ScriptGenerator
import java.io.File
import java.io.Writer

class JavaagentAwareStartScriptGenerator(
    private val javaagentConfiguration: Provider<Set<File>>,
    private val platform: Platform,
    private val inner: ScriptGenerator =
        DefaultTemplateBasedStartScriptGenerator(
            platform.lineSeparator,
            FakeTransformer(platform.templateBindingFactory),
            platform.template,
        ),
) : ScriptGenerator {
    override fun generateScript(
        details: JavaAppStartScriptGenerationDetails,
        destination: Writer,
    ) {
        inner.generateScript(details, Fake(destination, javaagentConfiguration, platform.pathSeparator))
    }

    private class FakeTransformer(
        private val inner: StartScriptTemplateBindingFactory,
    ) : Transformer<MutableMap<String, String>, JavaAppStartScriptGenerationDetails> by inner {
        override fun transform(`in`: JavaAppStartScriptGenerationDetails): MutableMap<String, String> {
            val result = inner.transform(`in`)
            val jvmOpts = result["defaultJvmOpts"] ?: ""
            val trimmedJvmOpts =
                if (jvmOpts.startsWith("'") && jvmOpts.endsWith("'")) {
                    jvmOpts.substring(1, jvmOpts.length - 1)
                } else {
                    jvmOpts
                }
            result["defaultJvmOpts"] = trimmedJvmOpts.replace("\" \"", " ")
            return result
        }
    }

    private class Fake(
        private val inner: Writer,
        private val javaagentFiles: Provider<Set<File>>,
        private val pathSeparator: String,
        ) : Writer() {
        override fun close() {
            inner.close()
        }

        override fun flush() {
            inner.flush()
        }

        override fun write(
            cbuf: CharArray,
            off: Int,
            len: Int,
        ) {
            inner.write(cbuf, off, len)
        }

        override fun write(str: String) {
            val files = javaagentFiles.get()
            val replace =
                if (files.isEmpty()) {
                    // handles case gracefully where there is a trailing space that needs to be removed if ogther default jvm opts are supplied
                    str
                        .replace(
                            "-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar ",
                            "",
                        ).replace("-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar", "")
                } else {
                    str.replace(
                        "-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar",
                        javaagentFiles.get().joinToString(
                            " ",
                        ) { jar -> "-javaagent:\$APP_HOME${pathSeparator}agent-libs${pathSeparator}${jar.name}" },
                    )
                }
            super.write(replace)
        }
    }
}
