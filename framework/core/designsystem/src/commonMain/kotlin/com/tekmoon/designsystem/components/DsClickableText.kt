package com.tekmoon.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.DsTheme
import com.tekmoon.designsystem.foundation.dsMinimumTouchTarget

/**
 * @param contentDescription Accessibility label that screen readers announce instead of the
 *                           text content. Pass the same string as the visible text for simple
 *                           cases, or a more descriptive label when the visible text is terse
 *                           (e.g. "Read more"). Pass `null` only if the underlying text is
 *                           already a complete description.
 */
@Composable
fun DsClickableText(
    text: AnnotatedString,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = DsTheme.typography.sm,
    color: Color = DsTheme.textColors.primary,
    hoverColor: Color = DsTheme.textColors.secondary,
    textAlign: TextAlign = TextAlign.Unspecified,
    onClick: () -> Unit,
) {
    var isHovered by remember { mutableStateOf(false) }

    DsText(
        text = text,
        modifier = modifier
            .pointerHoverIcon(Hand)
            .dsMinimumTouchTarget(48.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        style = style,
        color = if (isHovered) hoverColor else color,
        textAlign = textAlign,
    )
}
