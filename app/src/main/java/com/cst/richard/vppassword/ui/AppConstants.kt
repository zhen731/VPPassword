package com.cst.richard.vppassword.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class Language(val code: String, val label: String) {
    EN("en", "English"),
    CN("zh", "简体中文"),
    TW("zh-TW", "繁體中文"),
    JP("ja", "日本語"),
    FR("fr", "Français"),
    DE("de", "Deutsch"),
    KR("ko", "한국어"),
    ES("es", "Español")
}

object AppLanguage {
    var currentLanguage by mutableStateOf(getDefaultLanguage())

    private fun getDefaultLanguage(): Language {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "zh" -> if (locale.country == "TW" || locale.country == "HK") Language.TW else Language.CN
            "ja" -> Language.JP
            "fr" -> Language.FR
            "de" -> Language.DE
            "ko" -> Language.KR
            "es" -> Language.ES
            else -> Language.EN
        }
    }

    fun t(
        en: String,
        cn: String,
        tw: String? = null,
        jp: String? = null,
        fr: String? = null,
        de: String? = null,
        kr: String? = null,
        es: String? = null
    ): String {
        return when (currentLanguage) {
            Language.CN -> cn
            Language.TW -> tw ?: cn
            Language.JP -> jp ?: en
            Language.FR -> fr ?: en
            Language.DE -> de ?: en
            Language.KR -> kr ?: en
            Language.ES -> es ?: en
            else -> en
        }
    }
}

enum class SortOrder(val id: Int) {
    NAME_ASC(0),
    NAME_DESC(1),
    NEWEST(2),
    OLDEST(3);
    
    fun getTitle(): String = when(this) {
        NAME_ASC -> AppLanguage.t("Name (A-Z)", "名称 (A-Z)", "名稱 (A-Z)", "名前 (A-Z)", "Nom (A-Z)", "Name (A-Z)", "이름 (A-Z)", "Nombre (A-Z)")
        NAME_DESC -> AppLanguage.t("Name (Z-A)", "名称 (Z-A)", "名稱 (Z-A)", "名前 (Z-A)", "Nom (Z-A)", "Name (Z-A)", "이름 (Z-A)", "Nombre (Z-A)")
        NEWEST -> AppLanguage.t("Newest First", "最新优先", "最新優先", "新しい順", "Plus récent", "Neueste zuerst", "최신순", "Más reciente primero")
        OLDEST -> AppLanguage.t("Oldest First", "最早优先", "最早優先", "古い顺", "Plus ancien", "Älteste zuerst", "오래된순", "Más antiguo primero")
    }
}

enum class EntryCategory(val id: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PASSWORD(0, Icons.Default.VpnKey),
    BACKUP_CODE(1, Icons.Default.FactCheck),
    CRYPTO(2, Icons.Default.AccountBalanceWallet);

    fun getTitle(): String = when(this) {
        PASSWORD -> AppLanguage.t("Passwords", "普通密码", "普通密碼", "パスワード", "Mots de passe", "Passwörter", "비밀번호", "Contraseñas")
        BACKUP_CODE -> AppLanguage.t("Backup Codes", "备用验证码", "备用验证码", "バックアップコード", "Codes de secours", "Backup-Codes", "백업 코드", "Códigos de respaldo")
        CRYPTO -> AppLanguage.t("Crypto", "加密货币", "加密貨幣", "暗号通貨", "Crypto", "Krypto", "암호화폐", "Cripto")
    }
}
