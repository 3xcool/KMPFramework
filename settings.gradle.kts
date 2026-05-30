rootProject.name = "KMPFramework"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":framework:sdk")
include(":framework:core:designsystem")
include(":framework:core:data")
include(":framework:core:domain")
include(":framework:core:utils")
include(":framework:logger")
include(":framework:core:presentation")
include(":framework:core:session")
include(":framework:core:permissions")
include(":framework:core:media")
include(":framework:core:storage")
include(":framework:feature:analytics")
include(":framework:kompass")
