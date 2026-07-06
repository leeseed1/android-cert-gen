import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun main() = application {
    val windowState = rememberWindowState(width = 600.dp, height = 500.dp)

    Window(onCloseRequest = ::exitApplication, title = "自签名证书生成器", state = windowState) {
        MaterialTheme {
            CertGeneratorScreen()
        }
    }
}

@Composable
fun CertGeneratorScreen() {
    var ipInput by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedCertText by remember { mutableStateOf("") }
    var generatedKeyText by remember { mutableStateOf("") }
    var saveResultStatus by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "自签名证书生成器", style = MaterialTheme.typography.h5)
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
                    
                    // 1. 后台线程生成证书
                    val result = withContext(Dispatchers.Default) {
                        CertificateGenerator.generateSelfSignedCert(cleanIp)
                    }
                    val certText = result["cert"] ?: ""
                    val keyText = result["key"] ?: ""
                    generatedCertText = certText
                    generatedKeyText = keyText

                    // 2. PC 端保存文件到系统的电脑“下载”文件夹下
                    val saveStatus = withContext(Dispatchers.IO) {
                        try {
                            val userHome = System.getProperty("user.home")
                            val downloadDir = File(userHome, "Downloads")
                            val folderName = cleanIp.replace(".", "_")
                            val targetFolder = File(downloadDir, folderName)
                            if (!targetFolder.exists()) targetFolder.mkdirs()

                            File(targetFolder, "server.crt").writeText(certText)
                            File(targetFolder, "server.key").writeText(keyText)
                            "文件已成功保存至电脑：\n${targetFolder.absolutePath}"
                        } catch (e: Exception) {
                            "保存失败: ${e.localizedMessage}"
                        }
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
                // 修改了这里：使用 Modifier.size() 替代 size 参数
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colors.onPrimary)
            } else {
                Text("开始生成并保存证书")
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(modifier = Modifier.fillMaxSize().padding(16.dp), elevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text(text = "【保存状态】", style = MaterialTheme.typography.subtitle1)
                    Text(text = saveResultStatus, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "【证书文件内容 (server.crt)】", style = MaterialTheme.typography.subtitle2)
                    SelectionContainer { Text(text = generatedCertText, style = MaterialTheme.typography.caption) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "【私钥文件内容 (server.key)】", style = MaterialTheme.typography.subtitle2)
                    SelectionContainer { Text(text = generatedKeyText, style = MaterialTheme.typography.caption) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}
