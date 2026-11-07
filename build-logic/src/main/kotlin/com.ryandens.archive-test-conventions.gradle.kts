plugins {
    id("com.ryandens.kotlin-test-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(libs.findLibrary("commons-compress").orElseThrow())
}
