import com.android.build.api.dsl.androidLibrary
import com.tekmoon.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpAndroidTestConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {

        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension>("kotlin") {

            androidLibrary {

                // JVM / Host unit tests (androidHostTest)
                withHostTestBuilder { }

                // Instrumented / device tests (androidDeviceTest)
                withDeviceTestBuilder {
                    sourceSetTreeName = "test"
                }.configure {
                    instrumentationRunner =
                        "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            // androidDeviceTest (instrumented)
            // Device test dependencies
            sourceSets.matching { it.name == "androidDeviceTest" }.configureEach {
                dependencies {
                    implementation(libs.findLibrary("androidx-runner").get())
                    implementation(libs.findLibrary("androidx-test-core").get())
                    implementation(libs.findLibrary("androidx-junit").get())
                }
            }

            // androidHostTest (JVM on Android)
            // Host / unit test dependencies
            sourceSets.matching { it.name == "androidHostTest" }.configureEach {
                dependencies {
                    implementation(libs.findLibrary("androidx-junit").get())
                    implementation(libs.findLibrary("androidx-test-core").get())
                }
            }
        }
    }
}
