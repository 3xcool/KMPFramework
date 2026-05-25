package com.tekmoon.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.platform.LocalPlatformAccessibility
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ─── Model ───────────────────────────────────────────────────────────────────

/** Semantic intent of a toast — controls the leading accent color. */
enum class DsToastType { Info, Success, Warning, Error }

/**
 * A single transient toast.
 *
 * Toasts are simpler than [DsSnackbarMessage] — no action button. For feedback
 * that requires the user to do something, reach for [DsSnackbar] instead.
 *
 * @param message    Text shown in the toast.
 * @param type       Semantic intent.
 * @param durationMs How long (ms) the toast stays visible before auto-dismissing.
 *                   Pass [Long.MAX_VALUE] to keep it until dismissed manually.
 */
data class DsToastMessage(
    val message: String,
    val type: DsToastType = DsToastType.Info,
    val durationMs: Long = 2_500L,
)

// ─── Controller ──────────────────────────────────────────────────────────────

/**
 * State holder for [DsToastHost].
 *
 * ```kotlin
 * val toast = rememberDsToastController()
 *
 * DsToastHost(controller = toast, modifier = Modifier.align(Alignment.TopCenter))
 *
 * // Show from anywhere:
 * toast.show("Copied!", DsToastType.Success)
 * ```
 *
 * Back-to-back [show] calls replace the current toast — only one is visible
 * at a time. This matches the platform "toast" mental model and keeps the
 * UI from queuing up stale messages.
 */
class DsToastController {
    private val _current = MutableStateFlow<DsToastMessage?>(null)
    val current = _current.asStateFlow()

    fun show(message: DsToastMessage) { _current.update { message } }

    fun show(
        message: String,
        type: DsToastType = DsToastType.Info,
        durationMs: Long = 2_500L,
    ) = show(DsToastMessage(message, type, durationMs))

    fun dismiss() { _current.update { null } }
}

@Composable
fun rememberDsToastController(): DsToastController = remember { DsToastController() }

// ─── Host ────────────────────────────────────────────────────────────────────

/**
 * Renders the current toast from [controller] and announces every new message
 * through the platform accessibility service (TalkBack on Android, VoiceOver on
 * iOS) — critical for transient feedback the user might not be looking at.
 *
 * Place at the top of your scaffold or screen overlay.
 *
 * @param controller The [DsToastController] driving visibility.
 * @param modifier   Typically used to position the host
 *                   (e.g. `Modifier.align(Alignment.TopCenter)`).
 */
@Composable
fun DsToastHost(
    controller: DsToastController,
    modifier: Modifier = Modifier,
) {
    val current by controller.current.collectAsState()
    val accessibility = LocalPlatformAccessibility.current

    LaunchedEffect(current) {
        val msg = current ?: return@LaunchedEffect
        // Fire the screen-reader announcement up front — happens once per show, even
        // if the user dismisses before the animation finishes.
        accessibility.announce(msg.message)
        if (msg.durationMs != Long.MAX_VALUE) {
            delay(msg.durationMs)
            controller.dismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AnimatedVisibility(
            visible = current != null,
            enter   = slideInVertically { -it } + fadeIn(),
            exit    = slideOutVertically { -it } + fadeOut(),
        ) {
            current?.let { msg -> DsToastItem(msg) }
        }
    }
}

// ─── Item ────────────────────────────────────────────────────────────────────

@Composable
private fun DsToastItem(message: DsToastMessage) {
    val accent = resolveToastAccent(message.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DsTheme.shapes.md)
            .background(DsTheme.surfaceColors.floating)
            .padding(horizontal = DsTheme.spacing.md, vertical = DsTheme.spacing.sm)
            // LiveRegion.Polite ensures assistive tech reads any subsequent text change
            // within this region — belt-and-suspenders with the explicit announce() above.
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsTheme.spacing.sm),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .background(accent, DsTheme.shapes.pill)
                .padding(vertical = DsTheme.spacing.sm),
        )
        DsText(
            text = message.message,
            style = DsTheme.typography.sm,
            color = DsTheme.textColors.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─── Internal ────────────────────────────────────────────────────────────────

@Composable
private fun resolveToastAccent(type: DsToastType): Color = when (type) {
    DsToastType.Info    -> DsTheme.colors.info
    DsToastType.Success -> DsTheme.colors.success
    DsToastType.Warning -> DsTheme.colors.warning
    DsToastType.Error   -> DsTheme.colors.danger
}
