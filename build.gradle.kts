plugins {
    id("com.diffplug.spotless") version "5.12.4"
}

group = "com.ryandens"
version = "0.1.0"

allprojects {
    apply<com.diffplug.gradle.spotless.SpotlessPlugin>()

    spotless {
        project.plugins.withId("java") {
            java {
                googleJavaFormat("1.10.0")
            }
        }
        kotlinGradle {
            ktlint()
        }
    }
}
