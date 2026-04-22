package com.cst.richard.vppassword.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cst.richard.vppassword.MainViewModel
import com.cst.richard.vppassword.ui.theme.ThemeVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onPickImageTrigger: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        AppLanguage.t("Appearance", "外观个性化", "外观个性化", "外観", "Apparence", "Aussehen", "모양", "Apariencia"),
        AppLanguage.t("Security", "安全与数据", "安全與數據", "セキュリティ", "Sécurité", "Sicherheit", "보안", "Seguridad")
    )

    BackHandler(onBack = onDismiss)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = { Text(AppLanguage.t("Settings", "设置", "設置", "設定", "Paramètres", "Einstellungen", "설정", "Ajustes"), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                        }
                    )
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (selectedTab == 0) {
                    AppearanceSettings(viewModel, onPickImageTrigger)
                } else {
                    SecuritySettings(viewModel)
                }
            }
        }
    }
}

@Composable
fun SecuritySettings(viewModel: MainViewModel) {
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val enabledCategories by viewModel.enabledCategories.collectAsState()
    var showBiometricWarning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(AppLanguage.t("Access Protection", "访问保护", "訪問保護", "アクセス保護", "Protection d'accès", "Zugriffsschutz", "액세스 보호", "Protección de acceso"))
        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text(AppLanguage.t("Fingerprint Unlock", "指纹解锁", "指紋解鎖", "指紋認証", "Déverrouillage par empreinte", "Fingerabdruck", "지문 인식", "Desbloqueo por huella")) },
                leadingContent = { Icon(Icons.Default.Fingerprint, null) },
                trailingContent = {
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { 
                            if (!it) showBiometricWarning = true 
                            else viewModel.setBiometricEnabled(true)
                        }
                    )
                }
            )
        }

        SectionHeader(AppLanguage.t("Category Management", "栏目管理", "欄目管理", "カテゴリ管理", "Gestion des catégories", "Kategorie-Management", "카테고리 관리", "Gestión de categorías"))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                for (cat in EntryCategory.values()) {
                    ListItem(
                        headlineContent = { Text(cat.getTitle()) },
                        leadingContent = { Icon(cat.icon, null, modifier = Modifier.size(20.dp)) },
                        trailingContent = {
                            Switch(
                                checked = enabledCategories.contains(cat.id),
                                onCheckedChange = { viewModel.toggleCategoryVisibility(cat.id, it) },
                                enabled = enabledCategories.size > 1 || !enabledCategories.contains(cat.id)
                            )
                        }
                    )
                }
            }
        }
    }

    if (showBiometricWarning) {
        AlertDialog(
            onDismissRequest = { showBiometricWarning = false },
            title = { Text(AppLanguage.t("Disable Fingerprint?", "关闭指纹解锁？", "關閉指紋解鎖？", "指紋認証を無効にする？", "Désactiver l'empreinte ?", "Fingerabdruck deaktivieren?", "지문 인식 비활성화?", "¿Desactivar huella?"), color = Color.Red) },
            text = { Text(AppLanguage.t("Warning: Anyone with access to your phone can view all passwords.", "警告：关闭后任何拿到您手机的人都可以查看您的所有密码。", "警告：關閉後任何拿到您手機的人都可以查看您的所有密碼。", "警告：無効にすると、あなたの携帯にアクセスできる誰もがすべてのパスワードを表示できます。", "Attention : toute personne ayant accès à votre téléphone pourra voir tous vos mots de passe.", "Warnung: Jeder mit Zugriff auf Ihr Telefon kann alle Passwörter sehen.", "경고: 휴대전화에 액세스할 수 있는 사람이 모든 비밀번호를 볼 수 있습니다.", "Advertencia: Cualquier persona con acceso a su teléfono puede ver todas las contraseñas.")) },
            confirmButton = {
                TextButton(onClick = { 
                    showBiometricWarning = false
                    viewModel.setBiometricEnabled(false)
                }) { Text(AppLanguage.t("Confirm", "确认关闭", "確認關閉", "確認", "Confirmer", "Bestätigen", "확인", "Confirmar"), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricWarning = false }) { Text(AppLanguage.t("Keep Enabled", "保持开启", "保持開啟", "有効のままにする", "Garder activé", "Aktiviert lassen", "활성 유지", "Mantener activado")) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettings(viewModel: MainViewModel, onPickImageTrigger: () -> Unit) {
    val currentTheme by viewModel.themeVariant.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val bgBlur by viewModel.bgBlur.collectAsState()
    val bgDim by viewModel.bgDim.collectAsState()
    val bgScaleType by viewModel.bgScaleType.collectAsState()
    val customBgPath by viewModel.customBgPath.collectAsState()
    val hasCustomBg = customBgPath != null

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(AppLanguage.t("General", "通用设置", "通用設置", "一般設定", "Général", "Allgemein", "일반", "General"))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(AppLanguage.t("Language", "语言", "語言", "言語", "Langue", "Sprache", "언어", "Idioma"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (lang in Language.values()) {
                        FilterChip(
                            selected = currentLanguage == lang,
                            onClick = { viewModel.setLanguage(lang) },
                            label = { Text(lang.label) }
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                
                Text(AppLanguage.t("Theme Color", "主题色彩", "主題色彩", "テーマカラー", "Couleur du thème", "Themenfarbe", "테마 색상", "Color del tema"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (variant in ThemeVariant.values()) {
                        FilterChip(
                            selected = currentTheme == variant.ordinal,
                            onClick = { viewModel.setThemeVariant(variant.ordinal) },
                            label = { Text(variant.getTitle()) }
                        )
                    }
                }
            }
        }

        SectionHeader(AppLanguage.t("Background", "背景样式", "背景樣式", "背景スタイル", "Arrière-plan", "Hintergrund", "배경", "Fondo"))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onPickImageTrigger, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppLanguage.t("Pick Background", "选择背景图片", "選擇背景圖片", "背景画像を選択", "Choisir une image", "Hintergrund wählen", "배경 이미지 선택", "Elegir fondo"))
                }

                if (hasCustomBg) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(AppLanguage.t("Scaling Mode", "缩放模式"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = bgScaleType == 0,
                                onClick = { viewModel.setBgScaleType(0) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                label = { Text(AppLanguage.t("Crop", "裁剪")) }
                            )
                            SegmentedButton(
                                selected = bgScaleType == 1,
                                onClick = { viewModel.setBgScaleType(1) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                label = { Text(AppLanguage.t("Fit", "适应")) }
                            )
                            SegmentedButton(
                                selected = bgScaleType == 2,
                                onClick = { viewModel.setBgScaleType(2) },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                label = { Text(AppLanguage.t("Fill", "拉伸")) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BlurOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppLanguage.t("Blur Intensity", "虚化程度", "虚化程度", "ぼかし强度", "Flou", "Unschärfe", "블러 강도", "Desenfoque"), style = MaterialTheme.typography.labelSmall)
                            }
                            Slider(value = bgBlur, onValueChange = { viewModel.setBgBlur(it) }, valueRange = 0f..25f)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Brightness6, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppLanguage.t("Darkness Intensity", "暗度程度", "暗度程度", "暗さ强度", "Obscurité", "Dunkelheit", "어두움 정도", "Oscuridad"), style = MaterialTheme.typography.labelSmall)
                            }
                            Slider(value = bgDim, onValueChange = { viewModel.setBgDim(it) }, valueRange = 0f..0.9f)
                            
                            Button(
                                onClick = { viewModel.removeBackground() },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.NoPhotography, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppLanguage.t("Remove Background", "移除背景", "移除背景", "背景を削除", "Supprimer l'arrière-plan", "Hintergrund entfernen", "배경 제거", "Quitar fondo"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}
