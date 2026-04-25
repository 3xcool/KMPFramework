import com.android.build.api.dsl.androidLibrary
import com.tekmoon.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpCommonTestConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {

//        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension>("kotlin") {

            // commonTest (multiplatform, shared)
            sourceSets.matching { it.name == "commonTest" }.configureEach {
                dependencies {
                    implementation(kotlin("test"))
                }
            }
        }
    }
}
