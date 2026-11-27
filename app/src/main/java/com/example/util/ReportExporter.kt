package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ClearingSettlementEntity
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.ProjectMemberEntity
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.GoldPricesModel
import com.example.data.model.GoldPurchaseEntity
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    fun exportToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        filterPeriodLabel: String
    ) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "لا توجد صفقات للتصدير", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "Gold_Transactions_Report_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            // Add UTF-8 Byte Order Mark (BOM) for Excel Arabic compatibility
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val writer = outputStream.bufferedWriter(Charsets.UTF_8)

            // Title and Metadata header
            writer.write("تقرير صفقات وتداولات الذهب - منصة OOZOX (oozox.com)\n")
            writer.write("تاريخ التصدير,${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar", "EG")).format(Date())}\n")
            writer.write("فترة التقرير,${escapeCsv(filterPeriodLabel)}\n")
            writer.write("إجمالي عدد الصفقات,${transactions.size}\n")
            val totalVolume = transactions.sumOf { it.totalPrice }
            writer.write("إجمالي حجم الصفقات,${totalVolume} ج.م\n\n")

            // Table headers
            writer.write("رقم الصفقة,العميل / الجهة,نوع الذهب,العيار,الوزن (جرام),سعر الجرام (ج.م),مصناعية الجرام,الإجمالي (ج.م),الحالة,التاريخ,الملاحظات\n")

            // Data rows
            transactions.forEachIndexed { index, tx ->
                val statusLabel = when (tx.status) {
                    TransactionStatus.PURCHASED.name -> "مشترى"
                    TransactionStatus.RESOLD.name -> "مُعاد بيعه"
                    TransactionStatus.RETURNED.name -> "مُرجَع"
                    else -> tx.status
                }
                val dateStr = GoldUtils.formatDate(tx.timestamp)

                val line = listOf(
                    (index + 1).toString(),
                    escapeCsv(tx.contactName),
                    escapeCsv(tx.goldType),
                    escapeCsv(tx.goldKarat.replace("KARAT_", "عيار ")),
                    tx.weight.toString(),
                    tx.pricePerGram.toString(),
                    tx.manufacturingCostPerGram.toString(),
                    tx.totalPrice.toString(),
                    escapeCsv(statusLabel),
                    escapeCsv(dateStr),
                    escapeCsv(tx.notes ?: "")
                ).joinToString(",")

                writer.write(line + "\n")
            }

            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "تصدير تقرير صفقات الذهب (Excel)")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر إنشاء ملف Excel، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        filterPeriodLabel: String,
        totalVolume: Double
    ) {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "لا توجد صفقات للتصدير", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }

            var currentY = 40f

            // Helper function to draw header on page
            fun drawHeader() {
                // Top Header Banner
                paint.color = Color.parseColor("#1E222A") // Dark Surface
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 65f, paint)

                paint.color = Color.parseColor("#D4AF37") // Gold Accent
                canvas.drawRect(20f, currentY + 60f, (pageWidth - 20).toFloat(), currentY + 65f, paint)

                textPaint.color = Color.parseColor("#FFD700")
                textPaint.textSize = 18f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("OOZOX (oozox.com) - CONNECT • THINK • DO", 35f, currentY + 30f, textPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 12f
                textPaint.typeface = Typeface.DEFAULT
                canvas.drawText("تقرير صفقات وتداولات الذهب المعتمد", 35f, currentY + 50f, textPaint)

                val exportDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar", "EG")).format(Date())
                textPaint.textSize = 10f
                textPaint.color = Color.LTGRAY
                canvas.drawText("تاريخ التقرير: $exportDate", (pageWidth - 180).toFloat(), currentY + 35f, textPaint)

                currentY += 80f

                // Summary Metadata Card
                paint.color = Color.parseColor("#F5F5F5")
                canvas.drawRoundRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 50f, 8f, 8f, paint)

                textPaint.color = Color.BLACK
                textPaint.textSize = 11f
                textPaint.typeface = Typeface.DEFAULT_BOLD

                canvas.drawText("فترة الفلتر: $filterPeriodLabel", 35f, currentY + 20f, textPaint)
                canvas.drawText("عدد الصفقات: ${transactions.size}", 220f, currentY + 20f, textPaint)

                textPaint.color = Color.parseColor("#B8860B")
                canvas.drawText("الإجمالي: ${GoldUtils.formatCurrency(totalVolume)}", 360f, currentY + 20f, textPaint)

                currentY += 65f
            }

            fun drawTableHeaders() {
                // Table Header background
                paint.color = Color.parseColor("#D4AF37")
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 25f, paint)

                textPaint.color = Color.BLACK
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.DEFAULT_BOLD

                canvas.drawText("#", 30f, currentY + 17f, textPaint)
                canvas.drawText("الجهة / العميل", 55f, currentY + 17f, textPaint)
                canvas.drawText("النوع / العيار", 180f, currentY + 17f, textPaint)
                canvas.drawText("الوزن (جم)", 280f, currentY + 17f, textPaint)
                canvas.drawText("الإجمالي (ج.م)", 360f, currentY + 17f, textPaint)
                canvas.drawText("الحالة", 460f, currentY + 17f, textPaint)
                canvas.drawText("التاريخ", 520f, currentY + 17f, textPaint)

                currentY += 30f
            }

            drawHeader()
            drawTableHeaders()

            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 9.5f

            transactions.forEachIndexed { index, tx ->
                if (currentY > pageHeight - 50) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 40f
                    drawHeader()
                    drawTableHeaders()
                }

                // Row zebra shading
                if (index % 2 == 1) {
                    paint.color = Color.parseColor("#F9F9F9")
                    canvas.drawRect(20f, currentY - 14f, (pageWidth - 20).toFloat(), currentY + 8f, paint)
                }

                textPaint.color = Color.DKGRAY

                val statusLabel = when (tx.status) {
                    TransactionStatus.PURCHASED.name -> "مشترى"
                    TransactionStatus.RESOLD.name -> "مُعاد بيعه"
                    TransactionStatus.RETURNED.name -> "مُرجَع"
                    else -> tx.status
                }
                val dateStr = SimpleDateFormat("MM/dd HH:mm", Locale("ar", "EG")).format(Date(tx.timestamp))
                val karatStr = tx.goldKarat.replace("KARAT_", "عيار ")

                canvas.drawText("${index + 1}", 30f, currentY, textPaint)
                canvas.drawText(truncate(tx.contactName, 18), 55f, currentY, textPaint)
                canvas.drawText("${tx.goldType} $karatStr", 180f, currentY, textPaint)
                canvas.drawText("${tx.weight} جم", 280f, currentY, textPaint)

                textPaint.color = Color.parseColor("#2E7D32")
                canvas.drawText(GoldUtils.formatCurrency(tx.totalPrice), 360f, currentY, textPaint)

                textPaint.color = Color.DKGRAY
                canvas.drawText(statusLabel, 460f, currentY, textPaint)
                canvas.drawText(dateStr, 520f, currentY, textPaint)

                currentY += 22f
            }

            pdfDocument.finishPage(page)

            val fileName = "Gold_Transactions_Report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            shareFile(context, file, "application/pdf", "تصدير تقرير صفقات الذهب (PDF)")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر إنشاء ملف PDF، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportEmployeeCvToPdf(
        context: Context,
        user: com.example.data.model.UserEntity,
        reliabilityRecords: List<ReliabilityRecord>,
        language: AppLanguage = AppLanguage.ARABIC,
        templateId: String = "ATS_CLASSIC",
        customSectionOrder: List<String>? = null
    ) {
        val isEn = language == AppLanguage.ENGLISH
        val template = com.example.ui.components.CvTemplateSpec.fromId(templateId)
        val layoutStyle = template.layoutStyle
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply { isAntiAlias = true }
            val sectionAccent = Color.parseColor(template.accentHex)
            val secondaryAccent = Color.parseColor(template.secondaryHex)

            val sectionOrder = customSectionOrder?.takeIf { it.isNotEmpty() }
                ?: user.cvSectionOrder?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
                ?: listOf("BIO", "EXPERIENCE", "RELIABILITY", "SKILLS", "EDUCATION", "STORE")

            var currentY: Float

            if (layoutStyle == com.example.ui.components.CvVisualLayout.SIDEBAR_SPLIT) {
                // Two-column layout: colored sidebar (width 185) + clean white main column
                val sidebarW = 185f
                paint.color = Color.parseColor(template.headerBgHex)
                canvas.drawRect(0f, 0f, sidebarW, pageHeight.toFloat(), paint)

                // Avatar initial circle
                paint.color = sectionAccent
                canvas.drawCircle(sidebarW / 2f, 64f, 26f, paint)
                textPaint.color = Color.WHITE
                textPaint.textSize = 20f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(user.name.take(1).ifBlank { "O" }, sidebarW / 2f, 71f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT

                // Contact & Skills in Sidebar
                var sideY = 120f
                textPaint.color = secondaryAccent
                textPaint.textSize = 11f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(if (isEn) "CONTACT" else "التواصل", 18f, sideY, textPaint)
                sideY += 18f
                textPaint.color = Color.WHITE
                textPaint.textSize = 9.5f
                textPaint.typeface = Typeface.DEFAULT
                if (user.phone.isNotBlank()) {
                    canvas.drawText(user.phone, 18f, sideY, textPaint)
                    sideY += 15f
                }
                canvas.drawText("@${user.username}", 18f, sideY, textPaint)
                sideY += 15f
                canvas.drawText("oozox.com/u/${user.username}", 18f, sideY, textPaint)
                sideY += 28f

                if (!user.skills.isNullOrBlank()) {
                    textPaint.color = secondaryAccent
                    textPaint.textSize = 11f
                    textPaint.typeface = Typeface.DEFAULT_BOLD
                    canvas.drawText(if (isEn) "SKILLS" else "المهارات", 18f, sideY, textPaint)
                    sideY += 18f
                    textPaint.color = Color.WHITE
                    textPaint.textSize = 9.5f
                    textPaint.typeface = Typeface.DEFAULT
                    wrapText(user.skills!!, 24).take(12).forEach { line ->
                        canvas.drawText("• $line", 18f, sideY, textPaint)
                        sideY += 15f
                    }
                }

                // Main Column Header
                val mainX = sidebarW + 24f
                textPaint.color = Color.parseColor("#0F172A")
                textPaint.textSize = 22f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(user.name, mainX, 58f, textPaint)

                textPaint.color = sectionAccent
                textPaint.textSize = 13f
                textPaint.typeface = Typeface.DEFAULT
                canvas.drawText(user.jobTitle ?: if (isEn) "Certified Professional" else "عضو معتمد", mainX, 80f, textPaint)

                paint.color = sectionAccent
                canvas.drawRect(mainX, 94f, (pageWidth - 28).toFloat(), 96f, paint)
                currentY = 124f

                val bodyWrapWidth = 56
                sectionOrder.filter { it != "SKILLS" }.forEach { sectionKey ->
                    if (currentY > pageHeight - 100) return@forEach
                    when (sectionKey) {
                        "BIO" -> {
                            textPaint.color = sectionAccent
                            textPaint.textSize = 13f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "PROFESSIONAL SUMMARY" else "النبذة المهنية", mainX, currentY, textPaint)
                            currentY += 18f
                            textPaint.color = Color.DKGRAY
                            textPaint.textSize = 10.5f
                            textPaint.typeface = Typeface.DEFAULT
                            val bio = user.merchantBio ?: if (isEn) "Distinguished professional dedicated to performance excellence and verified quality." else "محترف متميز يمتلك خبرة عملية وسجلاً موثقاً من الالتزام والجودة."
                            wrapText(bio, bodyWrapWidth).forEach { line ->
                                canvas.drawText(line, mainX, currentY, textPaint)
                                currentY += 15f
                            }
                            currentY += 14f
                        }
                        "EXPERIENCE" -> {
                            textPaint.color = sectionAccent
                            textPaint.textSize = 13f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "EXPERIENCE & ROLE" else "الخبرة والمسمى الوظيفي", mainX, currentY, textPaint)
                            currentY += 18f
                            textPaint.color = Color.DKGRAY
                            textPaint.textSize = 10.5f
                            textPaint.typeface = Typeface.DEFAULT
                            canvas.drawText("• ${user.jobTitle ?: "Member"} — ${user.experienceYears} ${if (isEn) "yrs" else "سنوات"}", mainX, currentY, textPaint)
                            currentY += 20f
                        }
                        "RELIABILITY" -> {
                            textPaint.color = sectionAccent
                            textPaint.textSize = 13f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "VERIFIED RELIABILITY" else "سجل الموثوقية", mainX, currentY, textPaint)
                            currentY += 18f
                            textPaint.color = Color.DKGRAY
                            textPaint.textSize = 10f
                            textPaint.typeface = Typeface.DEFAULT
                            reliabilityRecords.take(6).forEach { rec ->
                                canvas.drawText("• ${rec.period}: ${rec.presenceRate}% / ${rec.punctuality}% (${rec.overallRating})", mainX, currentY, textPaint)
                                currentY += 15f
                            }
                            currentY += 12f
                        }
                        "EDUCATION" -> if (!user.education.isNullOrBlank()) {
                            textPaint.color = sectionAccent
                            textPaint.textSize = 13f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "EDUCATION" else "المؤهلات العلمية", mainX, currentY, textPaint)
                            currentY += 18f
                            textPaint.color = Color.DKGRAY
                            textPaint.textSize = 10.5f
                            textPaint.typeface = Typeface.DEFAULT
                            wrapText(user.education!!, bodyWrapWidth).forEach { line ->
                                canvas.drawText("• $line", mainX, currentY, textPaint)
                                currentY += 15f
                            }
                            currentY += 14f
                        }
                        "STORE" -> if (!user.traderShopName.isNullOrBlank()) {
                            textPaint.color = sectionAccent
                            textPaint.textSize = 13f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "STORE" else "المتجر", mainX, currentY, textPaint)
                            currentY += 18f
                            textPaint.color = Color.DKGRAY
                            textPaint.textSize = 10.5f
                            textPaint.typeface = Typeface.DEFAULT
                            canvas.drawText("• ${user.traderShopName}", mainX, currentY, textPaint)
                            currentY += 18f
                        }
                    }
                }
            } else {
                val isDarkBanner = layoutStyle in listOf(
                    com.example.ui.components.CvVisualLayout.EXECUTIVE_BANNER,
                    com.example.ui.components.CvVisualLayout.CORPORATE_SPLIT_HEADER,
                    com.example.ui.components.CvVisualLayout.PRESTIGE_DOUBLE_FRAME,
                    com.example.ui.components.CvVisualLayout.COMMERCE_SHOWCASE
                )
                val isSoftBand = layoutStyle in listOf(
                    com.example.ui.components.CvVisualLayout.ATS_TOP_BAND,
                    com.example.ui.components.CvVisualLayout.ACADEMIC_FORMAL,
                    com.example.ui.components.CvVisualLayout.COMPACT_TWO_COL
                )

                if (layoutStyle == com.example.ui.components.CvVisualLayout.PRESTIGE_DOUBLE_FRAME) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = sectionAccent
                    canvas.drawRect(14f, 14f, (pageWidth - 14).toFloat(), (pageHeight - 14).toFloat(), paint)
                    paint.strokeWidth = 0.8f
                    paint.color = secondaryAccent
                    canvas.drawRect(20f, 20f, (pageWidth - 20).toFloat(), (pageHeight - 20).toFloat(), paint)
                    paint.style = Paint.Style.FILL
                }

                if (isDarkBanner) {
                    paint.color = Color.parseColor(template.headerBgHex)
                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 164f, paint)
                    paint.color = sectionAccent
                    canvas.drawRect(0f, 160f, pageWidth.toFloat(), 164f, paint)

                    textPaint.color = Color.WHITE
                    textPaint.textSize = 24f
                    textPaint.typeface = Typeface.DEFAULT_BOLD
                    canvas.drawText(user.name, 40f, 64f, textPaint)

                    textPaint.textSize = 14f
                    textPaint.color = secondaryAccent
                    textPaint.typeface = Typeface.DEFAULT
                    canvas.drawText(user.jobTitle ?: if (isEn) "Certified Professional" else "عضو معتمد", 40f, 90f, textPaint)

                    textPaint.textSize = 10f
                    textPaint.color = Color.parseColor("#CBD5E1")
                    if (layoutStyle == com.example.ui.components.CvVisualLayout.CORPORATE_SPLIT_HEADER) {
                        textPaint.textAlign = Paint.Align.RIGHT
                        canvas.drawText(user.phone, (pageWidth - 40).toFloat(), 62f, textPaint)
                        canvas.drawText("@${user.username}", (pageWidth - 40).toFloat(), 80f, textPaint)
                        canvas.drawText("oozox.com/u/${user.username}", (pageWidth - 40).toFloat(), 98f, textPaint)
                        textPaint.textAlign = Paint.Align.LEFT
                    } else {
                        canvas.drawText(if (isEn) "Phone: ${user.phone}" else "الهاتف: ${user.phone}", 40f, 122f, textPaint)
                        canvas.drawText(if (isEn) "Username: @${user.username}" else "اسم المستخدم: @${user.username}", 40f, 138f, textPaint)
                        canvas.drawText("oozox.com/u/${user.username}", (pageWidth - 200).toFloat(), 138f, textPaint)
                    }
                    currentY = 196f
                } else {
                    if (isSoftBand) {
                        paint.color = Color.parseColor(template.headerBgHex)
                        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paint)
                    }
                    if (layoutStyle == com.example.ui.components.CvVisualLayout.ACADEMIC_FORMAL) {
                        paint.color = sectionAccent
                        canvas.drawRect(40f, 22f, (pageWidth - 40).toFloat(), 25f, paint)
                    }
                    if (layoutStyle == com.example.ui.components.CvVisualLayout.ATS_TECH_ACCENT) {
                        paint.color = sectionAccent
                        canvas.drawRect(40f, 34f, 45f, 96f, paint)
                    }

                    val headerX = if (layoutStyle == com.example.ui.components.CvVisualLayout.ATS_TECH_ACCENT) 54f else 40f
                    textPaint.color = sectionAccent
                    textPaint.textSize = 22f
                    textPaint.typeface = Typeface.DEFAULT_BOLD
                    canvas.drawText(user.name, headerX, 54f, textPaint)

                    textPaint.color = Color.parseColor("#334155")
                    textPaint.textSize = 13f
                    textPaint.typeface = Typeface.DEFAULT
                    canvas.drawText(user.jobTitle ?: if (isEn) "Certified Professional" else "عضو معتمد", headerX, 75f, textPaint)

                    textPaint.textSize = 10f
                    textPaint.color = Color.parseColor("#475569")
                    val contactLine = "${user.phone} | @${user.username} | oozox.com/u/${user.username}"
                    canvas.drawText(contactLine, headerX, 95f, textPaint)

                    paint.color = sectionAccent
                    if (layoutStyle == com.example.ui.components.CvVisualLayout.MINIMAL_EDITORIAL) {
                        canvas.drawRect(40f, 110f, 120f, 112f, paint)
                    } else {
                        canvas.drawRect(40f, 114f, (pageWidth - 40).toFloat(), 116f, paint)
                    }
                    currentY = 144f
                }

                sectionOrder.forEach { sectionKey ->
                    if (currentY > pageHeight - 150) {
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 40f
                    }

                    when (sectionKey) {
                        "BIO" -> {
                            if (layoutStyle == com.example.ui.components.CvVisualLayout.ATS_TECH_ACCENT) {
                                paint.color = sectionAccent
                                canvas.drawRect(40f, currentY - 12f, 44f, currentY + 2f, paint)
                            }
                            val titleX = if (layoutStyle == com.example.ui.components.CvVisualLayout.ATS_TECH_ACCENT) 50f else 40f
                            textPaint.color = sectionAccent
                            textPaint.textSize = 14f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "PROFESSIONAL SUMMARY" else "النبذة المهنية", titleX, currentY, textPaint)

                            paint.color = sectionAccent
                            canvas.drawRect(40f, currentY + 5f, 120f, currentY + 6.5f, paint)

                            currentY += 26f
                            textPaint.color = Color.DKGRAY
                            textPaint.textSize = 11f
                            textPaint.typeface = Typeface.DEFAULT
                            val bio = user.merchantBio ?: if (isEn) "Distinguished professional dedicated to performance excellence and verified quality." else "محترف متميز يمتلك خبرة عملية وسجلاً موثقاً من الالتزام والجودة."
                            wrapText(bio, 85).forEach { line ->
                                canvas.drawText(line, 40f, currentY, textPaint)
                                currentY += 17f
                            }
                            currentY += 18f
                        }
                        "EXPERIENCE" -> {
                            textPaint.color = sectionAccent
                            textPaint.textSize = 14f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "EXPERIENCE & ROLE" else "الخبرة والمسمى الوظيفي", 40f, currentY, textPaint)
                            currentY += 22f
                            textPaint.color = Color.DKGRAY
                            textPaint.textSize = 11f
                            textPaint.typeface = Typeface.DEFAULT
                            val expLine = "${user.jobTitle ?: (if (isEn) "Member" else "عضو معتمد")} — ${user.experienceYears} ${if (isEn) "years experience" else "سنوات خبرة"}"
                            canvas.drawText("• $expLine", 40f, currentY, textPaint)
                            currentY += 24f
                        }
                        "RELIABILITY" -> {
                            textPaint.color = sectionAccent
                            textPaint.textSize = 14f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "VERIFIED RELIABILITY RECORD" else "سجل الموثوقية والالتزام الفعلي", 40f, currentY, textPaint)

                            currentY += 22f
                            paint.color = Color.parseColor("#F1F5F9")
                            canvas.drawRoundRect(40f, currentY, (pageWidth - 40).toFloat(), currentY + 28f, 4f, 4f, paint)
                            if (layoutStyle == com.example.ui.components.CvVisualLayout.ATS_AUDIT_LEDGER) {
                                paint.style = Paint.Style.STROKE
                                paint.color = Color.parseColor("#CBD5E1")
                                paint.strokeWidth = 1f
                                canvas.drawRect(40f, currentY, (pageWidth - 40).toFloat(), currentY + 28f, paint)
                                paint.style = Paint.Style.FILL
                            }

                            textPaint.color = Color.BLACK
                            textPaint.textSize = 10f
                            textPaint.typeface = Typeface.DEFAULT_BOLD
                            canvas.drawText(if (isEn) "Period" else "الفترة", 50f, currentY + 18f, textPaint)
                            canvas.drawText(if (isEn) "Presence" else "الحضور", 160f, currentY + 18f, textPaint)
                            canvas.drawText(if (isEn) "Location" else "الموقع", 240f, currentY + 18f, textPaint)
                            canvas.drawText(if (isEn) "Punctuality" else "الالتزام", 320f, currentY + 18f, textPaint)
                            canvas.drawText(if (isEn) "Rating" else "التقييم", 420f, currentY + 18f, textPaint)

                            currentY += 34f
                            reliabilityRecords.take(10).forEachIndexed { index, record ->
                                if (index % 2 == 1) {
                                    paint.color = Color.parseColor("#F8FAFC")
                                    canvas.drawRect(40f, currentY - 13f, (pageWidth - 40).toFloat(), currentY + 7f, paint)
                                }
                                textPaint.color = Color.DKGRAY
                                textPaint.typeface = Typeface.DEFAULT
                                canvas.drawText(record.period, 50f, currentY, textPaint)
                                canvas.drawText("${record.presenceRate}%", 160f, currentY, textPaint)
                                canvas.drawText("${record.locationCompliance}%", 240f, currentY, textPaint)
                                canvas.drawText("${record.punctuality}%", 320f, currentY, textPaint)

                                textPaint.typeface = Typeface.DEFAULT_BOLD
                                val isIdeal = record.overallRating.contains("مثالي") || record.overallRating.contains("Ideal")
                                textPaint.color = if (isIdeal) Color.parseColor("#059669") else Color.parseColor("#1E293B")
                                val ratingText = if (isEn && isIdeal) "Ideal (100%)" else record.overallRating
                                canvas.drawText(ratingText, 420f, currentY, textPaint)
                                currentY += 20f
                            }
                            currentY += 20f
                        }
                        "SKILLS" -> {
                            if (!user.skills.isNullOrBlank()) {
                                textPaint.color = sectionAccent
                                textPaint.textSize = 14f
                                textPaint.typeface = Typeface.DEFAULT_BOLD
                                canvas.drawText(if (isEn) "SKILLS & COMPETENCIES" else "المهارات والتخصصات", 40f, currentY, textPaint)
                                currentY += 20f
                                textPaint.color = Color.DKGRAY
                                textPaint.textSize = 11f
                                textPaint.typeface = Typeface.DEFAULT
                                wrapText(user.skills!!, 85).forEach { line ->
                                    canvas.drawText("• $line", 40f, currentY, textPaint)
                                    currentY += 17f
                                }
                                currentY += 18f
                            }
                        }
                        "EDUCATION" -> {
                            if (!user.education.isNullOrBlank()) {
                                textPaint.color = sectionAccent
                                textPaint.textSize = 14f
                                textPaint.typeface = Typeface.DEFAULT_BOLD
                                canvas.drawText(if (isEn) "EDUCATION" else "المؤهلات العلمية", 40f, currentY, textPaint)
                                currentY += 20f
                                textPaint.color = Color.DKGRAY
                                textPaint.textSize = 11f
                                textPaint.typeface = Typeface.DEFAULT
                                wrapText(user.education!!, 85).forEach { line ->
                                    canvas.drawText("• $line", 40f, currentY, textPaint)
                                    currentY += 17f
                                }
                                currentY += 18f
                            }
                        }
                        "STORE" -> {
                            if (!user.traderShopName.isNullOrBlank()) {
                                textPaint.color = sectionAccent
                                textPaint.textSize = 14f
                                textPaint.typeface = Typeface.DEFAULT_BOLD
                                canvas.drawText(if (isEn) "BUSINESS & STORE" else "المتجر والنشاط التجاري", 40f, currentY, textPaint)
                                currentY += 20f
                                textPaint.color = Color.DKGRAY
                                textPaint.textSize = 11f
                                textPaint.typeface = Typeface.DEFAULT
                                canvas.drawText("• ${user.traderShopName}", 40f, currentY, textPaint)
                                currentY += 22f
                            }
                        }
                    }
                }
            }

            // Footer
            textPaint.color = Color.LTGRAY
            textPaint.textSize = 9f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                if (isEn) "This CV is digitally verified via OOZOX Platform based on verified GPS Geofencing and actual activity." else "هذه السيرة الذاتية موثقة رقمياً عبر منصة OOZOX وتعتمد على بيانات الموقع الجغرافي والنشاط الفعلي.",
                (pageWidth / 2).toFloat(),
                (pageHeight - 30).toFloat(),
                textPaint
            )

            pdfDocument.finishPage(page)

            val fileName = "CV_${user.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            shareFile(context, file, "application/pdf", if (isEn) "Share CV (PDF)" else "مشاركة السيرة الذاتية (CV)")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, if (isEn) "Couldn't export CV. Please try again" else "تعذر تصدير السيرة الذاتية، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.length + word.length + 1 <= maxChars) {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun truncate(str: String, maxLength: Int): String {
        return if (str.length > maxLength) str.substring(0, maxLength - 1) + "…" else str
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "تجد رفقه تقرير مسجل وموثق من منصة OOZOX (oozox.com).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)

        Toast.makeText(context, "تم جهوز الملف وتم فتح خيارات الحفظ والمشاركة", Toast.LENGTH_LONG).show()
    }

    fun exportPortfolioToCsv(
        context: Context,
        purchases: List<GoldPurchaseEntity>,
        goldPrices: GoldPricesModel
    ) {
        if (purchases.isEmpty()) {
            Toast.makeText(context, "حقيبة الذهب والفضة خالية من المشتريات", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "OOZOX_Gold_Portfolio_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM

            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            writer.write("تقرير حقيبة الذهب والفضة والاستثمارات - منصة OOZOX (oozox.com)\n")
            writer.write("تاريخ التصدير,${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar", "EG")).format(Date())}\n")
            writer.write("إجمالي عدد المشتريات بالحقيبة,${purchases.size}\n")
            val totalWeight = purchases.sumOf { it.extractedWeight }
            val totalCost = purchases.sumOf { it.totalPaid }
            writer.write("إجمالي وزن الحقيبة (جرام),${totalWeight}\n")
            writer.write("إجمالي تكلفة الشراء (ج.م),${totalCost}\n\n")

            writer.write("م,نوع المعدن,العيار / العيار النقي,الوزن (جرام),سعر الشراء للجرام (ج.م),الشركة المصنعة,مصنعية الجرام (ج.م),ضريبة الدمغة,الوسم / المحل,نسبة التآكل %,إجمالي تكلفة الشراء (ج.م),تاريخ الشراء\n")

            purchases.forEachIndexed { idx, p ->
                val line = listOf(
                    (idx + 1).toString(),
                    escapeCsv(if (p.metalType == "SILVER") "فضة" else "ذهب"),
                    escapeCsv(p.karat),
                    p.extractedWeight.toString(),
                    p.pricePerGram.toString(),
                    escapeCsv(p.manufacturer),
                    p.manufacturingCost.toString(),
                    p.stampDuty.toString(),
                    escapeCsv(p.traderHashtag),
                    p.metalLossRatePercent.toString(),
                    p.totalPaid.toString(),
                    escapeCsv(GoldUtils.formatDate(p.createdAt))
                ).joinToString(",")
                writer.write(line + "\n")
            }

            writer.flush()
            writer.close()
            shareFile(context, file, "text/csv", "تصدير حقيبة الذهب والفضة (Excel)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر تصدير الملف، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportLedgerToCsv(
        context: Context,
        entries: List<HashtagEntryEntity>,
        accountTitle: String
    ) {
        if (entries.isEmpty()) {
            Toast.makeText(context, "لا توجد قيود بالدفتر للتصدير", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "Ledger_Journal_Excel_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            writer.write("دفتر الأستاذ العام والقيود المزدوجة - $accountTitle\n")
            writer.write("تاريخ التقرير,${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar", "EG")).format(Date())}\n")
            writer.write("إجمالي عدد القيود,${entries.size}\n\n")

            writer.write("م,نوع القيد,البيان / الشرح,المبلغ (ج.م),الاتجاه (مدين/دائن),المُنشئ,التاريخ\n")

            entries.forEachIndexed { idx, entry ->
                val directionLabel = if (entry.direction == "DEBIT") "مدين (له)" else "دائن (عليه)"
                val line = listOf(
                    (idx + 1).toString(),
                    escapeCsv(entry.type),
                    escapeCsv(entry.description),
                    entry.amount.toString(),
                    escapeCsv(directionLabel),
                    escapeCsv(entry.createdBy),
                    escapeCsv(GoldUtils.formatDate(entry.createdAt))
                ).joinToString(",")
                writer.write(line + "\n")
            }

            writer.flush()
            writer.close()
            shareFile(context, file, "text/csv", "تصدير دفتر الأستاذ والقيود (Excel)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر تصدير الملف، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportProjectsToCsv(
        context: Context,
        projects: List<ProjectEntity>
    ) {
        if (projects.isEmpty()) {
            Toast.makeText(context, "لا توجد مشاريع مقاولات للتصدير", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "Projects_Summary_Excel_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            writer.write("تقرير ميزانيات مشاريع المقاولات والتطوير\n")
            writer.write("تاريخ التصدير,${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar", "EG")).format(Date())}\n\n")

            writer.write("م,اسم المشروع,نوع المشروع,رأس المال التقديري (ج.م),إجمالي المصاريف,إجمالي المقبوضات,صافي الربح التشغيلي,الموقع,المسؤولين\n")

            projects.forEachIndexed { idx, prj ->
                val netProfit = prj.totalIncome - prj.totalExpenses
                val line = listOf(
                    (idx + 1).toString(),
                    escapeCsv(prj.name),
                    escapeCsv(prj.type),
                    prj.estimatedCapital.toString(),
                    prj.totalExpenses.toString(),
                    prj.totalIncome.toString(),
                    netProfit.toString(),
                    escapeCsv(prj.location),
                    escapeCsv("${prj.coManagerName ?: ""} ${prj.coManagerPhone ?: ""}")
                ).joinToString(",")
                writer.write(line + "\n")
            }

            writer.flush()
            writer.close()
            shareFile(context, file, "text/csv", "تصدير مشاريع المقاولات (Excel)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر تصدير الملف، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    fun parseCsvToEntries(csvText: String): List<Triple<String, Double, String>> {
        val result = mutableListOf<Triple<String, Double, String>>()
        val lines = csvText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("م,") || trimmed.startsWith("نوع")) continue
            val parts = trimmed.split(",")
            if (parts.size >= 3) {
                val desc = parts[1].replace("\"", "").trim()
                val amount = parts[2].replace("\"", "").trim().toDoubleOrNull() ?: 0.0
                val direction = if (parts.size >= 4 && parts[3].contains("دائن")) "CREDIT" else "DEBIT"
                if (amount > 0) {
                    result.add(Triple(desc, amount, direction))
                }
            }
        }
        return result
    }

    fun exportQuotationPdf(
        context: Context,
        buyerName: String,
        sellerStoreName: String = "مجوهرات الصاغة",
        metalType: String,
        karat: String,
        weightGrams: Double,
        gramPrice: Double,
        mfgCostPerGram: Double,
        vatAmount: Double,
        stampDuty: Double,
        grandTotal: Double,
        notes: String? = null
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply { isAntiAlias = true }

            var currentY = 40f

            // Top Gold Header
            paint.color = Color.parseColor("#1E222A")
            canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 70f, paint)

            paint.color = Color.parseColor("#D4AF37")
            canvas.drawRect(20f, currentY + 65f, (pageWidth - 20).toFloat(), currentY + 70f, paint)

            textPaint.color = Color.parseColor("#FFD700")
            textPaint.textSize = 20f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("فاتورة وعرض سعر معتمد - صاغة الذهب والفضة", 35f, currentY + 32f, textPaint)

            textPaint.color = Color.LTGRAY
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.DEFAULT
            val quoteNumber = ShortCodeGenerator.generateInvoiceCode()
            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar", "EG")).format(Date())
            canvas.drawText("رقم الفاتورة: $quoteNumber  |  التاريخ: $dateStr", 35f, currentY + 54f, textPaint)

            currentY += 95f

            // Buyer Card
            paint.color = Color.parseColor("#F8F9FA")
            canvas.drawRoundRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 50f, 10f, 10f, paint)

            textPaint.color = Color.BLACK
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("العميل / المستلم: ${if (buyerName.isBlank()) "عميل نقدي" else buyerName}", 35f, currentY + 22f, textPaint)
            textPaint.color = Color.DKGRAY
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText("البيان: شراء وتوريد سبائك ومصاغ معتمد خاضع للدمغة الرسمية", 35f, currentY + 38f, textPaint)

            currentY += 65f

            // Table Header
            paint.color = Color.parseColor("#D4AF37")
            canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 26f, paint)

            textPaint.color = Color.BLACK
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("البند / المواصفة", 35f, currentY + 18f, textPaint)
            canvas.drawText("الوزن (جم)", 220f, currentY + 18f, textPaint)
            canvas.drawText("سعر الجرام", 330f, currentY + 18f, textPaint)
            canvas.drawText("الإجمالي (ج.م)", 450f, currentY + 18f, textPaint)

            currentY += 32f

            // Item Row
            val pureMetalTotal = weightGrams * gramPrice
            val totalMfg = weightGrams * mfgCostPerGram

            textPaint.color = Color.BLACK
            textPaint.textSize = 10.5f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText("$metalType $karat", 35f, currentY + 10f, textPaint)
            canvas.drawText("$weightGrams جم", 220f, currentY + 10f, textPaint)
            canvas.drawText("${GoldUtils.formatCurrency(gramPrice)}", 330f, currentY + 10f, textPaint)
            canvas.drawText("${GoldUtils.formatCurrency(pureMetalTotal)}", 450f, currentY + 10f, textPaint)

            currentY += 28f

            // Mfg row
            paint.color = Color.parseColor("#F5F5F5")
            canvas.drawRect(20f, currentY - 5f, (pageWidth - 20).toFloat(), currentY + 22f, paint)
            textPaint.color = Color.DKGRAY
            canvas.drawText("المصنعية والتشغيل", 35f, currentY + 10f, textPaint)
            canvas.drawText("$weightGrams جم", 220f, currentY + 10f, textPaint)
            canvas.drawText("${GoldUtils.formatCurrency(mfgCostPerGram)}", 330f, currentY + 10f, textPaint)
            canvas.drawText("${GoldUtils.formatCurrency(totalMfg)}", 450f, currentY + 10f, textPaint)

            currentY += 36f

            // Tax & Vat Row
            if (vatAmount > 0 || stampDuty > 0) {
                textPaint.color = Color.GRAY
                canvas.drawText("ضريبة القيمة المضافة ورسم الدمغة:", 35f, currentY + 8f, textPaint)
                canvas.drawText("${GoldUtils.formatCurrency(vatAmount + stampDuty)}", 450f, currentY + 8f, textPaint)
                currentY += 25f
            }

            // Total Bottom Card
            paint.color = Color.parseColor("#1E222A")
            canvas.drawRoundRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 55f, 10f, 10f, paint)

            textPaint.color = Color.parseColor("#D4AF37")
            textPaint.textSize = 15f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("الإجمالي الكلي المستحق:", 35f, currentY + 34f, textPaint)

            textPaint.color = Color.parseColor("#FFD700")
            textPaint.textSize = 17f
            canvas.drawText("${GoldUtils.formatCurrency(grandTotal)}", 400f, currentY + 34f, textPaint)

            currentY += 75f

            // Notes / Stamp
            if (!notes.isNullOrBlank()) {
                textPaint.color = Color.DKGRAY
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.DEFAULT
                canvas.drawText("ملاحظات وشروط التسليم: $notes", 35f, currentY, textPaint)
                currentY += 20f
            }

            textPaint.color = Color.GRAY
            textPaint.textSize = 9f
            canvas.drawText("• وثيقة إلكترونية معتمدة من تطبيق الصاغة لتداول الذهب والفضة.", 35f, currentY + 20f, textPaint)

            pdfDocument.finishPage(page)

            val fileName = "Gold_Invoice_${quoteNumber}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            shareFile(context, file, "application/pdf", "تصدير فاتورة / عرض سعر ذهب معتمد")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر إنشاء الفاتورة، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportAccountingRecordsToPdf(
        context: Context,
        groupName: String,
        companyName: String?,
        records: List<DailyAccountingRecordEntity>,
        filterCriteriaLabel: String = "جميع السجلات"
    ) {
        if (records.isEmpty()) {
            Toast.makeText(context, "لا توجد سجلات لتصديرها بصيغة PDF", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 842 // A4 Landscape width
            val pageHeight = 595 // A4 Landscape height

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }

            val totalRev = records.sumOf { it.dailyRevenue }
            val totalInc = records.sumOf { it.incomingAmount }
            val totalOut = records.sumOf { it.outgoingAmount }
            val totalDef = records.sumOf { it.deficitAmount }
            val netTotal = (totalRev + totalInc) - (totalOut + totalDef)

            var currentY = 30f

            fun drawHeader() {
                // Top Header Banner
                paint.color = Color.parseColor("#1E222A")
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 55f, paint)

                paint.color = Color.parseColor("#D4AF37")
                canvas.drawRect(20f, currentY + 52f, (pageWidth - 20).toFloat(), currentY + 55f, paint)

                textPaint.color = Color.parseColor("#FFD700")
                textPaint.textSize = 15f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                val titleOrg = "${companyName ?: groupName} - المركز المالي والمحاسبي"
                canvas.drawText(titleOrg, 35f, currentY + 24f, textPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 10.5f
                textPaint.typeface = Typeface.DEFAULT
                canvas.drawText("كشف حسابات وحركات اليومية والقيود المالية المعتمدة (Daily Ledger & Transactions)", 35f, currentY + 42f, textPaint)

                val exportDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar", "EG")).format(Date())
                textPaint.textSize = 9.5f
                textPaint.color = Color.LTGRAY
                canvas.drawText("تاريخ التقرير: $exportDate | صفحة $pageNumber", (pageWidth - 220).toFloat(), currentY + 24f, textPaint)
                canvas.drawText("نطاق الفلتر: $filterCriteriaLabel", (pageWidth - 220).toFloat(), currentY + 42f, textPaint)

                currentY += 68f

                // Summary Stats Bar on Page 1
                if (pageNumber == 1) {
                    paint.color = Color.parseColor("#F4F6F8")
                    canvas.drawRoundRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 42f, 6f, 6f, paint)

                    textPaint.color = Color.BLACK
                    textPaint.textSize = 9.5f
                    textPaint.typeface = Typeface.DEFAULT_BOLD
                    canvas.drawText("عدد الحركات: ${records.size}", 35f, currentY + 25f, textPaint)

                    textPaint.color = Color.parseColor("#2E7D32")
                    canvas.drawText("الوارد/الإيراد: +${GoldUtils.formatCurrency(totalRev + totalInc)}", 160f, currentY + 25f, textPaint)

                    textPaint.color = Color.parseColor("#C62828")
                    canvas.drawText("المصروفات: -${GoldUtils.formatCurrency(totalOut)}", 360f, currentY + 25f, textPaint)

                    textPaint.color = Color.parseColor("#EF6C00")
                    canvas.drawText("العجز: ${GoldUtils.formatCurrency(totalDef)}", 530f, currentY + 25f, textPaint)

                    textPaint.color = if (netTotal >= 0) Color.parseColor("#1565C0") else Color.parseColor("#C62828")
                    canvas.drawText("الصافي: ${GoldUtils.formatCurrency(netTotal)}", 680f, currentY + 25f, textPaint)

                    currentY += 52f
                }
            }

            fun drawTableHeaders() {
                paint.color = Color.parseColor("#D4AF37")
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 22f, paint)

                textPaint.color = Color.BLACK
                textPaint.textSize = 9.5f
                textPaint.typeface = Typeface.DEFAULT_BOLD

                canvas.drawText("#", 28f, currentY + 15f, textPaint)
                canvas.drawText("التاريخ", 50f, currentY + 15f, textPaint)
                canvas.drawText("الوقت", 120f, currentY + 15f, textPaint)
                canvas.drawText("القسم / الفرع", 165f, currentY + 15f, textPaint)
                canvas.drawText("الموظف المسجل", 260f, currentY + 15f, textPaint)
                canvas.drawText("الوارد/الإيراد (+)", 380f, currentY + 15f, textPaint)
                canvas.drawText("الصادر/المصروف (-)", 485f, currentY + 15f, textPaint)
                canvas.drawText("العجز", 595f, currentY + 15f, textPaint)
                canvas.drawText("الصافي", 665f, currentY + 15f, textPaint)
                canvas.drawText("البيان والملاحظات", 740f, currentY + 15f, textPaint)

                currentY += 26f
            }

            drawHeader()
            drawTableHeaders()

            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

            records.forEachIndexed { index, record ->
                if (currentY > pageHeight - 45) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 30f
                    drawHeader()
                    drawTableHeaders()
                }

                // Row zebra striping
                if (index % 2 == 1) {
                    paint.color = Color.parseColor("#F9F9FB")
                    canvas.drawRect(20f, currentY - 12f, (pageWidth - 20).toFloat(), currentY + 7f, paint)
                }

                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 8.5f
                textPaint.color = Color.DKGRAY

                val net = (record.dailyRevenue + record.incomingAmount) - (record.outgoingAmount + record.deficitAmount)
                val inAmt = record.dailyRevenue + record.incomingAmount

                canvas.drawText("${index + 1}", 28f, currentY, textPaint)
                canvas.drawText(record.entryDate, 50f, currentY, textPaint)
                canvas.drawText(timeFormatter.format(Date(record.timestamp)), 120f, currentY, textPaint)
                canvas.drawText(truncate(record.subGroupId ?: "الرئيسي", 14), 165f, currentY, textPaint)
                canvas.drawText(truncate("${record.staffName} (${record.staffRole})", 18), 260f, currentY, textPaint)

                // Income
                if (inAmt > 0) {
                    textPaint.color = Color.parseColor("#2E7D32")
                    canvas.drawText(GoldUtils.formatCurrency(inAmt), 380f, currentY, textPaint)
                } else {
                    textPaint.color = Color.LTGRAY
                    canvas.drawText("-", 380f, currentY, textPaint)
                }

                // Expense
                if (record.outgoingAmount > 0) {
                    textPaint.color = Color.parseColor("#C62828")
                    canvas.drawText(GoldUtils.formatCurrency(record.outgoingAmount), 485f, currentY, textPaint)
                } else {
                    textPaint.color = Color.LTGRAY
                    canvas.drawText("-", 485f, currentY, textPaint)
                }

                // Deficit
                if (record.deficitAmount > 0) {
                    textPaint.color = Color.parseColor("#EF6C00")
                    canvas.drawText(GoldUtils.formatCurrency(record.deficitAmount), 595f, currentY, textPaint)
                } else {
                    textPaint.color = Color.LTGRAY
                    canvas.drawText("-", 595f, currentY, textPaint)
                }

                // Net
                textPaint.color = if (net >= 0) Color.parseColor("#1565C0") else Color.parseColor("#C62828")
                canvas.drawText(GoldUtils.formatCurrency(net), 665f, currentY, textPaint)

                // Notes
                textPaint.color = Color.DKGRAY
                val noteText = record.notes?.takeIf { it.isNotBlank() } ?: "-"
                canvas.drawText(truncate(noteText, 18), 740f, currentY, textPaint)

                currentY += 19f
            }

            pdfDocument.finishPage(page)

            val safeName = (companyName ?: groupName).replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val fileName = "تقرير_حسابات_${safeName}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            shareFile(context, file, "application/pdf", "تصدير تقرير القيود والحسابات (PDF)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر إنشاء ملف PDF، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatQuotationText(
        buyerName: String,
        metalType: String,
        karat: String,
        weightGrams: Double,
        gramPrice: Double,
        mfgCostPerGram: Double,
        vatAmount: Double,
        stampDuty: Double,
        grandTotal: Double
    ): String {
        val pureMetalTotal = weightGrams * gramPrice
        val totalMfg = weightGrams * mfgCostPerGram
        val quoteNumber = ShortCodeGenerator.generateInvoiceCode()
        return """
            🧾 **فاتورة وعرض سعر معتمد** ($quoteNumber)
            👤 **العميل**: ${if (buyerName.isBlank()) "عميل محترم" else buyerName}
            ━━━━━━━━━━━━━━━━━━━━━
            👑 **المعدن والعيار**: $metalType $karat
            ⚖️ **الوزن**: $weightGrams جرام
            💰 **سعر الجرام**: ${GoldUtils.formatCurrency(gramPrice)}
            🔨 **المصنعية/جرام**: ${GoldUtils.formatCurrency(mfgCostPerGram)}
            ━━━━━━━━━━━━━━━━━━━━━
            • قيمة المعدن الخالص: ${GoldUtils.formatCurrency(pureMetalTotal)}
            • إجمالي المصنعية: ${GoldUtils.formatCurrency(totalMfg)}
            ${if (vatAmount + stampDuty > 0) "• الدمغة والضريبة: ${GoldUtils.formatCurrency(vatAmount + stampDuty)}\n" else ""}
            ✨ **الإجمالي النهائي المستحق**: **${GoldUtils.formatCurrency(grandTotal)}**
            ━━━━━━━━━━━━━━━━━━━━━
            *(صادر وموثق عبر منصة الصاغة لتداول الذهب)*
        """.trimIndent()
    }

    fun exportChatConversationToPdf(
        context: Context,
        chat: ChatEntity,
        messages: List<MessageEntity>
    ) {
        if (messages.isEmpty()) {
            Toast.makeText(context, "المحادثة فارغة، لا توجد رسائل لتصديرها", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply { isAntiAlias = true }

            var currentY = 40f

            fun drawHeader() {
                paint.color = Color.parseColor("#1E222A")
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 65f, paint)

                paint.color = Color.parseColor("#D4AF37")
                canvas.drawRect(20f, currentY + 60f, (pageWidth - 20).toFloat(), currentY + 65f, paint)

                textPaint.color = Color.parseColor("#FFD700")
                textPaint.textSize = 17f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("OOZOX - سجل المحادثة المعتمد", 35f, currentY + 28f, textPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 12f
                textPaint.typeface = Typeface.DEFAULT
                canvas.drawText("المحادثة مع: ${chat.contactName} (${messages.size} رسالة)", 35f, currentY + 50f, textPaint)

                val exportDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar", "EG")).format(Date())
                textPaint.textSize = 9.5f
                textPaint.color = Color.LTGRAY
                canvas.drawText("تاريخ التصدير: $exportDate", (pageWidth - 170).toFloat(), currentY + 35f, textPaint)

                currentY += 80f
            }

            drawHeader()

            val sortedMessages = messages.sortedBy { it.timestamp }
            for (msg in sortedMessages) {
                if (currentY > pageHeight - 70f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 40f
                    drawHeader()
                }

                val timeStr = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale("ar", "EG")).format(Date(msg.timestamp))
                val isVoice = msg.type == "VOICE"
                val content = if (isVoice) "[تسجيل صوتي مذكرّة - Voice Memo] ${msg.content}" else msg.content

                // Sender tag
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(25f, currentY - 14f, (pageWidth - 25).toFloat(), currentY + 24f, 8f, 8f, paint)

                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textSize = 10.5f
                textPaint.color = Color.parseColor("#1E293B")
                canvas.drawText("${msg.senderName}:", 35f, currentY, textPaint)

                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 8.5f
                textPaint.color = Color.GRAY
                canvas.drawText(timeStr, (pageWidth - 140).toFloat(), currentY, textPaint)

                // Message text lines
                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 10f
                textPaint.color = if (isVoice) Color.parseColor("#D97706") else Color.DKGRAY
                val lines = wrapText(content, 70)
                for (line in lines) {
                    currentY += 16f
                    if (currentY > pageHeight - 50f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 40f
                        drawHeader()
                    }
                    canvas.drawText(line, 45f, currentY, textPaint)
                }

                currentY += 24f
            }

            // Footer
            textPaint.color = Color.LTGRAY
            textPaint.textSize = 8.5f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "محادثة مشفرة وموثقة رقمياً عبر منصة OOZOX (oozox.com) - CONNECT • THINK • DO",
                (pageWidth / 2).toFloat(),
                (pageHeight - 20).toFloat(),
                textPaint
            )

            pdfDocument.finishPage(page)

            val safeName = chat.contactName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val fileName = "OOZOX_Chat_${safeName}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            shareFile(context, file, "application/pdf", "تصدير محادثة $safeName كـ PDF")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر تصدير المحادثة، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportProjectSummaryToPdf(
        context: Context,
        project: ProjectEntity,
        tasks: List<ProjectTaskEntity>? = null,
        members: List<ProjectMemberEntity>? = null
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = Paint().apply { isAntiAlias = true }

            var currentY = 40f

            // Top Header
            paint.color = Color.parseColor("#1E222A")
            canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 70f, paint)

            paint.color = Color.parseColor("#D4AF37")
            canvas.drawRect(20f, currentY + 65f, (pageWidth - 20).toFloat(), currentY + 70f, paint)

            textPaint.color = Color.parseColor("#FFD700")
            textPaint.textSize = 18f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("OOZOX - ملخص تنفيذي للمشروع والميزانية", 35f, currentY + 30f, textPaint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText("مشروع: ${project.name} (${project.type})", 35f, currentY + 52f, textPaint)

            val exportDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar", "EG")).format(Date())
            textPaint.textSize = 9.5f
            textPaint.color = Color.LTGRAY
            canvas.drawText("تاريخ التقرير: $exportDate", (pageWidth - 170).toFloat(), currentY + 35f, textPaint)

            currentY += 90f

            // Financial KPIs Box
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(25f, currentY, (pageWidth - 25).toFloat(), currentY + 110f, 12f, 12f, paint)

            paint.color = Color.parseColor("#CBD5E1")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(25f, currentY, (pageWidth - 25).toFloat(), currentY + 110f, 12f, 12f, paint)
            paint.style = Paint.Style.FILL

            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 13f
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText("المؤشرات المالية والميزانية المعتمدة:", 40f, currentY + 25f, textPaint)

            val remaining = project.estimatedCapital - project.totalExpenses
            val burnRate = if (project.estimatedCapital > 0) ((project.totalExpenses / project.estimatedCapital) * 100).toInt() else 0

            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 11f
            textPaint.color = Color.DKGRAY
            canvas.drawText("• رأس المال المعتمد (الميزانية): ${GoldUtils.formatCurrency(project.estimatedCapital)}", 40f, currentY + 50f, textPaint)

            textPaint.color = Color.parseColor("#DC2626")
            canvas.drawText("• إجمالي المصروفات الفعلية: ${GoldUtils.formatCurrency(project.totalExpenses)}", 40f, currentY + 70f, textPaint)

            textPaint.color = if (remaining >= 0) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
            canvas.drawText("• المتبقي من الميزانية (الفائض/العجز): ${GoldUtils.formatCurrency(remaining)}", 40f, currentY + 90f, textPaint)

            textPaint.color = Color.parseColor("#D97706")
            canvas.drawText("معدل استهلاك الميزانية: $burnRate%", (pageWidth - 180).toFloat(), currentY + 50f, textPaint)

            textPaint.color = Color.GRAY
            canvas.drawText("📍 الموقع: ${project.location}", (pageWidth - 180).toFloat(), currentY + 70f, textPaint)
            if (!project.coManagerName.isNullOrBlank()) {
                canvas.drawText("👤 المدير المشارك: ${project.coManagerName}", (pageWidth - 180).toFloat(), currentY + 90f, textPaint)
            }

            currentY += 130f

            // Project Tasks Section
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textSize = 14f
            textPaint.color = Color.parseColor("#1E222A")
            canvas.drawText("قائمة المهام وحالة الإنجاز:", 30f, currentY, textPaint)
            currentY += 20f

            if (!tasks.isNullOrEmpty()) {
                // Table header
                paint.color = Color.parseColor("#334155")
                canvas.drawRect(25f, currentY, (pageWidth - 25).toFloat(), currentY + 24f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("#", 35f, currentY + 16f, textPaint)
                canvas.drawText("المهمة", 60f, currentY + 16f, textPaint)
                canvas.drawText("المسؤول", 250f, currentY + 16f, textPaint)
                canvas.drawText("الأولوية", 360f, currentY + 16f, textPaint)
                canvas.drawText("الحالة", 430f, currentY + 16f, textPaint)
                canvas.drawText("التكلفة", 500f, currentY + 16f, textPaint)

                currentY += 28f

                tasks.forEachIndexed { index, task ->
                    if (currentY > pageHeight - 60f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 40f
                    }

                    if (index % 2 == 1) {
                        paint.color = Color.parseColor("#F8FAFC")
                        canvas.drawRect(25f, currentY - 12f, (pageWidth - 25).toFloat(), currentY + 10f, paint)
                    }

                    textPaint.typeface = Typeface.DEFAULT
                    textPaint.textSize = 9.5f
                    textPaint.color = Color.DKGRAY

                    canvas.drawText("${index + 1}", 35f, currentY, textPaint)
                    canvas.drawText(truncate(task.title, 25), 60f, currentY, textPaint)
                    canvas.drawText(task.assignedToName ?: "-", 250f, currentY, textPaint)
                    canvas.drawText(task.priority, 360f, currentY, textPaint)

                    val statusLabel = when (task.status) {
                        "COMPLETED" -> "مكتملة"
                        "IN_PROGRESS" -> "قيد التنفيذ"
                        "PENDING" -> "قيد الانتظار"
                        else -> task.status
                    }
                    canvas.drawText(statusLabel, 430f, currentY, textPaint)
                    canvas.drawText(GoldUtils.formatCurrency(task.costEstimate), 500f, currentY, textPaint)

                    currentY += 20f
                }
            } else {
                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = 10.5f
                textPaint.color = Color.GRAY
                canvas.drawText("لا توجد مهام مسجلة لهذا المشروع بعد.", 40f, currentY + 10f, textPaint)
                currentY += 30f
            }

            // Footer
            textPaint.color = Color.LTGRAY
            textPaint.textSize = 8.5f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "ملخص مشروع رسمي معتمد من منصة OOZOX (oozox.com) - CONNECT • THINK • DO",
                (pageWidth / 2).toFloat(),
                (pageHeight - 20).toFloat(),
                textPaint
            )

            pdfDocument.finishPage(page)

            val safeName = project.name.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val fileName = "OOZOX_Project_${safeName}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            shareFile(context, file, "application/pdf", "تصدير ملخص مشروع $safeName كـ PDF")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر تصدير ملخص المشروع، حاول مجدداً", Toast.LENGTH_SHORT).show()
        }
    }
}
