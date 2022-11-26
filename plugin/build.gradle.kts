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
    tags = listOf("javaagent", "instrumentation", "docker", "jib", "application")
}

val plugin: Configuration by configurations.creating

configurations {
    compileOnly {
        extendsFrom(plugin)
    }
    testImplementation {
        extendsFrom(plugin)
    }
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    // adds dependencies with the plugin configuration to the plugin classpath
    pluginClasspath.setFrom(pluginClasspath.plus(plugin.files))
    // avoid warnings
    dependsOn(tasks.compileKotlin)
    dependsOn(tasks.compileJava)
    dependsOn(tasks.processResources)
}

dependencies {
    plugin("com.google.cloud.tools:jib-gradle-plugin-extension-api:0.4.0")
    plugin("gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:3.2.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.apache.commons:commons-compress:1.21")
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
        create("javaagentApplicationPlugin") {
            id = "com.ryandens.javaagent-application"
            displayName = "Javaagent Application Plugin"
            description = "Automatically attaches javaagents to the Application Plugin run tasks and the main application distributions"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationPlugin"
        }
        create("javaagentApplicationDistributionPlugin") {
            id = "com.ryandens.javaagent-application-distribution"
            displayName = "Javaagent Application Distribution Plugin"
            description = "Automatically attaches javaagents to the Application Plugin distributions"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationDistributionPlugin"
        }
        create("javaagentApplicationRunPlugin") {
            id = "com.ryandens.javaagent-application-run"
            displayName = "Javaagent Application Run Plugin"
            description = "Automatically attaches javaagents to the Application Plugin run tasks"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationRunPlugin"
        }
        create("javaagentJibPlugin") {
            id = "com.ryandens.javaagent-jib"
            displayName = "Javaagent Jib Plugin"
            description = "Automatically includes javaagents in OCI images created by Jib"
            implementationClass = "com.ryandens.javaagent.JavaagentJibExtension"
        }
        create("javaagentTestPlugin") {
            id = "com.ryandens.javaagent-test"
            displayName = "Javaagent Test Plugin"
            description = "Automatically attaches javaagents to the Java Plugin Test tasks"
            implementationClass = "com.ryandens.javaagent.JavaagentTestPlugin"
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
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}
