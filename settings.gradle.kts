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
        exclusiveContent {
            forRepository {
                maven {
                    name = "MiuixGitHubPackages"
                    url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix")
                    credentials {
                        username = providers.environmentVariable("MIUIX_GITHUB_ACTOR").orNull
                            ?: providers.environmentVariable("GITHUB_ACTOR").orNull
                            ?: providers.gradleProperty("gpr.user").orNull
                        password = providers.environmentVariable("MIUIX_GITHUB_TOKEN").orNull
                            ?: providers.environmentVariable("GITHUB_TOKEN").orNull
                            ?: providers.gradleProperty("gpr.key").orNull
                    }
                }
            }
            filter {
                includeGroup("top.yukonga.miuix.kmp")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "CombinedStatus"
include(":app")
