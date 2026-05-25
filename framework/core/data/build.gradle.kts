plugins {
    alias(libs.plugins.convention.cmp.library)
    alias(libs.plugins.convention.kmp.android.test)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(compose.components.resources)
                implementation(projects.framework.core.domain)

                implementation(libs.bundles.ktor.common)
                implementation(libs.touchlab.kermit)

                // SQLDelight — runtime + coroutine Flow adapters (api so clients get them transitively)
                api(libs.sqldelight.runtime)
                api(libs.sqldelight.coroutines.extensions)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.android.driver)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native.driver)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}
