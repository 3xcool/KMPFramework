package com.tekmoon.backgroundwork

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Android implementation of [BackgroundScheduler] backed by `WorkManager`.
 *
 * Policy mapping:
 * - **Concurrent** — unique-name = `task.id` (no collision possible), so two
 *   tasks of the same kind enqueue as independent unique works.
 * - **Conflate** — unique-name = `task.kind`, `ExistingWorkPolicy.REPLACE`,
 *   so a new enqueue cancels any in-flight work of that kind.
 * - **Queue** — unique-name = `task.kind`, `ExistingWorkPolicy.APPEND_OR_REPLACE`,
 *   so tasks queue FIFO behind any current work of that kind.
 *
 * Periodic tasks honour [BackgroundSchedule.Periodic.every] (subject to
 * WorkManager's 15-minute minimum) and optional [BackgroundSchedule.Periodic.flex].
 *
 * Process-survival: WorkManager persists tasks, but the [BackgroundTaskRegistry]
 * holding handlers does not. Consuming apps must re-register every kind at
 * startup (typically from `Framework.start`) so a re-hydrated worker can find
 * its handler. This is enforced by [BackgroundTaskCoroutineWorker.doWork],
 * which returns `Result.failure()` when the registry has no entry for the kind.
 */
public actual class BackgroundScheduler(
    context: Context,
    registry: BackgroundTaskRegistry,
) {
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext)

    init {
        // Stash the registry on a process-wide holder so the worker (instantiated
        // by WorkManager via reflection) can find it. See AndroidRegistryHolder.
        AndroidRegistryHolder.set(registry)
    }

    public actual fun schedule(task: BackgroundTask) {
        val constraints = Constraints.Builder().apply {
            if (task.requiresNetwork) setRequiredNetworkType(NetworkType.CONNECTED)
            if (task.requiresCharging) setRequiresCharging(true)
        }.build()

        val inputData = Data.Builder()
            .putString(BackgroundTaskCoroutineWorker.KEY_TASK_ID, task.id)
            .putString(BackgroundTaskCoroutineWorker.KEY_TASK_KIND, task.kind.id)
            .putAll(task.input.mapKeys { "${BackgroundTaskCoroutineWorker.INPUT_PREFIX}${it.key}" })
            .build()

        when (val schedule = task.schedule) {
            BackgroundSchedule.Immediate,
            is BackgroundSchedule.Delayed -> enqueueOneTime(task, constraints, inputData, schedule)
            is BackgroundSchedule.Periodic -> enqueuePeriodic(task, constraints, inputData, schedule)
        }
    }

    private fun enqueueOneTime(
        task: BackgroundTask,
        constraints: Constraints,
        inputData: Data,
        schedule: BackgroundSchedule,
    ) {
        val request = OneTimeWorkRequestBuilder<BackgroundTaskCoroutineWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setId(uuidFromTaskId(task.id))
            .also { builder ->
                if (schedule is BackgroundSchedule.Delayed) {
                    builder.setInitialDelay(schedule.after.toJavaDuration())
                }
                builder.setBackoffCriteria(
                    backoffPolicy(task.retry.backoff),
                    backoffInitialDelay(task.retry.backoff).toJavaDuration(),
                )
            }
            .build()

        when (task.policy) {
            BackgroundPolicy.Concurrent ->
                workManager.enqueueUniqueWork(task.id, ExistingWorkPolicy.KEEP, request)
            BackgroundPolicy.Conflate ->
                workManager.enqueueUniqueWork(task.kind.id, ExistingWorkPolicy.REPLACE, request)
            BackgroundPolicy.Queue ->
                workManager.enqueueUniqueWork(task.kind.id, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }

    private fun enqueuePeriodic(
        task: BackgroundTask,
        constraints: Constraints,
        inputData: Data,
        schedule: BackgroundSchedule.Periodic,
    ) {
        val builder = if (schedule.flex != null) {
            PeriodicWorkRequestBuilder<BackgroundTaskCoroutineWorker>(
                schedule.every.toJavaDuration(),
                schedule.flex.toJavaDuration(),
            )
        } else {
            PeriodicWorkRequestBuilder<BackgroundTaskCoroutineWorker>(schedule.every.toJavaDuration())
        }
        val request = builder
            .setConstraints(constraints)
            .setInputData(inputData)
            .setId(uuidFromTaskId(task.id))
            .setBackoffCriteria(
                backoffPolicy(task.retry.backoff),
                backoffInitialDelay(task.retry.backoff).toJavaDuration(),
            )
            .build()

        val periodicPolicy = when (task.policy) {
            BackgroundPolicy.Concurrent,
            BackgroundPolicy.Queue -> ExistingPeriodicWorkPolicy.KEEP
            BackgroundPolicy.Conflate -> ExistingPeriodicWorkPolicy.UPDATE
        }
        val uniqueName = when (task.policy) {
            BackgroundPolicy.Concurrent -> task.id
            BackgroundPolicy.Conflate, BackgroundPolicy.Queue -> task.kind.id
        }
        workManager.enqueueUniquePeriodicWork(uniqueName, periodicPolicy, request)
    }

    public actual fun cancel(taskId: String) {
        workManager.cancelWorkById(uuidFromTaskId(taskId))
    }

    public actual fun cancelByKind(kind: BackgroundTaskKind) {
        workManager.cancelUniqueWork(kind.id)
    }

    public actual fun observe(taskId: String): Flow<BackgroundStatus> =
        workManager.getWorkInfoByIdFlow(uuidFromTaskId(taskId)).map { info ->
            workInfoToStatus(info)
        }

    private fun workInfoToStatus(info: WorkInfo?): BackgroundStatus = when (info?.state) {
        null,
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.BLOCKED -> BackgroundStatus.Enqueued
        WorkInfo.State.RUNNING -> BackgroundStatus.Running
        WorkInfo.State.SUCCEEDED -> BackgroundStatus.Succeeded
        WorkInfo.State.FAILED -> BackgroundStatus.Failed()
        WorkInfo.State.CANCELLED -> BackgroundStatus.Cancelled
    }

    private fun backoffPolicy(strategy: BackoffStrategy): BackoffPolicy = when (strategy) {
        is BackoffStrategy.Linear -> BackoffPolicy.LINEAR
        is BackoffStrategy.Exponential -> BackoffPolicy.EXPONENTIAL
    }

    private fun backoffInitialDelay(strategy: BackoffStrategy): Duration = when (strategy) {
        is BackoffStrategy.Linear -> strategy.delay
        is BackoffStrategy.Exponential -> strategy.initialDelay
    }

    /**
     * Deterministically map a string `taskId` to a [java.util.UUID] so we can
     * use `cancelWorkById` / `getWorkInfoByIdFlow` without maintaining a
     * separate UUID lookup table.
     */
    private fun uuidFromTaskId(taskId: String): java.util.UUID =
        java.util.UUID.nameUUIDFromBytes(taskId.toByteArray(Charsets.UTF_8))
}
