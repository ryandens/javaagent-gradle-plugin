import org.gradle.api.initialization.Settings

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    val enableDevelocity =
        providers.gradleProperty("develocityEnabled")
            .map(String::toBoolean)
            .orElse(providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) })
            .orElse(false)
            .get()
    val enableFoojayResolver =
        providers.gradleProperty("foojayResolverEnabled")
            .map(String::toBoolean)
            .orElse(providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) })
            .orElse(false)
            .get()

    if (enableDevelocity) {
        id("com.gradle.develocity") version "4.2.2"
    }
    if (enableFoojayResolver) {
        id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
    }
}

rootProject.name = "javaagent-plugin"
include("plugin", "simple-agent", "otel")

val enableDevelocity =
    providers.gradleProperty("develocityEnabled")
        .map(String::toBoolean)
        .orElse(providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) })
        .orElse(false)
        .get()

if (enableDevelocity) {
    configureDevelocityBuildScan(providers.environmentVariable("CI").isPresent)
}

private fun Settings.configureDevelocityBuildScan(isCI: Boolean) {
    val extension = extensions.findByName("develocity") ?: return
    val buildScanMethod =
        extension.javaClass.methods.firstOrNull { method ->
            method.name == "buildScan" && method.parameterTypes.size == 1
        }
            ?: return
    val actionParameter = buildScanMethod.parameterTypes.single()
    val action = java.lang.reflect.Proxy.newProxyInstance(
        actionParameter.classLoader,
        arrayOf(actionParameter),
    ) { _, method, args ->
        if (method.name != "execute" || args == null || args.size != 1) {
            return@newProxyInstance null
        }
        val buildScan = args[0]
        buildScan.javaClass.methods.firstOrNull { it.name == "setTermsOfUseUrl" }
            ?.invoke(buildScan, "https://gradle.com/help/legal-terms-of-use")
        buildScan.javaClass.methods.firstOrNull { it.name == "setTermsOfUseAgree" }
            ?.invoke(buildScan, "yes")
        buildScan.javaClass.methods.firstOrNull { it.name == "setUploadInBackground" }
            ?.invoke(buildScan, isCI)
        if (isCI) {
            val publishingMethod =
                buildScan.javaClass.methods.firstOrNull { candidate ->
                    candidate.name == "publishing" && candidate.parameterTypes.size == 1
                }
            if (publishingMethod != null) {
                val publishingActionType = publishingMethod.parameterTypes.single()
                val publishingAction = java.lang.reflect.Proxy.newProxyInstance(
                    publishingActionType.classLoader,
                    arrayOf(publishingActionType),
                ) { _, publishingActionMethod, publishingActionArgs ->
                    if (publishingActionMethod.name != "execute" || publishingActionArgs == null || publishingActionArgs.size != 1) {
                        return@newProxyInstance null
                    }
                    val publishing = publishingActionArgs[0]
                    val onlyIfMethod = publishing.javaClass.methods.firstOrNull { it.name == "onlyIf" }
                    if (onlyIfMethod != null && onlyIfMethod.parameterTypes.size == 1) {
                        val predicateType = onlyIfMethod.parameterTypes.single()
                        val predicate = java.lang.reflect.Proxy.newProxyInstance(
                            predicateType.classLoader,
                            arrayOf(predicateType),
                        ) { _, predicateMethod, _ ->
                            when (predicateMethod.name) {
                                "isSatisfiedBy", "test", "get", "getAsBoolean", "call" -> true
                                else -> null
                            }
                        }
                        onlyIfMethod.invoke(publishing, predicate)
                    }
                    null
                }
                publishingMethod.invoke(buildScan, publishingAction)
            }
        }
        null
    }
    buildScanMethod.invoke(extension, action)
}
