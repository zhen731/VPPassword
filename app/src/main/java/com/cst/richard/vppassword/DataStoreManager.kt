package com.cst.richard.vppassword

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 定义 DataStore 扩展属性
val Context.dataStore by preferencesDataStore(name = "vppassword_prefs")

class DataStoreManager(private val context: Context) {

    // 定义“钥匙”
    companion object {
        val PROJECT_KEY = stringPreferencesKey("project_name")
        val ACCOUNT_KEY = stringPreferencesKey("account")
        val PASSWORD_KEY = stringPreferencesKey("password")
    }

    // 1. 保存数据的方法
    suspend fun savePassword(project: String, acc: String, pass: String) {
        context.dataStore.edit { preferences ->
            preferences[PROJECT_KEY] = project
            preferences[ACCOUNT_KEY] = acc
            preferences[PASSWORD_KEY] = pass
        }
    }

    // 2. 读取数据的方法 (返回一个数据流)
    val getSavedData: Flow<Triple<String, String, String>> = context.dataStore.data
        .map { preferences ->
            Triple(
                preferences[PROJECT_KEY] ?: "暂无数据",
                preferences[ACCOUNT_KEY] ?: "",
                preferences[PASSWORD_KEY] ?: ""
            )
        }
}