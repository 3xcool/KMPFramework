plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.android.lint) apply false
}

// Configure GitHub Packages publishing for all framework modules
subprojects {
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/3xcool/KMPFramework")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                            ?: project.findProperty("gpr.user") as String? ?: ""
                        password = System.getenv("GITHUB_TOKEN")
                            ?: project.findProperty("gpr.key") as String? ?: ""
                    }
                }
            }
        }
    }
}
