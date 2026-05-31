package com.tekmoon.backgroundwork

/**
 * How a newly scheduled task interacts with other tasks that share the same
 * [BackgroundTask.kind].
 *
 * Semantics mirror Kotlin Flow / coroutine concurrency primitives:
 *
 * - [Concurrent] — `flatMapMerge`: every scheduled task runs in parallel.
 *   Two tasks of the same kind can overlap freely.
 * - [Conflate] — only the *latest* matters. Scheduling a new task of the same
 *   kind cancels any in-flight task of that kind and starts the new one.
 * - [Queue] — FIFO. A new task of a kind waits until previous tasks of that
 *   kind finish, then runs.
 *
 * On Android, these map to WorkManager's `ExistingWorkPolicy`:
 * `Concurrent` → unique name = `task.id` (no collision possible);
 * `Conflate` → unique name = `task.kind` + `ExistingWorkPolicy.REPLACE`;
 * `Queue` → unique name = `task.kind` + `ExistingWorkPolicy.APPEND`.
 */
public enum class BackgroundPolicy {
    Concurrent,
    Conflate,
    Queue,
}
