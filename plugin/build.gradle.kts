plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

gradlePlugin {
    plugins {
        create("javaagentApplicationPlugin") {
            id = "com.ryandens.javaagent-application"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationPlugin"
        }
        create("javaagentApplicationDistributionPlugin") {
            id = "com.ryandens.javaagent-application-distribution"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationDistributionPlugin"
        }
        create("javaagentApplicationRunPlugin") {
            id = "com.ryandens.javaagent-application-run"
            implementationClass = "com.ryandens.javaagent.JavaagentApplicationRunPlugin"
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
