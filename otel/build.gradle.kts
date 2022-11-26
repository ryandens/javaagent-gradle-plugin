plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.8.0-Beta"
    id("com.netflix.nebula.maven-apache-license") version "19.0.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

pluginBundle {
    website = "https://www.ryandens.com"
    vcsUrl = "https://github.com/ryandens/javaagent-gradle-plugin"
    tags = listOf("otel", "instrumentation", "observability")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(project(":plugin"))
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.allWarningsAsErrors = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create("JavaagentOTelModificationPlugin") {
            id = "com.ryandens.javaagent-otel-modification"
            displayName = "OpenTelemetry Javaagent Plugin"
            description =
                "Enables easy modification of OpenTelemetry javaagent distributions with 3rd party extensions or auto-instrumentation modules"
            implementationClass = "com.ryandens.javaagent.otel.JavaagentOTelModificationPlugin"
        }
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}
