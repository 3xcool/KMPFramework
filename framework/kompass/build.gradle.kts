plugins {
    alias(libs.plugins.convention.cmp.library)
    alias(libs.plugins.convention.kmp.android.test) // for Tests
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here

                implementation(libs.material3.adaptive)
                implementation(libs.kotlinx.collections.immutable)

                implementation(compose.components.uiToolingPreview)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.compose.ui.tooling)
                implementation(libs.androidx.compose.ui.tooling.preview)

                implementation(libs.bundles.android.preview.support)
            }
        }

        iosMain {
            dependencies {
            }
        }
        
        jvmMain {
            dependencies {
            }
        }

    }
}

// Make String Res public
compose {
    resources {
        publicResClass = true
    }
}
