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

        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                )
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
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
                val (certPem, keyPem, sha256) = CertificateGenerator.generateSelfSignedCert(ip)

                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val certDir = File(downloadDir, ip)
                
                if (!certDir.exists()) {
                    certDir.mkdirs()
                }

                // 生成文件
                val certFile = File(certDir, "server.crt")
                certFile.writeText(certPem)

                val keyFile = File(certDir, "server.key")
                keyFile.writeText(keyPem)

                val shaFile = File(certDir, "certificate.sha256")
                shaFile.writeText("SHA-256: $sha256")

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
                    证书配置
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
