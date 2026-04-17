package com.cst.richard.vppassword.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeVariant(val titleEn: String, val titleCn: String) {
    PEBBLE_LIGHT("Pebble Light", "鹅卵石浅色"),
    MIDNIGHT_DARK("Midnight Dark", "午夜极暗"),
    OCEAN_BLUE("Ocean Blue", "深海静谧青"),
    FOREST_GREEN("Forest Green", "迷雾森林绿"),
    CYBERPUNK("Cyberpunk", "赛博朋克深紫"),
    IVORY_SILK("Ivory Silk", "老钱·象牙丝绸"),
    ROYAL_NAVY("Royal Navy", "老钱·皇家海军"),
    OXFORD_WINE("Oxford Wine", "老钱·牛津红酒")
}

val MidnightColorScheme = darkColorScheme(
    primary = SoftCyan,
    onPrimary = DeepBlack,
    secondary = AccentGold,
    background = DeepBlack,
    surface = DarkGrey,
    surfaceVariant = CardGrey,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f)
)

val PebbleColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = AccentGold,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E5EA),
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black.copy(alpha = 0.7f)
)

val OceanColorScheme = darkColorScheme(
    primary = OceanAccent,
    background = OceanBlueDark,
    surface = OceanBlueDark,
    surfaceVariant = OceanCard,
    onBackground = Color.White,
    onSurface = Color.White
)

val ForestColorScheme = darkColorScheme(
    primary = ForestAccent,
    background = ForestGreenDark,
    surface = ForestGreenDark,
    surfaceVariant = ForestCard,
    onBackground = Color.White,
    onSurface = Color.White
)

val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkPink,
    background = CyberpunkDark,
    surface = CyberpunkDark,
    surfaceVariant = CyberpunkCard,
    onBackground = Color.White,
    onSurface = Color.White
)

// --- OLD MONEY Color Schemes ---

val IvorySilkColorScheme = lightColorScheme(
    primary = CamelTan,
    onPrimary = Color.White,
    secondary = CamelTan,
    background = IvoryBeige,
    surface = Color.White,
    surfaceVariant = IvoryBeige.copy(alpha = 0.5f),
    onBackground = DeepEspresso,
    onSurface = DeepEspresso,
    onSurfaceVariant = DeepEspresso.copy(alpha = 0.7f)
)

val RoyalNavyColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = RoyalNavyDark,
    secondary = ChampagneGold,
    background = RoyalNavyDark,
    surface = RoyalNavyDark,
    surfaceVariant = NavyCard,
    onBackground = ChampagneGold,
    onSurface = ChampagneGold,
    onSurfaceVariant = ChampagneGold.copy(alpha = 0.7f)
)

val OxfordWineColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = OxfordBurgundy,
    secondary = ChampagneGold,
    background = OxfordBurgundy,
    surface = OxfordBurgundy,
    surfaceVariant = WineCard,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f)
)

@Composable
fun VPPasswordTheme(
    themeVariant: ThemeVariant = ThemeVariant.MIDNIGHT_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeVariant) {
        ThemeVariant.PEBBLE_LIGHT -> PebbleColorScheme
        ThemeVariant.MIDNIGHT_DARK -> MidnightColorScheme
        ThemeVariant.OCEAN_BLUE -> OceanColorScheme
        ThemeVariant.FOREST_GREEN -> ForestColorScheme
        ThemeVariant.CYBERPUNK -> CyberpunkColorScheme
        ThemeVariant.IVORY_SILK -> IvorySilkColorScheme
        ThemeVariant.ROYAL_NAVY -> RoyalNavyColorScheme
        ThemeVariant.OXFORD_WINE -> OxfordWineColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}