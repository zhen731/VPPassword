package com.cst.richard.vppassword

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    // 将用户输入的“导出密码”转化成 256 位的 Key
    private fun generateKey(password: String): SecretKeySpec {
        // 使用 SHA-256 确保无论输入多少位（甚至 1 位或汉字），都能生成固定的 32 字节（256位）密钥
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(data: String, password: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        // 固定 IV（16字节）
        val iv = IvParameterSpec(ByteArray(16))
        cipher.init(Cipher.ENCRYPT_MODE, generateKey(password), iv)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String, password: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = IvParameterSpec(ByteArray(16))
        cipher.init(Cipher.DECRYPT_MODE, generateKey(password), iv)
        val decoded = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
        return String(cipher.doFinal(decoded), Charsets.UTF_8)
    }
}