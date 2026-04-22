package com.cst.richard.vppassword

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.cst.richard.vppassword.ui.AppLanguage
import java.io.OutputStream
import java.lang.StringBuilder

object ExportUtils {

    fun generateCSV(entries: List<PasswordEntry>): String {
        val sb = StringBuilder()
        sb.append("Category,Project Name,Account,Password\n")
        entries.forEach { entry ->
            sb.append("${entry.category},\"${entry.projectName}\",\"${entry.account}\",\"${entry.password}\"\n")
        }
        return sb.toString()
    }

    fun generateMarkdown(entries: List<PasswordEntry>): String {
        val sb = StringBuilder()
        sb.append("# VPPassword Private Ledger\n")
        sb.append("Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
        
        val categories = entries.groupBy { it.category }
        categories.forEach { (cat, list) ->
            val catName = when(cat) {
                0 -> AppLanguage.t("Passwords", "普通密码", "普通密碼", "パスワード", "Mots de passe", "Passwörter", "비밀번호", "Contraseñas")
                1 -> AppLanguage.t("Backup Codes", "备用验证码", "备用验证码", "バックアップコード", "Codes de secours", "Backup-Codes", "백업 코드", "Códigos de respaldo")
                2 -> AppLanguage.t("Crypto", "加密货币", "加密貨幣", "暗号通貨", "Crypto", "Krypto", "암호화폐", "Cripto")
                else -> "Other"
            }
            sb.append("## $catName\n\n")
            sb.append("| Project | Account / ID | Credential / Key |\n")
            sb.append("| :--- | :--- | :--- |\n")
            list.forEach { entry ->
                sb.append("| ${entry.projectName} | ${entry.account} | `${entry.password}` |\n")
            }
            sb.append("\n---\n\n")
        }
        return sb.toString()
    }

    fun generatePDF(entries: List<PasswordEntry>, includePasswords: Boolean, outputStream: OutputStream) {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.BLACK }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true; color = Color.WHITE }
        val textPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        val metaPaint = Paint().apply { textSize = 8f; color = Color.GRAY; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC) }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        val zebraPaint = Paint().apply { color = Color.parseColor("#F9F9F9") }
        val headerBgPaint = Paint().apply { color = Color.parseColor("#333333") }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = 60f
        val title = if (includePasswords) "Private Password Ledger" else "Account Directory"
        canvas.drawText(title, 50f, y, titlePaint)
        
        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText("Generated via VPPassword on $dateStr", 50f, y + 20f, metaPaint)
        y += 60f

        // Table Headers
        canvas.drawRect(50f, y - 20f, 545f, y + 10f, headerBgPaint)
        canvas.drawText("PROJECT", 60f, y, headerPaint)
        canvas.drawText("ACCOUNT / USERNAME", 180f, y, headerPaint)
        canvas.drawText("PASSWORD / KEY", 360f, y, headerPaint)
        y += 25f

        entries.forEachIndexed { index, entry ->
            if (y > 780) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 60f
                // Re-draw headers on new page
                canvas.drawRect(50f, y - 20f, 545f, y + 10f, headerBgPaint)
                canvas.drawText("PROJECT", 60f, y, headerPaint)
                canvas.drawText("ACCOUNT / USERNAME", 180f, y, headerPaint)
                canvas.drawText("PASSWORD / KEY", 360f, y, headerPaint)
                y += 25f
            }

            if (index % 2 != 0) {
                canvas.drawRect(50f, y - 18f, 545f, y + 8f, zebraPaint)
            }

            canvas.drawText(entry.projectName, 60f, y, textPaint)
            canvas.drawText(entry.account, 180f, y, textPaint)
            if (includePasswords) {
                canvas.drawText(entry.password, 360f, y, textPaint)
            } else {
                canvas.drawText("********", 360f, y, textPaint)
            }
            
            canvas.drawLine(50f, y + 8f, 545f, y + 8f, linePaint)
            y += 22f
        }

        // Footer
        canvas.drawText("Page $pageNumber | Confidential Physical Backup", 250f, 820f, metaPaint)

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}
