import com.android.build.api.dsl.androidLibrary
import com.tekmoon.convention.configureKotlinAndroid
import com.tekmoon.convention.configureKotlinMultiplatform
import com.tekmoon.convention.libs
import com.tekmoon.convention.pathToResourcePrefix
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.text.get

//import com.android.build.api.dsl.LibraryExtension
//import com.android.build.api.dsl.androidLibrary
//import com.tekmoon.convention.configureKotlinAndroid
//import com.tekmoon.convention.pathToResourcePrefix

class KmpLibraryConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
//                apply("com.android.library") // deprecated
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.kotlin.plugin.serialization")
                apply("com.tekmoon.convention.maven.publish")
            }

            configureKotlinMultiplatform()

            // deprecated, used when "com.android.library"
//            extensions.configure<LibraryExtension> {
//                configureKotlinAndroid(this)
//
//                resourcePrefix = this@with.pathToResourcePrefix()
//
//                // Required to make debug build of app run in iOS simulator
//                experimentalProperties["android.experimental.kmp.enableAndroidResources"] = "true"
//            }

            extensions.configure<KotlinMultiplatformExtension> {
                androidLibrary {
                    compileSdk = libs.findVersion("projectCompileSdkVersion")
                        .get().toString().toInt()

                    minSdk = libs.findVersion("projectMinSdkVersion")
                        .get().toString().toInt()

                    // Required for Android resources in KMP
                    experimentalProperties["android.experimental.kmp.enableAndroidResources"] = "true"
                }
            }

            dependencies {
                "commonMainImplementation"(libs.findLibrary("kotlinx-serialization-json").get())
                "commonTestImplementation"(libs.findLibrary("kotlin-test").get())
            }
        }
    }
}