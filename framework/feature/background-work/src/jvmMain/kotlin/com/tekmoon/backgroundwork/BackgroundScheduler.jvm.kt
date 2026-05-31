package com.tekmoon.backgroundwork

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * JVM / desktop implementation of [BackgroundScheduler].
 *
 * In-memory coroutine dispatcher backed by [scope]. State does not survive
 * process death — JVM hosts (desktop apps, tests) are expected to re-schedule
 * any pending work on startup themselves.
 *
 * Policy semantics ([BackgroundPolicy]):
 * - **Concurrent** — every task runs in its own coroutine, no coordination.
 * - **Conflate** — scheduling a new task whose [BackgroundTask.kind] matches
 *   an in-flight task cancels the in-flight task first.
 * - **Queue** — a per-kind unbounded [Channel] serialises tasks FIFO.
 *
 * Retry honours [BackgroundRetry] with [BackoffStrategy.Linear] /
 * [BackoffStrategy.Exponential]. [BackgroundSchedule.Periodic] re-enqueues
 * with the same configuration after each Success/Failure; the [flex] field
 * is ignored on JVM.
 */
public actual class BackgroundScheduler(
    private val scope: CoroutineScope,
    private val registry: BackgroundTaskRegistry,
) {
    private val mutex = Mutex()
    private val statuses: MutableMap<String, MutableStateFlow<BackgroundStatus>> = mutableMapOf()
    private val running: MutableMap<String, Job> = mutableMapOf()
    private val taskKind: MutableMap<String, String> = mutableMapOf()
    private val queues: MutableMap<String, Channel<BackgroundTask>> = mutableMapOf()

    public actual fun schedule(task: BackgroundTask) {
        scope.launch { scheduleInternal(task) }
    }

    private suspend fun scheduleInternal(task: BackgroundTask) {
        val status = statusFlow(task.id)
        status.value = BackgroundStatus.Enqueued
        val kindId = task.kind.id
        mutex.withLock { taskKind[task.id] = kindId }

        when (task.policy) {
            BackgroundPolicy.Concurrent -> {
                val job = scope.launch { runWithDelayAndRetry(task, status) }
                mutex.withLock { running[task.id] = job }
            }
            BackgroundPolicy.Conflate -> {
                mutex.withLock {
                    // Cancel every in-flight task of the same kind.
                    val toCancel = taskKind.entries.filter { it.value == kindId && it.key != task.id }
                    toCancel.forEach { (id, _) -> running[id]?.cancel() }
                }
                val job = scope.launch { runWithDelayAndRetry(task, status) }
                mutex.withLock { running[task.id] = job }
            }
            BackgroundPolicy.Queue -> enqueueOnKindChannel(task, status)
        }
    }

    private suspend fun enqueueOnKindChannel(
        task: BackgroundTask,
        status: MutableStateFlow<BackgroundStatus>,
    ) {
        val channel = mutex.withLock {
            queues.getOrPut(task.kind.id) {
                Channel<BackgroundTask>(Channel.UNLIMITED).also { ch ->
                    scope.launch {
                        for (queued in ch) {
                            val queuedStatus = statusFlow(queued.id)
                            runWithDelayAndRetry(queued, queuedStatus)
                        }
                    }
                }
            }
        }
        // Track the queue worker job for cancel().
        mutex.withLock { running[task.id] = scope.coroutineContext[Job] ?: return@withLock }
        channel.send(task)
        // Status will transition from Enqueued → Running inside runWithDelayAndRetry.
        @Suppress("UNUSED_EXPRESSION")
        status
    }

    public actual fun cancel(taskId: String) {
        scope.launch {
            mutex.withLock {
                running[taskId]?.cancel()
                running.remove(taskId)
                taskKind.remove(taskId)
            }
            statusFlow(taskId).value = BackgroundStatus.Cancelled
        }
    }

    public actual fun cancelByKind(kind: BackgroundTaskKind) {
        scope.launch {
            val kindId = kind.id
            val ids = mutex.withLock { taskKind.entries.filter { it.value == kindId }.map { it.key } }
            ids.forEach { cancel(it) }
        }
    }

    public actual fun observe(taskId: String): Flow<BackgroundStatus> = statusFlow(taskId).asStateFlow()

    private fun statusFlow(taskId: String): MutableStateFlow<BackgroundStatus> {
        // getOrPut is safe here: synchronisation isn't needed for readers because
        // MutableStateFlow itself is thread-safe and we only insert with a fresh
        // Enqueued value when the entry is missing.
        return statuses.getOrPut(taskId) { MutableStateFlow(BackgroundStatus.Enqueued) }
    }

    private suspend fun runWithDelayAndRetry(
        task: BackgroundTask,
        status: MutableStateFlow<BackgroundStatus>,
    ) {
        try {
            val initialDelay = (task.schedule as? BackgroundSchedule.Delayed)?.after
                ?: (task.schedule as? BackgroundSchedule.Periodic)?.every
            if (initialDelay != null && initialDelay > Duration.ZERO) {
                delay(initialDelay)
            }

            val handler = registry.handler(task.kind)
            if (handler == null) {
                status.value = BackgroundStatus.Failed(
                    IllegalStateException("No handler registered for kind=${task.kind.id}"),
                )
                cleanup(task.id)
                return
            }

            val outcome = executeWithRetry(task, handler, status)
            status.value = outcome

            if (task.schedule is BackgroundSchedule.Periodic && outcome !is BackgroundStatus.Cancelled) {
                delay(task.schedule.every)
                // Re-enqueue the next iteration.
                scheduleInternal(task)
            } else {
                cleanup(task.id)
            }
        } catch (e: CancellationException) {
            // The Job hosting this coroutine was cancelled (e.g. Conflate
            // replaced this task, or cancel() was called). Surface the
            // cancellation in the status flow before letting the exception
            // propagate so the coroutine terminates cleanly.
            status.value = BackgroundStatus.Cancelled
            cleanup(task.id)
            throw e
        }
    }

    private suspend fun executeWithRetry(
        task: BackgroundTask,
        handler: BackgroundTaskHandler,
        status: MutableStateFlow<BackgroundStatus>,
    ): BackgroundStatus {
        var attempt = 0
        while (true) {
            attempt++
            status.value = BackgroundStatus.Running
            val result = try {
                handler.execute(task.input)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                BackgroundResult.Failure(cause = e, retriable = true)
            }

            when (result) {
                is BackgroundResult.Success -> return BackgroundStatus.Succeeded
                is BackgroundResult.Cancelled -> return BackgroundStatus.Cancelled
                is BackgroundResult.Failure -> {
                    if (!result.retriable || attempt >= task.retry.maxAttempts) {
                        return BackgroundStatus.Failed(result.cause)
                    }
                    delay(backoffDelay(task.retry.backoff, attempt))
                }
            }
        }
    }

    private fun backoffDelay(strategy: BackoffStrategy, attempt: Int): Duration = when (strategy) {
        is BackoffStrategy.Linear -> strategy.delay
        is BackoffStrategy.Exponential -> {
            // Cap the exponent to avoid overflow on absurd attempt counts.
            val safeExp = (attempt - 1).coerceAtMost(MAX_EXPONENT_BACKOFF)
            strategy.initialDelay * (1 shl safeExp)
        }
    }

    private suspend fun cleanup(taskId: String) {
        mutex.withLock {
            running.remove(taskId)
            taskKind.remove(taskId)
        }
    }

    private companion object {
        const val MAX_EXPONENT_BACKOFF: Int = 16
    }
}
