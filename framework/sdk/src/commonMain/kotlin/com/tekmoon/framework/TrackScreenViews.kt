package com.tekmoon.framework

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tekmoon.analytics.AnalyticsClient
import com.tekmoon.kompass.NavController
import com.tekmoon.kompass.screenChanges

/**
 * Wires Kompass navigation events to the analytics screen-view channel. Drop this anywhere
 * inside your app's root composable — typically right next to the `NavigationHost` — and
 * `analytics.screen(...)` fires automatically on every destination change.
 *
 * ```kotlin
 * val navController = rememberNavController(start = MainGraph.Home)
 * TrackScreenViews(navController)
 * NavigationHost(navController, ...)
 * ```
 *
 * The emitted event includes:
 * - **name**: the new top-most `destinationId`
 * - **params**: `{ "from": <previous destinationId or null> }` — `null` on the cold-start landing
 *
 * Use the [analytics] override when you have multiple analytics clients in scope (e.g. a
 * scoped test recorder). The default reads [Framework.analytics], so the call is safe only
 * after `Framework.start(...)` has run.
 */
@Composable
fun TrackScreenViews(
    navController: NavController,
    analytics: AnalyticsClient = Framework.analytics,
) {
    LaunchedEffect(navController, analytics) {
        navController.screenChanges().collect { change ->
            analytics.screen(
                name = change.current,
                params = mapOf("from" to change.previous),
            )
        }
    }
}
