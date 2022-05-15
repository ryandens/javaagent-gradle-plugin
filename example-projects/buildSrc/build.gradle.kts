plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
  implementation("com.ryandens:otel:0.3.0")
  implementation("com.ryandens:plugin:0.3.0")
}
