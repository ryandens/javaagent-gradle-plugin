plugins {
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(libs.findLibrary("junit-jupiter").orElseThrow())
    testRuntimeOnly(libs.findLibrary("junit-launcher").orElseThrow())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
