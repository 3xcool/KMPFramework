package com.tekmoon.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kmpframework.framework.core.designsystem.generated.resources.Res
import kmpframework.framework.core.designsystem.generated.resources.inter_bold
import kmpframework.framework.core.designsystem.generated.resources.inter_regular
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Immutable
data class DsTypographyScale(
    val xs: TextStyle,
    val sm: TextStyle,
    val base: TextStyle,
    val md: TextStyle,
    val lg: TextStyle,
    val xl: TextStyle,
)

@OptIn(ExperimentalResourceApi::class)
val DsFontFamily @Composable get() = FontFamily(
    Font(
        resource = Res.font.inter_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resource = Res.font.inter_bold,
        weight = FontWeight.Bold
    )

)

@Composable
private fun baseTextStyle(
    fontSize: TextUnit,
    weight: FontWeight
)  = TextStyle(
    fontFamily = DsFontFamily,
    fontSize = fontSize,
    fontWeight = weight,
    lineHeight = fontSize * 1.4f
)

val DsTypography @Composable get() = DsTypographyScale(
    xs = baseTextStyle(12.sp, FontWeight.Normal),
    sm = baseTextStyle(14.sp, FontWeight.Normal),
    base = baseTextStyle(16.sp, FontWeight.Normal),
    md = baseTextStyle(18.sp, FontWeight.Normal),
    lg = baseTextStyle(20.sp, FontWeight.Bold),
    xl = baseTextStyle(24.sp, FontWeight.Bold),
)

object DsGray {
    fun lightness(value: Int): Color =
        Color.hsl(
            hue = 0f,
            saturation = 0f,
            lightness = value / 100f
        )

    val L100 = lightness(100)
    val L90  = lightness(90)
    val L80  = lightness(80)
    val L70  = lightness(70)
    val L60  = lightness(60)
    val L50  = lightness(50)
    val L40  = lightness(40)
    val L30  = lightness(30)
    val L20  = lightness(20)
    val L10  = lightness(10)
    val L0   = lightness(0)
}

@Immutable
data class DsTextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val disabled: Color,
)

val LightTextColors = DsTextColors(
    primary = DsGray.L0,
    secondary = DsGray.L30,
    tertiary = DsGray.L50,
    disabled = DsGray.L70
)

val DarkTextColors = DsTextColors(
    primary = DsGray.L100,
    secondary = DsGray.L70,
    tertiary = DsGray.L50,
    disabled = DsGray.L30
)

val LocalDsTypography = staticCompositionLocalOf<DsTypographyScale> {
    error("DsTypography not provided")
}

val LocalDsTextColors = staticCompositionLocalOf<DsTextColors> {
    error("DsTextColors not provided")
}