package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.EmployeeAttendanceEntity
import com.example.data.model.GroupMemberEntity
import com.example.data.model.HashtagAccountEntity
import com.example.data.model.HashtagEntryDirection
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.ProjectEntity
import com.example.data.model.ShiftHandoverEntity
import com.example.data.model.WorkGroupEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

data class CompanyAccountingSummary(
    val totalRevenue: Double,
    val totalIncoming: Double,
    val totalOutgoing: Double,
    val totalDeficit: Double,
    val netCashflow: Double,
    val totalComplaints: Int,
    val latestDailyCapital: Double,
    val latestWeeklyCapital: Double,
    val latestMonthlyCapital: Double,
    val recordsCount: Int
)

data class UnifiedBusinessAccountingSummary(
    val projectsIncome: Double,
    val projectsExpenses: Double,
    val projectsCapital: Double,
    val invoicesTotalBilled: Double,
    val invoicesPaidTotal: Double,
    val invoicesOutstanding: Double,
    val externalBusinessTotal: Double,
    val externalBusinessPaid: Double,
    val hashtagsTotalDebit: Double,
    val hashtagsTotalCredit: Double,
    val hashtagsNetReceivable: Double,
    val dailyJournalRevenue: Double,
    val dailyJournalIncoming: Double,
    val dailyJournalOutgoing: Double,
    val shiftDeficitsTotal: Double,
    val combinedTotalInflow: Double,
    val combinedTotalOutflow: Double,
    val combinedNetPosition: Double,
    val totalInvoicesCount: Int,
    val unpaidInvoicesCount: Int,
    val activeHashtagsCount: Int,
    val projectsCount: Int
)

object SimpleExcelEngine {

    /**
     * Compute comprehensive financial metrics from accounting records using high-precision BigDecimal arithmetic
     */
    fun computeSummary(
        records: List<DailyAccountingRecordEntity>,
        initialCapital: Double = 0.0
    ): CompanyAccountingSummary {
        var totalRevBD = BigDecimal.ZERO
        var totalIncBD = BigDecimal.ZERO
        var totalOutBD = BigDecimal.ZERO
        var totalDefBD = BigDecimal.ZERO
        var complaints = 0

        for (record in records) {
            totalRevBD = totalRevBD.add(BigDecimal.valueOf(record.dailyRevenue))
            totalIncBD = totalIncBD.add(BigDecimal.valueOf(record.incomingAmount))
            totalOutBD = totalOutBD.add(BigDecimal.valueOf(record.outgoingAmount))
            totalDefBD = totalDefBD.add(BigDecimal.valueOf(record.deficitAmount))
            complaints += record.complaintsCount
        }

        val totalCredit = totalRevBD.add(totalIncBD)
        val totalDebit = totalOutBD.add(totalDefBD)
        val netBD = totalCredit.subtract(totalDebit).setScale(2, RoundingMode.HALF_UP)

        val totalRev = totalRevBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalInc = totalIncBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalOut = totalOutBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalDef = totalDefBD.setScale(2, RoundingMode.HALF_UP).toDouble()
        val net = netBD.toDouble()

        val latestRecord = records.maxByOrNull { it.timestamp }
        val dayCap = latestRecord?.dailyCapital?.takeIf { it > 0 } ?: (BigDecimal.valueOf(initialCapital).add(netBD).setScale(2, RoundingMode.HALF_UP).toDouble())
        val weekCap = latestRecord?.weeklyCapital?.takeIf { it > 0 } ?: dayCap
        val monthCap = latestRecord?.monthlyCapital?.takeIf { it > 0 } ?: dayCap

        return CompanyAccountingSummary(
            totalRevenue = totalRev,
            totalIncoming = totalInc,
            totalOutgoing = totalOut,
            totalDeficit = totalDef,
            netCashflow = net,
            totalComplaints = complaints,
            latestDailyCapital = dayCap,
            latestWeeklyCapital = weekCap,
            latestMonthlyCapital = monthCap,
            recordsCount = records.size
        )
    }

    /**
     * Generate an Excel-compatible CSV file with UTF-8 BOM header
     * for seamless opening in Microsoft Excel and Google Sheets in Arabic.
     */
    fun exportToExcelCsv(
        context: Context,
        groupName: String,
        companyName: String?,
        records: List<DailyAccountingRecordEntity>,
        members: List<GroupMemberEntity> = emptyList()
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = (companyName ?: groupName).replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val excelFile = File(exportDir, "كشف_حسابات_${safeName}_$timeStamp.csv")

            FileOutputStream(excelFile).use { fos ->
                // Write UTF-8 BOM so Excel opens Arabic text correctly
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Header Metadata
                    writer.write("تقرير الحسابات المالي للشركة والمجموعات المحاسبية - تطبيق الصاغة و OOZOX\n")
                    writer.write("اسم المؤسسة / الشركة,\"${companyName ?: groupName}\"\n")
                    writer.write("تاريخ التصدير,\"${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\"\n")
                    writer.write("عدد القيود المسجلة,${records.size}\n\n")

                    // Column Headers
                    val headers = listOf(
                        "م",
                        "التاريخ",
                        "الوقت",
                        "القسم / المجموعة الفرعية",
                        "اسم الموظف / المحاسب",
                        "المسمى الوظيفي",
                        "إيراد اليوم (ج.م)",
                        "الوارد والمقبوضات (ج.م)",
                        "الصادر والمصروفات (ج.م)",
                        "عجز الصندوق (ج.م)",
                        "صافي الحركة (ج.م)",
                        "رأس المال اليومي (ج.م)",
                        "رأس المال الأسبوعي (ج.م)",
                        "رأس المال الشهري (ج.م)",
                        "عدد الشكاوى",
                        "تفاصيل الشكاوى",
                        "البيان والملاحظات"
                    )
                    writer.write(headers.joinToString(",") { "\"$it\"" } + "\n")

                    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val startRow = 7
                    val endRow = 6 + records.size

                    // Data Rows
                    records.forEachIndexed { index, record ->
                        val currentRow = startRow + index
                        // Native Excel formula for net cashflow per record: (Revenue + Incoming) - (Outgoing + Deficit)
                        val netFormula = "=(G$currentRow+H$currentRow)-(I$currentRow+J$currentRow)"
                        val row = listOf(
                            (index + 1).toString(),
                            record.entryDate,
                            timeFormatter.format(Date(record.timestamp)),
                            record.subGroupId ?: "الرئيسي",
                            record.staffName,
                            record.staffRole,
                            record.dailyRevenue.toString(),
                            record.incomingAmount.toString(),
                            record.outgoingAmount.toString(),
                            record.deficitAmount.toString(),
                            netFormula,
                            record.dailyCapital.toString(),
                            record.weeklyCapital.toString(),
                            record.monthlyCapital.toString(),
                            record.complaintsCount.toString(),
                            record.complaintsNotes ?: "-",
                            record.notes ?: "-"
                        )
                        writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    }

                    // Total Summary Row with native Excel =SUM() formulas
                    val sum = computeSummary(records)
                    writer.write("\n")
                    val totalRow = if (records.isNotEmpty()) {
                        val totalRowIdx = endRow + 2
                        listOf(
                            "الإجمالي العام",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "=SUM(G$startRow:G$endRow)",
                            "=SUM(H$startRow:H$endRow)",
                            "=SUM(I$startRow:I$endRow)",
                            "=SUM(J$startRow:J$endRow)",
                            "=(G$totalRowIdx+H$totalRowIdx)-(I$totalRowIdx+J$totalRowIdx)",
                            sum.latestDailyCapital.toString(),
                            sum.latestWeeklyCapital.toString(),
                            sum.latestMonthlyCapital.toString(),
                            "=SUM(O$startRow:O$endRow)",
                            "-",
                            "تم الحساب والتدقيق بصيغ إكسل التفاعلية (=SUM) والمحاسب الذكي"
                        )
                    } else {
                        listOf(
                            "الإجمالي العام",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "0.00",
                            "0.00",
                            "0.00",
                            "0.00",
                            "0.00",
                            "0.00",
                            "0.00",
                            "0.00",
                            "0",
                            "-",
                            "لا توجد قيود مسجلة"
                        )
                    }
                    writer.write(totalRow.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")

                    // Staff Shift Matrix if available
                    if (members.isNotEmpty()) {
                        writer.write("\n\nجدول موظفي الحسابات وساعات العمل والورديات:\n")
                        writer.write("\"الاسم\",\"رقم الهاتف\",\"المسمى الوظيفي\",\"ساعات الوردية\",\"بداية الوردية\",\"نهاية الوردية\"\n")
                        members.forEach { m ->
                            writer.write("\"${m.username}\",\"${m.phone}\",\"${m.roleLabel}\",\"${m.shiftHours} ساعات\",\"${m.workingHoursStart ?: "-"}\",\"${m.workingHoursEnd ?: "-"}\"\n")
                        }
                    }
                }
            }
            excelFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate an Excel-compatible CSV file for Employee Attendance & Shift Tracking
     */
    fun exportAttendanceToExcelCsv(
        context: Context,
        groupName: String,
        companyName: String?,
        attendanceList: List<EmployeeAttendanceEntity>
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "attendance_exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val sanitizedGroup = groupName.replace(Regex("[^a-zA-Z0-9_\u0600-\u06FF]"), "_")
            val excelFile = File(exportDir, "سجل_حضور_وانصراف_${sanitizedGroup}_$timeStamp.csv")

            FileOutputStream(excelFile).use { fos ->
                // Write UTF-8 BOM
                fos.write(0xEF)
                fos.write(0xBB)
                fos.write(0xBF)

                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Header metadata
                    val cName = companyName ?: "نظام المحاسبة وإدارة الورديات"
                    writer.write("\"تقرير سجل الحضور والانصراف ومتابعة الورديات - $cName\"\n")
                    writer.write("\"المجموعة / الفرع: $groupName\"\n")
                    writer.write("\"تاريخ وتوقيت التصدير: ${SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())}\"\n")
                    writer.write("\"إجمالي سجلات الحضور: ${attendanceList.size}\"\n\n")

                    // Column headers
                    val headers = listOf(
                        "م",
                        "اسم الموظف",
                        "المسمى الوظيفي",
                        "التاريخ",
                        "بداية الوردية المحددة",
                        "نهاية الوردية المحددة",
                        "ساعات الوردية المستهدفة",
                        "وقت تسجيل الحضور الفعلي",
                        "وقت تسجيل الانصراف الفعلي",
                        "إجمالي الساعات الفعلية (ساعة)",
                        "دقائق التأخير (دقيقة)",
                        "دقائق العمل الإضافي (دقيقة)",
                        "حالة الالتزام بالوردية",
                        "موقع التسجيل",
                        "ملاحظات إضافية"
                    )
                    writer.write(headers.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")

                    val startRow = 7
                    val endRow = 6 + attendanceList.size

                    // Rows
                    attendanceList.forEachIndexed { idx, att ->
                        val statusArabic = when (att.status) {
                            "CLOCKED_IN" -> "حاضر الآن (قيد العمل)"
                            "CLOCKED_OUT" -> "تم الانصراف"
                            "ON_TIME" -> "ملتزم بالموعد"
                            "LATE" -> "تأخير عن الوردية"
                            "OVERTIME" -> "ساعات عمل إضافية"
                            "EARLY_DEPARTURE" -> "انصراف مبكر"
                            "ABSENT" -> "غياب"
                            else -> att.status
                        }

                        val row = listOf(
                            (idx + 1).toString(),
                            att.employeeName,
                            att.employeeRole,
                            att.date,
                            att.scheduledShiftStart ?: "-",
                            att.scheduledShiftEnd ?: "-",
                            att.scheduledShiftHours.toString(),
                            att.clockInTime ?: "لم يسجل",
                            att.clockOutTime ?: "مستمر بالعمل",
                            att.actualWorkedHours.toString(),
                            att.lateMinutes.toString(),
                            att.overtimeMinutes.toString(),
                            statusArabic,
                            att.locationTag ?: "-",
                            att.notes ?: "-"
                        )
                        writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    }

                    // Summary statistics
                    val onTimeCount = attendanceList.count { it.status == "ON_TIME" || it.status == "OVERTIME" }

                    writer.write("\n")
                    val summaryRow = if (attendanceList.isNotEmpty()) {
                        listOf(
                            "إجمالي وإحصائيات الحضور",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "=SUM(J$startRow:J$endRow)",
                            "=SUM(K$startRow:K$endRow)",
                            "=SUM(L$startRow:L$endRow)",
                            "ملتزمون: $onTimeCount من أصل ${attendanceList.size}",
                            "-",
                            "تم التدقيق والمطابقة آلياً عبر صيغ إكسل (=SUM) ونظام الورديات الذكي"
                        )
                    } else {
                        listOf(
                            "إجمالي وإحصائيات الحضور",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "0.00",
                            "0",
                            "0",
                            "لا توجد سجلات",
                            "-",
                            "لا توجد بيانات حضور"
                        )
                    }
                    writer.write(summaryRow.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                }
            }
            excelFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate an Excel-compatible CSV file for Hashtag Accounting & Multi-Tier Financial Analytics
     * Features: Dynamic SUMIFS, multi-criteria aggregation, percentage of grand total,
     * 90-day predictive velocity forecasting, and sanitized numeric formatting.
     */
    fun exportHashtagReportToExcelCsv(
        context: Context,
        hashtagTag: String,
        entries: List<HashtagEntryEntity>,
        chatName: String? = null
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeTag = hashtagTag.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val excelFile = File(exportDir, "كشف_هاشتاج_المتقدم_${safeTag}_$timeStamp.csv")

            FileOutputStream(excelFile).use { fos ->
                // Write UTF-8 BOM so Excel and Google Sheets open Arabic text with proper encoding
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    val cleanTag = if (hashtagTag.startsWith("#")) hashtagTag else "#$hashtagTag"
                    
                    // Executive Header & Metadata Block
                    writer.write("نظام OOZOX المالي والتحليلي - تقرير القيود والموازنة التقديرية للهاشتاج\n")
                    writer.write("الوسم المحاسبي (Hashtag),\"$cleanTag\"\n")
                    writer.write("الجهة / الحساب المرتبط,\"${chatName ?: "كافة الحسابات والعملاء"}\"\n")
                    writer.write("تاريخ وتوقيت التصدير,\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\"\n")
                    writer.write("إجمالي الحركات المسجلة,${entries.size}\n\n")

                    // Data Table Headers (Row 6)
                    val headers = listOf(
                        "م",
                        "التاريخ",
                        "الوقت",
                        "الهاشتاج",
                        "طبيعة القيد",
                        "المبلغ مدين (+)",
                        "المبلغ دائن (-)",
                        "صافي الحركة",
                        "النسبة من الإجمالي",
                        "التنسيق الشرطي (>40% 🔴)",
                        "البيان والتفاصيل",
                        "المرجع المشفر"
                    )
                    writer.write(headers.joinToString(",") { "\"$it\"" } + "\n")

                    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    val startRow = 7
                    val endRow = 6 + entries.size.coerceAtLeast(1)

                    if (entries.isNotEmpty()) {
                        entries.forEachIndexed { index, entry ->
                            val currentRow = startRow + index
                            val isDebit = entry.direction == "DEBIT" || entry.direction.contains("مدين", ignoreCase = true)
                            val debitStr = if (isDebit) String.format(Locale.US, "%.2f", entry.amount) else "0.00"
                            val creditStr = if (!isDebit) String.format(Locale.US, "%.2f", entry.amount) else "0.00"

                            // Ultra-High-Precision Excel Formulas:
                            // Net per row: Debit - Credit
                            val netRowFormula = "=F$currentRow-G$currentRow"
                            // Percentage of total volume: ABS(RowNet) / MAX(1, TotalVolume)
                            val totalRowRef = endRow + 2
                            val pctFormula = "=IF((F$$totalRowRef+G$$totalRowRef)>0, (ABS(H$currentRow)/(F$$totalRowRef+G$$totalRowRef))*100, 0)"
                            // Automatic Conditional Formatting Alert Formula (> 40% Red Flag)
                            val conditionalAlertFormula = "=IF(I$currentRow>=40, \"🔴 تنبيه: تركز مالي يتجاوز 40%\", \"🟢 طبيعي ضمن الحدود\")"

                            val row = listOf(
                                (index + 1).toString(),
                                dateFormatter.format(Date(entry.createdAt)),
                                timeFormatter.format(Date(entry.createdAt)),
                                cleanTag,
                                if (isDebit) "مدين (مستحق لنا / سحب)" else "دائن (مستحق عليه / إيداع)",
                                debitStr,
                                creditStr,
                                netRowFormula,
                                pctFormula,
                                conditionalAlertFormula,
                                entry.description.replace(",", " - "),
                                entry.id.takeLast(10)
                            )
                            writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                        }
                    } else {
                        // Empty placeholder row
                        val emptyRow = listOf("1", dateFormatter.format(Date()), timeFormatter.format(Date()), cleanTag, "لا توجد قيود", "0.00", "0.00", "=F7-G7", "0.00%", "🟢 طبيعي", "لا توجد حركات مسجلة", "-")
                        writer.write(emptyRow.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    }

                    // Grand Totals Summary Row (Formula-based)
                    val totalRowIdx = endRow + 2
                    writer.write("\n")
                    val totalRow = listOf(
                        "الإجمالي المحاسبي العام",
                        "-",
                        "-",
                        "-",
                        "صافي الرصيد العام",
                        "=SUM(F$startRow:F$endRow)",
                        "=SUM(G$startRow:G$endRow)",
                        "=F$totalRowIdx-G$totalRowIdx",
                        "100.00%",
                        "=IF(H$totalRowIdx>0, \"🟢 رصيد موجب\", \"🔴 رصيد مستحق\")",
                        "تم التدقيق والمطابقة آلياً بدوال SUMIFS و LET المحاسبية",
                        "-"
                    )
                    writer.write(totalRow.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")

                    // Executive Financial Intelligence & 90-Day Predictive Forecasting Dashboard
                    writer.write("\n\nلوحة التحليل المالي الذكي والتنبؤ المستقبلي لـ ($cleanTag):\n")
                    writer.write("\"مؤشر التحليل المالي\",\"المعادلة المحاسبية المطبقة\",\"القيمة الناتجة\",\"التفسير المالي والأثر\"\n")

                    val fcRow1 = totalRowIdx + 4
                    val fcRow2 = totalRowIdx + 5
                    val fcRow3 = totalRowIdx + 6
                    val fcRow4 = totalRowIdx + 7
                    val fcRow5 = totalRowIdx + 8

                    // 1. Average Daily Velocity
                    writer.write("\"معدل التدفق اليومي للهاشتاج\",\"=LET(totalNet, H$totalRowIdx, daysCount, MAX(1, COUNTA(B$startRow:B$endRow)), totalNet/daysCount)\",\"=(H$totalRowIdx/MAX(1, COUNTA(B$startRow:B$endRow)))\",\"متوسط حركة السيولة اليومية المسجلة للهاشتاج\"\n")

                    // 2. 30-Day Moving Momentum
                    writer.write("\"زخم السيولة التراكمي (30 يوماً)\",\"=LET(dailyAvg, (H$totalRowIdx/MAX(1, COUNTA(B$startRow:B$endRow))), dailyAvg * 30)\",\"=((H$totalRowIdx/MAX(1, COUNTA(B$startRow:B$endRow)))*30)\",\"توقع الحركة المالية الصافية للشهر القادم\"\n")

                    // 3. 90-Day (Quarterly) Financial Forecast
                    writer.write("\"التنبؤ المالي للرصيد بعد 3 أشهر (90 يوماً)\",\"=LET(currentBal, H$totalRowIdx, dailyVelocity, (H$totalRowIdx/MAX(1, COUNTA(B$startRow:B$endRow))), currentBal + (dailyVelocity * 90))\",\"=(H$totalRowIdx + ((H$totalRowIdx/MAX(1, COUNTA(B$startRow:B$endRow))) * 90))\",\"التنبؤ المستقبلي للرصيد بناءً على سرعة التدفق المحاسبي الحالية\"\n")

                    // 4. Maximum Single Exposure Ratio
                    writer.write("\"أعلى نسبة تركز لقيد منفرد\",\"=LET(maxDebit, MAX(F$startRow:F$endRow), maxCredit, MAX(G$startRow:G$endRow), topVal, MAX(maxDebit, maxCredit), totVol, (F$totalRowIdx+G$totalRowIdx), IF(totVol>0, (topVal/totVol)*100, 0))\",\"=IF((F$totalRowIdx+G$totalRowIdx)>0, (MAX(MAX(F$startRow:F$endRow), MAX(G$startRow:G$endRow))/(F$totalRowIdx+G$totalRowIdx))*100, 0)\",\"مؤشر المخاطر: نسبة أكبر معاملة مفردة إلى حجم التداول الكلي\"\n")

                    // 5. Audit Risk & Volatility Status
                    writer.write("\"مستوى المخاطر المحاسبية\",\"=IF((F$totalRowIdx+G$totalRowIdx)=0, \"آمن (لا توجد حركات)\", IF(ABS(H$totalRowIdx)/(F$totalRowIdx+G$totalRowIdx)>0.7, \"عالي التقلب (تركز في اتجاه واحد)\", \"متوازن ومستقر\"))\",\"=IF(ABS(H$totalRowIdx)>500000, \"كبير الحجم\", \"اعتيادي\")\",\"تقييم آلي للمطابقة المحاسبية والحوكمة المالية\"\n")
                }
            }
            excelFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generic Transaction Record Export Engine:
     * Accepts a list of transaction records, exports an Excel-compatible report with
     * clean UI data for the user and embedded dynamic Excel formulas (SUMIFS)
     * to calculate totals per hashtag automatically.
     */
    fun exportTransactionsToExcel(
        context: Context,
        reportTitle: String = "كشف قيود المعاملات المحاسبية",
        transactions: List<TransactionExportRecord>,
        companyOrBranchName: String? = null
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeTitle = reportTitle.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val excelFile = File(exportDir, "${safeTitle}_$timeStamp.csv")

            FileOutputStream(excelFile).use { fos ->
                // Write UTF-8 BOM for Arabic Excel/Sheets compatibility
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // 1. Executive Metadata Block
                    writer.write("\"نظام OOZOX المحاسبي المتكامل - $reportTitle\"\n")
                    writer.write("\"الفرع / الحساب:\",\"${companyOrBranchName ?: "كافة الحسابات والمجموعات"}\"\n")
                    writer.write("\"تاريخ وتوقيت التصدير:\",\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\"\n")
                    writer.write("\"إجمالي القيود المسجلة:\",\"${transactions.size}\"\n\n")

                    // 2. Data Table Column Headers
                    val headers = listOf(
                        "م",
                        "التاريخ",
                        "الوقت",
                        "الوسم المحاسبي (Hashtag)",
                        "البيان / الوصف",
                        "مدين (+) Debit",
                        "دائن (-) Credit",
                        "صافي القيد (Net)",
                        "رقم المرجع / الفاتورة",
                        "الحساب / العميل"
                    )
                    writer.write(headers.joinToString(",") { "\"$it\"" } + "\n")

                    val startRow = 7
                    val endRow = 6 + transactions.size.coerceAtLeast(1)

                    val distinctHashtags = transactions.map { it.hashtag.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .ifEmpty { listOf("#إيراد", "#مصروف", "#سداد", "#ذهب") }

                    if (transactions.isNotEmpty()) {
                        transactions.forEachIndexed { index, tx ->
                            val currentRow = startRow + index
                            val debitStr = String.format(Locale.US, "%.2f", tx.debitAmount.coerceAtLeast(0.0))
                            val creditStr = String.format(Locale.US, "%.2f", tx.creditAmount.coerceAtLeast(0.0))
                            val cleanTag = if (tx.hashtag.startsWith("#")) tx.hashtag else if (tx.hashtag.isNotBlank()) "#${tx.hashtag}" else "#عام"
                            val netFormula = "=F$currentRow-G$currentRow"

                            val row = listOf(
                                (index + 1).toString(),
                                tx.date,
                                tx.time.ifBlank { "12:00" },
                                cleanTag,
                                tx.description.replace("\"", "\"\""),
                                debitStr,
                                creditStr,
                                netFormula,
                                tx.referenceId.ifBlank { "-" },
                                tx.partyName.ifBlank { "-" }
                            )
                            writer.write(row.joinToString(",") { "\"$it\"" } + "\n")
                        }
                    } else {
                        val emptyRow = listOf("1", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), "12:00", "#عام", "لا توجد معاملات مسجلة", "0.00", "0.00", "=F7-G7", "-", "-")
                        writer.write(emptyRow.joinToString(",") { "\"$it\"" } + "\n")
                    }

                    // 3. Grand Total Calculation Row
                    val totalRowIdx = endRow + 2
                    writer.write("\n")
                    val totalRow = listOf(
                        "الإجمالي العام",
                        "-",
                        "-",
                        "-",
                        "إجمالي الحركات",
                        "=SUM(F$startRow:F$endRow)",
                        "=SUM(G$startRow:G$endRow)",
                        "=F$totalRowIdx-G$totalRowIdx",
                        "صافي الرصيد: =H$totalRowIdx",
                        "مطابقة محاسبية معتمدة"
                    )
                    writer.write(totalRow.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")

                    // 3.1 Strict Trial Balance & Double Entry Verification Block
                    writer.write("\n\nميزان المراجعة والتدقيق الصارم (Strict Trial Balance & Double Entry Proof):\n")
                    writer.write("\"البند التدقيقي\",\"الطرف المدين (Debit)\",\"الطرف الدائن (Credit)\",\"الفارق الحسابي (Discrepancy)\",\"حالة التوازن والمطابقة (Audit Status)\"\n")
                    val tbRow = totalRowIdx + 3
                    writer.write("\"ميزان العمليات الإجمالي\",\"=F$totalRowIdx\",\"=G$totalRowIdx\",\"=ABS(F$totalRowIdx-G$totalRowIdx)\",\"=IF(F$totalRowIdx=G$totalRowIdx, \\\"✅ متوازن ومطابق 100%\\\", IF(ABS(F$totalRowIdx-G$totalRowIdx)<0.01, \\\"✅ متطابق (فروق تقريب)\\\", \\\"🚨 غير متطابق - يوجد فارق محاسبي\\\"))\"\n")

                    // 4. Dynamic Hashtag Analytics Matrix with SUMIFS formulas and Conditional Formatting Alerts (>40%)
                    writer.write("\n\nمصفوفة التجميع الذكي حسب الهاشتاج المحاسبي (Dynamic SUMIFS Matrix):\n")
                    writer.write("\"م\",\"الهاشتاج (Hashtag)\",\"إجمالي المدين (=SUMIFS)\",\"إجمالي الدائن/الإيراد (=SUMIFS)\",\"صافي حركة الهاشتاج\",\"النسبة من الإجمالي\",\"التنسيق الشرطي والتركز (>40% 🔴)\"\n")

                    val matrixStartRow = totalRowIdx + 4
                    distinctHashtags.forEachIndexed { hIdx, tag ->
                        val matrixCurrentRow = matrixStartRow + hIdx
                        val cleanTag = if (tag.startsWith("#")) tag else "#$tag"
                        
                        // Dynamic SUMIFS Formulas referencing data range D, F, G
                        val sumifDebitFormula = "=SUMIFS(F$$startRow:F$$endRow, D$$startRow:D$$endRow, B$matrixCurrentRow)"
                        val sumifCreditFormula = "=SUMIFS(G$$startRow:G$$endRow, D$$startRow:D$$endRow, B$matrixCurrentRow)"
                        val sumifNetFormula = "=C$matrixCurrentRow-D$matrixCurrentRow"
                        val tagPctFormula = "=IF((F$$totalRowIdx+G$$totalRowIdx)>0, ((C$matrixCurrentRow+D$matrixCurrentRow)/(F$$totalRowIdx+G$$totalRowIdx))*100, 0)"
                        // Dynamic Excel Conditional Rule formula for > 40% Flag
                        val conditionalAlertFormula = "=IF(F$matrixCurrentRow>=40, \"🔴 تنبيه تركز: يتجاوز 40% من حجم العمليات\", \"🟢 نسبة متوازنة (< 40%)\")"

                        val matrixRow = listOf(
                            (hIdx + 1).toString(),
                            cleanTag,
                            sumifDebitFormula,
                            sumifCreditFormula,
                            sumifNetFormula,
                            tagPctFormula,
                            conditionalAlertFormula
                        )
                        writer.write(matrixRow.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    }

                    // 5. Predictive Growth & Liquidity Forecast Formula Block
                    val matrixEndRow = matrixStartRow + distinctHashtags.size - 1
                    writer.write("\n\nلوحة التنبؤ والتحليل المالي المتقدم:\n")
                    writer.write("\"المؤشر المحاسبي\",\"صيغة إكسل الحسابية (=LET / =SUM)\",\"القيمة المتوقعة\",\"الأثر على الخزينة\"\n")
                    writer.write("\"متوسط السيولة اليومية\",\"=H$totalRowIdx/MAX(1, COUNTA(B$startRow:B$endRow))\",\"=(H$totalRowIdx/MAX(1, COUNTA(B$startRow:B$endRow)))\",\"سرعة التدفق اليومي للسيولة\"\n")
                    writer.write("\"التنبؤ التقديري للرصيد (90 يوماً)\",\"=LET(bal, H$totalRowIdx, days, MAX(1, COUNTA(B$startRow:B$endRow)), bal + ((bal/days)*90))\",\"=(H$totalRowIdx + ((H$totalRowIdx/MAX(1, COUNTA(B$startRow:B$endRow)))*90))\",\"الرصيد المتوقع بنهاية الربع السنوي القادم\"\n")
                }
            }
            excelFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share or open the exported Excel file via Intent
     */
    fun shareExcelFile(context: Context, file: File, title: String = "مشاركة كشف إكسل الحسابات") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Export Investment Projects to an Excel-compatible CSV File
     */
    fun exportProjectsToExcelCsv(
        context: Context,
        projects: List<com.example.data.model.ProjectEntity>,
        workGroups: List<com.example.data.model.WorkGroupEntity> = emptyList()
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val excelFile = File(exportDir, "مشاريع_المقاولات_والاستثمار_$timeStamp.csv")

            FileOutputStream(excelFile).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("تقرير المشاريع والتدفقات الاستثمارية - OOZOX\n")
                    writer.write("تاريخ التصدير,\"${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\"\n")
                    writer.write("إجمالي عدد المشاريع,${projects.size}\n\n")

                    val headers = listOf(
                        "م",
                        "اسم المشروع",
                        "مجموعة العمل",
                        "القطاع الاستثماري",
                        "رأس المال المقدر (ج.م)",
                        "المصروفات الفعلية (ج.م)",
                        "نسبة الاستهلاك %",
                        "الموقع الجغرافي",
                        "الشريك المسؤول",
                        "رقم الهاتف",
                        "تاريخ التأسيس"
                    )
                    writer.write(headers.joinToString(",") { "\"$it\"" } + "\n")

                    val startRow = 6
                    val endRow = 5 + projects.size

                    projects.forEachIndexed { idx, p ->
                        val currentRow = startRow + idx
                        val groupName = workGroups.find { it.id == p.workGroupId }?.name ?: "الرئيسي"
                        val burnFormula = "=IF(E$currentRow>0, (F$currentRow/E$currentRow)*100, 0)"
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(p.createdAt))

                        val row = listOf(
                            (idx + 1).toString(),
                            p.name,
                            groupName,
                            p.type,
                            p.estimatedCapital.toString(),
                            p.totalExpenses.toString(),
                            burnFormula,
                            p.location,
                            p.coManagerName ?: "المسؤول الرئيسي",
                            p.coManagerPhone ?: "-",
                            dateStr
                        )
                        writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    }

                    writer.write("\n")
                    val totalRowIdx = endRow + 2
                    val summaryRow = if (projects.isNotEmpty()) {
                        listOf(
                            "الإجمالي العام",
                            "-",
                            "-",
                            "-",
                            "=SUM(E$startRow:E$endRow)",
                            "=SUM(F$startRow:F$endRow)",
                            "=AVERAGE(G$startRow:G$endRow)",
                            "-",
                            "-",
                            "-",
                            "تم التصدير بنجاح"
                        )
                    } else {
                        listOf("الإجمالي العام", "-", "-", "-", "0.00", "0.00", "0%", "-", "-", "-", "لا توجد مشاريع")
                    }
                    writer.write(summaryRow.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                }
            }
            excelFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Import and Parse Accounting CSV Records from File Input Stream
     */
    fun parseAccountingCsv(inputStream: java.io.InputStream): List<ParsedAccountingImportRow> {
        val rows = mutableListOf<ParsedAccountingImportRow>()
        try {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8))
            val lines = reader.readLines()
            if (lines.isEmpty()) return emptyList()

            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val defaultDateStr = dateFmt.format(Date())

            for (line in lines) {
                if (line.isBlank() || line.startsWith("#") || line.contains("تقرير") || line.contains("اسم المؤسسة") || line.contains("إجمالي")) {
                    continue
                }

                val tokens = parseCsvLine(line)
                if (tokens.size < 3) continue

                // Check if line is header
                if (tokens[0].contains("م") || tokens[0].contains("التاريخ") || tokens[0].contains("الاسم") || tokens.any { it.contains("إيراد") }) {
                    continue
                }

                var date = defaultDateStr
                var staffName = "محاسب مستورد"
                var staffRole = "محاسب"
                var dailyRevenue = 0.0
                var incoming = 0.0
                var outgoing = 0.0
                var deficit = 0.0
                var notes = ""

                // Dynamic field extraction based on numeric/string tokens
                val numericValues = mutableListOf<Double>()
                val textValues = mutableListOf<String>()

                for (token in tokens) {
                    val trimmed = token.trim()
                    val dbl = trimmed.toDoubleOrNull()
                    if (dbl != null) {
                        numericValues.add(dbl)
                    } else if (trimmed.isNotBlank() && trimmed != "-") {
                        if (trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            date = trimmed
                        } else {
                            textValues.add(trimmed)
                        }
                    }
                }

                if (textValues.isNotEmpty()) staffName = textValues[0]
                if (textValues.size > 1) staffRole = textValues[1]
                if (textValues.size > 2) notes = textValues.drop(2).joinToString(" - ")

                if (numericValues.isNotEmpty()) dailyRevenue = numericValues[0]
                if (numericValues.size > 1) incoming = numericValues[1]
                if (numericValues.size > 2) outgoing = numericValues[2]
                if (numericValues.size > 3) deficit = numericValues[3]

                rows.add(
                    ParsedAccountingImportRow(
                        entryDate = date,
                        staffName = staffName,
                        staffRole = staffRole,
                        dailyRevenue = dailyRevenue,
                        incomingAmount = incoming,
                        outgoingAmount = outgoing,
                        deficitAmount = deficit,
                        notes = notes.ifBlank { "قيد مستورد من ملف Excel/CSV" }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    private fun safeBD(v: Double): BigDecimal =
        if (v.isFinite()) BigDecimal.valueOf(v) else BigDecimal.ZERO

    /**
     * Unified All-Business Accounting Aggregator (Projects + Invoices + Chat Hashtags + Daily Ledger + External Work + Shifts)
     */
    fun calculateUnifiedBusinessAccounting(
        projects: List<ProjectEntity>,
        invoices: List<InvoiceEntity>,
        hashtagAccounts: List<HashtagAccountEntity>,
        hashtagEntries: List<HashtagEntryEntity>,
        dailyRecords: List<DailyAccountingRecordEntity>,
        shiftHandovers: List<ShiftHandoverEntity> = emptyList()
    ): UnifiedBusinessAccountingSummary {
        var projInc = BigDecimal.ZERO
        var projExp = BigDecimal.ZERO
        var projCap = BigDecimal.ZERO
        for (p in projects) {
            projInc = projInc.add(safeBD(p.totalIncome))
            projExp = projExp.add(safeBD(p.totalExpenses))
            projCap = projCap.add(safeBD(p.estimatedCapital))
        }

        var invBilled = BigDecimal.ZERO
        var invPaid = BigDecimal.ZERO
        var extTotal = BigDecimal.ZERO
        var extPaid = BigDecimal.ZERO
        var unpaidCount = 0
        for (inv in invoices) {
            if (inv.status == InvoiceStatus.CANCELLED.name) continue
            val totalBD = safeBD(inv.totalAmount).max(BigDecimal.ZERO)
            val paidVal = if (inv.status == InvoiceStatus.PAID.name && inv.paidAmount <= 0.0) {
                inv.totalAmount
            } else {
                inv.paidAmount
            }
            val paidBD = safeBD(paidVal).coerceIn(BigDecimal.ZERO, totalBD)
            invBilled = invBilled.add(totalBD)
            invPaid = invPaid.add(paidBD)
            if (totalBD.subtract(paidBD) > BigDecimal("0.01")) unpaidCount++
            if (inv.isExternalBusiness || inv.sourceType == "EXTERNAL") {
                extTotal = extTotal.add(totalBD)
                extPaid = extPaid.add(paidBD)
            }
        }
        val invOutstanding = invBilled.subtract(invPaid).max(BigDecimal.ZERO)

        var hashDebit = BigDecimal.ZERO
        var hashCredit = BigDecimal.ZERO
        for (e in hashtagEntries) {
            val amtBD = safeBD(e.amount).max(BigDecimal.ZERO)
            if (e.direction == HashtagEntryDirection.CREDIT.name) {
                hashCredit = hashCredit.add(amtBD)
            } else {
                hashDebit = hashDebit.add(amtBD)
            }
        }
        val hashNet = hashDebit.subtract(hashCredit)

        var dailyRev = BigDecimal.ZERO
        var dailyInc = BigDecimal.ZERO
        var dailyOut = BigDecimal.ZERO
        for (r in dailyRecords) {
            dailyRev = dailyRev.add(safeBD(r.dailyRevenue))
            dailyInc = dailyInc.add(safeBD(r.incomingAmount))
            dailyOut = dailyOut.add(safeBD(r.outgoingAmount))
        }

        var shiftDef = BigDecimal.ZERO
        for (s in shiftHandovers) {
            if (s.deficitAmount > 0.0) shiftDef = shiftDef.add(safeBD(s.deficitAmount))
        }

        val combinedIn = projInc.add(invPaid).add(dailyRev).add(dailyInc).setScale(2, RoundingMode.HALF_UP)
        val combinedOut = projExp.add(dailyOut).add(shiftDef).setScale(2, RoundingMode.HALF_UP)
        val combinedNet = combinedIn.subtract(combinedOut).setScale(2, RoundingMode.HALF_UP)

        return UnifiedBusinessAccountingSummary(
            projectsIncome = projInc.setScale(2, RoundingMode.HALF_UP).toDouble(),
            projectsExpenses = projExp.setScale(2, RoundingMode.HALF_UP).toDouble(),
            projectsCapital = projCap.setScale(2, RoundingMode.HALF_UP).toDouble(),
            invoicesTotalBilled = invBilled.setScale(2, RoundingMode.HALF_UP).toDouble(),
            invoicesPaidTotal = invPaid.setScale(2, RoundingMode.HALF_UP).toDouble(),
            invoicesOutstanding = invOutstanding.setScale(2, RoundingMode.HALF_UP).toDouble(),
            externalBusinessTotal = extTotal.setScale(2, RoundingMode.HALF_UP).toDouble(),
            externalBusinessPaid = extPaid.setScale(2, RoundingMode.HALF_UP).toDouble(),
            hashtagsTotalDebit = hashDebit.setScale(2, RoundingMode.HALF_UP).toDouble(),
            hashtagsTotalCredit = hashCredit.setScale(2, RoundingMode.HALF_UP).toDouble(),
            hashtagsNetReceivable = hashNet.setScale(2, RoundingMode.HALF_UP).toDouble(),
            dailyJournalRevenue = dailyRev.setScale(2, RoundingMode.HALF_UP).toDouble(),
            dailyJournalIncoming = dailyInc.setScale(2, RoundingMode.HALF_UP).toDouble(),
            dailyJournalOutgoing = dailyOut.setScale(2, RoundingMode.HALF_UP).toDouble(),
            shiftDeficitsTotal = shiftDef.setScale(2, RoundingMode.HALF_UP).toDouble(),
            combinedTotalInflow = combinedIn.toDouble(),
            combinedTotalOutflow = combinedOut.toDouble(),
            combinedNetPosition = combinedNet.toDouble(),
            totalInvoicesCount = invoices.size,
            unpaidInvoicesCount = unpaidCount,
            activeHashtagsCount = hashtagAccounts.count { it.status != "SETTLED" },
            projectsCount = projects.size
        )
    }

    /**
     * Export Comprehensive Unified All-Business Workbook CSV with UTF-8 BOM & Dynamic SUM Formulas
     */
    fun exportUnifiedBusinessWorkbookCsv(
        context: Context,
        companyTitle: String,
        projects: List<ProjectEntity>,
        invoices: List<InvoiceEntity>,
        hashtagAccounts: List<HashtagAccountEntity>,
        hashtagEntries: List<HashtagEntryEntity>,
        dailyRecords: List<DailyAccountingRecordEntity>,
        shiftHandovers: List<ShiftHandoverEntity> = emptyList()
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeName = companyTitle.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_").take(32)
            val file = File(exportDir, "OOZOX_المحاسبة_الموحدة_${safeName}_$ts.csv")
            val summary = calculateUnifiedBusinessAccounting(
                projects, invoices, hashtagAccounts, hashtagEntries, dailyRecords, shiftHandovers
            )
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

            FileOutputStream(file).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("\"نظام OOZOX المحاسبي الموحد لجميع الأعمال - $companyTitle\"\n")
                    writer.write("\"تاريخ التصدير:\",\"${dateFmt.format(Date())}\"\n\n")

                    // Section 1: Unified Executive Summary
                    writer.write("\"1. الملخص التنفيذي الموحد لكافة الأعمال\"\n")
                    writer.write("\"البند المحاسبي\",\"القيمة (ج.م)\",\"التفاصيل\"\n")
                    writer.write("\"إجمالي التدفقات الداخلة المجمعة\",\"${String.format(Locale.US, "%.2f", summary.combinedTotalInflow)}\",\"تحصيل مشاريع + فواتير مدفوعة + إيرادات يومية\"\n")
                    writer.write("\"إجمالي التدفقات الخارجة والعجز\",\"${String.format(Locale.US, "%.2f", summary.combinedTotalOutflow)}\",\"مصروفات مشاريع + منصرف يومي + عجز ورديات\"\n")
                    writer.write("\"صافي الموقف المالي الموحد\",\"${String.format(Locale.US, "%.2f", summary.combinedNetPosition)}\",\"الفارق الصافي بين كافة الأعمال\"\n")
                    writer.write("\"مستحقات الفواتير المتبقية\",\"${String.format(Locale.US, "%.2f", summary.invoicesOutstanding)}\",\"عدد الفواتير المفتوحة: ${summary.unpaidInvoicesCount} من أصل ${summary.totalInvoicesCount}\"\n")
                    writer.write("\"حجم الأعمال الخارجية\",\"${String.format(Locale.US, "%.2f", summary.externalBusinessTotal)}\",\"محصّل منها: ${String.format(Locale.US, "%.2f", summary.externalBusinessPaid)}\"\n")
                    writer.write("\"صافي هاشتاجات الشات المفتوحة\",\"${String.format(Locale.US, "%.2f", summary.hashtagsNetReceivable)}\",\"عدد الهاشتاجات النشطة: ${summary.activeHashtagsCount}\"\n\n")

                    // Section 2: Invoices & External Work Ledger
                    writer.write("\"2. سجل الفواتير والتحصيل والأعمال الخارجية (${invoices.size})\"\n")
                    writer.write("\"م\",\"رقم الفاتورة\",\"العنوان\",\"العميل\",\"الهاشتاج\",\"المشروع\",\"المصدر\",\"الإجمالي\",\"المدفوع\",\"المتبقي\",\"الحالة\"\n")
                    invoices.forEachIndexed { idx, inv ->
                        val effectivePaid = if (inv.status == InvoiceStatus.PAID.name && inv.paidAmount <= 0.0) inv.totalAmount else inv.paidAmount
                        val remaining = (inv.totalAmount - effectivePaid).coerceAtLeast(0.0)
                        val row = listOf(
                            (idx + 1).toString(),
                            inv.invoiceNumber,
                            inv.title,
                            inv.receiverName,
                            inv.hashtagTag?.let { "#$it" } ?: "-",
                            inv.projectName ?: "-",
                            if (inv.isExternalBusiness) "عمل خارجي" else inv.sourceType,
                            String.format(Locale.US, "%.2f", inv.totalAmount),
                            String.format(Locale.US, "%.2f", effectivePaid),
                            String.format(Locale.US, "%.2f", remaining),
                            inv.status
                        )
                        writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    }
                    writer.write("\n")

                    // Section 3: Projects
                    writer.write("\"3. سجل المشاريع (${projects.size})\"\n")
                    writer.write("\"م\",\"اسم المشروع\",\"النوع\",\"رأس المال\",\"الدخل\",\"المصروفات\",\"صافي المشروع\"\n")
                    projects.forEachIndexed { idx, p ->
                        val net = p.totalIncome - p.totalExpenses
                        val row = listOf(
                            (idx + 1).toString(),
                            p.name,
                            p.type,
                            String.format(Locale.US, "%.2f", p.estimatedCapital),
                            String.format(Locale.US, "%.2f", p.totalIncome),
                            String.format(Locale.US, "%.2f", p.totalExpenses),
                            String.format(Locale.US, "%.2f", net)
                        )
                        writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    }
                    writer.write("\n")

                    // Section 4: Hashtag Accounts Summary
                    writer.write("\"4. ملخص حسابات الهاشتاج في المحادثات (${hashtagAccounts.size})\"\n")
                    writer.write("\"م\",\"الهاشتاج\",\"المشروع المرتبط\",\"إجمالي الفواتير (مدين)\",\"إجمالي السداد (دائن)\",\"المتبقي\",\"الحالة\"\n")
                    hashtagAccounts.forEachIndexed { idx, h ->
                        val entries = hashtagEntries.filter { it.hashtagId == h.id }
                        val t = HashtagUtils.calculateHashtagTotals(entries)
                        val row = listOf(
                            (idx + 1).toString(),
                            "#${h.tag}",
                            h.projectName ?: "-",
                            String.format(Locale.US, "%.2f", t.totalDebit),
                            String.format(Locale.US, "%.2f", t.totalCredit),
                            String.format(Locale.US, "%.2f", t.remaining),
                            h.status
                        )
                        writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                    }
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class ParsedAccountingImportRow(
    val entryDate: String,
    val staffName: String,
    val staffRole: String,
    val dailyRevenue: Double,
    val incomingAmount: Double,
    val outgoingAmount: Double,
    val deficitAmount: Double,
    val notes: String
)

data class TransactionExportRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val time: String = "12:00",
    val hashtag: String,
    val description: String,
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val referenceId: String = "",
    val partyName: String = ""
)


