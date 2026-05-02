import com.android.build.api.dsl.androidLibrary

//plugins {
//    alias(libs.plugins.kotlin.multiplatform)
//    alias(libs.plugins.android.kotlin.multiplatform.library)
//}
//
//kotlin {
//    jvm()
//
//    // Target declarations - add or remove as needed below. These define
//    // which platforms this KMP module supports.
//    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
//    androidLibrary {
//        namespace = "com.tekmoon.designsystem"
//        compileSdk = 36
//        minSdk = 26
//
//        withHostTestBuilder {
//        }
//
//        withDeviceTestBuilder {
//            sourceSetTreeName = "test"
//        }.configure {
//            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        }
//    }
//
//    // For iOS targets, this is also where you should
//    // configure native binary output. For more information, see:
//    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks
//
//    // A step-by-step guide on how to include this library in an XCode
//    // project can be found here:
//    // https://developer.android.com/kotlin/multiplatform/migrate
//    val xcfName = "framework:core:designsystemKit"
//
//    iosX64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }
//
//    iosArm64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }
//
//    iosSimulatorArm64 {
//        binaries.framework {
//            baseName = xcfName
//        }
//    }
//
//    // Source set declarations.
//    // Declaring a target automatically creates a source set with the same name. By default, the
//    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
//    // common to share sources between related targets.
//    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
//    sourceSets {
//        commonMain {
//            dependencies {
//                implementation(libs.kotlin.stdlib)
//                // Add KMP dependencies here
//            }
//        }
//
//        commonTest {
//            dependencies {
//                implementation(libs.kotlin.test)
//            }
//        }
//
//        androidMain {
//            dependencies {
//                // Add Android-specific dependencies here. Note that this source set depends on
//                // commonMain by default and will correctly pull the Android artifacts of any KMP
//                // dependencies declared in commonMain.
//            }
//        }
//
//        getByName("androidDeviceTest") {
//            dependencies {
//                implementation(libs.androidx.runner)
//                implementation(libs.androidx.test.core)
//                implementation(libs.androidx.junit)
//            }
//        }
//
//        iosMain {
//            dependencies {
//                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
//                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
//                // part of KMP’s default source set hierarchy. Note that this source set depends
//                // on common by default and will correctly pull the iOS artifacts of any
//                // KMP dependencies declared in commonMain.
//            }
//        }
//    }
//
//}






plugins {
    alias(libs.plugins.convention.cmp.library)
    alias(libs.plugins.convention.kmp.android.test)
}

kotlin {
    jvm()

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here

                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)


                api(libs.coil.core)
//                implementation(libs.coil.compose) // Android only
                implementation(libs.coil.compose.core)
                implementation(libs.coil.network.ktor)

                implementation(libs.bundles.ktor.common)

                implementation(libs.kotlinx.collections.immutable)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
                implementation(libs.androidx.compose.ui.tooling)
                implementation(libs.androidx.compose.ui.tooling.preview)

                implementation(libs.bundles.android.preview.support)

                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)

                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}


// Make String Res public, with a stable package independent of rootProject.name
// so this artifact's API contract (Res class location) is preserved across consumers.
compose {
    resources {
        publicResClass = true
        packageOfResClass = "com.tekmoon.designsystem.generated.resources"
    }
}