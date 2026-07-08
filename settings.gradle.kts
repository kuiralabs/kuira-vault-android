pluginManagement {
    repositories {
        // Resolve the Kuira Gradle plugins (contract, localnet) from the
        // Prefer locally-published SDK builds when present (no-op otherwise).
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
        // Locally-published artifacts resolve ahead of Maven Central when present.
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "midnight-vault"

include(":app")
