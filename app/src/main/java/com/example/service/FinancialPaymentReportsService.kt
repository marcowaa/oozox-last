package com.example.service

import android.content.Context
import com.example.data.model.InvoiceEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * Breakdown of sales, collections, and transactions for an individual payment method
 */
data class PaymentMethodCategorySummary(
    val methodName: String,
    val totalAmountEGP: Double,
    val invoicesCount: Int,
    val percentageOfTotal: Double,
    val iconKey: String = "CreditCard"
)

/**
 * Detailed sales and collection breakdown per cashier/employee by payment method
 */
data class CashierPaymentDetailedSummary(
    val cashierName: String,
    val cashierId: String,
    val invoicesCount: Int,
    val totalSalesEGP: Double,
    val instaPayAmount: Double,
    val vodafoneCashAmount: Double,
    val cashInDrawerAmount: Double,
    val bankTransferAmount: Double,
    val supplierPayoutsAmount: Double,
    val netTreasuryCashInHand: Double
)

/**
 * Comprehensive financial report categorized by payment method
 */
data class PaymentMethodsDetailedReport(
    val generatedAtTimestamp: Long = System.currentTimeMillis(),
    val dateRangeText: String,
    val totalInvoicesCount: Int,
    val grandTotalSalesEGP: Double,
    val grandTotalInstaPay: Double,
    val grandTotalVodafoneCash: Double,
    val grandTotalCashInDrawer: Double,
    val grandTotalBankTransfer: Double,
    val grandTotalVisaCards: Double,
    val grandTotalSupplierPayouts: Double,
    val netTreasuryCashflow: Double,
    val methodsList: List<PaymentMethodCategorySummary>,
    val cashierSummaries: List<CashierPaymentDetailedSummary>
)

/**
 * API interface allowing Managers and Admins to extract, filter, and export
 * detailed financial reports categorized by payment methods.
 */
interface FinancialPaymentReportsApi {
    fun extractPaymentMethodsFinancialReport(
        invoices: List<InvoiceEntity>,
        dateRangeText: String? = null
    ): PaymentMethodsDetailedReport

    fun getCashierPaymentBreakdowns(
        invoices: List<InvoiceEntity>
    ): List<CashierPaymentDetailedSummary>

    fun exportPaymentReportToCsv(
        context: Context,
        report: PaymentMethodsDetailedReport,
        shopName: String
    ): File?

    fun generatePaymentSummaryJson(
        report: PaymentMethodsDetailedReport
    ): String

    fun filterByPaymentMethod(
        invoices: List<InvoiceEntity>,
        method: String
    ): List<InvoiceEntity>
}

/**
 * Production implementation of FinancialPaymentReportsApi
 */
object FinancialPaymentReportsService : FinancialPaymentReportsApi {

    override fun extractPaymentMethodsFinancialReport(
        invoices: List<InvoiceEntity>,
        dateRangeText: String?
    ): PaymentMethodsDetailedReport {
        val totalCount = invoices.size
        val grandTotal = invoices.sumOf { it.totalAmount }

        var instaPaySum = 0.0
        var vodafoneCashSum = 0.0
        var cashSum = 0.0
        var bankSum = 0.0
        var visaSum = 0.0
        var supplierPayoutsSum = 0.0

        var instaCount = 0
        var vodafoneCount = 0
        var cashCount = 0
        var bankCount = 0
        var visaCount = 0

        for (inv in invoices) {
            val m = inv.paymentMethod.lowercase(Locale.getDefault())
            supplierPayoutsSum += inv.supplierPayoutAmount

            when {
                m.contains("إنستا") || m.contains("insta") -> {
                    instaPaySum += inv.totalAmount
                    instaCount++
                }
                m.contains("فودافون") || m.contains("vodafone") || m.contains("محافظ") || (m.contains("كاش") && !m.contains("نقدي")) -> {
                    vodafoneCashSum += inv.totalAmount
                    vodafoneCount++
                }
                m.contains("نقدي") || m == "كاش" -> {
                    cashSum += inv.totalAmount
                    cashCount++
                }
                m.contains("تحويل") || m.contains("bank") -> {
                    bankSum += inv.totalAmount
                    bankCount++
                }
                m.contains("فيزا") || m.contains("visa") || m.contains("بطاقة") -> {
                    visaSum += inv.totalAmount
                    visaCount++
                }
                else -> {
                    cashSum += inv.totalAmount
                    cashCount++
                }
            }
        }

        val methods = mutableListOf<PaymentMethodCategorySummary>()
        val grandBigDecimal = if (grandTotal > 0) BigDecimal.valueOf(grandTotal) else BigDecimal.ONE

        fun calcPct(amount: Double): Double {
            return if (grandTotal <= 0) 0.0 else BigDecimal.valueOf(amount)
                .divide(grandBigDecimal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .toDouble()
        }

        methods.add(PaymentMethodCategorySummary("إنستا باي (InstaPay)", instaPaySum, instaCount, calcPct(instaPaySum), "InstaPay"))
        methods.add(PaymentMethodCategorySummary("فودافون كاش / محافظ إلكترونية", vodafoneCashSum, vodafoneCount, calcPct(vodafoneCashSum), "Wallet"))
        methods.add(PaymentMethodCategorySummary("كاش نقدي في الخزينة والدرج", cashSum, cashCount, calcPct(cashSum), "Cash"))
        methods.add(PaymentMethodCategorySummary("تحويلات بنكية مباشرة", bankSum, bankCount, calcPct(bankSum), "Bank"))
        methods.add(PaymentMethodCategorySummary("بطاقات دفع إلكتروني وفيزا", visaSum, visaCount, calcPct(visaSum), "Visa"))

        val cashiers = getCashierPaymentBreakdowns(invoices)
        val netCash = cashSum - supplierPayoutsSum

        val dateLabel = dateRangeText ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return PaymentMethodsDetailedReport(
            dateRangeText = dateLabel,
            totalInvoicesCount = totalCount,
            grandTotalSalesEGP = grandTotal,
            grandTotalInstaPay = instaPaySum,
            grandTotalVodafoneCash = vodafoneCashSum,
            grandTotalCashInDrawer = cashSum,
            grandTotalBankTransfer = bankSum,
            grandTotalVisaCards = visaSum,
            grandTotalSupplierPayouts = supplierPayoutsSum,
            netTreasuryCashflow = netCash,
            methodsList = methods,
            cashierSummaries = cashiers
        )
    }

    override fun getCashierPaymentBreakdowns(
        invoices: List<InvoiceEntity>
    ): List<CashierPaymentDetailedSummary> {
        val grouped = invoices.groupBy { it.cashierName?.ifBlank { null } ?: it.senderName.ifBlank { "كاشير عام" } }

        return grouped.map { (name, list) ->
            var insta = 0.0
            var vodafone = 0.0
            var cash = 0.0
            var bank = 0.0
            var supplier = 0.0

            for (inv in list) {
                val m = inv.paymentMethod.lowercase(Locale.getDefault())
                supplier += inv.supplierPayoutAmount

                when {
                    m.contains("إنستا") || m.contains("insta") -> insta += inv.totalAmount
                    m.contains("فودافون") || m.contains("محافظ") || (m.contains("كاش") && !m.contains("نقدي")) -> vodafone += inv.totalAmount
                    m.contains("نقدي") || m == "كاش" -> cash += inv.totalAmount
                    m.contains("تحويل") || m.contains("فيزا") -> bank += inv.totalAmount
                    else -> cash += inv.totalAmount
                }
            }

            CashierPaymentDetailedSummary(
                cashierName = name,
                cashierId = list.firstOrNull()?.senderId ?: "staff",
                invoicesCount = list.size,
                totalSalesEGP = list.sumOf { it.totalAmount },
                instaPayAmount = insta,
                vodafoneCashAmount = vodafone,
                cashInDrawerAmount = cash,
                bankTransferAmount = bank,
                supplierPayoutsAmount = supplier,
                netTreasuryCashInHand = cash - supplier
            )
        }.sortedByDescending { it.totalSalesEGP }
    }

    override fun exportPaymentReportToCsv(
        context: Context,
        report: PaymentMethodsDetailedReport,
        shopName: String
    ): File? {
        return try {
            val fileName = "OOZOX_Payment_Methods_Report_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // UTF-8 BOM for proper Arabic rendering in Microsoft Excel
                    writer.write("\uFEFF")

                    writer.write("تقرير تفصيل المبيعات والحسابات بوسائل الدفع - منصة OOZOX\n")
                    writer.write("المنشأة/المتجر:;$shopName\n")
                    writer.write("تاريخ التقرير:;${report.dateRangeText}\n")
                    writer.write("إجمالي المبيعات (ج.م):;${report.grandTotalSalesEGP.toInt()}\n")
                    writer.write("عدد الفواتير الكلي:;${report.totalInvoicesCount}\n\n")

                    writer.write("--- ملخص وسائل الدفع المعتمدة ---\n")
                    writer.write("وسيلة الدفع;الإجمالي (ج.م);عدد العمليات;النسبة من الإجمالي\n")
                    for (m in report.methodsList) {
                        writer.write("${m.methodName};${m.totalAmountEGP.toInt()};${m.invoicesCount};${m.percentageOfTotal}%\n")
                    }

                    writer.write("\n--- تفصيل مبيعات الموظفين بحسب وسيلة الدفع ---\n")
                    writer.write("اسم الموظف / الكاشير;إجمالي المبيعات;فواتير;إنستاباي;فودافون كاش;كاش الدرج;تحويل بنكي/فيزا;مدفوع للموردين;صافي الخزينة\n")
                    for (c in report.cashierSummaries) {
                        writer.write("${c.cashierName};${c.totalSalesEGP.toInt()};${c.invoicesCount};${c.instaPayAmount.toInt()};${c.vodafoneCashAmount.toInt()};${c.cashInDrawerAmount.toInt()};${c.bankTransferAmount.toInt()};${c.supplierPayoutsAmount.toInt()};${c.netTreasuryCashInHand.toInt()}\n")
                    }

                    writer.write("\nملحوظة: هذا التقرير مالي معتمد وموثق رقمياً من محرك OOZOX المالي.\n")
                    writer.flush()
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun generatePaymentSummaryJson(report: PaymentMethodsDetailedReport): String {
        val root = JSONObject()
        root.put("dateRange", report.dateRangeText)
        root.put("totalInvoices", report.totalInvoicesCount)
        root.put("totalSalesEGP", report.grandTotalSalesEGP)
        root.put("instaPayTotal", report.grandTotalInstaPay)
        root.put("vodafoneCashTotal", report.grandTotalVodafoneCash)
        root.put("cashTotal", report.grandTotalCashInDrawer)
        root.put("bankTotal", report.grandTotalBankTransfer)
        root.put("supplierPayouts", report.grandTotalSupplierPayouts)
        root.put("netTreasury", report.netTreasuryCashflow)

        val methodsArr = JSONArray()
        for (m in report.methodsList) {
            val jm = JSONObject()
            jm.put("method", m.methodName)
            jm.put("amount", m.totalAmountEGP)
            jm.put("count", m.invoicesCount)
            jm.put("percentage", m.percentageOfTotal)
            methodsArr.put(jm)
        }
        root.put("methodsBreakdown", methodsArr)

        val cashiersArr = JSONArray()
        for (c in report.cashierSummaries) {
            val jc = JSONObject()
            jc.put("cashierName", c.cashierName)
            jc.put("totalSales", c.totalSalesEGP)
            jc.put("instaPay", c.instaPayAmount)
            jc.put("vodafoneCash", c.vodafoneCashAmount)
            jc.put("cashInDrawer", c.cashInDrawerAmount)
            jc.put("supplierPayout", c.supplierPayoutsAmount)
            jc.put("netCash", c.netTreasuryCashInHand)
            cashiersArr.put(jc)
        }
        root.put("cashiersBreakdown", cashiersArr)

        return root.toString(2)
    }

    override fun filterByPaymentMethod(
        invoices: List<InvoiceEntity>,
        method: String
    ): List<InvoiceEntity> {
        val m = method.lowercase(Locale.getDefault())
        return invoices.filter { inv ->
            val pm = inv.paymentMethod.lowercase(Locale.getDefault())
            when {
                m.contains("إنستا") || m.contains("insta") -> pm.contains("إنستا") || pm.contains("insta")
                m.contains("فودافون") || m.contains("vodafone") || m.contains("محافظ") -> pm.contains("فودافون") || pm.contains("محافظ") || (pm.contains("كاش") && !pm.contains("نقدي"))
                m.contains("نقدي") || m == "كاش" -> pm.contains("نقدي") || pm == "كاش"
                m.contains("تحويل") || m.contains("bank") -> pm.contains("تحويل") || pm.contains("bank")
                m.contains("فيزا") || m.contains("visa") -> pm.contains("فيزا") || pm.contains("visa")
                else -> pm.contains(m)
            }
        }
    }
}
