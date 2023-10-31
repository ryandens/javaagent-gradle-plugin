plugins {
    id("com.diffplug.spotless")
    java
}

repositories {
    mavenCentral()
}

spotless {
    java {
        googleJavaFormat()
    }
    kotlinGradle {
        ktlint()
    }
    tasks.withType(JavaCompile::class.java) {
        this.options.compilerArgs.add("-Werror")
    }
}