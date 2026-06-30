pluginManagement {
    repositories {
        // Resolve the Kuira Gradle plugins (contract, localnet) from the
        // in-progress alpha05 build published to mavenLocal first.
        mavenLocal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Kuira SDK (dapp-ui + full graph) at the current alpha05 work, taken
        // from mavenLocal ahead of Maven Central.
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "midnight-vault"

include(":app")
