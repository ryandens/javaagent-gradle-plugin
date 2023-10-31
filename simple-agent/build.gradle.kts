plugins {
    id("com.ryandens.java-conventions")
    `java-library`
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "com.ryandens.SimpleAgent",
        )
    }
}
