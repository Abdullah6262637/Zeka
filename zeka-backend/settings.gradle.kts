rootProject.name = "zeka-backend"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = java.net.URI("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    }
}
