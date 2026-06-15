package com.bandu.tiji.core.designsystem.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

val BanduLightColorScheme = lightColorScheme(
    primary = BanduColors.Primary,
    onPrimary = BanduColors.OnDark,
    primaryContainer = BanduColors.PrimaryContainer,
    onPrimaryContainer = BanduColors.TextPrimary,
    secondary = BanduColors.TextSecondary,
    onSecondary = BanduColors.OnDark,
    secondaryContainer = BanduColors.SurfaceMuted,
    onSecondaryContainer = BanduColors.TextPrimary,
    tertiary = BanduColors.Accent,
    onTertiary = BanduColors.OnDark,
    tertiaryContainer = BanduColors.AccentContainer,
    onTertiaryContainer = BanduColors.TextPrimary,
    error = BanduColors.Danger,
    onError = BanduColors.OnDark,
    errorContainer = BanduColors.DangerContainer,
    onErrorContainer = BanduColors.Danger,
    background = BanduColors.Background,
    onBackground = BanduColors.TextPrimary,
    surface = BanduColors.Surface,
    onSurface = BanduColors.TextPrimary,
    surfaceVariant = BanduColors.SurfaceSubtle,
    onSurfaceVariant = BanduColors.TextSecondary,
    outline = BanduColors.Border,
    outlineVariant = BanduColors.BorderStrong,
    scrim = BanduColors.TextPrimary,
)

val BanduTypography = Typography(
    displayLarge = banduTextStyle(size = 57, lineHeight = 64, weight = FontWeight.Normal),
    displayMedium = banduTextStyle(size = 45, lineHeight = 52, weight = FontWeight.Normal),
    displaySmall = banduTextStyle(size = 36, lineHeight = 44, weight = FontWeight.Normal),
    headlineLarge = banduTextStyle(size = 32, lineHeight = 40, weight = FontWeight.SemiBold),
    headlineMedium = banduTextStyle(size = 28, lineHeight = 36, weight = FontWeight.SemiBold),
    headlineSmall = banduTextStyle(size = 24, lineHeight = 32, weight = FontWeight.SemiBold),
    titleLarge = banduTextStyle(size = 22, lineHeight = 28, weight = FontWeight.SemiBold),
    titleMedium = banduTextStyle(size = 16, lineHeight = 24, weight = FontWeight.Medium),
    titleSmall = banduTextStyle(size = 14, lineHeight = 20, weight = FontWeight.Medium),
    bodyLarge = banduTextStyle(size = 16, lineHeight = 24, weight = FontWeight.Normal),
    bodyMedium = banduTextStyle(size = 14, lineHeight = 20, weight = FontWeight.Normal),
    bodySmall = banduTextStyle(size = 12, lineHeight = 16, weight = FontWeight.Normal),
    labelLarge = banduTextStyle(size = 14, lineHeight = 20, weight = FontWeight.Medium),
    labelMedium = banduTextStyle(size = 12, lineHeight = 16, weight = FontWeight.Medium),
    labelSmall = banduTextStyle(size = 11, lineHeight = 16, weight = FontWeight.Medium),
)

val BanduShapes = Shapes(
    extraSmall = RoundedCornerShape(BanduRadii.Small),
    small = RoundedCornerShape(BanduRadii.Small),
    medium = RoundedCornerShape(BanduRadii.Card),
    large = RoundedCornerShape(BanduRadii.Large),
    extraLarge = RoundedCornerShape(BanduRadii.Large),
)

@Composable
fun BanduTijiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BanduLightColorScheme,
        typography = BanduTypography,
        shapes = BanduShapes,
        content = content,
    )
}

private fun banduTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

@Preview(showBackground = true, name = "伴读题集浅色主题")
@Composable
private fun BanduThemePreview() {
    BanduTijiTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BanduSpacing.PageHorizontal),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = BanduElevation.Card,
            shadowElevation = BanduElevation.Card,
        ) {
            Column(
                modifier = Modifier.padding(BanduSpacing.PageHorizontal),
                verticalArrangement = Arrangement.spacedBy(BanduSpacing.CardGap),
            ) {
                Text("伴读题集", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "整理错题，持续复习",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
