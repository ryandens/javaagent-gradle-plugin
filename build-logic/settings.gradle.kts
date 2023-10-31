rootProject.name = "build-logic"

dependencyResolutionManagement {
    versionCatalogs {
        create("buildlibs") {
            from(files("../gradle/buildlibs.versions.toml"))
        }
    }
}
