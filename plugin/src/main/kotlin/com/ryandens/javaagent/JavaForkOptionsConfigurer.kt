package com.ryandens.javaagent

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.process.JavaForkOptions

object JavaForkOptionsConfigurer {

    fun configureJavaForkOptions(javaForkOptions: JavaForkOptions, javaagentConfiguration: NamedDomainObjectProvider<Configuration>) {
        javaForkOptions.jvmArgumentProviders.add {
            mutableListOf(
                *javaagentConfiguration.get().asPath.split(":")
                    .map { javaagentJarPath -> "-javaagent:$javaagentJarPath" }.toTypedArray()
            ).plus(javaForkOptions.jvmArgs ?: mutableListOf())
        }
    }
}
