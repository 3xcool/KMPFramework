import app.cash.sqldelight.gradle.SqlDelightExtension
import com.tekmoon.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

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
        with(target) {
            pluginManager.apply("app.cash.sqldelight")

            // Expose a sensible default: put generated sources under the standard
            // sqldelight source directory so IDE indexing picks them up automatically.
            extensions.configure<SqlDelightExtension> {
                linkSqlite.set(false) // let individual modules opt-in if needed
            }
        }
    }
}
