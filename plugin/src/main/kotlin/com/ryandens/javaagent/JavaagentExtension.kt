package com.ryandens.javaagent

import org.gradle.api.provider.MapProperty

/**
 * Extension for configuring the javaagents attached by this plugin.
 *
 * Currently exposes [agentOptions], which associates agent option strings (the `=<options>` portion of the
 * `-javaagent:<jar>=<options>` JVM argument, see the
 * [java.lang.instrument package documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.instrument/java/lang/instrument/package-summary.html#starting-an-agent-from-the-command-line-interface-heading))
 * with the agent they apply to.
 *
 * Options are keyed by the dependency coordinate the user declared in the `javaagent` (or `testJavaagent`)
 * configuration, so they survive version bumps and do not depend on the resolved jar file name:
 *  - module dependencies are keyed by `group:name` (version is intentionally excluded)
 *  - project dependencies are keyed by their project path, e.g. `:simple-agent`
 *
 * ```
 * dependencies {
 *   javaagent 'io.prometheus.jmx:jmx_prometheus_javaagent:0.20.0'
 * }
 *
 * javaagent {
 *   agentOptions.put('io.prometheus.jmx:jmx_prometheus_javaagent', '12345:config.yaml')
 * }
 * ```
 */
abstract class JavaagentExtension {
    /**
     * Maps an agent's dependency coordinate (`group:name` for module dependencies, project path for project
     * dependencies) to the option string appended after `=` in the `-javaagent:<jar>=<options>` argument.
     */
    abstract val agentOptions: MapProperty<String, String>
}
