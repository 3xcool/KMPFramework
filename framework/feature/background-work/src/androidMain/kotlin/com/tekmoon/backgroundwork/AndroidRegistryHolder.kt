package com.tekmoon.backgroundwork

import java.util.concurrent.atomic.AtomicReference

/**
 * Process-wide holder for the active [BackgroundTaskRegistry] on Android.
 *
 * WorkManager instantiates [BackgroundTaskCoroutineWorker] via reflection
 * with only `(Context, WorkerParameters)` available, so the registry can't
 * flow through the constructor. The first [BackgroundScheduler] constructed
 * in the process publishes its registry here; the worker reads it back.
 *
 * If the process dies and is recreated by WorkManager to run a deferred
 * task, the app's bootstrap code (typically `Framework.start`) is expected
 * to rebuild and publish the registry before any worker executes. Workers
 * that find no registry return `Result.failure()` and the task is dropped.
 */
internal object AndroidRegistryHolder {
    private val ref: AtomicReference<BackgroundTaskRegistry?> = AtomicReference(null)

    fun set(registry: BackgroundTaskRegistry) {
        ref.set(registry)
    }

    fun registry(): BackgroundTaskRegistry? = ref.get()

    /** Test hook only. */
    internal fun clear() {
        ref.set(null)
    }
}
