package com.tekmoon.backgroundwork

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundSchedulerPolicyTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val schedulerScope = CoroutineScope(dispatcher + SupervisorJob())

    @AfterTest
    fun tearDown() {
        schedulerScope.cancel()
    }

    @Test
    fun concurrent_runs_tasks_in_parallel() = testScope.runTest {
        val registry = BackgroundTaskRegistry()
        val concurrentObserved = mutableListOf<Int>()
        var inFlight = 0
        registry.register(StringTaskKind("ping")) { _ ->
            inFlight++
            concurrentObserved.add(inFlight)
            delay(100)
            inFlight--
            BackgroundResult.Success
        }
        val scheduler = BackgroundScheduler(schedulerScope, registry)

        scheduler.schedule(task("t1", "ping", BackgroundPolicy.Concurrent))
        scheduler.schedule(task("t2", "ping", BackgroundPolicy.Concurrent))
        scheduler.schedule(task("t3", "ping", BackgroundPolicy.Concurrent))
        advanceUntilIdle()

        assertEquals(3, concurrentObserved.max())
    }

    @Test
    fun conflate_cancels_in_flight_same_kind() = testScope.runTest {
        val registry = BackgroundTaskRegistry()
        val completed = mutableListOf<String>()
        registry.register(StringTaskKind("upload")) { input ->
            // `delay()` throws CancellationException on cancel, which
            // propagates naturally — no need to catch and rethrow.
            delay(500)
            completed.add(input["id"] ?: "")
            BackgroundResult.Success
        }
        val scheduler = BackgroundScheduler(schedulerScope, registry)

        scheduler.schedule(task("t1", "upload", BackgroundPolicy.Conflate, input = persistentMapOf("id" to "v1")))
        advanceTimeBy(100)
        scheduler.schedule(task("t2", "upload", BackgroundPolicy.Conflate, input = persistentMapOf("id" to "v2")))
        advanceUntilIdle()

        assertEquals(listOf("v2"), completed)
        val t1Status = scheduler.observe("t1").firstOrNull()
        assertTrue(t1Status is BackgroundStatus.Cancelled, "expected t1 cancelled, was $t1Status")
    }

    @Test
    fun queue_serializes_same_kind_fifo() = testScope.runTest {
        val registry = BackgroundTaskRegistry()
        val started = mutableListOf<String>()
        val finished = mutableListOf<String>()
        registry.register(StringTaskKind("sync")) { input ->
            val id = input["id"] ?: ""
            started.add(id)
            delay(100)
            finished.add(id)
            BackgroundResult.Success
        }
        val scheduler = BackgroundScheduler(schedulerScope, registry)

        scheduler.schedule(task("t1", "sync", BackgroundPolicy.Queue, input = persistentMapOf("id" to "a")))
        scheduler.schedule(task("t2", "sync", BackgroundPolicy.Queue, input = persistentMapOf("id" to "b")))
        scheduler.schedule(task("t3", "sync", BackgroundPolicy.Queue, input = persistentMapOf("id" to "c")))
        advanceUntilIdle()

        // FIFO: each task starts only after the previous finishes.
        assertEquals(listOf("a", "b", "c"), started)
        assertEquals(listOf("a", "b", "c"), finished)
    }

    @Test
    fun status_transitions_enqueued_running_succeeded() = testScope.runTest {
        val registry = BackgroundTaskRegistry()
        registry.register(StringTaskKind("once")) { BackgroundResult.Success }
        val scheduler = BackgroundScheduler(schedulerScope, registry)

        scheduler.schedule(task("t1", "once"))
        advanceUntilIdle()
        val final = withTimeout(1.seconds) { scheduler.observe("t1").first() }
        assertEquals(BackgroundStatus.Succeeded, final)
    }

    @Test
    fun failure_with_retry_eventually_succeeds() = testScope.runTest {
        val registry = BackgroundTaskRegistry()
        var attempts = 0
        registry.register(StringTaskKind("retry-me")) {
            attempts++
            if (attempts < 3) BackgroundResult.Failure(retriable = true) else BackgroundResult.Success
        }
        val scheduler = BackgroundScheduler(schedulerScope, registry)
        val taskUnderTest = task("t1", "retry-me").copy(
            retry = BackgroundRetry(
                maxAttempts = 3,
                backoff = BackoffStrategy.Linear(10.milliseconds),
            ),
        )

        scheduler.schedule(taskUnderTest)
        advanceUntilIdle()

        assertEquals(3, attempts)
        val final = withTimeout(1.seconds) { scheduler.observe("t1").first() }
        assertEquals(BackgroundStatus.Succeeded, final)
    }

    @Test
    fun missing_handler_is_failure() = testScope.runTest {
        val registry = BackgroundTaskRegistry()
        val scheduler = BackgroundScheduler(schedulerScope, registry)
        scheduler.schedule(task("t1", "no-such-kind"))
        advanceUntilIdle()
        val final = withTimeout(1.seconds) { scheduler.observe("t1").first() }
        assertTrue(final is BackgroundStatus.Failed, "expected Failed, was $final")
    }

    @Test
    fun cancel_marks_status_cancelled() = testScope.runTest {
        val registry = BackgroundTaskRegistry()
        registry.register(StringTaskKind("slow")) {
            delay(5_000)
            BackgroundResult.Success
        }
        val scheduler = BackgroundScheduler(schedulerScope, registry)
        scheduler.schedule(task("t1", "slow"))
        advanceTimeBy(100)
        scheduler.cancel("t1")
        advanceUntilIdle()
        val final = withTimeout(1.seconds) { scheduler.observe("t1").first() }
        assertEquals(BackgroundStatus.Cancelled, final)
    }

    private fun task(
        id: String,
        kind: String,
        policy: BackgroundPolicy = BackgroundPolicy.Concurrent,
        input: kotlinx.collections.immutable.ImmutableMap<String, String> = persistentMapOf(),
    ) = BackgroundTask(
        id = id,
        kind = StringTaskKind(kind),
        policy = policy,
        input = input,
    )
}
