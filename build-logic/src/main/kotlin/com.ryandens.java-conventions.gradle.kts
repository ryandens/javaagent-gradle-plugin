plugins {
    id("com.diffplug.spotless")
    id("org.gradlex.reproducible-builds")
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile> {
    options.isDeprecation = true
    options.release.set(17)
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
