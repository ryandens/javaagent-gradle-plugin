plugins {
    id("com.ryandens.plugin-conventions")
}

val plugin: Configuration by configurations.creating

configurations {
    compileOnly {
        extendsFrom(plugin)
    }
    testImplementation {
        extendsFrom(plugin)
    }
}

dependencies {
    implementation(project(":plugin"))
    plugin("com.google.cloud.tools:jib-gradle-plugin-extension-api:0.4.0")
    plugin("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.5.3")
}
