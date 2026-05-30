plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.framework.core.utils)

                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.datastore.preferences.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
