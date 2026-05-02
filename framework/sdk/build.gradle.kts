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
            api(projects.framework.kompass)
            api(projects.framework.logger)
        }
    }
}
