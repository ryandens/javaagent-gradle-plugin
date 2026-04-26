plugins {
    id("com.ryandens.plugin-conventions")
}

dependencies {
    implementation(project(":plugin"))
    compileOnly("com.google.cloud.tools:jib-gradle-plugin-extension-api:0.4.0")
    compileOnly("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.5.3")
}
