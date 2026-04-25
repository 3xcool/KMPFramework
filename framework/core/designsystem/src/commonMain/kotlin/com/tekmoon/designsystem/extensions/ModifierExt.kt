package com.tekmoon.designsystem.extensions

import androidx.compose.ui.Modifier

inline fun Modifier.applyIf(
    condition: Boolean,
    block: Modifier.() -> Modifier
): Modifier =
    if (condition) {
        this.then(block(Modifier))
    } else {
        this
    }