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
     * Separator placed between multiple `-javaagent` options. On both platforms each option must remain its
     * own quoted token so the launcher never word-splits a path (or any other option) on internal spaces. On
     * Windows the tokens are literally double-quoted (`" "` = close quote, space, open quote); on Unix the
     * whole `DEFAULT_JVM_OPTS` value is itself wrapped in double quotes so `$APP_HOME` expands, so the inner
     * per-option quotes are backslash-escaped (`\" \"` = escaped close quote, space, escaped open quote).
     */
    val agentArgSeparator: String,
    /**
     * Normalizes the template's rendered `defaultJvmOpts`. On Windows the batch script does not word-split
     * inside quotes, so each option stays its own literally-quoted token and the value is left unchanged. On
     * Unix the value is wrapped in an outer pair of double quotes -- required so the embedded `$APP_HOME` in a
     * `-javaagent:` path is expanded when the assignment runs -- and each option's own double quotes are
     * backslash-escaped so they survive into the value. That keeps every option a distinct quoted token that
     * the launcher's `xargs` pass parses back out intact, including options whose value contains spaces (e.g.
     * `-Dgreeting=hello world`). Collapsing the per-option quotes into a single quoted string would let the
     * shell word-split those values apart -- see issue #95.
     */
    val defaultJvmOptsMapper: (String) -> String,
) {
    UNIX(
        "\n",
        "/",
        "\$APP_HOME",
        StartScriptTemplateBindingFactory.unix(),
        UnixStartScriptGenerator().template,
        "\\\" \\\"",
        { "\"" + it.replace("\"", "\\\"") + "\"" },
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
