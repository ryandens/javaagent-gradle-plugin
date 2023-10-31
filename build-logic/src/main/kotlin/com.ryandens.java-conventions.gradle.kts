plugins {
    id("com.diffplug.spotless")
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.isDeprecation = true
    options.release.set(11)
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
