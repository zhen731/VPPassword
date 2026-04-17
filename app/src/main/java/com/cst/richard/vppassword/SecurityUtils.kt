package com.cst.richard.vppassword

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

object SecurityUtils {
    private const val KEY_ALIAS = "vppassword_db_wrapper_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "security_prefs"
    private const val ENCRYPTED_PASSPHRASE_KEY = "encrypted_passphrase"
    private const val IV_KEY = "passphrase_iv"

    /**
     * Get or create a stable database passphrase.
     * The actual passphrase is a random string encrypted by the Android Keystore.
     * This avoids calling .encoded on hardware-backed keys which returns null.
     */
    fun getOrCreatePassphrase(context: Context): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        
        // 1. Ensure master key exists in KeyStore
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
        val ivBase64 = prefs.getString(IV_KEY, null)

        return if (encryptedBase64 == null || ivBase64 == null) {
            // 2. No passphrase yet? Generate a random one.
            val random = SecureRandom()
            val passphraseBytes = ByteArray(32)
            random.nextBytes(passphraseBytes)
            // Use Base64 as a stable string representation
            val freshPassphrase = Base64.encodeToString(passphraseBytes, Base64.NO_WRAP)
            
            // 3. Encrypt it with the master key
            val masterKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val encryptedBytes = cipher.doFinal(freshPassphrase.toByteArray())
            
            // 4. Save encrypted data and IV
            prefs.edit().apply {
                putString(ENCRYPTED_PASSPHRASE_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                apply()
            }
            freshPassphrase
        } else {
            // 5. Already have one? Decrypt it.
            try {
                val masterKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
                val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(128, iv))
                val decryptedBytes = cipher.doFinal(encryptedBytes)
                String(decryptedBytes)
            } catch (e: Exception) {
                // If decryption fails (e.g. KeyStore wiped), we might need to reset.
                // For now, re-throwing to let the app catch it or crash with info.
                throw e
            }
        }
    }
}
