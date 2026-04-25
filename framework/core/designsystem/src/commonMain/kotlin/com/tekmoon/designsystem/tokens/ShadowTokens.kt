package com.tekmoon.designsystem.tokens

data class ShadowSpec(
    val alpha: Float,
    val offsetY: Float,
    val blur: Float
)

object ShadowTokens {
    val None = ShadowSpec(0f, 0f, 0f)
    val Small = ShadowSpec(0.15f, 1f, 4f)
    val Medium = ShadowSpec(0.20f, 4f, 12f)
    val Large = ShadowSpec(0.25f, 8f, 24f)
}
