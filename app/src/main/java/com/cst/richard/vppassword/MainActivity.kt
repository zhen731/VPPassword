package com.cst.richard.vppassword

import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.platform.LocalContext
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.material.icons.filled.Lock

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val db = AppDatabase.getDatabase(this)
        val dao = db.passwordDao()
        setContent {
            MaterialTheme {
                var isUnlocked by remember { mutableStateOf(false) }
                if (isUnlocked) {
                    val passwordList by dao.getAllPasswords().collectAsState(initial = emptyList())
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AddPasswordScreen(dao, passwordList)
                    }
                } else {
                    AuthScreen(onUnlockSuccess = { isUnlocked = true })
                }
            }
        }
    }
    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess() // 验证成功，回调
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("身份验证")
            .setSubtitle("请使用指纹或面部识别解锁保险箱")
            //.setNegativeButtonText("取消")
            // 允许使用设备密码（图案/PIN）作为备份
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    @Composable
    fun AuthScreen(onUnlockSuccess: () -> Unit) {
        // 进入这个界面时自动弹窗
        LaunchedEffect(Unit) {
            showBiometricPrompt(onUnlockSuccess)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("保险箱已锁定", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showBiometricPrompt(onUnlockSuccess) }) {
                Text("点击解锁")
            }
        }
    }
}

@Composable
fun AddPasswordScreen(dao: PasswordDao, passwordList: List<PasswordEntry>) {
    var projectName by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isInputPasswordVisible by remember { mutableStateOf(true) }
    // 外部用一个大 Column 包裹所有内容
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "VPPassword 保险箱", style = MaterialTheme.typography.headlineMedium)

        // --- 上半部分：输入区域 ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("项目") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = account, onValueChange = { account = it }, label = { Text("账号 (选填)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码 (必填)") },
                    modifier = Modifier.fillMaxWidth(),
                    // 动态切换明文/密文
                    visualTransformation = if (isInputPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    // 在输入框尾部加上眼睛图标
                    trailingIcon = {
                        IconButton(onClick = { isInputPasswordVisible = !isInputPasswordVisible }) {
                            Icon(
                                imageVector = if (isInputPasswordVisible)
                                    Icons.Default.VisibilityOff
                                else
                                    Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true
                )

                Button(
                    onClick = {
                        scope.launch {
                            // 存入 Room 数据库
                            dao.insert(PasswordEntry(projectName = projectName, account = account, password = password))
                            // 清空输入框，方便下一次输入
                            projectName = ""; account = ""; password = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = projectName.isNotBlank() && password.isNotBlank()
                ) {
                    Text("存入保险箱")
                }
            }
        }

        HorizontalDivider()

        Text(text = "已保存的密码 (${passwordList.size})", style = MaterialTheme.typography.titleLarge)

        // --- 下半部分：滚动列表区域 ---
        // LazyColumn 相当于安卓里的增强版 ListView，性能极高
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f), // weight(1f) 让列表占满剩余空间
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = passwordList,
                key = { it.id }) { item ->
                // 这里调用我们自定义的每一行显示样式的卡片
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            // 当滑到最左边时，触发数据库删除操作
                            scope.launch {
                                dao.delete(item)
                            }
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false, // 禁用从左往右滑
                    backgroundContent = {
                        // 滑开后背景显示的“底色”和图标（通常是红色垃圾桶）
                        val color = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, shape = MaterialTheme.shapes.medium)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = Color.White
                            )
                        }
                    }
                ) {
                    // 这里放原来的卡片内容
                    PasswordItemCard(entry = item, dao = dao, scope = scope)
                }
            }
        }
    }
}

@Composable
fun PasswordItemCard(
    entry: PasswordEntry,
    dao: PasswordDao,
    scope: kotlinx.coroutines.CoroutineScope) {
    var isVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- 整个卡片的外壳 ---
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 第一行：项目名 + 编辑 + 删除
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(text = entry.projectName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "编辑")
                    }
                    // 点击这里应该能弹出对话框
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // 第二行：账号 + 复制
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(text = "账号: ${entry.account.ifBlank { "未填写" }}", modifier = Modifier.weight(1f))
                if (entry.account.isNotBlank()) {
                    IconButton(onClick = { copyToClipboard(context, entry.account, "账号已复制") }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // 第三行：密码 + 复制 + 眼睛
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                val displayPass = if (isVisible) entry.password else "••••••"
                Text(text = "密码: $displayPass", modifier = Modifier.weight(1f))
                IconButton(onClick = { copyToClipboard(context, entry.password, "密码已复制") }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { isVisible = !isVisible }) {
                    Icon(imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            }
        }
    }

    // --- 【重点】弹窗代码必须放在 Card 的外面，确保能正常弹出 ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除 ${entry.projectName} 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.delete(entry) }
                    showDeleteDialog = false
                }) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    if (showEditDialog) {
        EditPasswordDialog(
            entry = entry,
            onDismiss = { showEditDialog = false },
            onConfirm = { updated ->
                scope.launch {
                    dao.update(updated)
                    showEditDialog = false
                }
            }
        )
    }
}
@Composable
fun EditPasswordDialog(
    entry: PasswordEntry,
    onDismiss: () -> Unit,
    onConfirm: (PasswordEntry) -> Unit) {
    // 这里的变量用来存你在弹窗里修改的新内容
    var editedProject by remember { mutableStateOf(entry.projectName) }
    var editedAccount by remember { mutableStateOf(entry.account) }
    var editedPassword by remember { mutableStateOf(entry.password) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑密码信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = editedProject, onValueChange = { editedProject = it }, label = { Text("项目") })
                OutlinedTextField(value = editedAccount, onValueChange = { editedAccount = it }, label = { Text("账号") })
                OutlinedTextField(value = editedPassword, onValueChange = { editedPassword = it }, label = { Text("密码") })
            }
        },
        confirmButton = {
            Button(onClick = {
                // 点保存时，把改好的新对象传回去
                onConfirm(entry.copy(projectName = editedProject, account = editedAccount, password = editedPassword))
            }) { Text("保存修改") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
// 这是一个普通的 Kotlin 函数，负责把文字塞进剪贴板并弹个泡泡提示
fun copyToClipboard(context: Context, text: String, toastMsg: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("VPPassword", text)
    clipboard.setPrimaryClip(clip)

    // 弹出一个短暂的提示告诉用户成功了
    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
}