package com.tekmoon.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.tekmoon.designsystem.tokens.RadiusTokens

@Immutable
data class DsShapes(
    // raw tokens
    val sm: RoundedCornerShape,
    val md: RoundedCornerShape,
    val lg: RoundedCornerShape,
    val xl: RoundedCornerShape,

    // semantic
    val card: RoundedCornerShape,
    val surface: RoundedCornerShape,
    val pill: RoundedCornerShape,
    val dialog: RoundedCornerShape
)

val DsShapesDefault = DsShapes(
    sm = RoundedCornerShape(RadiusTokens.SM.dp),
    md = RoundedCornerShape(RadiusTokens.MD.dp),
    lg = RoundedCornerShape(RadiusTokens.LG.dp),
    xl = RoundedCornerShape(RadiusTokens.XL.dp),

    card = RoundedCornerShape(RadiusTokens.XL.dp),
    surface = RoundedCornerShape(RadiusTokens.NONE.dp),
    pill = RoundedCornerShape(999.dp),
    dialog = RoundedCornerShape(RadiusTokens.XL.dp)
)