import com.diffplug.spotless.LineEnding

plugins {
    id("com.diffplug.spotless")
    java
}

repositories {
    mavenCentral()
}

spotless {
    this.lineEndings = LineEnding.UNIX
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