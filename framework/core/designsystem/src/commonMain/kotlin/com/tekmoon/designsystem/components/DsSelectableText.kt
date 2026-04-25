package com.tekmoon.designsystem.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.tekmoon.designsystem.DsTheme

@Composable
fun DsSelectableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = DsTheme.typography.sm,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    SelectionContainer {
        DsText(
            text = text,
            modifier = modifier,
            style = style,
            textAlign = textAlign
        )
    }
}

@Composable
fun DsSelectableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = DsTheme.typography.sm,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    DsSelectableText(
        text = AnnotatedString(text),
        modifier = modifier,
        style = style,
        textAlign = textAlign
    )
}
