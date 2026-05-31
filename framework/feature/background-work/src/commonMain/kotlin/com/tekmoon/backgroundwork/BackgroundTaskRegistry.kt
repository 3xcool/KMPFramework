package com.tekmoon.backgroundwork

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Maps a task [BackgroundTask.kind] to the [BackgroundTaskHandler] that
 * runs it.
 *
 * Apps register every kind they'll schedule at startup (typically from
 * `Framework.start`). The scheduler queries the registry when a task
 * fires.
 *
 * Thread-safe: backed by a [Mutex] so multiple modules can register kinds
 * concurrently without racing.
 *
 * The registry stores handlers keyed by [BackgroundTaskKind.id] internally
 * because Android workers receive the kind as a string in WorkManager `Data`
 * and need to look up the handler from that raw id.
 */
public class BackgroundTaskRegistry {
    private val mutex = Mutex()
    private val handlers: MutableMap<String, BackgroundTaskHandler> = mutableMapOf()

    public suspend fun register(kind: BackgroundTaskKind, handler: BackgroundTaskHandler) {
        mutex.withLock { handlers[kind.id] = handler }
    }

    public suspend fun unregister(kind: BackgroundTaskKind) {
        mutex.withLock { handlers.remove(kind.id) }
    }

    public fun handler(kind: BackgroundTaskKind): BackgroundTaskHandler? = handlers[kind.id]

    /** Lookup by raw id — used by the Android worker bridge where the kind
     *  comes off the wire as a string. Prefer the typed overload. */
    public fun handler(kindId: String): BackgroundTaskHandler? = handlers[kindId]

    public fun kinds(): Set<String> = handlers.keys.toSet()
}
