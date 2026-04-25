package com.tekmoon.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.platform.handCursor

@Composable
fun DsClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = DsTheme.typography.sm,
    color: Color = DsTheme.textColors.primary,
    hoverColor: Color = DsTheme.textColors.secondary,
    textAlign: TextAlign = TextAlign.Unspecified,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    DsText(
        text = text,
        modifier = modifier
            .pointerHoverIcon(Hand)
//            .handCursor()
            .clickable(onClick = onClick)
            .then(
                if (isHovered) Modifier else Modifier
            ),
        style = style,
        color = if (isHovered) hoverColor else color,
        textAlign = textAlign
    )
}
