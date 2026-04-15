package com.cst.richard.vppassword

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.graphics.BitmapFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cst.richard.vppassword.ui.theme.ThemeVariant
import com.cst.richard.vppassword.ui.theme.VPPasswordTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Palette
import com.cst.richard.vppassword.ui.theme.VPPasswordTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow

object AppLanguage {
    var isEnglish by mutableStateOf(false)
    fun t(en: String, cn: String): String = if (isEnglish) en else cn
}

class MainActivity : FragmentActivity() {

    private val EXPORT_REQUEST_CODE = 0x4321
    private val IMPORT_REQUEST_CODE = 0x4322
    private val PICK_IMAGE_REQUEST_CODE = 0x4323
    private var dataToExport: String = ""
    internal var pendingPickBgCallback: ((Uri?) -> Unit)? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("dataToExport", dataToExport)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        dataToExport = savedInstanceState.getString("dataToExport") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val dao = db.passwordDao()

        setContent {
            MainEntryPoint(dao)
        }
    }

    @Composable
    fun MainEntryPoint(dao: PasswordDao) {
        val context = LocalContext.current
        val themeOrdinal = remember { mutableStateOf(getSettingInt(context, "theme_variant", ThemeVariant.MIDNIGHT_DARK.ordinal)) }
        val currentTheme = ThemeVariant.values()[themeOrdinal.value.coerceIn(0, ThemeVariant.values().size - 1)]
        val isBiometricEnabled = remember { mutableStateOf(getSetting(context, "biometric_enabled", true)) }
        val isEng = remember { mutableStateOf(getSetting(context, "is_english", false)) }
        AppLanguage.isEnglish = isEng.value
        
        var isUnlocked by remember { mutableStateOf(!isBiometricEnabled.value) }

        val customBackgroundUri = remember { mutableStateOf<String?>(getSettingString(context, "custom_bg_path", null)) }
        var bgBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var bgScaleType by remember { mutableStateOf(getSettingInt(context, "bg_scale_type", 0)) }
        val scope = rememberCoroutineScope()
        
        (context as? MainActivity)?.pendingPickBgCallback = { uri ->
            scope.launch(Dispatchers.IO) {
                if (uri != null) {
                    saveCustomBackgroundImage(context, uri)
                    val newPath = File(context.filesDir, "custom_bg.jpg").absolutePath
                    withContext(Dispatchers.Main) {
                        customBackgroundUri.value = newPath
                        saveSettingString(context, "custom_bg_path", newPath)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        customBackgroundUri.value = null
                        saveSettingString(context, "custom_bg_path", null)
                        File(context.filesDir, "custom_bg.jpg").delete()
                    }
                }
            }
        }
        
        LaunchedEffect(customBackgroundUri.value) {
            val path = customBackgroundUri.value
            if (path != null && File(path).exists()) {
                withContext(Dispatchers.IO) {
                    try {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(path, options)
                        
                        var inSampleSize = 1
                        val reqSize = 2000
                        if (options.outHeight > reqSize || options.outWidth > reqSize) {
                            val halfHeight: Int = options.outHeight / 2
                            val halfWidth: Int = options.outWidth / 2
                            while (halfHeight / inSampleSize >= reqSize && halfWidth / inSampleSize >= reqSize) {
                                inSampleSize *= 2
                            }
                        }
                        options.apply {
                            this.inSampleSize = inSampleSize
                            this.inJustDecodeBounds = false
                        }
                        
                        val bitmap = BitmapFactory.decodeFile(path, options)
                        if (bitmap != null) {
                            bgBitmap = bitmap.asImageBitmap()
                        } else {
                            bgBitmap = null
                        }
                    } catch (e: Throwable) { e.printStackTrace() }
                }
            } else {
                bgBitmap = null
            }
        }

        VPPasswordTheme(themeVariant = currentTheme) {
            val pendingImport = _pendingImportDialog.value
            if (pendingImport != null) {
                PromptPasswordDialog(
                    title = AppLanguage.t("Enter Decryption Password", "输入解密密码"),
                    onDismiss = { _pendingImportDialog.value = null },
                    onConfirm = { pwd ->
                        try {
                            val json = CryptoUtils.decrypt(pendingImport, pwd)
                            processImportJson(json)
                            _pendingImportDialog.value = null
                        } catch (e: Exception) {
                            Toast.makeText(context, AppLanguage.t("Invalid password or decryption failed", "密码错误或解密失败"), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            if (isUnlocked) {
                val passwordList by dao.getAllPasswords().collectAsState(initial = emptyList())
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        bgBitmap?.let {
                            val scale = when(bgScaleType) {
                                1 -> ContentScale.Fit
                                2 -> ContentScale.FillBounds
                                else -> ContentScale.Crop
                            }
                            Image(bitmap = it, contentDescription = null, contentScale = scale, modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                        }
                        AddPasswordScreen(
                            dao = dao,
                            passwordList = passwordList,
                            currentTheme = currentTheme,
                            hasCustomBg = bgBitmap != null,
                            bgScaleType = bgScaleType,
                            onBgScaleTypeChange = { 
                                bgScaleType = it
                                saveSettingInt(context, "bg_scale_type", it)
                            },
                            isBiometricEnabled = isBiometricEnabled.value,
                            onThemeSelect = { variant ->
                                themeOrdinal.value = variant.ordinal
                                saveSettingInt(context, "theme_variant", variant.ordinal)
                            },
                            onPickBackground = { uri ->
                                (context as? MainActivity)?.pendingPickBgCallback?.invoke(uri)
                            },
                            onPickImageTrigger = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "image/*"
                                }
                                (context as? MainActivity)?.startActivityForResult(intent, 0x4323)
                            },
                        onBiometricChange = { 
                            isBiometricEnabled.value = it
                            saveSetting(context, "biometric_enabled", it)
                        },
                        onLanguageChange = {
                            isEng.value = it
                            saveSetting(context, "is_english", it)
                        },
                        onExportTrigger = { text ->
                            dataToExport = text
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TITLE, "vppass_backup.txt")
                            }
                            startActivityForResult(intent, EXPORT_REQUEST_CODE)
                        },
                        onImportTrigger = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "text/plain"
                            }
                            startActivityForResult(intent, IMPORT_REQUEST_CODE)
                        }
                    )
                    }
                }
            } else {
                AuthScreen(onUnlockSuccess = { isUnlocked = true })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                EXPORT_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        try {
                            contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(dataToExport.toByteArray(Charsets.UTF_8))
                                Toast.makeText(this, AppLanguage.t("File exported", "文件已导出"), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { Toast.makeText(this, AppLanguage.t("Export failed", "导出失败"), Toast.LENGTH_SHORT).show() }
                    }
                }
                IMPORT_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        try {
                            contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                                handleImport(reader.readText())
                            }
                        } catch (e: Exception) { Toast.makeText(this, AppLanguage.t("Import failed", "导入失败"), Toast.LENGTH_SHORT).show() }
                    }
                }
                0x4323 -> { // PICK_IMAGE_REQUEST_CODE
                    data?.data?.let { uri ->
                        pendingPickBgCallback?.invoke(uri)
                    }
                }
            }
        }
    }

    private val _pendingImportDialog = mutableStateOf<String?>(null)

    private fun handleImport(content: String) {
        if (content.startsWith("VPP_ENCRYPTED:")) {
            _pendingImportDialog.value = content.removePrefix("VPP_ENCRYPTED:")
        } else {
            processImportJson(content)
        }
    }

    internal fun processImportJson(json: String) {
        lifecycleScope.launch {
            try {
                val list: List<PasswordEntry>? = Gson().fromJson(json, object : TypeToken<List<PasswordEntry>>() {}.type)
                
                val dao = AppDatabase.getDatabase(this@MainActivity).passwordDao()
                list?.forEach { 
                    // id=0 让 Room 放弃原有 id，生成新 id 防冲突
                    dao.insert(it.copy(id = 0)) 
                }
                
                Toast.makeText(this@MainActivity, AppLanguage.t("Successfully imported ${list?.size ?: 0} records", "成功导入 ${list?.size ?: 0} 条"), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { 
                Toast.makeText(this@MainActivity, AppLanguage.t("Parsing or import failed", "解析或导入失败"), Toast.LENGTH_SHORT).show() 
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            // 建议加上失败回调，防止用户取消后界面卡死
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // 如果不是用户主动取消，可以弹个提示
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                }
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(AppLanguage.t("Authentication", "身份验证"))
            .setSubtitle(AppLanguage.t("Please authenticate to unlock the safe", "请通过验证解锁保险箱"))
            // 允许使用强生物特征或设备凭据（密码/PIN图案），如果开启此项则不能设置 NegativeButtonText
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(info)
    }

    @Composable
    fun AuthScreen(onUnlockSuccess: () -> Unit) {
        LaunchedEffect(Unit) { showBiometricPrompt(onUnlockSuccess) }
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { showBiometricPrompt(onUnlockSuccess) }) { Text(AppLanguage.t("Unlock Safe", "解锁保险箱")) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordScreen(
    dao: PasswordDao,
    passwordList: List<PasswordEntry>,
    currentTheme: ThemeVariant,
    hasCustomBg: Boolean,
    bgScaleType: Int,
    onBgScaleTypeChange: (Int) -> Unit,
    isBiometricEnabled: Boolean,
    onThemeSelect: (ThemeVariant) -> Unit,
    onPickBackground: (Uri?) -> Unit,
    onPickImageTrigger: () -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onLanguageChange: (Boolean) -> Unit,
    onExportTrigger: (String) -> Unit,
    onImportTrigger: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }

    val iconMod = Modifier
    val textShadow = if (hasCustomBg) androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 6f) else null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("VPPassword", fontWeight = FontWeight.Black, style = androidx.compose.ui.text.TextStyle(shadow = textShadow, color = if(hasCustomBg) Color.White else Color.Unspecified)) },
                navigationIcon = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }, modifier = iconMod) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = if(hasCustomBg) Color.White else androidx.compose.material3.LocalContentColor.current)
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
                                onClick = { showMenu = false; onExportTrigger(Gson().toJson(passwordList)) }
                            )
                        }
                    }
                },
                actions = {
                    var showThemeMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showThemeMenu = true }, modifier = iconMod) { 
                        Icon(Icons.Default.Palette, null, tint = if(hasCustomBg) Color.White else androidx.compose.material3.LocalContentColor.current) 
                        DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                            ThemeVariant.values().forEach { variant ->
                                DropdownMenuItem(
                                    text = { Text(if (AppLanguage.isEnglish) variant.titleEn else variant.titleCn) },
                                    onClick = { showThemeMenu = false; onThemeSelect(variant) }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showSettings = true }, modifier = iconMod) { Icon(Icons.Default.Settings, null, tint = if(hasCustomBg) Color.White else androidx.compose.material3.LocalContentColor.current) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (hasCustomBg) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null) }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
            items(passwordList, key = { it.id }) { item -> PasswordItemCard(item, dao, scope, hasCustomBg) }
        }
    }

    if (showAddDialog) AddPasswordDialog(dao = dao, onDismiss = { showAddDialog = false })

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                ListItem(
                    headlineContent = { Text(AppLanguage.t("Fingerprint Unlock", "指纹解锁")) },
                    supportingContent = { Text(AppLanguage.t("Fingerprint required for each unlock", "每次解锁需要指纹")) },
                    trailingContent = { Switch(checked = isBiometricEnabled, onCheckedChange = onBiometricChange) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text(AppLanguage.t("Language", "语言")) },
                    supportingContent = { Text("English / 中文") },
                    trailingContent = { Switch(checked = AppLanguage.isEnglish, onCheckedChange = onLanguageChange) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text(AppLanguage.t("Custom Background", "自定义背景图片")) },
                    trailingContent = { 
                        androidx.compose.material3.TextButton(
                            onClick = { 
                                try {
                                    onPickImageTrigger()
                                } catch (e: Exception) {
                                    Toast.makeText(context, AppLanguage.t("Failed to open picker: ", "打开相册失败: ") + e.message, Toast.LENGTH_LONG).show()
                                    e.printStackTrace()
                                }
                            }
                        ) { Text(AppLanguage.t("Pick Image", "选择图片")) } 
                    },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (hasCustomBg) {
                    ListItem(
                        headlineContent = { Text(AppLanguage.t("Background Ratio", "背景比例")) },
                        trailingContent = { 
                            androidx.compose.material3.TextButton(onClick = { onBgScaleTypeChange((bgScaleType + 1) % 3) }) { 
                                val scales = listOf(AppLanguage.t("Crop to Fill", "充满裁剪"), AppLanguage.t("Center Fit", "适应居中"), AppLanguage.t("Stretch to Fill", "拉伸铺满"))
                                Text(scales[bgScaleType]) 
                            } 
                        },
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(AppLanguage.t("Clear Background", "清除自定义背景")) },
                        trailingContent = { androidx.compose.material3.TextButton(onClick = { onPickBackground(null) }) { Text(AppLanguage.t("Clear", "清除")) } },
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (showExportPasswordDialog) {
        PromptPasswordDialog(
            title = AppLanguage.t("Set Export Encryption Password", "设置导出加密密码"),
            onDismiss = { showExportPasswordDialog = false },
            onConfirm = { pwd ->
                showExportPasswordDialog = false
                val encryptedData = "VPP_ENCRYPTED:" + CryptoUtils.encrypt(Gson().toJson(passwordList), pwd)
                onExportTrigger(encryptedData)
            }
        )
    }
}

@Composable
fun PasswordItemCard(entry: PasswordEntry, dao: PasswordDao, scope: kotlinx.coroutines.CoroutineScope, hasCustomBg: Boolean) {
    var isVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val cardColor = if (hasCustomBg) MaterialTheme.colorScheme.surface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
    val cardBorder = if (hasCustomBg) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null

    val itemIconMod = Modifier

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = if (hasCustomBg) 0.dp else 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(entry.projectName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if(hasCustomBg) Color.White else Color.Unspecified)
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(if(isVisible) entry.password else "•••••••••", fontFamily = FontFamily.Monospace, color = if(hasCustomBg) Color.White.copy(alpha=0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val iconTint = if (hasCustomBg) Color.White else androidx.compose.material3.LocalContentColor.current
                androidx.compose.material3.IconButton(onClick = { isVisible = !isVisible }, modifier = Modifier.size(32.dp).then(itemIconMod)) { androidx.compose.material3.Icon(if(isVisible) androidx.compose.material.icons.Icons.Default.Visibility else androidx.compose.material.icons.Icons.Default.Lock, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                androidx.compose.material3.IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(32.dp).then(itemIconMod)) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                androidx.compose.material3.IconButton(onClick = { copyToClipboard(context, entry.password, AppLanguage.t("Copied", "已复制")) }, modifier = Modifier.size(32.dp).then(itemIconMod)) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                androidx.compose.material3.IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp).then(itemIconMod)) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = if(hasCustomBg) Color(0xFFFF5252) else Color.Red.copy(alpha = 0.8f)) }
            }
        }
    }
    if (showDeleteDialog) androidx.compose.material3.AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { androidx.compose.material3.Text(AppLanguage.t("Confirm delete?", "确认删除？")) }, confirmButton = { androidx.compose.material3.TextButton(onClick = { scope.launch { dao.delete(entry) }; showDeleteDialog = false }) { androidx.compose.material3.Text(AppLanguage.t("Confirm", "确认"), color = Color.Red) } }, dismissButton = { androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) { androidx.compose.material3.Text(AppLanguage.t("Cancel", "取消")) } })
    if (showEditDialog) EditPasswordDialog(entry, onDismiss = { showEditDialog = false }, onConfirm = { scope.launch { dao.update(it) }; showEditDialog = false })
}

@Composable
fun EditPasswordDialog(entry: PasswordEntry, onDismiss: () -> Unit, onConfirm: (PasswordEntry) -> Unit) {
    var p by remember { mutableStateOf(entry.projectName) }
    var a by remember { mutableStateOf(entry.account) }
    var pw by remember { mutableStateOf(entry.password) }
    val isValid = p.isNotBlank() && pw.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppLanguage.t("Edit", "编辑")) },
        text = { Column { OutlinedTextField(p, { p = it }, label = { Text(AppLanguage.t("Project *", "项目 *")) }); OutlinedTextField(a, { a = it }, label = { Text(AppLanguage.t("Account (Optional)", "账号 (选填)")) }); OutlinedTextField(pw, { pw = it }, label = { Text(AppLanguage.t("Password *", "密码 *")) }) } },
        confirmButton = { Button(onClick = { onConfirm(entry.copy(projectName = p, account = a, password = pw)) }, enabled = isValid) { Text(AppLanguage.t("Save", "保存")) } }
    )
}

@Composable
fun AddPasswordDialog(dao: PasswordDao, onDismiss: () -> Unit) {
    var p by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val isValid = p.isNotBlank() && pw.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppLanguage.t("New Record", "新建记录")) },
        text = { Column { OutlinedTextField(p, { p = it }, label = { Text(AppLanguage.t("Project *", "项目 *")) }); OutlinedTextField(a, { a = it }, label = { Text(AppLanguage.t("Account (Optional)", "账号 (选填)")) }); OutlinedTextField(pw, { pw = it }, label = { Text(AppLanguage.t("Password *", "密码 *")) }) } },
        confirmButton = {
            Button(onClick = {
                scope.launch { dao.insert(PasswordEntry(projectName = p, account = a, password = pw)) }
                onDismiss() // 建议放在 launch 外面或者紧跟其后
            }, enabled = isValid) { Text(AppLanguage.t("Add", "添加")) }
        }
    )
}

@Composable
fun PromptPasswordDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    val isValid = pwd.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(pwd, { pwd = it }, label = { Text(AppLanguage.t("Password", "密码")) }) },
        confirmButton = { Button(onClick = { onConfirm(pwd) }, enabled = isValid) { Text(AppLanguage.t("Confirm", "确认")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(AppLanguage.t("Cancel", "取消")) } }
    )
}

fun copyToClipboard(context: Context, text: String, msg: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("VP", text))
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun saveSetting(c: Context, k: String, v: Boolean) { c.getSharedPreferences("vpp", Context.MODE_PRIVATE).edit().putBoolean(k, v).apply() }
fun getSetting(c: Context, k: String, d: Boolean = true): Boolean = c.getSharedPreferences("vpp", Context.MODE_PRIVATE).getBoolean(k, d)

fun saveSettingString(c: Context, k: String, v: String?) { c.getSharedPreferences("vpp", Context.MODE_PRIVATE).edit().putString(k, v).apply() }
fun getSettingString(c: Context, k: String, d: String? = null): String? = c.getSharedPreferences("vpp", Context.MODE_PRIVATE).getString(k, d)

fun saveSettingInt(c: Context, k: String, v: Int) { c.getSharedPreferences("vpp", Context.MODE_PRIVATE).edit().putInt(k, v).apply() }
fun getSettingInt(c: Context, k: String, d: Int = 0): Int = c.getSharedPreferences("vpp", Context.MODE_PRIVATE).getInt(k, d)

fun saveCustomBackgroundImage(context: Context, uri: Uri) {
    try {
        val destFile = File(context.filesDir, "custom_bg.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

