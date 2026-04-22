package com.cst.richard.vppassword.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cst.richard.vppassword.MainViewModel
import com.cst.richard.vppassword.PasswordEntry

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordItemCard(
    entry: PasswordEntry, 
    viewModel: MainViewModel, 
    hasCustomBg: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onEditRequest: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBackupCodeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val cardColor = if (hasCustomBg) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize()
            .combinedClickable(
                onClick = { 
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        if (entry.category == 1) showBackupCodeDialog = true
                        else isVisible = !isVisible 
                    }
                },
                onLongClick = { onToggleSelection() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else cardColor
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasCustomBg) 4.dp else 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.projectName, 
                        fontWeight = FontWeight.SemiBold, 
                        style = MaterialTheme.typography.titleMedium, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (entry.category == 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.FactCheck, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (entry.account.isNotBlank()) {
                    Text(
                        entry.account,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (entry.notes.isNotBlank()) {
                    Text(
                        entry.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (entry.category == 1) {
                    val codes = entry.password.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
                    val used = entry.usedCodes.split(",").toSet()
                    val availableCount = codes.count { !used.contains(it) }
                    Text(
                        AppLanguage.t("$availableCount codes remaining", "剩余 $availableCount 个码", "剩餘 $availableCount 個碼", "残り $availableCount 個", "$availableCount codes restants", "$availableCount Codes übrig", "${availableCount}개 남음", "Quedan $availableCount códigos"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (availableCount > 0) MaterialTheme.colorScheme.primary else Color.Red
                    )
                } else {
                    Text(
                        if(isVisible) entry.password else "•••••••••", 
                        fontFamily = FontFamily.Monospace, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val iconTint = MaterialTheme.colorScheme.onSurface
                if (entry.category == 1) {
                    IconButton(onClick = { showBackupCodeDialog = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                } else {
                    IconButton(onClick = { isVisible = !isVisible }, modifier = Modifier.size(32.dp)) { Icon(if(isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                }
                IconButton(onClick = { onEditRequest() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                if (entry.category == 1) {
                    IconButton(onClick = {
                        val codes = entry.password.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
                        val used = entry.usedCodes.split(",").toSet()
                        val firstUnused = codes.firstOrNull { !used.contains(it) }
                        if (firstUnused != null) {
                            copyToClipboard(context, firstUnused, AppLanguage.t("Copied: $firstUnused", "已复制: $firstUnused"))
                            viewModel.toggleBackupCodeUsed(entry, firstUnused)
                        } else {
                            Toast.makeText(context, AppLanguage.t("All codes used", "所有验证码已用完"), Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                } else {
                    IconButton(onClick = { copyToClipboard(context, entry.password, AppLanguage.t("Copied", "已复制")) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = if(hasCustomBg) Color(0xFFFF5252) else Color.Red.copy(alpha = 0.8f)) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false }, 
            modifier = Modifier.widthIn(max = 400.dp),
            title = { Text(AppLanguage.t("Confirm delete?", "确认删除？")) }, 
            confirmButton = { TextButton(onClick = { viewModel.deletePassword(entry); showDeleteDialog = false }) { Text(AppLanguage.t("Confirm", "确认"), color = Color.Red) } }, 
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(AppLanguage.t("Cancel", "取消")) } }
        )
    }

    if (showBackupCodeDialog) {
        BackupCodeDialog(
            entry = entry,
            onToggleUsed = { code -> viewModel.toggleBackupCodeUsed(entry, code) },
            onDismiss = { showBackupCodeDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupCodeDialog(
    entry: PasswordEntry,
    onToggleUsed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val codes = remember(entry.password) { entry.password.split(Regex("[\\s,]+")).filter { it.isNotBlank() } }
    val used = entry.usedCodes.split(",").toSet()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 440.dp),
        title = { 
            Column {
                Text(entry.projectName, fontWeight = FontWeight.Bold)
                if (entry.notes.isNotBlank()) {
                    Text(entry.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(AppLanguage.t("Click to mark as used/unused", "点击标记已用/未用"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    codes.forEach { code ->
                        val isUsed = used.contains(code)
                        FilterChip(
                            selected = isUsed,
                            onClick = { onToggleUsed(code) },
                            label = { 
                                Text(
                                    code, 
                                    style = if (isUsed) MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) 
                                            else MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            ),
                            leadingIcon = {
                                if (isUsed) Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                else Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(AppLanguage.t("Close", "关闭")) }
        }
    )
}

@Composable
fun PromptPasswordDialog(
    title: String, 
    tip: String? = null, 
    extraContent: @Composable (() -> Unit)? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String) -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    val isValid = pwd.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 400.dp),
        title = { Text(title) },
        text = { 
            Column {
                if (tip != null) {
                    Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))
                }
                OutlinedTextField(pwd, { pwd = it }, label = { Text(AppLanguage.t("Password", "密码")) }, modifier = Modifier.fillMaxWidth()) 
                extraContent?.invoke()
            }
        },
        confirmButton = { Button(onClick = { onConfirm(pwd) }, enabled = isValid) { Text(AppLanguage.t("Confirm", "确认")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(AppLanguage.t("Cancel", "取消")) } }
    )
}

fun copyToClipboard(context: Context, text: String, msg: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("VP", text))
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}


