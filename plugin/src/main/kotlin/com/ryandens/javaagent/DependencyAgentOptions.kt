package com.ryandens.javaagent

import org.gradle.api.artifacts.ModuleDependency

/**
 * Options for this agent dependency, appended after `=` in its `-javaagent` argument.
 *
 * Import `com.ryandens.javaagent.agentOptions` in a Kotlin build script and set
 * `agentOptions = "12345:config.yaml"` inside a `javaagent` or `testJavaagent` dependency block.
 * Declare the dependency in `applicationRunJavaagent`, `testJavaagent`, `applicationDistributionJavaagent`, or
 * `jibJavaagent` to scope its options to the corresponding plugin. A `javaagent` dependency is
 * inherited by all of these configurations; a direct declaration overrides inherited options.
 */
var ModuleDependency.agentOptions: String
    get() = attributes.getAttribute(AgentOptions.OPTIONS) ?: ""
    set(value) {
        attributes { it.attribute(AgentOptions.OPTIONS, value) }
    }
