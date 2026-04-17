package com.cst.richard.vppassword

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY id DESC")
    fun getAllPasswords(): kotlinx.coroutines.flow.Flow<List<PasswordEntry>>

    @Insert
    suspend fun insert(entry: PasswordEntry)

    @Delete
    suspend fun delete(entry: PasswordEntry)

    @Update
    suspend fun update(entry: PasswordEntry)

    @Query("DELETE FROM passwords WHERE id IN (:ids)")
    suspend fun deleteBatch(ids: List<Int>)
}