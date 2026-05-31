package com.tekmoon.backgroundwork

import kotlin.jvm.JvmInline

/**
 * Logical *type* of a background task — the "what kind of work is this?"
 * label. Drives two distinct behaviors:
 *
 * 1. **Handler dispatch.** [BackgroundTaskRegistry] maps kinds to handlers
 *    by [id]. Apps register one handler per kind at startup.
 * 2. **Policy grouping.** [BackgroundPolicy.Conflate] and
 *    [BackgroundPolicy.Queue] operate *per kind*. Two tasks of the same
 *    kind interact; two tasks of different kinds are independent.
 *
 * `id` is the wire-format string written into Android WorkManager `Data`
 * (so it survives process death). It must be stable across app upgrades —
 * changing an id stranded any in-flight tasks scheduled under the old name.
 *
 * ## Recommended shapes
 *
 * Define your own type per app — the library does not ship a global enum
 * because the catalog of background work is app-specific. Three idiomatic
 * patterns:
 *
 * ### Enum (closed set, flat)
 * ```kotlin
 * enum class AppTaskKind(override val id: String) : BackgroundTaskKind {
 *     SyncMessages("messaging.sync"),
 *     UploadAttachment("messaging.upload-attachment"),
 *     RefreshFeed("feed.refresh"),
 *     ReplayPendingCalls("net.replay-pending"),
 * }
 * ```
 *
 * ### Sealed class (closed set, grouped, supports parameters)
 * ```kotlin
 * sealed class AppTaskKind(override val id: String) : BackgroundTaskKind {
 *     object SyncMessages : AppTaskKind("messaging.sync")
 *     object UploadAttachment : AppTaskKind("messaging.upload-attachment")
 *     data class Analytics(val target: String) : AppTaskKind("analytics.$target")
 * }
 * ```
 *
 * ### Ad-hoc string (open set, quick prototyping)
 * Wrap a raw string in [StringTaskKind] when defining a dedicated type
 * would be overkill (early prototyping, library-internal kinds, tests).
 * ```kotlin
 * val quickKind = StringTaskKind("debug.dump-state")
 * ```
 *
 * ## Naming `id`
 *
 * - Use a namespaced dotted-or-hyphenated string (`messaging.sync`,
 *   `feed.refresh`). The library does not enforce this; it just helps
 *   when reading WorkManager dumps or analytics events that record kinds.
 * - Stay below ~64 chars to keep Android `Data` small.
 * - Treat ids as part of your public contract: rename = breaking change.
 *
 * ## Concrete examples
 *
 * | `kind.id`                | Policy       | Why                                                          |
 * |--------------------------|--------------|--------------------------------------------------------------|
 * | `"refresh-feed"`         | `Conflate`   | Only the latest refresh matters; cancel any older one.       |
 * | `"upload-photo"`         | `Queue`      | Order matters; one at a time so the radio isn't saturated.   |
 * | `"sync-messages"`        | `Queue`      | Serialize concurrent syncs so updates don't race.            |
 * | `"replay-pending-calls"` | `Conflate`   | A new drain supersedes any in-flight drain.                  |
 * | `"send-analytics-batch"` | `Queue`      | FIFO flush; never drop events.                               |
 * | `"image-thumbnail"`      | `Concurrent` | Independent per id; safe to parallelize.                     |
 *
 * Note: `id` here is the *category* (kind). The distinct instance of one
 * scheduled call is [BackgroundTask.id], typically formed as
 * `"$kindId:$someKey"` (e.g. `"upload-photo:photo-42"`).
 */
public interface BackgroundTaskKind {
    public val id: String
}

/**
 * Lightweight wrapper turning a raw string into a [BackgroundTaskKind]
 * with zero runtime overhead on the JVM.
 *
 * Useful for quick prototyping, library-internal kinds, or tests. For
 * production code prefer defining an app-level enum or sealed class so
 * the catalog of valid kinds is enumerable and refactor-safe.
 */
@JvmInline
public value class StringTaskKind(override val id: String) : BackgroundTaskKind
