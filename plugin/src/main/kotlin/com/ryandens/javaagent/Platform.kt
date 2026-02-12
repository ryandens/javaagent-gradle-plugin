package com.ryandens.javaagent

import org.gradle.api.internal.plugins.StartScriptTemplateBindingFactory
import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.resources.TextResource

enum class Platform(
    val lineSeparator: String,
    val pathSeparator: String,
    val templateBindingFactory: StartScriptTemplateBindingFactory,
    val template: TextResource,
) {
    UNIX("\n", "/", StartScriptTemplateBindingFactory.unix(), UnixStartScriptGenerator().template),
    WINDOWS("\r\n", "\\", StartScriptTemplateBindingFactory.windows(), WindowsStartScriptGenerator().template),
}
