pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        exclusiveContent {
            forRepository {
                maven {
                    name = "LibXposedMavenCentral"
                    url = uri("https://repo.maven.apache.org/maven2")
                }
            }
            filter {
                includeGroup("io.github.libxposed")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "CombinedStatus"
include(":app")
