package com.ryandens.javaagent

import org.gradle.api.Transformer
import org.gradle.api.internal.plugins.DefaultTemplateBasedStartScriptGenerator
import org.gradle.api.internal.plugins.StartScriptTemplateBindingFactory
import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.jvm.application.scripts.JavaAppStartScriptGenerationDetails
import org.gradle.jvm.application.scripts.ScriptGenerator
import java.io.Writer

class JavaagentAwareStartScriptGenerator(
    private val inner: ScriptGenerator = DefaultTemplateBasedStartScriptGenerator(
        "\n", FakeTransformer(StartScriptTemplateBindingFactory.unix()), UnixStartScriptGenerator().template
    )
) : ScriptGenerator {

    override fun generateScript(details: JavaAppStartScriptGenerationDetails, destination: Writer) {
        println(destination.javaClass.name)
        inner.generateScript(details, Fake(destination))
    }

    private class FakeTransformer(private val inner: StartScriptTemplateBindingFactory) :
        Transformer<MutableMap<String, String>, JavaAppStartScriptGenerationDetails> by inner {

        override fun transform(`in`: JavaAppStartScriptGenerationDetails): MutableMap<String, String> {
            val result = inner.transform(`in`)
            val jvmOpts = result["defaultJvmOpts"] ?: ""
            val trimmedJvmOpts = if (jvmOpts.startsWith("'") && jvmOpts.endsWith("'")) {
                jvmOpts.substring(1, jvmOpts.length - 1)
            } else {
                jvmOpts
            }
            result["defaultJvmOpts"] = trimmedJvmOpts.replace("\" \"", " ")
            return result
        }
    }

    private class Fake(private val inner: Writer) : Writer() {
        override fun close() {
            inner.close()
        }

        override fun flush() {
            inner.flush()
        }

        override fun write(cbuf: CharArray, off: Int, len: Int) {
            inner.write(cbuf, off, len)
        }

        override fun write(str: String) {
            super.write(str.replace("COM_RYANDENS_APP_HOME_ENV_VAR_PLACEHOLDER", "\$APP_HOME"))
        }
    }
}
