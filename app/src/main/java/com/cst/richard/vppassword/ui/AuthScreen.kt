package com.cst.richard.vppassword.ui

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity


@Composable
fun AuthScreen(onUnlockSuccess: () -> Unit) {
    val context = LocalContext.current as FragmentActivity
    LaunchedEffect(Unit) { showBiometricPrompt(context, onUnlockSuccess) }
    
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { showBiometricPrompt(context, onUnlockSuccess) }) { 
            Text(AppLanguage.t("Unlock Safe", "解锁保险箱", "解鎖保險箱", "保管庫を解除", "Déverrouiller le coffre", "Tresor entsperren", "금고 잠금 해제", "Desbloquear caja fuerte")) 
        }
    }
}

fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
            }
        }
    })

    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(AppLanguage.t("Authentication", "身份验证", "身份驗證", "認証", "Authentification", "Authentifizierung", "인증", "Autenticación"))
        .setSubtitle(AppLanguage.t("Please authenticate to unlock the safe", "请通过验证解锁保险箱", "請通過驗證解鎖保險箱", "保管庫を解除するには認証してください", "Veuillez vous authentifier pour déverrouiller le coffre", "Bitte authentifizieren Sie sich, um den Tresor zu entsperren", "금고를 잠금 해제하려면 인증하십시오", "Por favor, autentíquese para desbloquear la caja fuerte"))
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(info)
}
