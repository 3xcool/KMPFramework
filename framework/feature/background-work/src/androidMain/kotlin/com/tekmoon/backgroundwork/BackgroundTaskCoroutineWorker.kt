package com.tekmoon.backgroundwork

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager bridge that dispatches a scheduled task to its
 * [BackgroundTaskHandler] via the process-wide [AndroidRegistryHolder].
 *
 * WorkManager instantiates this class via reflection, so we can't take
 * dependencies through the constructor — the registry has to be reachable
 * via a static holder. The holder is populated when an app constructs
 * [BackgroundScheduler] for the first time (and again on every process
 * launch by the app's own bootstrap code, since the registry is in-memory).
 */
internal class BackgroundTaskCoroutineWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val kind = inputData.getString(KEY_TASK_KIND)
            ?: return Result.failure()
        val handler = AndroidRegistryHolder.registry()?.handler(kind)
            ?: return Result.failure()

        val input: Map<String, String> = inputData.keyValueMap
            .filterKeys { it.startsWith(INPUT_PREFIX) }
            .mapKeys { it.key.removePrefix(INPUT_PREFIX) }
            .mapValues { it.value as? String ?: it.value.toString() }

        return when (val outcome = handler.execute(input)) {
            is BackgroundResult.Success -> Result.success()
            is BackgroundResult.Cancelled -> Result.success()
            is BackgroundResult.Failure ->
                if (outcome.retriable && runAttemptCount < MAX_RUN_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    internal companion object {
        const val KEY_TASK_ID = "tekmoon.bg.task_id"
        const val KEY_TASK_KIND = "tekmoon.bg.task_kind"
        const val INPUT_PREFIX = "tekmoon.bg.input."
        // BackgroundRetry.DEFAULT_MAX_ATTEMPTS = 3; WorkManager's runAttemptCount is 0-indexed.
        const val MAX_RUN_ATTEMPTS = 3
    }
}
