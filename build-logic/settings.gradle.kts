rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { gradlePluginPortal() }
    versionCatalogs {
        create("buildlibs") {
            from(files("../gradle/buildlibs.versions.toml"))
        }
    }
}
