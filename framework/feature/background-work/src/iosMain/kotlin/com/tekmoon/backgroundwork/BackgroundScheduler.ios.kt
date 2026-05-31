package com.tekmoon.backgroundwork

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS no-op stub for [BackgroundScheduler]. Phase 4 wires this to
 * `BGTaskScheduler` and `URLSession` background sessions.
 *
 * Until then, [schedule] silently drops the task and the observable status
 * stays at [BackgroundStatus.Enqueued]. The constructor accepts a
 * [registry] for API parity with the JVM/Android actuals; the registry is
 * not consulted yet.
 */
@Suppress("UnusedPrivateProperty", "UNUSED_PARAMETER")
public actual class BackgroundScheduler(
    private val registry: BackgroundTaskRegistry,
) {
    private val statuses: MutableMap<String, MutableStateFlow<BackgroundStatus>> = mutableMapOf()

    public actual fun schedule(task: BackgroundTask) {
        // Phase 4: hand off to BGTaskScheduler. For now, leave the task in
        // Enqueued so callers can still observe a stable status without crash.
        statuses.getOrPut(task.id) { MutableStateFlow(BackgroundStatus.Enqueued) }
    }

    public actual fun cancel(taskId: String) {
        statuses[taskId]?.value = BackgroundStatus.Cancelled
    }

    public actual fun cancelByKind(kind: BackgroundTaskKind) {
        // Phase 4 will track per-kind identifiers; the stub has nothing to cancel.
    }

    public actual fun observe(taskId: String): Flow<BackgroundStatus> =
        statuses.getOrPut(taskId) { MutableStateFlow(BackgroundStatus.Enqueued) }.asStateFlow()
}
