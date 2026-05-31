import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin that applies the SQLDelight Gradle plugin to a KMP module.
 *
 * Usage in a consuming module's build.gradle.kts:
 * ```
 * plugins {
 *     alias(libs.plugins.convention.sqldelight)
 * }
 *
 * sqldelight {
 *     databases {
 *         create("AppDatabase") {
 *             packageName.set("com.example.app.db")
 *         }
 *     }
 * }
 * ```
 *
 * The plugin intentionally does NOT pre-configure any database — each client module
 * owns its schema and package name.  The framework's [DatabaseDriverFactory] provides
 * the platform-specific [app.cash.sqldelight.db.SqlDriver] that clients pass to their
 * generated Database constructor.
 */
class SqlDelightConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("app.cash.sqldelight")
    }
}
