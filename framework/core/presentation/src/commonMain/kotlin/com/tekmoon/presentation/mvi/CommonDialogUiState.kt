package com.tekmoon.presentation.mvi

import com.tekmoon.designsystem.image.DsImageSource
import com.tekmoon.designsystem.ui.UiText

/**
 * Base UI state for a confirm/alert-style dialog. Feature dialog states extend this
 * and override only what they need. Uses design-system [UiText] / [DsImageSource].
 */
abstract class CommonDialogUiState(
    open val title: UiText? = null,
    open val message: UiText? = null,
    open val cancelText: UiText? = null,
    open val confirmText: UiText? = null,
    open val icon: DsImageSource? = null,
    open val isDestructive: Boolean = false,
    open val cancelable: Boolean = true,
    open val showCancelButton: Boolean = true,
    open val onConfirm: () -> Unit = {},
    open val onCancel: () -> Unit = {},
)
