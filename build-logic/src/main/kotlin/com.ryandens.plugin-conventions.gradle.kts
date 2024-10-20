import dev.sigstore.sign.tasks.SigstoreSignFilesTask
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.kotlin.dsl.`java-gradle-plugin`
import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("dev.sigstore.sign")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
    id("com.netflix.nebula.maven-apache-license")
    id("com.ryandens.java-conventions")
}

repositories {
    gradlePluginPortal()
}

spotless {
    kotlin {
        ktlint()
    }
}

tasks.publishPlugins {
    dependsOn(
        publishing.publications.map {
                publication ->
            tasks.named<SigstoreSignFilesTask>("sigstoreSign${publication.name.capitalized()}Publication")
        },
    )
}

gradlePlugin {
    website.set("https://www.ryandens.com")
    vcsUrl.set("https://github.com/ryandens/javaagent-gradle-plugin")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.allWarningsAsErrors = true
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet =
    sourceSets.create("functionalTest") {
    }

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}
