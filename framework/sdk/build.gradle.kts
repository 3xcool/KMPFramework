plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(projects.framework.core.designsystem)
            api(projects.framework.core.data)
            api(projects.framework.core.domain)
            api(projects.framework.core.presentation)
            api(projects.framework.core.utils)
            api(projects.framework.core.session)
            api(projects.framework.core.permissions)
            api(projects.framework.core.media)
            api(projects.framework.kompass)
            api(projects.framework.logger)

            api(libs.ktor.client.core)
        }
    }
}
