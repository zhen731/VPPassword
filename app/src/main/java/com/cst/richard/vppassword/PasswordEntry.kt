package com.cst.richard.vppassword

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectName: String,
    val account: String,
    val password: String,
    val category: Int = 0, // 0: Password, 1: Backup Code, 2: Crypto
    val notes: String = "",
    val usedCodes: String = "" // Comma-separated list of used codes
)