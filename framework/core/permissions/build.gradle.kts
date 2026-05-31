plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.framework.core.utils)
                api(projects.framework.core.presentation)
                api(projects.framework.logger)
                implementation(projects.framework.core.designsystem)

                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)

                api(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.jetbrains.compose.viewmodel)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
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
                implementation(libs.jetbrains.lifecycle.compose)
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}
