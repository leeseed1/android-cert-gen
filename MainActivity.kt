package com.example.certgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CertGeneratorScreen()
                }
            }
        }
    }
}

@Composable
fun CertGeneratorScreen() {
    var ipInput by remember { mutableStateOf("") } // 默认空白，等用户输入
    var showDialog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedCertText by remember { mutableStateOf("") }
    var generatedKeyText by remember { mutableStateOf("") }
    var saveResultStatus by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "自签名证书生成器", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            label = { Text("请输入服务器 IP") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                isGenerating = true
                coroutineScope.launch {
                    val cleanIp = ipInput.trim()
                    val result = withContext(Dispatchers.Default) {
                        CertificateGenerator.generateSelfSignedCert(cleanIp)
                    }
                    val certText = result["cert"] ?: ""
                    val keyText = result["key"] ?: ""
                    generatedCertText = certText
                    generatedKeyText = keyText

                    val saveStatus = withContext(Dispatchers.IO) {
                        FileSaver.saveCertToDownloads(context, cleanIp, certText, keyText)
                    }
                    saveResultStatus = saveStatus
                    isGenerating = false
                    showDialog = true
                }
            },
            enabled = ipInput.isNotBlank() && !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(size = 20.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("开始生成并保存证书")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("生成并保存成功") },
            text = {
                Column(modifier = Modifier.maxHeight(400.dp).verticalScroll(rememberScrollState())) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(text = saveResultStatus, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                    Text(text = "【证书文件内容 (server.crt)】", style = MaterialTheme.typography.titleSmall)
                    SelectionContainer { Text(text = generatedCertText, style = MaterialTheme.typography.bodySmall) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "【私钥文件内容 (server.key)】", style = MaterialTheme.typography.titleSmall)
                    SelectionContainer { Text(text = generatedKeyText, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("完成") } }
        )
    }
}
