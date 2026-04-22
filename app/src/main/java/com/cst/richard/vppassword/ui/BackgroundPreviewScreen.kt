package com.cst.richard.vppassword.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.cst.richard.vppassword.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundPreviewScreen(
    viewModel: MainViewModel,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val pendingBitmap by viewModel.pendingBgBitmap.collectAsState()
    val bgScaleType by viewModel.bgScaleType.collectAsState()
    val bgBlur by viewModel.bgBlur.collectAsState()
    val bgDim by viewModel.bgDim.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(AppLanguage.t("Preview & Adjust", "预览与调整背景")) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = onConfirm) { Icon(Icons.Default.Check, null) }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Background Preview
            pendingBitmap?.let {
                val scale = when(bgScaleType) {
                    1 -> ContentScale.Fit
                    2 -> ContentScale.FillBounds
                    else -> ContentScale.Crop
                }
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = scale,
                    modifier = Modifier.fillMaxSize().blur(bgBlur.dp)
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgDim)))
            }

            // Controls Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(AppLanguage.t("Scale Mode", "缩放模式"), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AppLanguage.t("Crop", "裁剪") to 0,
                            AppLanguage.t("Fit", "适应") to 1,
                            AppLanguage.t("Stretch", "填满") to 2
                        ).forEach { (label, type) ->
                            FilterChip(
                                selected = bgScaleType == type,
                                onClick = { viewModel.setBgScaleType(type) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(AppLanguage.t("Blur", "模糊"), modifier = Modifier.width(60.dp))
                        Slider(value = bgBlur, onValueChange = { viewModel.setBgBlur(it) }, valueRange = 0f..25f)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(AppLanguage.t("Dim", "暗度"), modifier = Modifier.width(60.dp))
                        Slider(value = bgDim, onValueChange = { viewModel.setBgDim(it) }, valueRange = 0f..0.9f)
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) { Text(AppLanguage.t("Cancel", "取消"), color = MaterialTheme.colorScheme.error) }
                        
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f)
                        ) { Text(AppLanguage.t("Set Background", "应用背景")) }
                    }
                }
            }
        }
    }
}
