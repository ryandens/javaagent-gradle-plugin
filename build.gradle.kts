plugins {
    id("com.diffplug.spotless") version "5.14.1"
}

allprojects {
    group = "com.ryandens"
    version = "0.2.1"
    apply<com.diffplug.gradle.spotless.SpotlessPlugin>()

    spotless {
        project.plugins.withId("java") {
            java {
                googleJavaFormat("1.10.0")
            }
        }
        project.plugins.withId("kotlin") {
            kotlin {
                ktlint()
            }
        }
        kotlinGradle {
            ktlint()
        }
    }
}
