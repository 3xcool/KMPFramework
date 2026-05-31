package com.tekmoon.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.analytics.LocalAnalytics
import com.tekmoon.designsystem.foundation.dsMinimumTouchTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ─── Model ───────────────────────────────────────────────────────────────────

/** Semantic intent of a snackbar — mirrors [AlertType] but only for transient messages. */
enum class DsSnackbarType { Info, Success, Warning, Error }

/**
 * A single snackbar message.
 *
 * @param message         Text shown in the snackbar.
 * @param type            Semantic intent — controls the accent color.
 * @param actionLabel     Optional label for the action button.
 * @param onAction        Invoked when the action button is tapped.
 * @param durationMs      How long (ms) the snackbar stays visible before auto-dismissing.
 *                        Pass [Long.MAX_VALUE] to keep it until dismissed manually.
 * @param analyticsId     Stable identifier emitted with the `"ds_snackbar_action_clicked"`
 *                        analytics event when the action button is tapped. `null` (default)
 *                        disables analytics for this snackbar. Ignored if [onAction] is `null`
 *                        (no action button rendered).
 * @param analyticsParams Extra params merged into the analytics event payload alongside
 *                        `id` + `type` + `label`.
 */
data class DsSnackbarMessage(
    val message: String,
    val type: DsSnackbarType = DsSnackbarType.Info,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMs: Long = 3_000L,
    val analyticsId: String? = null,
    val analyticsParams: Map<String, Any?> = emptyMap(),
)

// ─── Controller ──────────────────────────────────────────────────────────────

/**
 * State holder for [DsSnackbarHost].
 *
 * Call [show] from any coroutine scope (e.g. a ViewModel or a LaunchedEffect)
 * and place [DsSnackbarHost] anywhere in the composition tree.
 *
 * ```kotlin
 * val snackbar = rememberDsSnackbarController()
 *
 * DsSnackbarHost(controller = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
 *
 * // Show from a button click:
 * snackbar.show(DsSnackbarMessage("Saved!", type = DsSnackbarType.Success))
 * ```
 */
class DsSnackbarController {
    private val _current = MutableStateFlow<DsSnackbarMessage?>(null)
    val current = _current.asStateFlow()

    fun show(message: DsSnackbarMessage) { _current.update { message } }

    fun show(
        message: String,
        type: DsSnackbarType = DsSnackbarType.Info,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        durationMs: Long = 3_000L,
        analyticsId: String? = null,
        analyticsParams: Map<String, Any?> = emptyMap(),
    ) = show(
        DsSnackbarMessage(
            message = message,
            type = type,
            actionLabel = actionLabel,
            onAction = onAction,
            durationMs = durationMs,
            analyticsId = analyticsId,
            analyticsParams = analyticsParams,
        )
    )

    fun dismiss() { _current.update { null } }
}

@Composable
fun rememberDsSnackbarController(): DsSnackbarController = remember { DsSnackbarController() }

// ─── Host ────────────────────────────────────────────────────────────────────

/**
 * Renders the current snackbar from [controller].
 *
 * Place this at the bottom of your scaffold or screen overlay.
 *
 * @param controller  The [DsSnackbarController] driving visibility.
 * @param modifier    Typically used to position the host (e.g. `Modifier.align(Alignment.BottomCenter)`).
 */
@Composable
fun DsSnackbarHost(
    controller: DsSnackbarController,
    modifier: Modifier = Modifier,
) {
    val current by controller.current.collectAsState()

    // Auto-dismiss after duration
    LaunchedEffect(current) {
        val msg = current ?: return@LaunchedEffect
        if (msg.durationMs != Long.MAX_VALUE) {
            delay(msg.durationMs)
            controller.dismiss()
        }
    }

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        AnimatedVisibility(
            visible = current != null,
            enter = slideInVertically { it } + fadeIn(),
            exit  = slideOutVertically { it } + fadeOut(),
        ) {
            current?.let { msg ->
                DsSnackbarItem(
                    message = msg,
                    onDismiss = { controller.dismiss() }
                )
            }
        }
    }
}

// ─── Item ────────────────────────────────────────────────────────────────────

@Composable
private fun DsSnackbarItem(
    message: DsSnackbarMessage,
    onDismiss: () -> Unit,
) {
    val accentColor = resolveSnackbarAccent(message.type)
    val analytics = LocalAnalytics.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DsTheme.shapes.md)
            .background(DsTheme.surfaceColors.floating)
            .padding(horizontal = DsTheme.spacing.md, vertical = DsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsTheme.spacing.sm)
    ) {
        // Accent stripe
        Box(
            Modifier
                .background(accentColor, DsTheme.shapes.pill)
                .padding(horizontal = 3.dp, vertical = DsTheme.spacing.sm)
        )

        // Message
        DsText(
            text = message.message,
            style = DsTheme.typography.sm,
            color = DsTheme.textColors.primary,
            modifier = Modifier.weight(1f)
        )

        // Action — visible label doubles as the screen-reader announcement.
        if (message.actionLabel != null) {
            Box(
                modifier = Modifier
                    .dsMinimumTouchTarget(48.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        if (message.analyticsId != null) {
                            analytics.track(
                                event = "ds_snackbar_action_clicked",
                                params = mapOf(
                                    "id" to message.analyticsId,
                                    "type" to message.type.name,
                                    "label" to message.actionLabel,
                                ) + message.analyticsParams,
                            )
                        }
                        message.onAction?.invoke()
                        onDismiss()
                    }
                    .semantics { role = Role.Button }
                    .padding(horizontal = DsTheme.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                DsText(
                    text = message.actionLabel,
                    style = DsTheme.typography.sm,
                    color = accentColor,
                )
            }
        }
    }
}

// ─── Internal ────────────────────────────────────────────────────────────────

@Composable
private fun resolveSnackbarAccent(type: DsSnackbarType): Color = when (type) {
    DsSnackbarType.Info    -> DsTheme.colors.info
    DsSnackbarType.Success -> DsTheme.colors.success
    DsSnackbarType.Warning -> DsTheme.colors.warning
    DsSnackbarType.Error   -> DsTheme.colors.danger
}
