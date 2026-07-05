package com.example.certgenerator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var ipInput: TextInputEditText
    private lateinit var generateBtn: Button
    private lateinit var statusText: TextView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipInput = findViewById(R.id.ipInput)
        generateBtn = findViewById(R.id.generateBtn)
        statusText = findViewById(R.id.statusText)

        generateBtn.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            if (ip.isEmpty()) {
                statusText.text = "❌ 错误：请输入 IP 地址"
                return@setOnClickListener
            }
            generateCertificate(ip)
        }

        // 检查权限
        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                )
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            // Android 10 及以下
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val needRequest = permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needRequest) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun generateCertificate(ip: String) {
        statusText.text = "⏳ 正在生成证书，请稍候..."

        Thread {
            try {
                // 生成证书（包含证书、私钥、SHA256）
                val (certPem, keyPem, sha256) = CertificateGenerator.generateSelfSignedCert(ip)

                // ===== 文件保存路径 =====
                // 获取 Downloads 目录
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                // 以用户输入的 IP 为文件夹名
                val certDir = File(downloadDir, ip)
                
                // 创建目录（如果不存在）
                if (!certDir.exists()) {
                    certDir.mkdirs()
                }

                // ===== 生成的文件列表 =====
                // 1. server.crt - 证书文件（公钥）
                val certFile = File(certDir, "server.crt")
                certFile.writeText(certPem)

                // 2. server.key - 私钥文件
                val keyFile = File(certDir, "server.key")
                keyFile.writeText(keyPem)

                // 3. certificate.sha256 - SHA256 校验值
                val shaFile = File(certDir, "certificate.sha256")
                shaFile.writeText("SHA-256: $sha256")

                // 4. 生成信息说明文件
                val infoFile = File(certDir, "README.txt")
                val infoText = """
                    ====================================
                    自签名证书生成信息
                    ====================================
                    
                    生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                    
                    目标 IP: $ip
                    
                    证书有效期: 10年
                    
                    生成的文件:
                    1. server.crt  - 证书文件（公钥）
                    2. server.key  - 私钥文件
                    3. certificate.sha256 - SHA-256 校验值
                    
                    ====================================
                    证书配置说明（对应 OpenSSL 配置）
                    ====================================
                    
                    [req_distinguished_name]
                    C  = CN
                    O  = MySelfHost
                    CN = $ip
                    
                    [v3_req]
                    basicConstraints = critical, CA:TRUE
                    keyUsage = critical, digitalSignature, keyEncipherment, keyCertSign
                    extendedKeyUsage = serverAuth
                    subjectAltName = IP:$ip
                    
                    ====================================
                    SHA-256: $sha256
                    ====================================
                """.trimIndent()
                infoFile.writeText(infoText)

                // 更新 UI - 显示详细的保存信息
                runOnUiThread {
                    statusText.text = """
                        ✅ 证书生成成功！
                        
                        📁 保存位置: 
                        Downloads/$ip/
                        
                        📄 生成的文件:
                        • server.crt  (证书/公钥)
                        • server.key  (私钥)
                        • certificate.sha256  (SHA-256校验值)
                        • README.txt  (说明文件)
                        
                        🔐 SHA-256: $sha256
                        
                        ⏰ 有效期: 10年
                    """.trimIndent()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = """
                        ❌ 生成失败: ${e.message}
                        
                        详细信息:
                        ${e.stackTrace.take(5).joinToString("\n")}
                    """.trimIndent()
                }
                e.printStackTrace()
            }
        }.start()
    }
}
