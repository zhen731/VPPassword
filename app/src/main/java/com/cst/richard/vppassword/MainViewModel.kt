package com.cst.richard.vppassword

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cst.richard.vppassword.ui.AppLanguage
import com.cst.richard.vppassword.ui.Language
import com.cst.richard.vppassword.ui.SortOrder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).passwordDao()
    private val prefs = application.getSharedPreferences("vpp_settings", Context.MODE_PRIVATE)
    private val context = application

    private val _passwordList = MutableStateFlow<List<PasswordEntry>>(emptyList())
    val passwordList: StateFlow<List<PasswordEntry>> = _passwordList.asStateFlow()

    private val _allSecrets = MutableStateFlow<List<PasswordEntry>>(emptyList())
    val allSecrets: StateFlow<List<PasswordEntry>> = _allSecrets.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentCategory = MutableStateFlow(0)
    val currentCategory: StateFlow<Int> = _currentCategory.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.values()[getSettingInt("sort_order", 0)])
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isUnlocked = mutableStateOf(false)
    val isUnlocked: Boolean get() = _isUnlocked.value

    private val _isFirstLaunch = MutableStateFlow(getSetting("is_first_launch", true))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _themeVariant = MutableStateFlow(getSettingInt("theme_variant", 0))
    val themeVariant: StateFlow<Int> = _themeVariant.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(getSetting("biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _currentLanguage = MutableStateFlow(Language.values()[getSettingInt("language_index", Language.EN.ordinal)])
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _bgBitmap = MutableStateFlow<ImageBitmap?>(null)
    val bgBitmap: StateFlow<ImageBitmap?> = _bgBitmap.asStateFlow()

    private val _bgBlur = MutableStateFlow(getSettingFloat("bg_blur", 0f))
    val bgBlur: StateFlow<Float> = _bgBlur.asStateFlow()

    private val _bgDim = MutableStateFlow(getSettingFloat("bg_dim", 0f))
    val bgDim: StateFlow<Float> = _bgDim.asStateFlow()

    private val _bgScaleType = MutableStateFlow(getSettingInt("bg_scale_type", 0))
    val bgScaleType: StateFlow<Int> = _bgScaleType.asStateFlow()

    private val _customBgPath = MutableStateFlow(getSettingString("custom_bg_path", null))
    val customBgPath: StateFlow<String?> = _customBgPath.asStateFlow()
    
    private val _pendingBgBitmap = MutableStateFlow<ImageBitmap?>(null)
    val pendingBgBitmap: StateFlow<ImageBitmap?> = _pendingBgBitmap.asStateFlow()

    var isPreviewingBg by mutableStateOf(false)
    var lastPickedUri: Uri? = null

    private val _enabledCategories = MutableStateFlow(
        getSettingString("enabled_categories", "0,1,2")?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(0, 1, 2)
    )
    val enabledCategories: StateFlow<Set<Int>> = _enabledCategories.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIds: StateFlow<Set<Int>> = _selectedIds.asStateFlow()

    private val _pendingImportJson = MutableStateFlow<String?>(null)
    val pendingImportJson: StateFlow<String?> = _pendingImportJson.asStateFlow()

    enum class ExportFormat { JSON, CSV, MARKDOWN, PDF, JSON_ENCRYPTED, CSV_ENCRYPTED }
    enum class ExportDestination { LOCAL, SHARE }

    var draftProject by mutableStateOf("")
    var draftAccount by mutableStateOf("")
    var draftPassword by mutableStateOf("")
    var draftNotes by mutableStateOf("")

    private var dataToExport: String = ""
    private var pendingExportFormat: ExportFormat = ExportFormat.JSON

    init {
        AppLanguage.currentLanguage = _currentLanguage.value
        loadBackgroundImage()
        combine(
            dao.getAllPasswords(),
            _searchQuery,
            _currentCategory,
            _sortOrder
        ) { all, query, cat, order ->
            _allSecrets.value = all
            val filtered = all.filter { it.category == cat && (it.projectName.contains(query, ignoreCase = true) || it.account.contains(query, ignoreCase = true)) }
            val sorted = when(order) {
                SortOrder.NAME_ASC -> filtered.sortedBy { it.projectName.lowercase() }
                SortOrder.NAME_DESC -> filtered.sortedByDescending { it.projectName.lowercase() }
                SortOrder.NEWEST -> filtered.sortedByDescending { it.id }
                SortOrder.OLDEST -> filtered.sortedBy { it.id }
            }
            sorted
        }.onEach { _passwordList.value = it }.launchIn(viewModelScope)
    }

    fun completeOnboarding() { _isFirstLaunch.value = false; saveSetting("is_first_launch", false) }
    fun unlock() { _isUnlocked.value = true }
    fun lock() { _isUnlocked.value = false }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategory(categoryId: Int) { _currentCategory.value = categoryId }
    fun setSortOrder(order: SortOrder) { _sortOrder.value = order; saveSettingInt("sort_order", order.ordinal) }

    fun addPassword(projectName: String, account: String, pass: String, category: Int, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(PasswordEntry(projectName = projectName, account = account, password = pass, category = category, notes = notes))
        }
    }

    fun updatePassword(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { dao.update(entry) }
    }

    fun toggleBackupCodeUsed(entry: PasswordEntry, code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val used = entry.usedCodes.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (used.contains(code)) used.remove(code) else used.add(code)
            dao.update(entry.copy(usedCodes = used.joinToString(",")))
        }
    }

    fun deletePassword(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) { dao.delete(entry) }
    }

    fun toggleCategoryVisibility(id: Int, visible: Boolean) {
        val current = _enabledCategories.value.toMutableSet()
        if (visible) current.add(id) else current.remove(id)
        if (current.isNotEmpty()) {
            _enabledCategories.value = current
            saveSettingString("enabled_categories", current.joinToString(","))
        }
    }

    fun setThemeVariant(v: Int) { _themeVariant.value = v; saveSettingInt("theme_variant", v) }
    fun setBiometricEnabled(enabled: Boolean) { _isBiometricEnabled.value = enabled; saveSetting("biometric_enabled", enabled) }
    fun setLanguage(lang: Language) { _currentLanguage.value = lang; AppLanguage.currentLanguage = lang; saveSettingInt("language_index", lang.ordinal) }
    fun setBgBlur(v: Float) { _bgBlur.value = v; saveSettingFloat("bg_blur", v) }
    fun setBgDim(v: Float) { _bgDim.value = v; saveSettingFloat("bg_dim", v) }
    private val _isImportHintShown = MutableStateFlow(getSetting("import_hint_shown", false))
    val isImportHintShown: StateFlow<Boolean> = _isImportHintShown.asStateFlow()

    fun setImportHintShown() {
        _isImportHintShown.value = true
        saveSetting("import_hint_shown", true)
    }

    fun setBgScaleType(v: Int) { _bgScaleType.value = v; saveSettingInt("bg_scale_type", v) }

    fun updatePendingImport(json: String?) { _pendingImportJson.value = json }

    fun importData(content: String, onComplete: (Int) -> Unit, onError: () -> Unit) {
        val currentCat = _currentCategory.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trimmed = content.trim()
                if (trimmed.startsWith("[")) {
                    // 1. JSON Format
                    val list: List<PasswordEntry>? = Gson().fromJson(trimmed, object : TypeToken<List<PasswordEntry>>() {}.type)
                    if (list != null && list.any { it.category != currentCat }) {
                        withContext(Dispatchers.Main) { onError() }
                        return@launch
                    }
                    list?.forEach { dao.insert(it.copy(id = 0)) }
                    withContext(Dispatchers.Main) { onComplete(list?.size ?: 0) }
                } else if (content.contains("Project Name,Account,Password") && !content.contains("| Project |")) {
                    // 2. VPP CSV Format
                    val lines = content.lines().filter { it.isNotBlank() }
                    val entriesToImport = mutableListOf<PasswordEntry>()
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
                        if (parts.size >= 4) {
                            val cat = parts[0].toIntOrNull() ?: 0
                            if (cat == currentCat) {
                                entriesToImport.add(PasswordEntry(projectName = parts[1], account = parts[2], password = parts[3], category = cat))
                            } else {
                                withContext(Dispatchers.Main) { onError() }
                                return@launch
                            }
                        }
                    }
                    entriesToImport.forEach { dao.insert(it) }
                    withContext(Dispatchers.Main) { onComplete(entriesToImport.size) }
                } else if (content.lowercase().contains("name,url,username,password") || content.lowercase().contains("name,url,user,password")) {
                    // 3. Google CSV Format
                    val lines = content.lines().filter { it.isNotBlank() }
                    if (lines.isEmpty()) { withContext(Dispatchers.Main) { onError() }; return@launch }
                    
                    fun splitCsv(line: String): List<String> {
                        val result = mutableListOf<String>()
                        var cur = StringBuilder(); var inQuotes = false; var i = 0
                        while (i < line.length) {
                            val c = line[i]
                            if (c == '\"') inQuotes = !inQuotes
                            else if (c == ',' && !inQuotes) {
                                result.add(cur.toString().trim().removeSurrounding("\""))
                                cur = StringBuilder()
                            } else cur.append(c)
                            i++
                        }
                        result.add(cur.toString().trim().removeSurrounding("\""))
                        return result
                    }

                    val header = splitCsv(lines[0].lowercase())
                    val nameIdx = header.indexOf("name"); val userIdx = header.indexOf("username").let { if (it == -1) header.indexOf("user") else it }; val passIdx = header.indexOf("password")
                    
                    if (nameIdx == -1 || passIdx == -1) { withContext(Dispatchers.Main) { onError() }; return@launch }
                    
                    var imported = 0
                    for (i in 1 until lines.size) {
                        val parts = splitCsv(lines[i])
                        if (parts.size > maxOf(nameIdx, passIdx)) {
                            val pName = parts[nameIdx]; val acc = if (userIdx != -1 && userIdx < parts.size) parts[userIdx] else ""; val pass = parts[passIdx]
                            if (pName.isNotBlank() && pass.isNotBlank()) {
                                dao.insert(PasswordEntry(projectName = pName, account = acc, password = pass, category = currentCat))
                                imported++
                            }
                        }
                    }
                    withContext(Dispatchers.Main) { onComplete(imported) }
                } else if (content.contains("|")) {
                    // 4. Markdown/Text Table Format
                    val lines = content.lines().filter { it.isNotBlank() }
                    val entriesToImport = mutableListOf<PasswordEntry>()
                    lines.forEach { line ->
                        if (line.count { it == '|' } >= 2 && !line.contains(":---")) {
                            val parts = line.split("|").map { it.trim() }.let { if (it.first().isEmpty()) it.drop(1) else it }.let { if (it.isNotEmpty() && it.last().isEmpty()) it.dropLast(1) else it }
                            if (parts.size >= 3 && parts[0].lowercase() != "project") {
                                val pName = parts[0]; val acc = parts[1]; val pass = parts[2].removeSurrounding("`")
                                if (pName.isNotBlank() && pName != "---") {
                                    entriesToImport.add(PasswordEntry(projectName = pName, account = acc, password = pass, category = currentCat))
                                }
                            }
                        }
                    }
                    if (entriesToImport.isNotEmpty()) {
                        entriesToImport.forEach { dao.insert(it) }
                        withContext(Dispatchers.Main) { onComplete(entriesToImport.size) }
                    } else {
                        withContext(Dispatchers.Main) { onError() }
                    }
                } else {
                    withContext(Dispatchers.Main) { onError() }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onError() } }
        }
    }

    fun prepareExport(format: ExportFormat, entries: List<PasswordEntry>, encryptionPassword: String? = null, onReady: (String) -> Unit) {
        pendingExportFormat = format
        viewModelScope.launch(Dispatchers.Default) {
            val rawData = when(format) {
                ExportFormat.JSON, ExportFormat.JSON_ENCRYPTED -> Gson().toJson(entries)
                ExportFormat.CSV, ExportFormat.CSV_ENCRYPTED -> ExportUtils.generateCSV(entries)
                ExportFormat.MARKDOWN -> ExportUtils.generateMarkdown(entries)
                else -> "PDF_MARKER"
            }
            val finalData = if (encryptionPassword != null) "VPP_ENCRYPTED:" + CryptoUtils.encrypt(rawData, encryptionPassword) else rawData
            withContext(Dispatchers.Main) { dataToExport = finalData; onReady(finalData) }
        }
    }

    fun writeExportToStream(outputStream: java.io.OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            if (pendingExportFormat == ExportFormat.PDF) ExportUtils.generatePDF(allSecrets.value, true, outputStream)
            else outputStream.write(dataToExport.toByteArray(Charsets.UTF_8))
            outputStream.close()
        }
    }

    fun importGoogleCsv(csv: String, onComplete: (Int) -> Unit, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lines = csv.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) { withContext(Dispatchers.Main) { onError() }; return@launch }
                
                fun splitCsv(line: String): List<String> {
                    val result = mutableListOf<String>()
                    var current = StringBuilder()
                    var inQuotes = false
                    var i = 0
                    while (i < line.length) {
                        val c = line[i]
                        if (c == '\"') inQuotes = !inQuotes
                        else if (c == ',' && !inQuotes) {
                            result.add(current.toString().trim().removeSurrounding("\""))
                            current = StringBuilder()
                        } else current.append(c)
                        i++
                    }
                    result.add(current.toString().trim().removeSurrounding("\""))
                    return result
                }

                val header = splitCsv(lines[0].lowercase())
                val nameIdx = header.indexOf("name")
                val userIdx = header.indexOf("username")
                val passIdx = header.indexOf("password")
                
                if (nameIdx == -1 || passIdx == -1) { withContext(Dispatchers.Main) { onError() }; return@launch }
                
                var imported = 0
                for (i in 1 until lines.size) {
                    val parts = splitCsv(lines[i])
                    if (parts.size > maxOf(nameIdx, passIdx)) {
                        val projectName = parts[nameIdx]
                        val account = if (userIdx != -1 && userIdx < parts.size) parts[userIdx] else ""
                        val password = parts[passIdx]
                        if (projectName.isNotBlank() && password.isNotBlank()) {
                            dao.insert(PasswordEntry(projectName = projectName, account = account, password = password))
                            imported++
                        }
                    }
                }
                withContext(Dispatchers.Main) { onComplete(imported) }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onError() } }
        }
    }

    fun pickBackground(uri: Uri?) {
        if (uri == null) { isPreviewingBg = false; return }
        viewModelScope.launch(Dispatchers.Main) {
            val bitmap = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }
            _pendingBgBitmap.value = bitmap?.asImageBitmap()
            isPreviewingBg = true
        }
    }

    fun confirmBackground() {
        viewModelScope.launch(Dispatchers.Main) {
            val uri = lastPickedUri
            if (uri != null) {
                withContext(Dispatchers.IO) {
                    val destFile = File(context.filesDir, "custom_bg.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input -> destFile.outputStream().use { input.copyTo(it) } }
                }
                val path = File(context.filesDir, "custom_bg.jpg").absolutePath
                _customBgPath.value = path
                saveSettingString("custom_bg_path", path)
                loadBackgroundImage()
            }
            isPreviewingBg = false
        }
    }

    fun cancelPreview() { isPreviewingBg = false; _pendingBgBitmap.value = null; lastPickedUri = null }

    fun removeBackground() {
        _bgBitmap.value = null
        _customBgPath.value = null
        saveSettingString("custom_bg_path", null)
        File(context.filesDir, "custom_bg.jpg").delete()
    }

    private fun loadBackgroundImage() {
        viewModelScope.launch(Dispatchers.IO) {
            val path = _customBgPath.value
            if (path != null && File(path).exists()) {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                val bitmap = BitmapFactory.decodeFile(path, options)
                withContext(Dispatchers.Main) { _bgBitmap.value = bitmap?.asImageBitmap() }
            }
        }
    }

    fun toggleSelectionMode(enabled: Boolean) { _isSelectionMode.value = enabled; if (!enabled) _selectedIds.value = emptySet() }
    fun toggleItemSelection(id: Int) { val current = _selectedIds.value; _selectedIds.value = if (current.contains(id)) current - id else current + id }
    fun selectAll() { _selectedIds.value = _passwordList.value.map { it.id }.toSet() }
    fun invertSelection() {
        val current = _selectedIds.value
        _selectedIds.value = _passwordList.value.map { it.id }.filter { !current.contains(it) }.toSet()
    }
    fun deleteSelected(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteBatch(_selectedIds.value.toList())
            withContext(Dispatchers.Main) { toggleSelectionMode(false); onComplete() }
        }
    }

    private fun saveSetting(k: String, v: Boolean) { prefs.edit().putBoolean(k, v).apply() }
    private fun getSetting(k: String, d: Boolean): Boolean = prefs.getBoolean(k, d)
    private fun saveSettingString(k: String, v: String?) { prefs.edit().putString(k, v).apply() }
    private fun getSettingString(k: String, d: String?): String? = prefs.getString(k, d)
    private fun saveSettingInt(k: String, v: Int) { prefs.edit().putInt(k, v).apply() }
    private fun getSettingInt(k: String, d: Int): Int = prefs.getInt(k, d)
    private fun saveSettingFloat(k: String, v: Float) { prefs.edit().putFloat(k, v).apply() }
    private fun getSettingFloat(k: String, d: Float): Float = prefs.getFloat(k, d)
}
