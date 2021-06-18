package com.ryandens.javaagent

import org.gradle.api.Plugin
import org.gradle.api.Project

class JavaagentApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(JavaagentApplicationDistributionPlugin::class.java)
        target.pluginManager.apply(JavaagentApplicationRunPlugin::class.java)
    }
}
