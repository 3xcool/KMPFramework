package com.tekmoon.designsystem.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.tekmoon.designsystem.DsTheme

@Composable
fun DsText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DsTheme.textColors.primary,
    style: TextStyle = DsTheme.typography.sm,
    textAlign: TextAlign= TextAlign.Unspecified,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    DsText(
        text = AnnotatedString(text),
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow,
        onTextLayout = onTextLayout
    )
}


@Composable
fun DsText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = DsTheme.textColors.primary,
    style: TextStyle = DsTheme.typography.sm,
    textAlign: TextAlign= TextAlign.Unspecified,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            TextStyle(
                color = color,
                textAlign = textAlign
            )
        ),
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout
    )
}



/* ─────────────────────────────────────────────
 * Display / Hero
 * 24sp · Bold · primary
 * ───────────────────────────────────────────── */

@Composable
fun DsTextDisplay(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    DsText(
        text = text,
        modifier = modifier,
        style = DsTheme.typography.xl,
        color = DsTheme.textColors.primary,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/* ─────────────────────────────────────────────
 * Title
 * 20sp · Bold · primary
 * ───────────────────────────────────────────── */

@Composable
fun DsTextTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    DsText(
        text = text,
        modifier = modifier,
        style = DsTheme.typography.lg,
        color = DsTheme.textColors.primary,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/* ─────────────────────────────────────────────
 * SubTitle
 * 18sp · Regular · secondary
 * ───────────────────────────────────────────── */

@Composable
fun DsTextSubTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    DsText(
        text = text,
        modifier = modifier,
        style = DsTheme.typography.md,
        color = DsTheme.textColors.secondary,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/* ─────────────────────────────────────────────
 * Body / Display text
 * 16sp · Regular · primary
 * ───────────────────────────────────────────── */

@Composable
fun DsTextBody(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    DsText(
        text = text,
        modifier = modifier,
        style = DsTheme.typography.base,
        color = DsTheme.textColors.primary,
        textAlign = textAlign,
        maxLines = maxLines
    )
}

/* ─────────────────────────────────────────────
 * Label / Caption
 * 14sp · Regular · secondary
 * ───────────────────────────────────────────── */

@Composable
fun DsTextLabel(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    DsText(
        text = text,
        modifier = modifier,
        style = DsTheme.typography.sm,
        color = DsTheme.textColors.secondary,
        textAlign = textAlign,
        maxLines = maxLines
    )
}

/* ─────────────────────────────────────────────
 * Caption / Hint
 * 12sp · Regular · tertiary
 * ───────────────────────────────────────────── */

@Composable
fun DsTextCaption(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    DsText(
        text = text,
        modifier = modifier,
        style = DsTheme.typography.xs,
        color = DsTheme.textColors.tertiary,
        textAlign = textAlign,
        maxLines = maxLines
    )
}