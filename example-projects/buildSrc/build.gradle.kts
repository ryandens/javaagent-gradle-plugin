plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
  implementation("com.ryandens:otel:0.2.2")
  implementation("com.ryandens:plugin:0.2.2")
}
