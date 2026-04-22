package com.cst.richard.vppassword.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cst.richard.vppassword.CryptoUtils
import com.cst.richard.vppassword.MainViewModel
import com.cst.richard.vppassword.PasswordEntry
import com.cst.richard.vppassword.ui.theme.VPPasswordTheme

@Composable
fun VPPasswordApp(
    viewModel: MainViewModel,
    onPickImageTrigger: () -> Unit,
    onExportTrigger: (MainViewModel.ExportFormat, List<PasswordEntry>, String?, MainViewModel.ExportDestination) -> Unit,
    onImportTrigger: () -> Unit
) {
    val context = LocalContext.current
    val currentThemeInt by viewModel.themeVariant.collectAsState()
    val currentTheme = com.cst.richard.vppassword.ui.theme.ThemeVariant.values().getOrElse(currentThemeInt) { com.cst.richard.vppassword.ui.theme.ThemeVariant.MIDNIGHT_DARK }
    val bgBitmap by viewModel.bgBitmap.collectAsState()
    val bgScaleType by viewModel.bgScaleType.collectAsState()
    val bgBlur by viewModel.bgBlur.collectAsState()
    val bgDim by viewModel.bgDim.collectAsState()
    val isUnlocked = viewModel.isUnlocked
    val isPreviewingBg = viewModel.isPreviewingBg
    val pendingImportJson by viewModel.pendingImportJson.collectAsState()

    VPPasswordTheme(themeVariant = currentTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                val currentPendingImport = pendingImportJson
                if (currentPendingImport != null) {
                    PromptPasswordDialog(
                        title = AppLanguage.t("Enter Decryption Password", "输入解密密码"),
                        extraContent = null,
                        onDismiss = { viewModel.updatePendingImport(null) },
                        onConfirm = { pwd ->
                            try {
                                val json = CryptoUtils.decrypt(currentPendingImport, pwd)
                                viewModel.importData(json, 
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

                if (isPreviewingBg) {
                    BackgroundPreviewScreen(
                        viewModel = viewModel,
                        onConfirm = { viewModel.confirmBackground() },
                        onCancel = { viewModel.cancelPreview() }
                    )
                } else if (isUnlocked) {
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
                        onPickImageTrigger = onPickImageTrigger,
                        onExportTrigger = onExportTrigger,
                        onImportTrigger = onImportTrigger
                    )
                } else {
                    AuthScreen(onUnlockSuccess = { viewModel.unlock() })
                }
            }
        }
    }
}
