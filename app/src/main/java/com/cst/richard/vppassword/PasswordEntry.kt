package com.cst.richard.vppassword

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // 自动递增的 ID
    val projectName: String,
    val account: String,
    val password: String
)