package com.tekmoon.designsystem.tokens

data class SemanticColors(
    val bgDark: Float,
    val bg: Float,
    val bgLight: Float,
    val text: Float,
    val textMuted: Float
)

val DarkSemanticColors = SemanticColors(
//    bgDark = NeutralScale.N0,
//    bg = NeutralScale.N5,
//    bgLight = NeutralScale.N10,
    bgDark = NeutralScale.N10,
    bg = NeutralScale.N15,
    bgLight = NeutralScale.N20,
    text = NeutralScale.N95,
    textMuted = NeutralScale.N70
)

val LightSemanticColors = SemanticColors(
    bgDark = NeutralScale.N90,
    bg = NeutralScale.N95,
    bgLight = NeutralScale.N100,
    text = NeutralScale.N5,
    textMuted = NeutralScale.N30
)
