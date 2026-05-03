plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Android-specific utility actuals (e.g., randomUUID using java.util.UUID)
            }
        }

        iosMain {
            dependencies {
                // iOS-specific utility actuals (e.g., randomUUID using NSUUID)
            }
        }

        jvmMain {
            dependencies {
                // JVM-specific utility actuals (e.g., randomUUID using java.util.UUID)
            }
        }
    }
}
