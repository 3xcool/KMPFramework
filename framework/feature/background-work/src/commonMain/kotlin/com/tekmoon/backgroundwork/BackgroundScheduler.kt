package com.tekmoon.backgroundwork

import kotlinx.coroutines.flow.Flow

/**
 * The platform-specific dispatcher that turns a [BackgroundTask] into actual
 * background execution.
 *
 * Construction is platform-specific:
 * - **Android** — `BackgroundScheduler(context, registry)` using WorkManager.
 * - **JVM (desktop / tests)** — `BackgroundScheduler(scope, registry)` using
 *   an in-memory coroutine dispatcher.
 * - **iOS** — `BackgroundScheduler(registry)` no-op stub until Phase 4 wires
 *   `BGTaskScheduler`. iOS callers can still construct the scheduler so the
 *   common code compiles; scheduling silently no-ops with a logged warning.
 *
 * Policy semantics (Concurrent / Conflate / Queue) are documented on
 * [BackgroundPolicy].
 */
public expect class BackgroundScheduler {
    public fun schedule(task: BackgroundTask)
    public fun cancel(taskId: String)
    public fun cancelByKind(kind: String)
    public fun observe(taskId: String): Flow<BackgroundStatus>
}
