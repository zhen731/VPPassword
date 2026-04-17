package com.cst.richard.vppassword.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.cst.richard.vppassword.AppLanguage
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
    onToggleSelection: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val cardColor = if (hasCustomBg) MaterialTheme.colorScheme.surface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                     else if (hasCustomBg) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) 
                     else null

    val itemIconMod = Modifier

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() else isVisible = !isVisible },
                onLongClick = { onToggleSelection() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else cardColor
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasCustomBg) 0.dp else 4.dp)
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
                Text(
                    entry.projectName, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleMedium.copy(shadow = if(hasCustomBg) Shadow(Color.Black.copy(alpha=0.5f), blurRadius = 8f) else null), 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if(isVisible) entry.password else "•••••••••", 
                    fontFamily = FontFamily.Monospace, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val iconTint = MaterialTheme.colorScheme.onSurface
                IconButton(onClick = { isVisible = !isVisible }, modifier = Modifier.size(32.dp).then(itemIconMod)) { Icon(if(isVisible) Icons.Default.Visibility else Icons.Default.Lock, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(32.dp).then(itemIconMod)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                IconButton(onClick = { copyToClipboard(context, entry.password, AppLanguage.t("Copied", "已复制")) }, modifier = Modifier.size(32.dp).then(itemIconMod)) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp), tint = iconTint) }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp).then(itemIconMod)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = if(hasCustomBg) Color(0xFFFF5252) else Color.Red.copy(alpha = 0.8f)) }
            }
        }
    }
    if (showDeleteDialog) AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text(AppLanguage.t("Confirm delete?", "确认删除？")) }, confirmButton = { TextButton(onClick = { viewModel.deletePassword(entry); showDeleteDialog = false }) { Text(AppLanguage.t("Confirm", "确认"), color = Color.Red) } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(AppLanguage.t("Cancel", "取消")) } })
    if (showEditDialog) EditPasswordDialog(entry, onDismiss = { showEditDialog = false }, onConfirm = { viewModel.updatePassword(it); showEditDialog = false })
}

@Composable
fun EditPasswordDialog(entry: PasswordEntry, onDismiss: () -> Unit, onConfirm: (PasswordEntry) -> Unit) {
    var p by remember { mutableStateOf(entry.projectName) }
    var a by remember { mutableStateOf(entry.account) }
    var pw by remember { mutableStateOf(entry.password) }
    val isValid = p.isNotBlank() && pw.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppLanguage.t("Edit", "编辑")) },
        text = { Column { OutlinedTextField(p, { p = it }, label = { Text(AppLanguage.t("Project *", "项目 *")) }); OutlinedTextField(a, { a = it }, label = { Text(AppLanguage.t("Account (Optional)", "账号 (选填)")) }); OutlinedTextField(pw, { pw = it }, label = { Text(AppLanguage.t("Password *", "密码 *")) }) } },
        confirmButton = { Button(onClick = { onConfirm(entry.copy(projectName = p, account = a, password = pw)) }, enabled = isValid) { Text(AppLanguage.t("Save", "保存")) } }
    )
}

@Composable
fun AddPasswordDialog(onAdd: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var p by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    val isValid = p.isNotBlank() && pw.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppLanguage.t("New Record", "新建记录")) },
        text = { Column { OutlinedTextField(p, { p = it }, label = { Text(AppLanguage.t("Project *", "项目 *")) }); OutlinedTextField(a, { a = it }, label = { Text(AppLanguage.t("Account (Optional)", "账号 (选填)")) }); OutlinedTextField(pw, { pw = it }, label = { Text(AppLanguage.t("Password *", "密码 *")) }) } },
        confirmButton = {
            Button(onClick = {
                onAdd(p, a, pw)
                onDismiss() 
            }, enabled = isValid) { Text(AppLanguage.t("Add", "添加")) }
        }
    )
}

@Composable
fun PromptPasswordDialog(title: String, tip: String? = null, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    val isValid = pwd.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { 
            Column {
                if (tip != null) {
                    Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))
                }
                OutlinedTextField(pwd, { pwd = it }, label = { Text(AppLanguage.t("Password", "密码")) }) 
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
