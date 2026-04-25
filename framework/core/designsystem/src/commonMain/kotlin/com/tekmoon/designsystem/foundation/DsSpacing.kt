package com.tekmoon.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.tokens.SpacingTokens

@Immutable
data class DsSpacing(
    // raw tokens
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,

    // semantic
    val contentPadding: Dp,
    val cardPadding: Dp,
    val sectionGap: Dp,
    val dialogPadding: Dp
)

val DefaultSpacing = DsSpacing(
    xs = SpacingTokens.XS.dp,
    sm = SpacingTokens.SM.dp,
    md = SpacingTokens.MD.dp,
    lg = SpacingTokens.LG.dp,
    xl = SpacingTokens.XL.dp,
    xxl = SpacingTokens.XXL.dp,

    contentPadding = SpacingTokens.LG.dp,
    cardPadding = SpacingTokens.XL.dp,
    dialogPadding = SpacingTokens.XL.dp,
    sectionGap = SpacingTokens.XXL.dp
)