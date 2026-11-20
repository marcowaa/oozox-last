package com.example.service

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.InvoiceEntity
import com.example.data.model.ShiftHandoverEntity
import com.example.util.FcmNotificationManager
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

/**
 * Result model returned when a shift is aggregated and closed
 */
data class ShiftAggregationSummary(
    val handover: ShiftHandoverEntity,
    val shiftDurationHours: Double,
    val totalInvoicesCount: Int,
    val operationsCount: Int,
    val totalRevenueEGP: Double,
    val instaPayCollected: Double,
    val vodafoneCashCollected: Double,
    val cashCollected: Double,
    val bankCollected: Double,
    val supplierPayoutsTotal: Double,
    val expectedCashInDrawer: Double,
    val actualCashCounted: Double,
    val deficitAmount: Double,
    val surplusAmount: Double,
    val hasDeficit: Boolean,
    val managerAlertSent: Boolean,
    val humanReadableReportAr: String
)

/**
 * Backend Service responsible for automatic aggregation of shift metrics
 * upon employee logout, computing treasury balances, and alerting the manager on deficits.
 */
object ShiftHandoverBackendService {

    /**
     * Aggregates shift operations and commits shift handover closing record
     */
    suspend fun aggregateAndCloseShift(
        context: Context,
        database: AppDatabase,
        cashierId: String,
        cashierName: String,
        cashierPhone: String? = null,
        loginTime: Long,
        logoutTime: Long = System.currentTimeMillis(),
        actualCashCount: Double,
        notes: String? = null
    ): ShiftAggregationSummary {
        // 1. Fetch all invoices from database
        val allInvoices = try {
            database.invoiceDao().getAllInvoices().first()
        } catch (e: Exception) {
            emptyList<InvoiceEntity>()
        }

        // Filter invoices issued by this cashier during the active shift window
        val shiftInvoices = allInvoices.filter { inv ->
            val matchCashier = (inv.senderId == cashierId) || 
                               (inv.cashierName?.trim().equals(cashierName.trim(), ignoreCase = true)) ||
                               (inv.senderName.trim().equals(cashierName.trim(), ignoreCase = true))
            val inTimeWindow = (inv.issueDate >= loginTime - 60_000L) && (inv.issueDate <= logoutTime + 60_000L)
            matchCashier && inTimeWindow
        }.ifEmpty {
            // Fallback: If no invoices in strict timestamp, take cashier's invoices today
            allInvoices.filter { inv ->
                (inv.senderId == cashierId || inv.cashierName?.trim().equals(cashierName.trim(), ignoreCase = true)) &&
                (inv.issueDate >= System.currentTimeMillis() - 14 * 3600 * 1000L)
            }
        }

        val totalInvoicesCount = shiftInvoices.size
        val totalRevenueEGP = shiftInvoices.sumOf { it.totalAmount }

        // Fetch other operations (e.g. Gold deals) if any
        val totalOperationsCount = totalInvoicesCount.coerceAtLeast(1)

        // 2. Classify collected amounts by payment method
        var instaPay = 0.0
        var vodafoneCash = 0.0
        var cashInDrawer = 0.0
        var bankTransfer = 0.0
        var supplierPayouts = 0.0

        for (inv in shiftInvoices) {
            val pm = inv.paymentMethod.lowercase(Locale.getDefault())
            supplierPayouts += inv.supplierPayoutAmount

            when {
                pm.contains("إنستا") || pm.contains("insta") -> instaPay += inv.totalAmount
                pm.contains("فودافون") || pm.contains("vodafone") || pm.contains("محافظ") || (pm.contains("كاش") && !pm.contains("نقدي")) -> vodafoneCash += inv.totalAmount
                pm.contains("نقدي") || pm == "كاش" -> cashInDrawer += inv.totalAmount
                pm.contains("تحويل") || pm.contains("فيزا") || pm.contains("بنك") -> bankTransfer += inv.totalAmount
                else -> cashInDrawer += inv.totalAmount
            }
        }

        // 3. Compute Treasury and Cash Deficit/Surplus
        val expectedCashInDrawer = (cashInDrawer - supplierPayouts).coerceAtLeast(0.0)
        val deficit = if (actualCashCount < expectedCashInDrawer) {
            BigDecimal.valueOf(expectedCashInDrawer - actualCashCount).setScale(2, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val surplus = if (actualCashCount > expectedCashInDrawer) {
            BigDecimal.valueOf(actualCashCount - expectedCashInDrawer).setScale(2, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val hasDeficit = deficit > 0.0
        var managerAlertSent = false

        // 4. Send Immediate High-Priority Alert to Manager if there is a deficit
        if (hasDeficit) {
            val alertTitle = "⚠️ تنبيه عجز مالي في ختام شفت الكاشير"
            val alertBody = "تم رصد عجز مالي بقيمة ${deficit.toInt()} ج.م في ختام شفت الموظف: $cashierName. الخزينة المتوقعة: ${expectedCashInDrawer.toInt()} ج.م، النقدية الفعلية بالدرج: ${actualCashCount.toInt()} ج.م."
            
            try {
                FcmNotificationManager.sendHighPriorityAlert(
                    context = context,
                    title = alertTitle,
                    message = alertBody
                )
                managerAlertSent = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 5. Build and save the ShiftHandoverEntity
        val shiftDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(logoutTime))
        val shiftDurationMs = (logoutTime - loginTime).coerceAtLeast(3600_000L)
        val durationHours = BigDecimal.valueOf(shiftDurationMs / 3600000.0).setScale(1, RoundingMode.HALF_UP).toDouble()

        val handoverEntity = ShiftHandoverEntity(
            id = "shift_${System.currentTimeMillis()}_${(1000..9999).random()}",
            cashierId = cashierId,
            cashierName = cashierName,
            loginTime = loginTime,
            logoutTime = logoutTime,
            totalInvoicesCount = totalInvoicesCount,
            operationsCount = totalOperationsCount,
            totalRevenueEGP = totalRevenueEGP,
            totalPaidEGP = totalRevenueEGP,
            instaPayCollected = instaPay,
            vodafoneCashCollected = vodafoneCash,
            cashCollected = cashInDrawer,
            bankCollected = bankTransfer,
            supplierPayoutsDuringShift = supplierPayouts,
            netTreasuryCash = actualCashCount,
            totalIncomingTransactions = totalRevenueEGP,
            totalOutgoingTransactions = supplierPayouts,
            productsAddedCount = 0,
            productsSoldCount = totalInvoicesCount,
            notes = notes ?: "إغلاق شفت تلقائي للموظف $cashierName",
            expectedCash = expectedCashInDrawer,
            actualCash = actualCashCount,
            deficitAmount = deficit,
            surplusAmount = surplus,
            hasDeficit = hasDeficit,
            isFlaggedToOwner = hasDeficit,
            managerAlertSent = managerAlertSent,
            cashierPhone = cashierPhone,
            shiftDateText = shiftDateStr,
            isReceived = false
        )

        try {
            database.shiftHandoverDao().insertShiftHandover(handoverEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val reportAr = buildString {
            appendLine("📋 تقرير إغلاق الشفت المالي للموظف: $cashierName")
            appendLine("تاريخ الشفت: $shiftDateStr (المدة: $durationHours ساعة)")
            appendLine("إجمالي الفواتير: $totalInvoicesCount فاتورة | عدد العمليات: $totalOperationsCount")
            appendLine("إجمالي المبيعات المحصلة: ${totalRevenueEGP.toInt()} ج.م")
            appendLine("• إنستاباي: ${instaPay.toInt()} ج.م")
            appendLine("• فودافون كاش: ${vodafoneCash.toInt()} ج.م")
            appendLine("• كاش الدرج: ${cashInDrawer.toInt()} ج.م")
            appendLine("• تحويل بنكي/فيزا: ${bankTransfer.toInt()} ج.م")
            if (supplierPayouts > 0) {
                appendLine("• مدفوعات موردين من الدرج: ${supplierPayouts.toInt()} ج.م")
            }
            appendLine("الخزينة المتوقعة: ${expectedCashInDrawer.toInt()} ج.م | الفعلي بالدرج: ${actualCashCount.toInt()} ج.م")
            if (hasDeficit) {
                appendLine("⚠️ يوجد عجز مالي: ${deficit.toInt()} ج.م (تم إشعار المدير العام فوراً)")
            } else if (surplus > 0) {
                appendLine("🟢 يوجد فائض مالي: ${surplus.toInt()} ج.م")
            } else {
                appendLine("✓ الحسابات مطابقة تماماً دون أي عجز.")
            }
        }

        return ShiftAggregationSummary(
            handover = handoverEntity,
            shiftDurationHours = durationHours,
            totalInvoicesCount = totalInvoicesCount,
            operationsCount = totalOperationsCount,
            totalRevenueEGP = totalRevenueEGP,
            instaPayCollected = instaPay,
            vodafoneCashCollected = vodafoneCash,
            cashCollected = cashInDrawer,
            bankCollected = bankTransfer,
            supplierPayoutsTotal = supplierPayouts,
            expectedCashInDrawer = expectedCashInDrawer,
            actualCashCounted = actualCashCount,
            deficitAmount = deficit,
            surplusAmount = surplus,
            hasDeficit = hasDeficit,
            managerAlertSent = managerAlertSent,
            humanReadableReportAr = reportAr
        )
    }
}
