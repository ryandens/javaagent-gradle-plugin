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
    private val optionsByFileName: Provider<Map<String, String>>,
    private val inner: ScriptGenerator =
        DefaultTemplateBasedStartScriptGenerator(
            platform.lineSeparator,
            FakeTransformer(platform.templateBindingFactory, platform.defaultJvmOptsMapper),
            platform.template,
        ),
) : ScriptGenerator {
    override fun generateScript(
        details: JavaAppStartScriptGenerationDetails,
        destination: Writer,
    ) {
        inner.generateScript(
            details,
            Fake(
                destination,
                javaagentConfiguration,
                platform.pathSeparator,
                platform.appHomeVar,
                platform.agentArgSeparator,
                optionsByFileName,
            ),
        )
    }

    private class FakeTransformer(
        private val inner: StartScriptTemplateBindingFactory,
        private val defaultJvmOptsMapper: (String) -> String,
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
            result["defaultJvmOpts"] = defaultJvmOptsMapper(trimmedJvmOpts)
            return result
        }
    }

    private class Fake(
        private val inner: Writer,
        private val javaagentFiles: Provider<Set<File>>,
        private val pathSeparator: String,
        private val appHomeVar: String,
        private val agentArgSeparator: String,
        private val optionsByFileName: Provider<Map<String, String>>,
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

        /**
         * Rewrites the javaagent placeholder that the templated `defaultJvmOpts` injects into the rendered start
         * script. When the configuration resolves to one or more agents, the single placeholder is replaced by one
         * `-javaagent:` option per agent, each pointing at the distribution's `agent-libs` directory (joined with
         * the platform's [agentArgSeparator]). When there are no agents, the placeholder together with its
         * surrounding quoting and trailing space is stripped out. Text that does not contain the placeholder is
         * written through unchanged.
         */
        override fun write(str: String) {
            val files = javaagentFiles.get()
            val replace =
                if (files.isEmpty()) {
                    // Remove the placeholder opt when there are no agents. Each opt is its own quoted token: on Unix
                    // the quotes are backslash-escaped inside the outer-quoted value
                    // (`"\"-javaagent:...PLACEHOLDER.jar\" \"-Xmx256m\""`), on Windows they are literal
                    // (`"-javaagent:...PLACEHOLDER.jar" "-Xmx256m"`). Strip the placeholder token together with its
                    // surrounding quotes and trailing separator. The trailing-space variants run first to avoid
                    // leaving a dangling separator; the escaped variants run before the bare ones so the leading `\`
                    // is not orphaned.
                    str
                        .replace("\\\"-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar\\\" ", "")
                        .replace("\\\"-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar\\\"", "")
                        .replace("\"-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar\" ", "")
                        .replace("\"-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar\"", "")
                        .replace("-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar ", "")
                        .replace("-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar", "")
                } else {
                    val options = optionsByFileName.get()
                    str.replace(
                        "-javaagent:COM_RYANDENS_JAVAAGENTS_PLACEHOLDER.jar",
                        files.joinToString(
                            agentArgSeparator,
                        ) { jar ->
                            val suffix = options[jar.name]?.let { "=$it" } ?: ""
                            "-javaagent:$appHomeVar${pathSeparator}agent-libs$pathSeparator${jar.name}$suffix"
                        },
                    )
                }
            super.write(replace)
        }
    }
}
