package com.example.util

import com.example.data.model.GoldKarat
import com.example.data.model.GoldPricesModel
import com.example.data.model.TransactionStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object GoldUtils {

    val DEFAULT_GOLD_PRICES = GoldPricesModel(
        karat14 = 2500.0,
        karat18 = 3200.0,
        karat21 = 3750.0,
        karat24 = 4300.0,
        goldPound = 30000.0,
        ounceUSD = 2650.0,
        updatedAt = System.currentTimeMillis()
    )

    val DEFAULT_MANUFACTURERS = listOf(
        "لازوردي", "معوض", "داماس", "السبيعي", "الفردان",
        "الرميزان", "مجوهرات طيف", "كارتييه", "بولغري"
    )

    val GOLD_TYPES = listOf(
        "مصاغ", "سبائك", "عملات", "جنيه ذهب", "أساور", "خواتم", "قلائد", "أقراط", "أخرى"
    )

    fun calculateGoldPrice(weight: Double, pricePerGram: Double, manufacturingCostPerGram: Double): Triple<Double, Double, Double> {
        val totalGoldPrice = weight * pricePerGram
        val totalManufacturing = weight * manufacturingCostPerGram
        val totalPrice = totalGoldPrice + totalManufacturing
        return Triple(totalGoldPrice, totalManufacturing, totalPrice)
    }

    fun calculateZakat(
        totalGoldWeight: Double,
        currentPricePerGram: Double,
        karat: Double = 21.0
    ): ZakatResult {
        // Standard Nisab for pure gold (24k) is 85 grams.
        // Convert the given karat weight to equivalent pure 24k weight.
        val pureGoldEquivalent = if (karat > 0 && karat <= 24) {
            totalGoldWeight * (karat / 24.0)
        } else {
            totalGoldWeight
        }
        val goldValue = totalGoldWeight * currentPricePerGram
        val isNisabMet = pureGoldEquivalent >= 85.0
        val nisabThresholdValue = 85.0 * currentPricePerGram
        val zakatAmount = if (isNisabMet) goldValue * 0.025 else 0.0
        return ZakatResult(
            goldValue = goldValue,
            zakatAmount = zakatAmount,
            isNisabMet = isNisabMet,
            nisabThreshold = nisabThresholdValue
        )
    }

    fun calculateProfitLoss(
        weight: Double,
        purchasePricePerGram: Double,
        currentPricePerGram: Double,
        manufacturingCost: Double,
        stampDuty: Double
    ): ProfitLossResult {
        val purchaseGoldValue = weight * purchasePricePerGram
        val totalCost = purchaseGoldValue + manufacturingCost + stampDuty
        val currentValue = weight * currentPricePerGram
        val profitLoss = currentValue - totalCost
        val profitLossPercent = if (totalCost > 0) (profitLoss / totalCost) * 100 else 0.0
        return ProfitLossResult(totalCost, currentValue, profitLoss, profitLossPercent)
    }

    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("ar", "EG"))
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        return "${format.format(amount)} ج.م"
    }

    fun formatWeight(weight: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("ar", "EG"))
        format.maximumFractionDigits = 2
        return "${format.format(weight)} جم"
    }

    fun formatDecimal(value: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("ar", "EG"))
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        return format.format(value)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar", "EG"))
        return sdf.format(Date(timestamp))
    }

    fun formatExactDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm:ss a", Locale("ar", "EG"))
        return sdf.format(Date(timestamp))
    }

    fun formatCallExactDateTime(timestamp: Long): String {
        val nowCal = Calendar.getInstance()
        val callCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val timeSdf = SimpleDateFormat("hh:mm:ss a", Locale("ar", "EG"))
        val formattedTime = timeSdf.format(Date(timestamp))

        val isToday = nowCal.get(Calendar.YEAR) == callCal.get(Calendar.YEAR) &&
                      nowCal.get(Calendar.DAY_OF_YEAR) == callCal.get(Calendar.DAY_OF_YEAR)

        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = nowCal.get(Calendar.YEAR) == callCal.get(Calendar.YEAR) &&
                          nowCal.get(Calendar.DAY_OF_YEAR) == callCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "اليوم • $formattedTime"
            isYesterday -> "أمس • $formattedTime"
            else -> {
                val dateSdf = SimpleDateFormat("yyyy/MM/dd • hh:mm:ss a", Locale("ar", "EG"))
                dateSdf.format(Date(timestamp))
            }
        }
    }

    fun formatShortTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar", "EG"))
        return sdf.format(Date(timestamp))
    }

    fun isSameHour(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
               cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY)
    }

    fun formatHourlyTimestampHeader(timestamp: Long): String {
        val nowCal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hourSdf = SimpleDateFormat("hh:00 a", Locale("ar", "EG"))
        val formattedHour = hourSdf.format(Date(timestamp))

        val isToday = nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                      nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                          nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "اليوم • $formattedHour"
            isYesterday -> "أمس • $formattedHour"
            else -> {
                val dateSdf = SimpleDateFormat("EEEE، d MMMM • hh:00 a", Locale("ar", "EG"))
                dateSdf.format(Date(timestamp))
            }
        }
    }

    fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex())
        return if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}"
        } else if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
            parts[0].take(2)
        } else {
            "م"
        }
    }

    fun getStatusLabel(status: TransactionStatus): String = when (status) {
        TransactionStatus.PURCHASED -> "مشترى"
        TransactionStatus.RETURNED -> "مُرجَع"
        TransactionStatus.RESOLD -> "مُعاد بيعه"
    }

    data class ZakatResult(
        val goldValue: Double,
        val zakatAmount: Double,
        val isNisabMet: Boolean,
        val nisabThreshold: Double
    )

    data class ProfitLossResult(
        val totalCost: Double,
        val currentValue: Double,
        val profitLoss: Double,
        val profitLossPercent: Double
    )

    fun isZoxAdminAccount(user: com.example.data.model.UserEntity?): Boolean {
        if (user == null) return false
        return user.type.equals("ADMIN", ignoreCase = true) || user.type.equals(com.example.data.model.UserType.ADMIN.name, ignoreCase = true)
    }

    fun formatPhoneForDisplay(
        phone: String,
        username: String? = null,
        name: String? = null,
        isCurrentUserAdmin: Boolean = false
    ): String {
        return phone
    }
}
