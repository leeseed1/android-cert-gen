package com.example.certgenerator

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object FileSaver {
    fun saveCertToDownloads(context: Context, ipAddress: String, certText: String, keyText: String): String {
        val folderName = ipAddress.replace(".", "_")
        val subFolderPath = "${Environment.DIRECTORY_DOWNLOADS}/$folderName"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveFileWithMediaStore(context, subFolderPath, "server.crt", certText)
                saveFileWithMediaStore(context, subFolderPath, "server.key", keyText)
                "文件已成功保存至：\n内部存储/Download/$folderName/"
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadDir, folderName)
                if (!appDir.exists()) appDir.mkdirs()
                File(appDir, "server.crt").writeText(certText)
                File(appDir, "server.key").writeText(keyText)
                "文件已成功保存至：\n${appDir.absolutePath}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "保存失败: ${e.localizedMessage}"
        }
    }

    private fun saveFileWithMediaStore(context: Context, relativePath: String, fileName: String, content: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        fileUri?.let { uri ->
            resolver.openOutputStream(uri).use { it?.write(content.toByteArray()) }
        } ?: throw Exception("无法创建文件 $fileName")
    }
}
