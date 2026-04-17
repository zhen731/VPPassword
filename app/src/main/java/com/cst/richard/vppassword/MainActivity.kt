package com.cst.richard.vppassword

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cst.richard.vppassword.ui.theme.VPPasswordTheme
import com.cst.richard.vppassword.ui.AddPasswordScreen
import com.cst.richard.vppassword.ui.AuthScreen
import com.cst.richard.vppassword.ui.PromptPasswordDialog
import java.io.File

object AppLanguage {
    var isEnglish by mutableStateOf(false)
    fun t(en: String, cn: String): String = if (isEnglish) en else cn
}

enum class SortOrder(val titleEn: String, val titleCn: String) {
    NAME_ASC("Name (A-Z)", "名称 (A-Z)"),
    NAME_DESC("Name (Z-A)", "名称 (Z-A)"),
    NEWEST("Newest First", "最新优先"),
    OLDEST("Oldest First", "最早优先")
}

class MainActivity : FragmentActivity() {

    private val EXPORT_REQUEST_CODE = 0x4321
    private val IMPORT_REQUEST_CODE = 0x4322
    private val PICK_IMAGE_REQUEST_CODE = 0x4323

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Temporarily disabled for stability during data recovery
        setHighRefreshRate()
        setContent {
            val viewModel: MainViewModel = viewModel()
            MainEntryPoint(viewModel)
        }
    }

    /**
     * Attempts to unlock the highest available refresh rate of the device.
     */
    @Suppress("DEPRECATION")
    private fun setHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11+, we can request a high frame rate via Display.Mode
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    this.display
                } else {
                    windowManager.defaultDisplay
                }
                
                val modes = display?.supportedModes ?: return
                // Find the mode with the highest refresh rate
                val maxMode = modes.maxByOrNull { it.refreshRate } ?: return
                
                val params = window.attributes
                params.preferredDisplayModeId = maxMode.modeId
                window.attributes = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    fun MainEntryPoint(viewModel: MainViewModel) {
        val context = LocalContext.current
        val isUnlocked = viewModel.isUnlocked
        val currentTheme by viewModel.themeVariant.collectAsState()
        val bgBitmap by viewModel.bgBitmap.collectAsState()
        val bgScaleType by viewModel.bgScaleType.collectAsState()
        val bgBlur by viewModel.bgBlur.collectAsState()
        val bgDim by viewModel.bgDim.collectAsState()
        val pendingImportJson = viewModel.pendingImportJson

        VPPasswordTheme(themeVariant = currentTheme) {
            if (pendingImportJson != null) {
                PromptPasswordDialog(
                    title = AppLanguage.t("Enter Decryption Password", "输入解密密码"),
                    onDismiss = { viewModel.updatePendingImport(null) },
                    onConfirm = { pwd ->
                        try {
                            val json = CryptoUtils.decrypt(pendingImportJson, pwd)
                            viewModel.importJson(json, 
                                onComplete = { size -> Toast.makeText(context, AppLanguage.t("Successfully imported $size records", "成功导入 $size 条"), Toast.LENGTH_SHORT).show() },
                                onError = { Toast.makeText(context, AppLanguage.t("Parsing or import failed", "解析或导入失败"), Toast.LENGTH_SHORT).show() }
                            )
                            viewModel.updatePendingImport(null)
                        } catch (e: Exception) {
                            Toast.makeText(context, AppLanguage.t("Invalid password or decryption failed", "密码错误或解密失败"), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            if (isUnlocked) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        bgBitmap?.let {
                            val scale = when(bgScaleType) {
                                1 -> ContentScale.Fit
                                2 -> ContentScale.FillBounds
                                else -> ContentScale.Crop
                            }
                            Image(
                                bitmap = it, 
                                contentDescription = null, 
                                contentScale = scale, 
                                modifier = Modifier.fillMaxSize().blur(bgBlur.dp)
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgDim)))
                        }
                        AddPasswordScreen(
                            viewModel = viewModel,
                            hasCustomBg = bgBitmap != null,
                            onPickImageTrigger = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "image/*"
                                }
                                startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
                            },
                            onExportTrigger = { text ->
                                viewModel.dataToExport = text
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
                AuthScreen(onUnlockSuccess = { viewModel.unlock() })
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val viewModel: MainViewModel by lazy {
            androidx.lifecycle.ViewModelProvider(this).get(MainViewModel::class.java)
        }
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                EXPORT_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        try {
                            contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(viewModel.dataToExport.toByteArray(Charsets.UTF_8))
                                Toast.makeText(this, AppLanguage.t("File exported", "文件已导出"), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { Toast.makeText(this, AppLanguage.t("Export failed", "导出失败"), Toast.LENGTH_SHORT).show() }
                    }
                }
                IMPORT_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        try {
                            contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                                val content = reader.readText()
                                if (content.startsWith("VPP_ENCRYPTED:")) {
                                    viewModel.updatePendingImport(content.removePrefix("VPP_ENCRYPTED:"))
                                } else {
                                    viewModel.importJson(content, 
                                        { size -> Toast.makeText(this, AppLanguage.t("Successfully imported $size records", "成功导入 $size 条"), Toast.LENGTH_SHORT).show() },
                                        { Toast.makeText(this, AppLanguage.t("Parsing or import failed", "解析或导入失败"), Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            }
                        } catch (e: Exception) { Toast.makeText(this, AppLanguage.t("Import failed", "导入失败"), Toast.LENGTH_SHORT).show() }
                    }
                }
                PICK_IMAGE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        viewModel.pickBackground(uri)
                    }
                }
            }
        }
    }
}

