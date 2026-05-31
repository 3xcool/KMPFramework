package com.tekmoon.presentation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable

fun Modifier.clickableWithoutIndication(onClick: () -> Unit) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.then(Modifier.clickable(interactionSource, indication = null, onClick = onClick))
}

fun Modifier.combinedClickableWithoutIndication(onLongClick: () -> Unit, onClick: () -> Unit) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.then(Modifier.combinedClickable(
        interactionSource,
        indication = null,
        onClick = onClick,
        onLongClick = onLongClick
    ))
}
