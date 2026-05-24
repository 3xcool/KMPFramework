plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.framework.core.utils)
                implementation(projects.framework.logger)
                implementation(projects.framework.core.designsystem)

                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)

                implementation(compose.runtime)
                implementation(compose.ui)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.palette)
                implementation(libs.androidx.exifinterface)
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}
