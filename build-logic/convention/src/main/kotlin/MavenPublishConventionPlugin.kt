import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class MavenPublishConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("maven-publish")

            version = findProperty("frameworkVersion") as String? ?: "0.0.1-SNAPSHOT"

            afterEvaluate {
                // Derive base artifactId from project path:
                // :framework:kompass -> kompass
                // :framework:core:designsystem -> core-designsystem
                // :framework -> framework
                val pathSegments = path
                    .removePrefix(":framework:")
                    .removePrefix(":framework")
                    .split(":")
                    .filter { it.isNotEmpty() }

                val baseArtifactId = when {
                    // :framework:sdk -> publish as "framework" (umbrella artifact)
                    pathSegments == listOf("sdk") -> "framework"
                    pathSegments.isNotEmpty() -> pathSegments.joinToString("-")
                    else -> "framework"
                }

                extensions.configure<PublishingExtension> {
                    publications.withType<MavenPublication> {
                        groupId = "com.tekmoon"

                        // KMP creates publications per target with suffixed names
                        // (e.g., "kotlinMultiplatform", "jvm", "android", "iosArm64")
                        // The artifactId already has the target suffix appended by KMP.
                        // We only replace the project-path-based prefix with our clean name.
                        val currentArtifact = artifactId ?: ""
                        val projectName = project.name

                        artifactId = if (currentArtifact == projectName) {
                            // Root publication (kotlinMultiplatform)
                            baseArtifactId
                        } else if (currentArtifact.startsWith("$projectName-")) {
                            // Target-specific publication (e.g., "kompass-jvm")
                            currentArtifact.replaceFirst(projectName, baseArtifactId)
                        } else {
                            currentArtifact
                        }
                    }
                }
            }
        }
    }
}
