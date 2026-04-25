package com.tekmoon.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.tekmoon.designsystem.DsTheme

@Composable
fun DsLinkText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = DsTheme.typography.sm,
    onLinkClick: (String) -> Unit
) {
    DsClickableText(
        text = text,
        modifier = modifier,
        style = style,
        onClick = {
            text
                .getStringAnnotations("URL", 0, text.length)
                .firstOrNull()
                ?.let { onLinkClick(it.item) }
        }
    )
}
