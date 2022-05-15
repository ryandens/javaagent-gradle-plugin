plugins {
    id("com.diffplug.spotless") version "6.3.0"
}

allprojects {
    group = "com.ryandens"
    version = "0.3.0"
    apply<com.diffplug.gradle.spotless.SpotlessPlugin>()

    repositories {
        mavenCentral()
    }

    tasks.withType(JavaCompile::class.java) {
        this.options.compilerArgs.add("-Werror")
    }

    spotless {
        project.plugins.withId("java") {
            java {
                googleJavaFormat()
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
