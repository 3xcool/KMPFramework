import com.tekmoon.convention.libs
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import java.io.File

/**
 * Applies detekt to a module with a shared config + per-module baseline.
 *
 * - Config lives at the repo root: config/detekt/detekt.yml
 * - Each module gets its own baseline: <module>/detekt-baseline.xml (grandfathers
 *   existing violations so detekt only fails on NEW issues).
 * - detekt-formatting (ktlint wrapper rules) is added as a detektPlugin.
 *
 * Applied automatically by the library convention plugins, so every framework
 * module is covered. Run with `./gradlew detekt`; regenerate baselines with
 * `./gradlew detektBaseline`.
 */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")

        val configFile = rootProject.file("config/detekt/detekt.yml")
        val baselineFile = file("detekt-baseline.xml")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            parallel = true
            config.setFrom(configFile)
            if (baselineFile.exists()) {
                baseline = baselineFile
            }
            autoCorrect = false
        }

        dependencies {
            "detektPlugins"(libs.findLibrary("detekt-formatting").get())
        }

        tasks.withType<Detekt>().configureEach {
            // KMP source dirs aren't picked up by the default JVM convention, so point
            // detekt at the common/platform Kotlin sources explicitly.
            setSource(files("src"))
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/resources/**", "**/generated/**")
            // Baseline is per-task so `detektBaseline` writes the right file.
            if (baselineFile.exists()) {
                baseline.set(baselineFile)
            }
            reports {
                html.required.set(true)
                sarif.required.set(true)
                xml.required.set(false)
                txt.required.set(false)
            }
        }
    }
}
