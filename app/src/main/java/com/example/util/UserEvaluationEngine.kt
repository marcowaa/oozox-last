package com.example.util

import com.example.data.model.GoldPurchaseEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.WorkGroupEntity

data class UserBusinessEvaluation(
    val tierStageName: String,         // Stage name e.g. "رجل أعمال - الفئة الأولى A+"
    val businessSpecialization: String, // Sector specialization e.g. "تجارة الذهب والمعادن النفيسة"
    val trustScore: Int,               // 0 to 100
    val reliabilityScore: Int = 100,    // 0 to 100 based on attendance
    val estimatedCapitalVolume: Double, // Calculated behind the scenes from projects & gold
    val tierBadgeColorHex: String,      // Visual color code
    val tierLevelCode: String          // e.g. "Executive L5"
)

data class ReliabilityRecord(
    val period: String,           // e.g. "سبتمبر 2026"
    val presenceRate: Int,        // 0 to 100
    val locationCompliance: Int,  // 0 to 100 (based on geofencing)
    val punctuality: Int,         // 0 to 100
    val overallRating: String     // e.g. "ممتاز", "جيد جداً"
)

object UserEvaluationEngine {

    fun calculateReliabilityRecords(attendance: List<com.example.data.model.EmployeeAttendanceEntity>): List<ReliabilityRecord> {
        if (attendance.isEmpty()) return emptyList()

        val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("ar"))
        val groupedByMonth = attendance.groupBy { 
            val date = try {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it.date)
            } catch (e: Exception) {
                java.util.Date(it.timestamp)
            }
            sdf.format(date)
        }

        return groupedByMonth.map { (month, records) ->
            val total = records.size
            val locationVerifiedCount = records.count { it.isLocationVerified }
            val onTimeCount = records.count { it.lateMinutes == 0 && it.status != "ABSENT" }
            val presenceCount = records.count { it.status != "ABSENT" }

            val presenceRate = (presenceCount * 100 / total).coerceIn(0, 100)
            val locationCompliance = (locationVerifiedCount * 100 / total).coerceIn(0, 100)
            val punctuality = (onTimeCount * 100 / total).coerceIn(0, 100)
            
            val score = (presenceRate + locationCompliance + punctuality) / 3
            val rating = when {
                score >= 95 -> "مثالي ✨"
                score >= 85 -> "ممتاز ✅"
                score >= 75 -> "جيد جداً"
                score >= 60 -> "جيد"
                else -> "يحتاج تحسين ⚠️"
            }

            ReliabilityRecord(month, presenceRate, locationCompliance, punctuality, rating)
        }.sortedByDescending { it.period } // Should probably sort by date actually, but simple for now
    }

    fun evaluateUser(
        user: UserEntity,
        workGroups: List<WorkGroupEntity>,
        projects: List<ProjectEntity>,
        goldPurchases: List<GoldPurchaseEntity>,
        transactions: List<TransactionEntity>
    ): UserBusinessEvaluation {
        // 1. Calculate project capital
        val userGroupIds = workGroups.filter { it.ownerId == user.id }.map { it.id }.toSet()
        val userProjects = projects.filter { userGroupIds.contains(it.workGroupId) }
        val projectCapital = userProjects.sumOf { it.estimatedCapital }

        // 2. Calculate gold & precious metals portfolio volume
        val goldVolume = goldPurchases.filter { it.buyerId == user.id }.sumOf { it.totalPaid }

        // 3. Calculate trade transaction activity
        val transactionVolume = transactions.filter { it.currentUserId == user.id || it.contactId == user.id }.sumOf { it.totalPrice }

        val totalEstimatedCapital = projectCapital + goldVolume + transactionVolume

        // Determine specialization from work group names and projects
        val isGoldTrader = user.type == "GOLD_TRADER" || goldPurchases.isNotEmpty()
        val hasConstruction = userProjects.any { it.type == "CONSTRUCTION" || it.type == "CONTRACTING" }
        val hasFinishing = userProjects.any { it.type == "FINISHING" }

        val specialization = when {
            isGoldTrader && hasConstruction -> "استثمار مزدوج (معادن نفيسة ومقاولات)"
            isGoldTrader -> "تجارة واستثمار الذهب والمعادن النفيسة"
            hasConstruction -> "المقاولات والتطوير العقاري"
            hasFinishing -> "التشطيبات والتوريدات الهندسية"
            userGroupIds.size >= 2 -> "إدارة المشاريع والاستثمارات العامة"
            else -> "تجارة عامة وخدمات استثمارية"
        }

        // Determine Tier Stage implicitly without revealing formula
        val tierStageName: String
        val trustScore: Int
        val colorHex: String
        val tierLevelCode: String

        when {
            totalEstimatedCapital >= 5_000_000 || (user.isVerified && userGroupIds.size >= 3) -> {
                tierStageName = "رجل أعمال - الفئة الأولى (A+)"
                trustScore = 98
                colorHex = "#FFD700" // Premium Gold
                tierLevelCode = "Senior Magnate"
            }
            totalEstimatedCapital >= 1_500_000 || userGroupIds.size >= 2 || user.isVerified -> {
                tierStageName = "كبير المستثمرين (Executive Investor)"
                trustScore = 92
                colorHex = "#10B981" // Emerald
                tierLevelCode = "Executive L4"
            }
            totalEstimatedCapital >= 500_000 || userProjects.isNotEmpty() -> {
                tierStageName = "تاجر ومستثمر نشط (Active Trader)"
                trustScore = 84
                colorHex = "#3B82F6" // Sapphire Blue
                tierLevelCode = "Pro Trader L3"
            }
            totalEstimatedCapital >= 100_000 || goldPurchases.isNotEmpty() -> {
                tierStageName = "مستثمر واعد (Emerging Business)"
                trustScore = 76
                colorHex = "#8B5CF6" // Amethyst
                tierLevelCode = "Associate L2"
            }
            else -> {
                tierStageName = "عضو ومستثمر مبتدئ (Standard Member)"
                trustScore = 65
                colorHex = "#9CA3AF" // Slate Gray
                tierLevelCode = "Member L1"
            }
        }

        return UserBusinessEvaluation(
            tierStageName = tierStageName,
            businessSpecialization = specialization,
            trustScore = trustScore,
            estimatedCapitalVolume = totalEstimatedCapital,
            tierBadgeColorHex = colorHex,
            tierLevelCode = tierLevelCode
        )
    }
}
