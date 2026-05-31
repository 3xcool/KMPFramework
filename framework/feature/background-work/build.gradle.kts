plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    jvm()

    androidLibrary {
        // Module folder is `background-work` (matches the roadmap), but a
        // hyphenated package isn't a valid Java identifier. Override the
        // auto-derived namespace so the AndroidManifest package is legal.
        namespace = "com.tekmoon.backgroundwork"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                api(libs.kotlinx.collections.immutable)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.work.runtime.ktx)
            }
        }
    }
}
