package com.cst.richard.vppassword

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cst.richard.vppassword.ui.theme.VPPasswordTheme
import com.cst.richard.vppassword.ui.AddPasswordScreen
import com.cst.richard.vppassword.ui.AuthScreen
import com.cst.richard.vppassword.ui.PromptPasswordDialog
import java.io.File

import com.cst.richard.vppassword.ui.VPPasswordApp
import com.cst.richard.vppassword.ui.AppLanguage

class MainActivity : FragmentActivity() {

    private val EXPORT_REQUEST_CODE = 0x4321
    private val IMPORT_REQUEST_CODE = 0x4322
    private val PICK_IMAGE_REQUEST_CODE = 0x4323
    private val IMPORT_GOOGLE_CSV_REQUEST_CODE = 0x4324

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent screenshots and screen recording
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setHighRefreshRate()
        setContent {
            val viewModel: MainViewModel = viewModel()
            VPPasswordApp(
                viewModel = viewModel,
                onPickImageTrigger = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    }
                    startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
                },
                onExportTrigger = { format, entries, pwd, destination ->
                    viewModel.prepareExport(format, entries, pwd) { text ->
                        val ext = when(format) {
                            MainViewModel.ExportFormat.JSON, MainViewModel.ExportFormat.JSON_ENCRYPTED -> "json"
                            MainViewModel.ExportFormat.CSV, MainViewModel.ExportFormat.CSV_ENCRYPTED -> "csv"
                            MainViewModel.ExportFormat.MARKDOWN -> "md"
                            else -> "pdf"
                        }
                        val mime = when(ext) {
                            "json" -> "application/json"
                            "csv" -> "text/csv"
                            "md" -> "text/markdown"
                            "pdf" -> "application/pdf"
                            else -> "text/plain"
                        }
                        val dateStr = java.text.SimpleDateFormat("dd_MM", java.util.Locale.getDefault()).format(java.util.Date())
                        val fileName = "vppwd_$dateStr.$ext"

                        if (destination == MainViewModel.ExportDestination.SHARE) {
                            shareExportFile(fileName, mime, viewModel)
                        } else {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = mime
                                putExtra(Intent.EXTRA_TITLE, fileName)
                            }
                            startActivityForResult(intent, EXPORT_REQUEST_CODE)
                        }
                    }
                },
                onImportTrigger = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    startActivityForResult(intent, IMPORT_REQUEST_CODE)
                }
            )
        }
    }

    private fun shareExportFile(fileName: String, mime: String, viewModel: MainViewModel) {
        try {
            val cacheDir = File(cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, fileName)
            val os = file.outputStream()
            viewModel.writeExportToStream(os)
            
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, AppLanguage.t("Share Backup", "分享备份")))
        } catch (e: Exception) {
            Toast.makeText(this, AppLanguage.t("Share failed", "分享失败") + ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun setHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) this.display else windowManager.defaultDisplay
                val modes = display?.supportedModes ?: return
                val maxMode = modes.maxByOrNull { it.refreshRate } ?: return
                val params = window.attributes
                params.preferredDisplayModeId = maxMode.modeId
                window.attributes = params
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val viewModel: MainViewModel by lazy { androidx.lifecycle.ViewModelProvider(this).get(MainViewModel::class.java) }
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                EXPORT_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        try {
                            contentResolver.openOutputStream(uri)?.let { os ->
                                viewModel.writeExportToStream(os)
                                Toast.makeText(this, AppLanguage.t("File exported", "文件已导出", "文件已導出", "ファイルをエクスポートしました", "Fichier exporté", "Datei exportiert", "파일을 내보냈습니다", "Archivo exportado"), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { Toast.makeText(this, AppLanguage.t("Export failed", "导出失败", "導出失敗", "エクスポートに失敗しました", "Échec de l'exportation", "Export fehlgeschlagen", "내보내기 실패", "Exportación fallida"), Toast.LENGTH_SHORT).show() }
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
                                    viewModel.importData(content, 
                                        { size -> Toast.makeText(this, AppLanguage.t("Successfully imported $size records", "成功导入 $size 条", "成功導入 $size 條", "$size 件のレコードをインポートしました", "$size enregistrements importés", "$size Datensätze importiert", "${size}개 항목을 가져왔습니다", "Se importaron $size registros"), Toast.LENGTH_SHORT).show() },
                                        { Toast.makeText(this, AppLanguage.t("Parsing or import failed", "解析或导入失败", "解析或導入失敗", "解析またはインポートに失敗しました", "Échec de l'analyse ou de l'importation", "Parsing oder Import fehlgeschlagen", "파싱 또는 가져오기 실패", "Fallo al analizar o importar"), Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            }
                        } catch (e: Exception) { Toast.makeText(this, AppLanguage.t("Import failed", "导入失败", "導入失敗", "インポートに失敗しました", "Échec de l'importation", "Import fehlgeschlagen", "가져오기 실패", "Importación fallida"), Toast.LENGTH_SHORT).show() }
                    }
                }
                PICK_IMAGE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        viewModel.lastPickedUri = uri
                        viewModel.pickBackground(uri)
                    }
                }
                IMPORT_GOOGLE_CSV_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        try {
                            contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                                val content = reader.readText()
                                viewModel.importGoogleCsv(content, 
                                    { size: Int -> Toast.makeText(this, AppLanguage.t("Successfully imported $size Google passwords", "成功导入 $size 条谷歌密码", "成功導入 $size 條谷歌密碼", "$size 件のGoogleパスワードをインポートしました", "$size mots de passe Google importés", "$size Google-Passwörter importiert", "${size}개의 Google 비밀번호를 가져왔습니다", "Se importaron $size contraseñas de Google"), Toast.LENGTH_SHORT).show() },
                                    { Toast.makeText(this, AppLanguage.t("Google CSV parsing failed", "谷歌CSV解析失败", "谷歌CSV解析失敗", "Google CSVの解析に失败しました", "Échec de l'analyse du CSV Google", "Google CSV-Parsing fehlgeschlagen", "Google CSV 파싱 실패", "Fallo al analizar el CSV de Google"), Toast.LENGTH_SHORT).show() }
                                )
                            }
                        } catch (e: Exception) { Toast.makeText(this, AppLanguage.t("Import failed", "导入失败", "導入失敗", "インポートに失敗しました", "Échec de l'importation", "Import fehlgeschlagen", "가져오기失败", "Importación fallida"), Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        }
    }
}
