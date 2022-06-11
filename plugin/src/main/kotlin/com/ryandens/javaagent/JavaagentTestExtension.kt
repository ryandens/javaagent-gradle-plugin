package com.ryandens.javaagent

import org.gradle.api.provider.Property

abstract class JavaagentTestExtension {

    abstract val enabled: Property<Boolean>

    init {
        enabled.convention(true)
    }
}
