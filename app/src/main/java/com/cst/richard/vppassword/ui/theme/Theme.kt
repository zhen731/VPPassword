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
    ROYAL_NAVY("Royal Navy", "皇家海军蓝"),
    TITANIUM_MATTE("Titanium Matte", "钛晶灰");

    fun getTitle(): String = when(this) {
        PEBBLE_LIGHT -> com.cst.richard.vppassword.ui.AppLanguage.t("Pebble Light", "鹅卵石浅色", "鵝卵石淺色", "ペブルライト", "Pebble Light", "Pebble Light", "조약돌 라이트", "Pebble Light")
        MIDNIGHT_DARK -> com.cst.richard.vppassword.ui.AppLanguage.t("Midnight Dark", "午夜极暗", "午夜極暗", "ミッドナイトダーク", "Minuit sombre", "Mitternachtsdunkel", "미드나잇 다크", "Medianoche oscuro")
        OCEAN_BLUE -> com.cst.richard.vppassword.ui.AppLanguage.t("Ocean Blue", "深海静谧青", "深海靜謐青", "オーシャンブルー", "Bleu océan", "Ozeanblau", "오션 블루", "Azul océano")
        FOREST_GREEN -> com.cst.richard.vppassword.ui.AppLanguage.t("Forest Green", "迷雾森林绿", "迷霧森林綠", "フォレストグリーン", "Vert forêt", "Waldgrün", "포레스트 그린", "Verde bosque")
        ROYAL_NAVY -> com.cst.richard.vppassword.ui.AppLanguage.t("Royal Navy", "皇家海军蓝", "皇家海軍藍", "ロイヤルネイビー", "Royal Navy", "Königliche Marine", "로열 네이비", "Armada real")
        TITANIUM_MATTE -> com.cst.richard.vppassword.ui.AppLanguage.t("Titanium Matte", "钛晶灰", "鈦晶灰", "チタンマット", "Titane mat", "Titanmatt", "티타늄 매트", "Titanio mate")
    }
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

val TitaniumColorScheme = darkColorScheme(
    primary = TitaniumAccent,
    onPrimary = TitaniumGrey,
    secondary = TitaniumAccent,
    background = TitaniumGrey,
    surface = TitaniumGrey,
    surfaceVariant = TitaniumCard,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f)
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
        ThemeVariant.ROYAL_NAVY -> RoyalNavyColorScheme
        ThemeVariant.TITANIUM_MATTE -> TitaniumColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}