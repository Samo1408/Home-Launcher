pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoryMode = RepositoryMode.FAIL_ON_PROJECT_REPOS
    versionCatalogs {
        create(libs)
    }
}

rootProject.name = "Home-Launcher"
include(":App")
