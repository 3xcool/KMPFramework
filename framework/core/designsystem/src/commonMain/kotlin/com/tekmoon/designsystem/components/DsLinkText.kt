package com.tekmoon.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import com.tekmoon.designsystem.DsTheme

/**
 * @param contentDescription Accessibility label for the link. Pass a descriptive sentence
 *                           that reads well in isolation ("Open privacy policy") rather than
 *                           a bare URL.
 */
@Composable
fun DsLinkText(
    text: AnnotatedString,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = DsTheme.typography.sm,
    onLinkClick: (String) -> Unit,
) {
    DsClickableText(
        text = text,
        contentDescription = contentDescription,
        modifier = modifier,
        style = style,
        onClick = {
            text
                .getStringAnnotations("URL", 0, text.length)
                .firstOrNull()
                ?.let { onLinkClick(it.item) }
        },
    )
}
