package com.ryandens.javaagent

import org.gradle.api.internal.plugins.StartScriptTemplateBindingFactory
import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.resources.TextResource

enum class Platform(
    val lineSeparator: String,
    val pathSeparator: String,
    val appHomeVar: String,
    val templateBindingFactory: StartScriptTemplateBindingFactory,
    val template: TextResource,
    /**
     * Separator placed between multiple `-javaagent` options. On Windows each option must remain its own
     * quoted token (`" "` = close quote, space, open quote); on Unix the options share a single quoted string,
     * so a plain space suffices.
     */
    val agentArgSeparator: String,
    /**
     * Normalizes the template's rendered `defaultJvmOpts`. On Unix the individually quoted options are
     * collapsed into a single space-separated string that the shell word-splits back apart when the unquoted
     * `$DEFAULT_JVM_OPTS` is expanded; on Windows the batch script does not word-split inside quotes, so each
     * option must stay its own quoted token and the value is left unchanged.
     */
    val defaultJvmOptsMapper: (String) -> String,
) {
    UNIX(
        "\n",
        "/",
        "\$APP_HOME",
        StartScriptTemplateBindingFactory.unix(),
        UnixStartScriptGenerator().template,
        " ",
        { it.replace("\" \"", " ") },
    ),
    WINDOWS(
        "\r\n",
        "\\",
        "%APP_HOME%",
        StartScriptTemplateBindingFactory.windows(),
        WindowsStartScriptGenerator().template,
        "\" \"",
        { it },
    ),
}
