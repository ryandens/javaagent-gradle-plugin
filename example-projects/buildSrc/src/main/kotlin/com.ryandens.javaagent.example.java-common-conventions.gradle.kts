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
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
