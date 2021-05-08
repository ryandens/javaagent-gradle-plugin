plugins {
    `java-gradle-plugin`
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
        create("attach") {
            id = "com.ryandens.javaagent.attach"
            implementationClass = "com.ryandens.javaagent.JavaagentAttachPlugin"
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
