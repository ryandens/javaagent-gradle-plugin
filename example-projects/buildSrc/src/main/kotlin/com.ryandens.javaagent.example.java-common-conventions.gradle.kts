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
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
