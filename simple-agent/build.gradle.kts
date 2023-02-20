plugins {
    `java-library`
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "com.ryandens.SimpleAgent",
        )
    }
}
