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
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
