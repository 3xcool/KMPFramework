package com.tekmoon.designsystem.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand

actual fun Modifier.handCursor(): Modifier =
    this.pointerHoverIcon(Hand)