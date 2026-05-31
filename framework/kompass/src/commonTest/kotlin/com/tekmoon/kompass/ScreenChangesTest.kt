package com.tekmoon.kompass

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenChangesTest {

    // ---- initial emission --------------------------------------------------

    @Test fun initial_emission_has_null_previous() = runTest(UnconfinedTestDispatcher()) {
        val controller = navController(start = "home")

        val first = controller.screenChanges().first()
        assertEquals(ScreenChange(current = "home", previous = null), first)
    }

    // ---- navigate ----------------------------------------------------------

    @Test fun navigate_to_new_destination_emits_change_with_previous_populated() =
        runTest(UnconfinedTestDispatcher()) {
            val controller = navController(start = "home")
            val collected = mutableListOf<ScreenChange>()
            val job = launch {
                controller.screenChanges().take(2).toList(collected)
            }

            // Snapshot needs an apply notification after the (synchronous) state mutation
            // for snapshotFlow to pick the new value up.
            controller.navigate(entry(destinationId = "detail"))
            Snapshot.sendApplyNotifications()

            job.join()
            assertEquals(
                listOf(
                    ScreenChange(current = "home", previous = null),
                    ScreenChange(current = "detail", previous = "home"),
                ),
                collected,
            )
        }

    // ---- de-duplication ---------------------------------------------------

    @Test fun consecutive_same_destination_is_not_re_emitted() =
        runTest(UnconfinedTestDispatcher()) {
            val controller = navController(start = "home")
            val collected = mutableListOf<ScreenChange>()
            val job = launch {
                controller.screenChanges().take(2).toList(collected)
            }

            // Push a new entry with the SAME destinationId — only scopeId differs.
            // distinctUntilChanged on destinationId should suppress this.
            controller.navigate(
                entry(destinationId = "home", scopeId = NavigationScopeId("home#2")),
            )
            Snapshot.sendApplyNotifications()

            // Now navigate to a genuinely different destination so `take(2)` can complete.
            controller.navigate(entry(destinationId = "detail"))
            Snapshot.sendApplyNotifications()

            job.join()
            assertEquals(
                listOf(
                    ScreenChange(current = "home", previous = null),
                    ScreenChange(current = "detail", previous = "home"),
                ),
                collected,
            )
        }

    // ---- pop --------------------------------------------------------------

    @Test fun pop_emits_the_uncovered_destination() = runTest(UnconfinedTestDispatcher()) {
        val controller = navController(start = "home")
        controller.navigate(entry(destinationId = "detail"))

        val collected = mutableListOf<ScreenChange>()
        val job = launch {
            controller.screenChanges().take(2).toList(collected)
        }

        controller.pop()
        Snapshot.sendApplyNotifications()

        job.join()
        assertEquals(
            listOf(
                ScreenChange(current = "detail", previous = null),  // initial seed
                ScreenChange(current = "home", previous = "detail"),
            ),
            collected,
        )
    }
}

// region: helpers

private fun navController(start: String): NavController {
    val initial = NavigationState(
        backStack = persistentListOf(entry(destinationId = start)),
    )
    return NavController(
        navState = mutableStateOf(initial),
        handler = NavigationHandler(),
        json = Json,
    )
}

private fun entry(
    destinationId: String,
    scopeId: NavigationScopeId = NavigationScopeId(destinationId),
): BackStackEntry = BackStackEntry(
    destinationId = destinationId,
    scopeId = scopeId,
)

// endregion
