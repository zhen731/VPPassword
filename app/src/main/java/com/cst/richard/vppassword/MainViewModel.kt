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
import com.cst.richard.vppassword.ui.theme.ThemeVariant
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val dao = AppDatabase.getDatabase(context).passwordDao()
    private val prefs = context.getSharedPreferences("vpp", Context.MODE_PRIVATE)

    // --- UI State ---
    
    private var _isFirstLaunch = getSetting("is_first_launch", true)
    private val _isFirstLaunchFlow = MutableStateFlow(_isFirstLaunch)
    val isFirstLaunch = _isFirstLaunchFlow.asStateFlow()

    var isUnlocked by mutableStateOf(_isFirstLaunch || !getSetting("biometric_enabled", true))
        private set

    // --- Selection State ---
    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _themeVariant = MutableStateFlow(
        ThemeVariant.values()[getSettingInt("theme_variant", ThemeVariant.MIDNIGHT_DARK.ordinal).coerceIn(0, ThemeVariant.values().size - 1)]
    )
    val themeVariant = _themeVariant.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(getSetting("biometric_enabled", true))
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _isEnglish = MutableStateFlow(getSetting("is_english", false))
    val isEnglish = _isEnglish.asStateFlow()

    private val _customBgPath = MutableStateFlow(getSettingString("custom_bg_path", null))
    val customBgPath = _customBgPath.asStateFlow()

    private val _bgScaleType = MutableStateFlow(getSettingInt("bg_scale_type", 0))
    val bgScaleType = _bgScaleType.asStateFlow()

    private val _bgBitmap = MutableStateFlow<ImageBitmap?>(null)
    val bgBitmap = _bgBitmap.asStateFlow()

    private val _bgBlur = MutableStateFlow(getSettingFloat("bg_blur", 0f))
    val bgBlur = _bgBlur.asStateFlow()

    private val _bgDim = MutableStateFlow(getSettingFloat("bg_dim", 0f))
    val bgDim = _bgDim.asStateFlow()

    private val _sortOrder = MutableStateFlow(
        SortOrder.values()[getSettingInt("sort_order", SortOrder.NEWEST.ordinal).coerceIn(0, SortOrder.values().size - 1)]
    )
    val sortOrder = _sortOrder.asStateFlow()

    var pendingImportJson by mutableStateOf<String?>(null)
    var dataToExport by mutableStateOf("")

    // --- Data Stream ---

    val passwordList: StateFlow<List<PasswordEntry>> = combine(
        dao.getAllPasswords(),
        _sortOrder
    ) { list, order ->
        when (order) {
            SortOrder.NAME_ASC -> list.sortedBy { it.projectName.lowercase() }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.projectName.lowercase() }
            SortOrder.NEWEST -> list.sortedByDescending { it.id }
            SortOrder.OLDEST -> list.sortedBy { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize language
        AppLanguage.isEnglish = _isEnglish.value
        loadBackgroundImage()
    }

    // --- Actions ---

    fun unlock() {
        isUnlocked = true
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        saveSettingInt("sort_order", order.ordinal)
    }

    fun setTheme(variant: ThemeVariant) {
        _themeVariant.value = variant
        saveSettingInt("theme_variant", variant.ordinal)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        saveSetting("biometric_enabled", enabled)
    }

    fun setLanguage(enabled: Boolean) {
        _isEnglish.value = enabled
        AppLanguage.isEnglish = enabled
        saveSetting("is_english", enabled)
    }

    fun setBgScaleType(type: Int) {
        _bgScaleType.value = type
        saveSettingInt("bg_scale_type", type)
    }

    fun setBgBlur(value: Float) {
        _bgBlur.value = value
        saveSettingFloat("bg_blur", value)
    }

    fun setBgDim(value: Float) {
        _bgDim.value = value
        saveSettingFloat("bg_dim", value)
    }

    fun completeOnboarding() {
        _isFirstLaunch = false
        _isFirstLaunchFlow.value = false
        saveSetting("is_first_launch", false)
    }

    fun updatePendingImport(json: String?) {
        pendingImportJson = json
    }

    // --- Database Operations ---

    fun addPassword(projectName: String, account: String, pass: String) {
        viewModelScope.launch {
            dao.insert(PasswordEntry(projectName = projectName, account = account, password = pass))
        }
    }

    fun updatePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            dao.update(entry)
        }
    }

    fun deletePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            dao.delete(entry)
        }
    }

    fun importJson(json: String, onComplete: (Int) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val list: List<PasswordEntry>? = Gson().fromJson(json, object : TypeToken<List<PasswordEntry>>() {}.type)
                list?.forEach { 
                    dao.insert(it.copy(id = 0)) 
                }
                onComplete(list?.size ?: 0)
            } catch (e: Exception) {
                onError()
            }
        }
    }

    // --- Background Image Handling ---

    fun pickBackground(uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uri != null) {
                saveCustomBackgroundImageLocal(uri)
                val newPath = File(context.filesDir, "custom_bg.jpg").absolutePath
                _customBgPath.value = newPath
                saveSettingString("custom_bg_path", newPath)
                loadBackgroundImage()
            } else {
                _customBgPath.value = null
                saveSettingString("custom_bg_path", null)
                File(context.filesDir, "custom_bg.jpg").delete()
                _bgBitmap.value = null
            }
        }
    }

    private fun loadBackgroundImage() {
        viewModelScope.launch(Dispatchers.IO) {
            val path = _customBgPath.value
            if (path != null && File(path).exists()) {
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
                    _bgBitmap.value = bitmap?.asImageBitmap()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveCustomBackgroundImageLocal(uri: Uri) {
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

    // --- Batch Operations ---
    
    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) _selectedIds.value = emptySet()
    }

    fun toggleItemSelection(id: Int) {
        val current = _selectedIds.value
        if (current.contains(id)) {
            _selectedIds.value = current - id
        } else {
            _selectedIds.value = current + id
        }
    }

    fun selectAll(allIds: List<Int>) {
        _selectedIds.value = allIds.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected(onComplete: () -> Unit) {
        viewModelScope.launch {
            dao.deleteBatch(_selectedIds.value.toList())
            toggleSelectionMode(false)
            onComplete()
        }
    }

    // --- Settings Helpers ---

    private fun saveSetting(k: String, v: Boolean) { prefs.edit().putBoolean(k, v).apply() }
    private fun getSetting(k: String, d: Boolean): Boolean = prefs.getBoolean(k, d)
    private fun saveSettingString(k: String, v: String?) { prefs.edit().putString(k, v).apply() }
    private fun getSettingString(k: String, d: String?): String? = prefs.getString(k, d)
    private fun saveSettingInt(k: String, v: Int) { prefs.edit().putInt(k, v).apply() }
    private fun getSettingInt(k: String, d: Int): Int = prefs.getInt(k, d)
    private fun saveSettingFloat(k: String, v: Float) { prefs.edit().putFloat(k, v).apply() }
    private fun getSettingFloat(k: String, d: Float): Float = prefs.getFloat(k, d)
}
