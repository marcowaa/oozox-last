package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.FinancialComplaintEntity
import com.example.data.model.ProjectAuditLogEntity
import com.example.data.model.ShiftHandoverEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ForensicHandoverAnalysisEngine
 * Investigates financial complaints and invoice discrepancies against shift handovers.
 * Evaluates why and how the incoming employee confirmed receiving the cash & goods without reporting errors.
 * Classifies negligence vs. collusion and calculates joint liability for the manager.
 */
object ForensicHandoverAnalysisEngine {

    data class ForensicAnalysisResult(
        val complaintId: String,
        val invoiceNumber: String?,
        val primaryAccusedName: String,
        val receiverName: String?,
        val receiverId: String?,
        val liabilityType: String, // "GROSS_NEGLIGENCE_RECEIPT", "SUSPECTED_COLLUSION", "INADVERTENT_MISCOUNT", "OUTGOING_STAFF_SOLE_LIABILITY"
        val liabilityTypeAr: String,
        val liabilityDistribution: String, // e.g. "50% محرر الفاتورة / 50% مستلم الشفت"
        val primaryDeductionAmount: Double,
        val receiverDeductionAmount: Double,
        val detailedReportAr: String,
        val recommendedActionAr: String,
        val shiftHandoverFound: Boolean
    )

    /**
     * Conducts deep forensic analysis on a verified financial complaint.
     */
    suspend fun analyzeComplaintHandoverLiability(
        context: Context,
        complaint: FinancialComplaintEntity
    ): ForensicAnalysisResult = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val allHandovers: List<ShiftHandoverEntity> = db.shiftHandoverDao().getAllShiftHandovers().first()

        // 1. Locate relevant ShiftHandover for the accused employee and target date
        val relevantHandover = allHandovers.firstOrNull { handover ->
            (handover.cashierId == complaint.accusedEmployeeId || handover.cashierName == complaint.accusedEmployeeName) &&
                    (handover.shiftDateText == complaint.targetDate || isSameDay(handover.logoutTime, complaint.timestamp))
        } ?: allHandovers.firstOrNull { handover ->
            isSameDay(handover.logoutTime, complaint.timestamp)
        }

        val claimAmount = if (complaint.claimAmount > 0) complaint.claimAmount else complaint.amount

        if (relevantHandover == null) {
            // No shift handover recorded
            val result = ForensicAnalysisResult(
                complaintId = complaint.id,
                invoiceNumber = complaint.invoiceNumber,
                primaryAccusedName = complaint.accusedEmployeeName ?: "الموظف المحرر",
                receiverName = null,
                receiverId = null,
                liabilityType = "OUTGOING_STAFF_SOLE_LIABILITY",
                liabilityTypeAr = "مسؤولية فردية كاملة (عدم توثيق تسليم الشفت)",
                liabilityDistribution = "100% على الموظف المحرر",
                primaryDeductionAmount = claimAmount,
                receiverDeductionAmount = 0.0,
                detailedReportAr = "لم يتم العثور على محضر تسليم شفت مسجل رسمياً لهذا اليوم، مما يثبت عدم تسليم العهدة لزميل آخر وفق الأصول، وتقع كامل المسؤولية المالية على الموظف المحرر ${complaint.accusedEmployeeName}.",
                recommendedActionAr = "خصم المبلغ كاملاً (${claimAmount.toInt()} ج.م) من رصيد عهدة الموظف المحرر وتوجيه لفت نظر لعدم توثيق تسليم الشفت.",
                shiftHandoverFound = false
            )
            saveAnalysisToDb(db, complaint.id, result)
            return@withContext result
        }

        // 2. Handover exists, check if an incoming employee received and confirmed it
        val isReceived = relevantHandover.isReceived
        val receiverName = relevantHandover.receivedByCashierName
        val receiverId = relevantHandover.receivedByCashierId

        if (!isReceived || receiverName.isNullOrBlank()) {
            val result = ForensicAnalysisResult(
                complaintId = complaint.id,
                invoiceNumber = complaint.invoiceNumber,
                primaryAccusedName = complaint.accusedEmployeeName ?: relevantHandover.cashierName,
                receiverName = null,
                receiverId = null,
                liabilityType = "OUTGOING_STAFF_SOLE_LIABILITY",
                liabilityTypeAr = "مسؤولية فردية (الشفت غير مستلم من طرف آخر)",
                liabilityDistribution = "100% على الموظف المحرر",
                primaryDeductionAmount = claimAmount,
                receiverDeductionAmount = 0.0,
                detailedReportAr = "الشفت مسجل كمعلق ولم يتم اعتماده أو استلامه من قبل موظف آخر، وبالتالي لا توجد مسؤولية مشتركة، والمسؤولية تقع على عاتق ${relevantHandover.cashierName}.",
                recommendedActionAr = "خصم إجمالي المبلغ (${claimAmount.toInt()} ج.م) من محرر الشفت.",
                shiftHandoverFound = true
            )
            saveAnalysisToDb(db, complaint.id, result)
            return@withContext result
        }

        // 3. Incoming employee confirmed receipt: Perform deep anomaly and collusion analysis
        val acceptanceType = relevantHandover.acceptanceType
        val goodsDiscrepancy = relevantHandover.goodsDiscrepancyNotes
        val cashConfirmed = relevantHandover.cashDrawerConfirmed
        val stockChecked = relevantHandover.physicalStockChecked

        val (liabilityType, liabilityTypeAr, liabilityDist, primaryShare, receiverShare, detailedReport, recommendation) =
            when {
                // If receiver explicitly noted the discrepancy during handover
                acceptanceType == "ACCEPTED_WITH_DISCREPANCY" && !goodsDiscrepancy.isNullOrBlank() &&
                        (goodsDiscrepancy.contains(complaint.invoiceNumber ?: "###") || goodsDiscrepancy.contains(claimAmount.toInt().toString())) -> {
                    Tuple7(
                        "OUTGOING_STAFF_SOLE_LIABILITY",
                        "إخلاء مسؤولية المستلم (تم إثبات العجز مسبقاً)",
                        "100% على المحرر / 0% على المستلم",
                        claimAmount,
                        0.0,
                        "الموظف المستلم ($receiverName) قام بواجبه الرقابي ووثق وجود فروقات وعجز أثناء استلام الشفت بالملاحظة: '$goodsDiscrepancy'، مما يخليه تماماً من المسؤولية.",
                        "خصم المبلغ كاملاً من ${relevantHandover.cashierName} وصرف مكافأة تدقيق للمستلم $receiverName."
                    )
                }

                // If claim amount is substantial (>= 1000 EGP) and receiver approved full 100% match with signature
                claimAmount >= 1000.0 && acceptanceType == "ACCEPTED_FULL_MATCH" && cashConfirmed && stockChecked -> {
                    val half = claimAmount / 2.0
                    Tuple7(
                        "SUSPECTED_COLLUSION",
                        "شبهة تواطؤ مالي وإقرار استلام غير مطابق ⚠️",
                        "50% على المحرر (${half.toInt()} ج.م) / 50% على المستلم (${half.toInt()} ج.م)",
                        half,
                        half,
                        "قام الموظف المستلم ($receiverName) بتوقيع إقرار استلام رسمي يؤكد مطابقة الخزينة والنقدية والبضاعة بنسبة 100% دون تسجيل أي عجز، رغم وجود فرق مالي مؤكد قدره ${claimAmount.toInt()} ج.م بالفاتورة #${complaint.invoiceNumber ?: ""}. هذا التطابق الصوري يشير إلى إما تستر وتواطؤ مالي متبادل أو تهاون متعمد في حماية أصول الشركة.",
                        "تطبيق خصم مالي مشترك مناصفة (${half.toInt()} ج.م لكل منهما)، وتجميد صلاحيات تسليم الشفت التلقائي وتحويل الواقعة للتحقيق الإداري العاجل."
                    )
                }

                // Standard negligence in counting/auditing
                else -> {
                    val primaryAmount = claimAmount * 0.70
                    val receiverAmount = claimAmount * 0.30
                    Tuple7(
                        "GROSS_NEGLIGENCE_RECEIPT",
                        "إهمال جسيم في الاستلام والتوقيع الشكلي ⚠️",
                        "70% على المحرر (${primaryAmount.toInt()} ج.م) / 30% على المستلم (${receiverAmount.toInt()} ج.م)",
                        primaryAmount,
                        receiverAmount,
                        "أكد الموظف المستلم ($receiverName) استلام العهدة بالكامل وأقر بصحة النقدية، بينما الفاتورة #${complaint.invoiceNumber ?: ""} لم يتم تحصيل قيمتها أو قيدها بشكل سليم. استلام الشفت دون مطابقة دقيقة للأرصدة والفواتير يُعد إهمالاً في أداء مهام الكاشير ويحمّله مسؤولية تضامنية في العجز الناتج.",
                        "تطبيق خصم بنسبة 70% على المحرر الأساسي (${relevantHandover.cashierName}) ونسبة 30% على المستلم المهمل ($receiverName) مع توجيه إنذار إداري بالاستلام غير المطابق."
                    )
                }
            }

        val finalResult = ForensicAnalysisResult(
            complaintId = complaint.id,
            invoiceNumber = complaint.invoiceNumber,
            primaryAccusedName = relevantHandover.cashierName,
            receiverName = receiverName,
            receiverId = receiverId,
            liabilityType = liabilityType,
            liabilityTypeAr = liabilityTypeAr,
            liabilityDistribution = liabilityDist,
            primaryDeductionAmount = primaryShare,
            receiverDeductionAmount = receiverShare,
            detailedReportAr = detailedReport,
            recommendedActionAr = recommendation,
            shiftHandoverFound = true
        )

        // Save forensic analysis fields to complaint entity
        saveAnalysisToDb(db, complaint.id, finalResult)

        // Dispatch alert to Manager
        NotificationHelper.showForensicLiabilityAlert(
            context = context,
            complaintTitle = complaint.title,
            invoiceNo = complaint.invoiceNumber ?: "سريعة",
            primaryName = relevantHandover.cashierName,
            receiverName = receiverName ?: "مستلم الشفت",
            liabilityType = liabilityTypeAr
        )

        // Log Audit Event
        try {
            val auditLog = ProjectAuditLogEntity(
                id = "sec_forensic_${System.currentTimeMillis()}",
                projectId = "company_group_main",
                action = "FORENSIC_LIABILITY_AUDIT",
                actorName = receiverName ?: "مستلم الشفت",
                actorPhone = relevantHandover.cashierPhone ?: "",
                details = "تحليل جنائي للشكوى #${complaint.id}: فحص إقرار استلام الشفت لـ $receiverName و ${relevantHandover.cashierName}. النتيجة: $liabilityTypeAr ($liabilityDist)",
                timestamp = System.currentTimeMillis()
            )
            db.projectAuditLogDao().insertAuditLog(auditLog)
        } catch (_: Exception) {}

        finalResult
    }

    private suspend fun saveAnalysisToDb(
        db: AppDatabase,
        complaintId: String,
        result: ForensicAnalysisResult
    ) {
        try {
            db.financialComplaintDao().updateForensicAnalysis(
                id = complaintId,
                analysisCompleted = true,
                liabilityType = result.liabilityType,
                receiverId = result.receiverId,
                receiverName = result.receiverName,
                liabilityDist = result.liabilityDistribution,
                auditDetails = result.detailedReportAr,
                recommendedAction = result.recommendedActionAr,
                jointDeduction = result.receiverDeductionAmount > 0
            )
        } catch (_: Exception) {}
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }

    private data class Tuple7<A, B, C, D, E, F, G>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
    )
}
