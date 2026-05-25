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
                implementation(libs.kotlinx.serialization.json)

                // SavedStateHandle support for DataSavedStateHandleStore
                api(libs.jetbrains.lifecycle.viewmodel.savedstate)
                api(libs.jetbrains.savedstate)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
