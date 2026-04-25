
import com.tekmoon.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class CmpLibraryConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.tekmoon.convention.kmp.library")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }

            dependencies {
//                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-runtime").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-ui").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-foundation").get())

//                "commonMainImplementation"(libs.findLibrary("org.jetbrains.compose.material3:material3").get())
//                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material3").get())

                // remove it
//                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material3").get())
//                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material-icons-core").get())

                "androidMainImplementation"(libs.findLibrary("androidx-activity-compose").get())
                "androidMainImplementation"(libs.findLibrary("androidx-lifecycle-runtime").get())
                "androidMainImplementation"(libs.findLibrary("androidx-lifecycle-process").get())
                "androidMainImplementation"(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())

            }
        }
    }
}
