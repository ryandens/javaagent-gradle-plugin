plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    constraints {
        // Define dependency versions as constraints
    }
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
