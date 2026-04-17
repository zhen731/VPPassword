package com.cst.richard.vppassword

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Database(entities = [PasswordEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao

    companion object {
        private const val TAG = "VP_EXPORT"
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                SQLiteDatabase.loadLibs(context)
                
                val passphrase = SecurityUtils.getOrCreatePassphrase(context)
                val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vppassword.db"
                )
                .openHelperFactory(factory)
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}