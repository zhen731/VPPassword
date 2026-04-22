package com.cst.richard.vppassword.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cst.richard.vppassword.MainViewModel
import com.cst.richard.vppassword.PasswordEntry
import com.cst.richard.vppassword.CryptoUtils
import com.cst.richard.vppassword.ui.AppLanguage
import com.cst.richard.vppassword.ui.EntryCategory
import com.cst.richard.vppassword.ui.SortOrder
import com.cst.richard.vppassword.ui.theme.VPPasswordTheme
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddPasswordScreen(
    viewModel: MainViewModel,
    hasCustomBg: Boolean,
    onPickImageTrigger: () -> Unit,
    onExportTrigger: (MainViewModel.ExportFormat, List<PasswordEntry>, String?, MainViewModel.ExportDestination) -> Unit,
    onImportTrigger: () -> Unit
) {
    var showAddScreen by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<PasswordEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showExportWizard by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var showBiometricWarning by remember { mutableStateOf(false) }
    val isImportHintShown by viewModel.isImportHintShown.collectAsState()
    var showImportHintDialog by remember { mutableStateOf(false) }
    var importHintDoNotShowAgain by remember { mutableStateOf(false) }
    
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val listToShow by viewModel.passwordList.collectAsState()
    val allSecrets by viewModel.allSecrets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentCategory by viewModel.currentCategory.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    // Back button handling
    BackHandler(enabled = showAddScreen || showSearchField || isSelectionMode) {
        if (showAddScreen) {
            showAddScreen = false
            editingEntry = null
        } else if (isSelectionMode) {
            viewModel.toggleSelectionMode(false)
        } else if (showSearchField) {
            showSearchField = false
            viewModel.setSearchQuery("")
        }
    }

    val enabledCategories by viewModel.enabledCategories.collectAsState()
    val filteredCategories = EntryCategory.values().filter { enabledCategories.contains(it.id) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                title = {
                    if (isSelectionMode) {
                        Text("${selectedIds.size} ${AppLanguage.t("Selected", "已选择", "已選擇", "選択済み", "Sélectionné", "Ausgewählt", "선택됨", "Seleccionado")}")
                    } else if (showSearchField) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            placeholder = AppLanguage.t("Search...", "搜索密码...", "搜索密碼...", "検索...", "Rechercher...", "Suchen...", "검색...", "Buscar...")
                        )
                    } else {
                        Text("VPPassword")
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.toggleSelectionMode(false) }) { Icon(Icons.Default.Close, null) }
                    } else if (showSearchField) {
                        IconButton(onClick = { showSearchField = false; viewModel.setSearchQuery("") }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    } else {
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, null) }
                    }
                },
                actions = {
                    val iconMod = Modifier.size(28.dp)
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.selectAll() }) { Icon(Icons.Default.SelectAll, null) }
                        IconButton(onClick = { viewModel.invertSelection() }) { Icon(Icons.Default.Flip, null) }
                        IconButton(onClick = { viewModel.deleteSelected {} }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    } else if (!showSearchField) {
                        IconButton(onClick = { showSearchField = true }) { Icon(Icons.Default.Search, null) }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.toggleSelectionMode(true) }) { Icon(Icons.Default.Rule, null) }
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }, modifier = iconMod) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(AppLanguage.t("Import Data", "导入数据", "導入數據", "データをインポート", "Importer des données", "Daten importieren", "데이터 가져오기", "Importar datos")) },
                                    onClick = { 
                                        showMenu = false
                                        if (isImportHintShown) onImportTrigger()
                                        else showImportHintDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.FileOpen, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(AppLanguage.t("Export Data", "导出备份与打印", "導出備份與打印", "データをエクスポート", "Exporter les données", "Daten exportieren", "데이터 내보내기", "Exportar datos")) },
                                    onClick = { showMenu = false; showExportWizard = true },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                )
                                HorizontalDivider()
                                
                                var showSortSubMenu by remember { mutableStateOf(false) }
                                DropdownMenuItem(
                                    text = { Text(AppLanguage.t("Sort Order", "排序方式", "排序方式", "並べ替え順序", "Ordre de tri", "Sortierreihenfolge", "정렬 순서", "Orden de clasificación")) },
                                    onClick = { showSortSubMenu = true },
                                    leadingIcon = { Icon(Icons.Default.Sort, null) }
                                )
                                DropdownMenu(expanded = showSortSubMenu, onDismissRequest = { showSortSubMenu = false }) {
                                    SortOrder.values().forEach { order ->
                                        DropdownMenuItem(
                                            text = { Text(order.getTitle()) },
                                            onClick = { viewModel.setSortOrder(order); showSortSubMenu = false; showMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showAddScreen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, null) }
            }
        },
        bottomBar = {
            if (!isSelectionMode && filteredCategories.size > 1) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 0.dp
                ) {
                    filteredCategories.forEach { cat ->
                        NavigationBarItem(
                            selected = currentCategory == cat.id,
                            onClick = { viewModel.setCategory(cat.id) },
                            icon = { Icon(cat.icon, null) },
                            label = { Text(cat.getTitle()) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (listToShow.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(AppLanguage.t("No passwords found", "暂无密码记录", "暫無密碼記錄", "パスワードが見つかりません", "Aucun mot de passe trouvé", "Keine Passwörter gefunden", "비밀번호를 찾을 수 없습니다", "No se encontraron contraseñas"), color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listToShow, key = { it.id }) { entry ->
                        PasswordItemCard(
                            entry = entry,
                            viewModel = viewModel,
                            hasCustomBg = hasCustomBg,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(entry.id),
                            onToggleSelection = { viewModel.toggleItemSelection(entry.id) },
                            onEditRequest = {
                                editingEntry = entry
                                showAddScreen = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddScreen) {
        PasswordDetailScreen(
            entry = editingEntry,
            category = currentCategory,
            viewModel = viewModel,
            onDismiss = { 
                showAddScreen = false
                editingEntry = null
            },
            onSave = { p, a, pw, n ->
                if (editingEntry != null) {
                    viewModel.updatePassword(editingEntry!!.copy(projectName = p, account = a, password = pw, notes = n))
                } else {
                    viewModel.addPassword(p, a, pw, category = currentCategory, notes = n)
                }
                showAddScreen = false
                editingEntry = null
            }
        )
    }

    if (showImportHintDialog) {
        AlertDialog(
            onDismissRequest = { showImportHintDialog = false },
            modifier = Modifier.widthIn(max = 400.dp),
            title = { Text(AppLanguage.t("Import Support", "支持导入的格式")) },
            text = {
                Column {
                    Text(AppLanguage.t("Supported formats: JSON (VPP Backup), CSV (Google/VPP), Markdown, and Text tables.", "支持格式：JSON (VPP 备份), CSV (谷歌/VPP 表格), Markdown 以及普通文本表格。"))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().combinedClickable { importHintDoNotShowAgain = !importHintDoNotShowAgain }) {
                        Checkbox(checked = importHintDoNotShowAgain, onCheckedChange = { importHintDoNotShowAgain = it })
                        Text(AppLanguage.t("Don't show again", "不再提醒"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showImportHintDialog = false
                    if (importHintDoNotShowAgain) viewModel.setImportHintShown()
                    onImportTrigger()
                }) { Text(AppLanguage.t("Got it, let's go", "知道了，去导入")) }
            }
        )
    }

    if (showExportWizard) {
        ExportWizardDialog(
            viewModel = viewModel,
            currentCategoryList = listToShow,
            allSecretsList = allSecrets,
            onDismiss = { showExportWizard = false },
            onConfirm = onExportTrigger
        )
    }

    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            onDismiss = { showSettings = false },
            onPickImageTrigger = onPickImageTrigger
        )
    }

    if (showBiometricWarning) {
        AlertDialog(
            onDismissRequest = { showBiometricWarning = false },
            title = { Text(AppLanguage.t("Disable Biometric Unlock?", "确定关闭指纹解锁？", "確定關閉指紋解鎖？", "指紋認証を無効にする？", "Désactiver l'empreinte ?", "Fingerabdruck deaktivieren?", "지문 인식 비활성화?", "¿Desactivar huella?"), color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text(AppLanguage.t(
                "Disabling this will allow anyone with physical access to your phone to open this app and see all your passwords. This is HIGHLY DISCOURAGED for security.",
                "关闭指纹解锁会导致任何拿起您手机的人都能直接看到您的所有密码。出于安全考虑，强烈建议保持开启状态。",
                "關閉指紋解鎖會導致任何拿起您手機的人都能直接看到您的所有密碼。出于安全考慮，強烈建議保持開啟狀態。",
                "無効にすると、あなたの携帯にアクセスできる誰もがこのアプリを開いてすべてのパスワードを表示できます。これはセキュリティのために強く推奨されません。",
                "La désactivation permettra à toute personne ayant accès à votre téléphone d'ouvrir cette application et de voir tous vos mots de passe. C'est FORTEMENT DÉCONSEILLÉ pour la sécurité.",
                "Das Deaktivieren ermöglicht jedem mit physischem Zugriff auf Ihr Telefon, diese App zu öffnen und alle Ihre Passwörter zu sehen. Dies wird aus Sicherheitsgründen DRINGEND ABGERATEN.",
                "이 기능을 비활성화하면 휴대전화에 물리적으로 액세스할 수 있는 사람이 이 앱을 열고 모든 비밀번호를 볼 수 있습니다. 보안을 위해 이 기능을 비활성화하지 않는 것이 좋습니다.",
                "Desactivar esto permitirá que cualquier persona con acceso físico a su teléfono abra esta aplicación y vea todas sus contraseñas. Se DESACONSEJA ENÉRGICAMENTE por seguridad."
            )) },
            confirmButton = {
                TextButton(onClick = { 
                    showBiometricWarning = false
                    viewModel.setBiometricEnabled(false)
                }) { Text(AppLanguage.t("Confirm Disable", "坚持关闭", "堅持關閉", "無効にする", "Confirmer la désactivation", "Deaktivieren bestätigen", "비활성화 확인", "Confirmar desactivación"), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricWarning = false }) { Text(AppLanguage.t("Keep Enabled", "保持开启", "保持開啟", "有効のままにする", "Garder activé", "Aktiviert lassen", "활성 유지", "Mantener activado")) }
            }
        )
    }

    if (isFirstLaunch) {
        AlertDialog(
            onDismissRequest = { }, // Force action
            title = { Text(AppLanguage.t("Security Recommendation", "安全建议", "安全建議", "セキュリティの推奨事項", "Recommandation de sécurité", "Sicherheitsempfehlung", "보안 권장 사항", "Recomendación de seguridad"), fontWeight = FontWeight.Bold) },
            text = { Text(AppLanguage.t(
                "Welcome to VPPassword. To keep your data safe, we strongly recommend enabling Fingerprint Unlock in the settings.",
                "欢迎使用 VPPassword。为了保护您的数据安全，我们强烈建议您在右上角的设置中开启“指纹解锁”功能。",
                "歡迎使用 VPPassword。為了保護您的數據安全，我們强烈建議您在右上角的設置中開啟“指纹解鎖”功能。",
                "VPPasswordへようこそ。 データを安全に保つために、設定で指紋認証を有効にすることを強くお勧めします。",
                "Bienvenue sur VPPassword. Pour protéger vos données, nous vous recommandons vivement d'activer le déverrouillage par empreinte digitale dans les paramètres.",
                "Willkommen bei VPPassword. Um Ihre Daten sicher zu halten, empfehlen wir dringend, die Fingerabdruck-Entsperrung in den Einstellungen zu aktivieren.",
                "VPPassword에 오신 것을 환영합니다. 데이터를 안전하게 보호하려면 설정에서 지문 인식을 활성화하는 것이 좋습니다.",
                "Bienvenido a VPPassword. Para mantener sus datos seguros, le recomendamos encarecidamente que active el desbloqueo por huella dactilar en los ajustes."
            )) },
            confirmButton = {
                TextButton(onClick = { viewModel.completeOnboarding() }) { 
                    Text(AppLanguage.t("I Understand", "我知道了", "我知道了", "了解しました", "Je comprends", "Ich verstehe", "이해했습니다", "Entiendo")) 
                }
            }
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(20.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    )
}

@Composable
fun ExportWizardDialog(
    viewModel: MainViewModel,
    currentCategoryList: List<PasswordEntry>,
    allSecretsList: List<PasswordEntry>,
    onDismiss: () -> Unit,
    onConfirm: (MainViewModel.ExportFormat, List<PasswordEntry>, String?, MainViewModel.ExportDestination) -> Unit
) {
    val currentCategory by viewModel.currentCategory.collectAsState()
    val categoryName = when(currentCategory) {
        0 -> AppLanguage.t("Passwords", "普通密码", "普通密碼", "パスワード", "Mots de passe", "Passwörter", "비밀번호", "Contraseñas")
        1 -> AppLanguage.t("Backup Codes", "备用验证码", "备用验证码", "バックアップコード", "Codes de secours", "Backup-Codes", "백업 코드", "Códigos de respaldo")
        2 -> AppLanguage.t("Crypto", "加密货币", "加密貨幣", "暗号通貨", "Crypto", "Krypto", "암호화폐", "Cripto")
        else -> "Other"
    }

    var securityEncrypted by remember { mutableStateOf(true) }
    var exportFormat by remember { mutableStateOf(MainViewModel.ExportFormat.JSON_ENCRYPTED) }
    var exportDestination by remember { mutableStateOf(MainViewModel.ExportDestination.LOCAL) }
    var encryptionPassword by remember { mutableStateOf("") }
    var showPasswordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppLanguage.t("Export Module: $categoryName", "导出模块：$categoryName"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Info Section
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)).padding(8.dp)) {
                    Text(
                        AppLanguage.t(
                            "You are exporting the '$categoryName' module only. Other modules will not be included.",
                            "您正在仅导出“$categoryName”模块。其他模块的数据不会包含在内。",
                            "您正在僅導出“$categoryName”模塊。其他模塊的數據不會包含在內。",
                            "「$categoryName」モジュールのみをエクスポートしています。他のモジュールは含まれません。",
                            "Vous exportez uniquement le module '$categoryName'. Les autres modules ne seront pas inclus.",
                            "Sie exportieren nur das Modul '$categoryName'. Andere Module werden nicht einbezogen.",
                            "'$categoryName' 모듈만 내보내고 있습니다. 다른 모듈은 포함되지 않습니다.",
                            "Solo está exportando el módulo '$categoryName'. No se incluirán otros módulos."
                        ),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Destination Section
                Column {
                    Text(AppLanguage.t("1. Export Destination", "1. 导出目的地"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = exportDestination == MainViewModel.ExportDestination.LOCAL, onClick = { exportDestination = MainViewModel.ExportDestination.LOCAL })
                        Text(AppLanguage.t("Local File", "本地文件"), modifier = Modifier.combinedClickable { exportDestination = MainViewModel.ExportDestination.LOCAL })
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = exportDestination == MainViewModel.ExportDestination.SHARE, onClick = { exportDestination = MainViewModel.ExportDestination.SHARE })
                        Text(AppLanguage.t("Cloud/Share", "云端/分享"), modifier = Modifier.combinedClickable { exportDestination = MainViewModel.ExportDestination.SHARE })
                    }
                }

                // Security Section
                Column {
                    Text(AppLanguage.t("2. Security Level", "2. 安全级别"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !securityEncrypted, onClick = { securityEncrypted = false; exportFormat = MainViewModel.ExportFormat.JSON })
                        Text(AppLanguage.t("Plaintext", "明文导出"), modifier = Modifier.combinedClickable { securityEncrypted = false; exportFormat = MainViewModel.ExportFormat.JSON })
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = securityEncrypted, onClick = { securityEncrypted = true; exportFormat = MainViewModel.ExportFormat.JSON_ENCRYPTED })
                        Text(AppLanguage.t("Encrypted", "加密导出"), modifier = Modifier.combinedClickable { securityEncrypted = true; exportFormat = MainViewModel.ExportFormat.JSON_ENCRYPTED })
                    }
                }

                if (!securityEncrypted) {
                    Box(modifier = Modifier.background(Color.Red.copy(alpha = 0.1f)).padding(8.dp)) {
                        Text(
                            AppLanguage.t(
                                "SEVERE WARNING: Plaintext export will save your passwords as a regular file. Anyone with this file can see your data. Only use this for printing or manual backup!",
                                "严重警告：明文导出将以不加密形式保存密码。任何拿到该文件的人都能直接看到您的密码。请仅用于打印或手动备份！",
                                "嚴重警告：明文導出將以不加密形式保存密碼。任何拿到該文件的人都能直接看到您的密碼。請僅用於打印或手動備份！",
                                "警告：プレーンテキストのエクスポートは、パスワードを通常のファイルとして保存します。このファイルにアクセスできる人は誰でもあなたのデータを閲覧できます。印刷または手動バックアップのみに使用してください！",
                                "AVERTISSEMENT SÉVÈRE : l'exportation en texte clair enregistrera vos mots de passe dans un fichier standard. Toute personne ayant ce fichier pourra voir vos données. À n'utiliser que pour l'impression ou une sauvegarde manuelle !",
                                "SCHWERE WARNUNG: Der Klartext-Export speichert Ihre Passwörter als reguläre Datei. Jeder mit dieser Datei kann Ihre Daten sehen. Nur zum Drucken oder für manuelle Backups verwenden!",
                                "심각한 경고: 평문 내보내기는 비밀번호를 일반 파일로 저장합니다. 이 파일이 있는 사람은 누구나 데이터를 볼 수 있습니다. 인쇄 또는 수동 백업용으로만 사용하십시오!",
                                "ADVERTENCIA GRAVE: La exportación en texto plano guardará sus contraseñas como un archivo normal. Cualquier persona con este archivo puede ver sus datos. ¡Úselo solo para imprimir o realizar copias de seguridad manuales!"
                            ),
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Format Section
                Column {
                    Text(AppLanguage.t("3. Select Format", "3. 选择文件格式"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!securityEncrypted) {
                            listOf(MainViewModel.ExportFormat.JSON, MainViewModel.ExportFormat.CSV, MainViewModel.ExportFormat.MARKDOWN, MainViewModel.ExportFormat.PDF).forEach { fmt ->
                                val isSelected = exportFormat == fmt
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { exportFormat = fmt },
                                    label = { Text(if (fmt == MainViewModel.ExportFormat.PDF) "PDF (Print)" else fmt.name) },
                                    leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        } else {
                            listOf(MainViewModel.ExportFormat.JSON_ENCRYPTED, MainViewModel.ExportFormat.CSV_ENCRYPTED).forEach { fmt ->
                                val isSelected = exportFormat == fmt
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { exportFormat = fmt },
                                    label = { Text(fmt.name.replace("_ENCRYPTED", "")) },
                                    leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                    if (exportFormat == MainViewModel.ExportFormat.PDF) {
                        Text(
                            AppLanguage.t("The PDF version is optimized for paper printing with an exquisite layout.", "PDF 版本已针对 A4 纸打印进行排版优化，视觉效果极佳。"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Password Section
                AnimatedVisibility(visible = securityEncrypted) {
                    Column {
                        Text(AppLanguage.t("4. Set Encryption Password", "4. 设置加密密码"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            AppLanguage.t(
                                "Tip: You can use ANY characters (including Chinese, symbols, non-ASCII) as your password.",
                                "提示：可以使用任何字符（包括汉字、符号、非 ASCII 字符）作为密码。",
                                "提示：可以使用任何字符（包括漢字、符號、非 ASCII 字符）作為密碼。",
                                "ヒント：パスワードには任意の文字（漢字、記号、非ASCII文字など）を使用できます。",
                                "Astuce : vous pouvez utiliser TOUS les caractères (y compris chinois, symboles, non-ASCII) comme mot de passe.",
                                "Tipp: Sie können BELIEBIGE Zeichen (einschließlich Chinesisch, Symbole, Nicht-ASCII) als Passwort verwenden.",
                                "팁: 비밀번호에는 모든 문자(한글, 기호, 비ASCII 문자 등)를 사용할 수 있습니다.",
                                "Consejo: puede usar CUALQUIER carácter (incluidos chino, símbolos, no ASCII) como contraseña."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = encryptionPassword,
                            onValueChange = { encryptionPassword = it; showPasswordError = false },
                            label = { Text(AppLanguage.t("Password", "密码", "密碼", "パスワード", "Mot de passe", "Passwort", "비밀번호", "Contraseña")) },
                            isError = showPasswordError,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (showPasswordError) {
                            Text(AppLanguage.t("Password cannot be empty", "密码不能为空", "密碼不能位空", "パスワードは空にできません", "Le mot de passe ne peut pas être vide", "Passwort darf nicht leer sein", "비밀번호는 비워 둘 수 없습니다", "La contraseña no puede estar vacía"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                if (!securityEncrypted && exportFormat != MainViewModel.ExportFormat.PDF) {
                    Text(
                        AppLanguage.t("Warning: Plaintext export is less secure.", "警告：明文导出存在安全风险，请妥善保管。", "警告：明文導出存在安全風險，請妥善保管。", "警告：プレーンテキストのエクスポートは安全性が低くなります。", "Attention : l'exportation en texte clair est moins sécurisée.", "Warnung: Der Klartext-Export ist weniger sicher.", "경고: 평문 내보내기는 보안성이 떨어집니다.", "Advertencia: La exportación en texto plano es menos segura."),
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (securityEncrypted && encryptionPassword.isBlank()) {
                    showPasswordError = true
                } else {
                    onConfirm(exportFormat, currentCategoryList, if (securityEncrypted) encryptionPassword else null, exportDestination)
                    onDismiss()
                }
            }) {
                Text(AppLanguage.t("Export Now", "立即导出", "立即導出", "今すぐエクスポート", "Exporter maintenant", "Jetzt exportieren", "지금 내보내기", "Exportar ahora"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppLanguage.t("Cancel", "取消", "取消", "キャンセル", "Annuler", "Abbrechen", "취소", "Cancelar")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    entry: PasswordEntry? = null,
    category: Int = 0,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var p by remember { mutableStateOf(entry?.projectName ?: "") }
    var a by remember { mutableStateOf(entry?.account ?: "") }
    var pw by remember { mutableStateOf(entry?.password ?: "") }
    var n by remember { mutableStateOf(entry?.notes ?: "") }
    
    LaunchedEffect(entry) {
        if (entry == null && (viewModel.draftProject.isNotEmpty() || viewModel.draftAccount.isNotEmpty() || viewModel.draftPassword.isNotEmpty())) {
            p = viewModel.draftProject
            a = viewModel.draftAccount
            pw = viewModel.draftPassword
        }
    }

    val isValid = p.isNotBlank() && pw.isNotBlank()

    var showExitDialog by remember { mutableStateOf(false) }
    val hasChanges = p != (entry?.projectName ?: viewModel.draftProject) || 
                     a != (entry?.account ?: viewModel.draftAccount) || 
                     pw != (entry?.password ?: viewModel.draftPassword)

    val handleExit = {
        if (hasChanges) {
            showExitDialog = true
        } else {
            onDismiss()
        }
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        handleExit()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(AppLanguage.t("Unsaved Changes", "未保存的更改", "未保存的更改", "未保存の変更", "Modifications non enregistrées", "Ungespeicherte Änderungen", "저장되지 않은 변경 사항", "Cambios sin guardar")) },
            text = { Text(AppLanguage.t("Do you want to save this entry as a draft, save it directly, or discard all changes?", "您想将此条目存为草稿、直接保存还是放弃更改？", "您想將此條目存為草稿、直接保存還是放棄更改？", "このエントリをドラフトとして保存するか、直接保存するか、すべての変更を破棄しますか？", "Voulez-vous enregistrer cette entrée en tant que brouillon, l'enregistrer directement ou abandonner toutes les modifications ?", "Möchten Sie diesen Eintrag als Entwurf speichern, direkt speichern oder alle Änderungen verwerfen?", "이 항목을 초안으로 저장하시겠습니까, 직접 저장하시겠습니까, 아니면 모든 변경 사항을 취소하시겠습니까?", "¿Desea guardar esta entrada como borrador, guardarla directamente o descartar todos los cambios?")) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSave(p, a, pw, n)
                            showExitDialog = false
                        },
                        enabled = isValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(AppLanguage.t("Save Directly", "直接保存", "直接保存", "直接保存", "Enregistrer directement", "Direkt speichern", "직접 저장", "Guardar directamente"))
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.draftProject = p
                            viewModel.draftAccount = a
                            viewModel.draftPassword = pw
                            showExitDialog = false
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(AppLanguage.t("Save as Draft", "存为草稿", "存為草稿", "下書きとして保存", "Enregistrer comme brouillon", "Als Entwurf speichern", "초안으로 저장", "Guardar como borrador"))
                    }
                    TextButton(
                        onClick = {
                            viewModel.draftProject = ""
                            viewModel.draftAccount = ""
                            viewModel.draftPassword = ""
                            showExitDialog = false
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(AppLanguage.t("Discard Changes", "放弃更改", "放棄更改", "変更を破棄", "Abandonner les modifications", "Änderungen verwerfen", "변경 사항 취소", "Descartar cambios"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (entry == null) AppLanguage.t("Add New", "新增条目", "新增條目", "新規追加", "Ajouter nouveau", "Neu hinzufügen", "새로 추가", "Agregar nuevo") else AppLanguage.t("Edit Entry", "编辑条目", "編輯條目", "エントリを編集", "Modifier l'entrée", "Eintrag bearbeiten", "항목 편집", "Editar entrada")) },
                    navigationIcon = { IconButton(onClick = handleExit) { Icon(Icons.Default.Close, null) } }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = p, 
                    onValueChange = { p = it }, 
                    label = { Text(AppLanguage.t("Project/Website (Required)", "项目/网站 (必填)", "項目/網站 (必填)", "プロジェクト/ウェブサイト (必須)", "Projet/Site (Requis)", "Projekt/Webseite (Erforderlich)", "프로젝트/웹사이트 (필수)", "Proyecto/Sitio web (Obligatorio)")) }, 
                    modifier = Modifier.fillMaxWidth(),
                    isError = p.isBlank() && pw.isNotBlank()
                )
                OutlinedTextField(
                    value = a, 
                    onValueChange = { a = it }, 
                    label = { Text(AppLanguage.t("Account (Optional)", "账号 (选填)", "賬號 (選填)", "アカウント (任意)", "Compte (Optionnel)", "Konto (Optional)", "계정 (선택 사항)", "Cuenta (Opcional)")) }, 
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pw, 
                    onValueChange = { pw = it }, 
                    label = { Text(if (category == 1) AppLanguage.t("Backup Codes (Required)", "备用验证码 (必填)", "備用驗证碼 (必填)", "バックアップコード (必須)", "Codes de secours (Requis)", "Backup-Codes (Erforderlich)", "백업 코드 (필수)", "Códigos de respaldo (Obligatorio)") else AppLanguage.t("Password (Required)", "密码 (必填)", "密碼 (必填)", "パスワード (必須)", "Mot de passe (Requis)", "Passwort (Erforderlich)", "비밀번호 (필수)", "Contraseña (Obligatorio)")) }, 
                    modifier = Modifier.fillMaxWidth(),
                    isError = pw.isBlank() && p.isNotBlank()
                )
                
                if (category == 1) {
                    Text(
                        AppLanguage.t("Tip: You can separate backup codes with spaces or newlines.", "提示：可以使用空格或换行符分隔多个备用验证码。", "提示：可以使用空格或換行符分隔多個備用驗證碼。", "ヒント：スペースまたは改行でバックアップコードを区切ることができます。", "Conseil : vous pouvez séparer les codes de secours par des espaces ou des sauts de ligne.", "Tipp: Sie können Backup-Codes durch Leerzeichen oder Zeilenumbrüche trennen.", "팁: 공백이나 줄바꿈으로 백업 코드를 구분할 수 있습니다.", "Consejo: puede separar los códigos de respaldo con espacios o saltos de línea."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                OutlinedTextField(
                    value = n, 
                    onValueChange = { n = it }, 
                    label = { Text(AppLanguage.t("Notes (Optional, e.g. Created date)", "备注 (选填，如：生成时间)", "備註 (選填，如：生成時間)", "メモ (任意、例：作成日)", "Notes (Optionnel, ex: Date de création)", "Notizen (Optional, z. B. Erstellungsdatum)", "메모 (선택 사항, 예: 생성 날짜)", "Notas (Opcional, p. ej., fecha de creación)")) }, 
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onSave(p, a, pw, n) }, 
                    enabled = isValid, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(AppLanguage.t("Save", "保存", "保存", "保存", "Enregistrer", "Speichern", "저장", "Guardar"))
                }
            }
        }
    }
}
