package com.cst.richard.vppassword.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cst.richard.vppassword.AppLanguage
import com.cst.richard.vppassword.CryptoUtils
import com.cst.richard.vppassword.MainViewModel
import com.cst.richard.vppassword.SortOrder
import com.cst.richard.vppassword.ui.theme.ThemeVariant
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordScreen(
    viewModel: MainViewModel,
    hasCustomBg: Boolean,
    onPickImageTrigger: () -> Unit,
    onExportTrigger: (String) -> Unit,
    onImportTrigger: () -> Unit
) {
    val passwordList by viewModel.passwordList.collectAsState()
    val currentTheme by viewModel.themeVariant.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val bgScaleType by viewModel.bgScaleType.collectAsState()
    val bgBlur by viewModel.bgBlur.collectAsState()
    val bgDim by viewModel.bgDim.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showPlaintextWarning by remember { mutableStateOf(false) }
    var showBiometricWarning by remember { mutableStateOf(false) }
    
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    val context = LocalContext.current

    val iconMod = Modifier
    val textShadow = if (hasCustomBg) Shadow(Color.Black, blurRadius = 6f) else null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("VPPassword", fontWeight = FontWeight.Black, style = TextStyle(shadow = textShadow, color = MaterialTheme.colorScheme.onSurface)) },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }, modifier = iconMod) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(AppLanguage.t("Import Passwords", "导入密码本")) },
                                    onClick = { showMenu = false; onImportTrigger() }
                                )
                                DropdownMenuItem(
                                    text = { Text(AppLanguage.t("Export Encrypted", "导出加密密码本")) },
                                    onClick = { showMenu = false; showExportPasswordDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(AppLanguage.t("Export Plaintext", "导出明文密码本")) },
                                    onClick = { showMenu = false; showPlaintextWarning = true }
                                )
                            }
                        }

                        if (isSelectionMode) {
                            IconButton(onClick = { viewModel.toggleSelectionMode(false) }, modifier = iconMod) {
                                Icon(Icons.Default.Close, null, tint = Color.Red)
                            }
                        }
                        
                        var showSortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSortMenu = true }, modifier = iconMod) {
                            Icon(
                                Icons.Default.Sort, 
                                contentDescription = AppLanguage.t("Sort", "排序"),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(if (isEnglish) order.titleEn else order.titleCn)
                                                if (sortOrder == order) {
                                                    Spacer(Modifier.size(8.dp))
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp)) 
                                                }
                                            }
                                        },
                                        onClick = { viewModel.setSortOrder(order); showSortMenu = false }
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        IconButton(onClick = { viewModel.toggleSelectionMode(true) }, modifier = iconMod) { 
                            Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.onSurface) 
                        }
                    }
                    IconButton(onClick = { showSettings = true }, modifier = iconMod) { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface) }
                },
            )
        },
        bottomBar = {
            if (isSelectionMode) {
                BottomAppBar(
                    actions = {
                        TextButton(onClick = { viewModel.selectAll(passwordList.map { it.id }) }) {
                            Text(AppLanguage.t("Select All", "全选"))
                        }
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text(AppLanguage.t("Clear", "清除选择"))
                        }
                    },
                    floatingActionButton = {
                        var showBulkDeleteConfirm by remember { mutableStateOf(false) }
                        ExtendedFloatingActionButton(
                            onClick = { if (selectedIds.isNotEmpty()) showBulkDeleteConfirm = true },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error,
                            icon = { Icon(Icons.Default.Delete, null) },
                            text = { Text(AppLanguage.t("Delete (${selectedIds.size})", "删除 (${selectedIds.size})")) }
                        )

                        if (showBulkDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showBulkDeleteConfirm = false },
                                title = { Text(AppLanguage.t("Confirm Batch Delete?", "确认批量删除？")) },
                                text = { Text(AppLanguage.t("This will permanently delete ${selectedIds.size} entries.", "这将永久删除选中的 ${selectedIds.size} 条记录。")) },
                                confirmButton = {
                                    TextButton(onClick = { 
                                        viewModel.deleteSelected { showBulkDeleteConfirm = false }
                                    }) { Text(AppLanguage.t("Confirm", "确认"), color = Color.Red) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showBulkDeleteConfirm = false }) { Text(AppLanguage.t("Cancel", "取消")) }
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null) }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
            items(passwordList, key = { it.id }) { item -> 
                PasswordItemCard(
                    entry = item, 
                    viewModel = viewModel, 
                    hasCustomBg = hasCustomBg,
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedIds.contains(item.id),
                    onToggleSelection = { 
                        if (!isSelectionMode) viewModel.toggleSelectionMode(true)
                        viewModel.toggleItemSelection(item.id) 
                    }
                ) 
            }
        }
    }

    if (showAddDialog) AddPasswordDialog(onAdd = { p, a, pw -> viewModel.addPassword(p, a, pw) }, onDismiss = { showAddDialog = false })

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                ListItem(
                    headlineContent = { Text(AppLanguage.t("Fingerprint Unlock", "指纹解锁")) },
                    supportingContent = { Text(AppLanguage.t("Fingerprint required for each unlock", "每次解锁需要指纹")) },
                    trailingContent = { Switch(checked = isBiometricEnabled, onCheckedChange = { 
                        if (!it) showBiometricWarning = true else viewModel.setBiometricEnabled(true)
                    }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text(AppLanguage.t("Language", "语言")) },
                    supportingContent = { Text("English / 中文") },
                    trailingContent = { Switch(checked = isEnglish, onCheckedChange = { viewModel.setLanguage(it) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text(AppLanguage.t("Custom Background", "自定义背景图片")) },
                    trailingContent = { 
                        TextButton(
                            onClick = { 
                                onPickImageTrigger()
                            }
                        ) { Text(AppLanguage.t("Pick Image", "选择图片")) } 
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (hasCustomBg) {
                    ListItem(
                        headlineContent = { Text(AppLanguage.t("Background Ratio", "背景比例")) },
                        trailingContent = { 
                            TextButton(onClick = { viewModel.setBgScaleType((bgScaleType + 1) % 3) }) { 
                                val scales = listOf(AppLanguage.t("Crop to Fill", "充满裁剪"), AppLanguage.t("Center Fit", "适应居中"), AppLanguage.t("Stretch to Fill", "拉伸铺满"))
                                Text(scales[bgScaleType]) 
                            } 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(AppLanguage.t("Clear Background", "清除自定义背景")) },
                        trailingContent = { TextButton(onClick = { viewModel.pickBackground(null) }) { Text(AppLanguage.t("Clear", "清除")) } },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                Text(AppLanguage.t("Detailed Theme Settings", "详细主题设置"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Theme Grid/List
                Text(AppLanguage.t("Appearance Theme", "视觉主题"), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeVariant.values().forEach { variant ->
                        FilterChip(
                            selected = currentTheme == variant,
                            onClick = { viewModel.setTheme(variant) },
                            label = { Text(if (isEnglish) variant.titleEn else variant.titleCn) }
                        )
                    }
                }
                
                if (hasCustomBg) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(AppLanguage.t("Background Blur", "背景高斯模糊"), style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = bgBlur,
                        onValueChange = { viewModel.setBgBlur(it) },
                        valueRange = 0f..20f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(AppLanguage.t("Background Dimming", "背景暗度调节"), style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = bgDim,
                        onValueChange = { viewModel.setBgDim(it) },
                        valueRange = 0f..0.9f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showExportPasswordDialog) {
        PromptPasswordDialog(
            title = AppLanguage.t("Set Export Encryption Password", "设置导出加密密码"),
            tip = AppLanguage.t(
                "Tip: You can use any characters (including Chinese, symbols, non-ASCII) as your password.",
                "提示：加密密码支持任何字符（包括汉字、特殊符号、非ASCII码）。"
            ),
            onDismiss = { showExportPasswordDialog = false },
            onConfirm = { pwd ->
                showExportPasswordDialog = false
                val encryptedData = "VPP_ENCRYPTED:" + com.cst.richard.vppassword.CryptoUtils.encrypt(Gson().toJson(passwordList), pwd)
                onExportTrigger(encryptedData)
            }
        )
    }

    if (showPlaintextWarning) {
        AlertDialog(
            onDismissRequest = { showPlaintextWarning = false },
            title = { Text(AppLanguage.t("SEVERE SECURITY WARNING!", "严重安全警告！"), color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text(AppLanguage.t(
                "Plaintext export will save ALL your passwords as a regular text file. Anyone who gets this file can see your passwords. Are you SURE you want to proceed?",
                "明文导出将所有的密码以普通文本格式保存。任何获取此文件的人都可以直接看到您的密码。您确定要继续吗？"
            )) },
            confirmButton = {
                TextButton(onClick = { 
                    showPlaintextWarning = false
                    onExportTrigger(Gson().toJson(passwordList))
                }) { Text(AppLanguage.t("PROCED", "确定导出"), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showPlaintextWarning = false }) { Text(AppLanguage.t("Cancel", "取消")) }
            }
        )
    }

    if (showBiometricWarning) {
        AlertDialog(
            onDismissRequest = { showBiometricWarning = false },
            title = { Text(AppLanguage.t("Disable Biometric Unlock?", "确定关闭指纹解锁？"), color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text(AppLanguage.t(
                "Disabling this will allow anyone with physical access to your phone to open this app and see all your passwords. This is HIGHLY DISCOURAGED for security.",
                "关闭指纹解锁会导致任何拿起您手机的人都能直接看到您的所有密码。出于安全考虑，强烈建议保持开启状态。"
            )) },
            confirmButton = {
                TextButton(onClick = { 
                    showBiometricWarning = false
                    viewModel.setBiometricEnabled(false)
                }) { Text(AppLanguage.t("Confirm Disable", "坚持关闭"), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricWarning = false }) { Text(AppLanguage.t("Keep Enabled", "保持开启")) }
            }
        )
    }

    if (isFirstLaunch) {
        AlertDialog(
            onDismissRequest = { }, // Force action
            title = { Text(AppLanguage.t("Security Recommendation", "安全建议"), fontWeight = FontWeight.Bold) },
            text = { Text(AppLanguage.t(
                "Welcome to VPPassword. To keep your data safe, we strongly recommend enabling Fingerprint Unlock in the settings.",
                "欢迎使用 VPPassword。为了保护您的数据安全，我们强烈建议您在右上角的设置中开启“指纹解锁”功能。"
            )) },
            confirmButton = {
                TextButton(onClick = { viewModel.completeOnboarding() }) { 
                    Text(AppLanguage.t("I Understand", "我知道了")) 
                }
            }
        )
    }
}
