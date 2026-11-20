package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object MainTabs : Screen

    @Serializable
    data class ChatDetail(val chatId: String) : Screen

    @Serializable
    data class TransactionDetail(val transactionId: String) : Screen

    @Serializable
    data class ProjectDetail(val projectId: String) : Screen

    @Serializable
    data object Nearby : Screen

    @Serializable
    data object Admin : Screen

    @Serializable
    data object Subscriptions : Screen

    @Serializable
    data object CompanyAccounting : Screen

    @Serializable
    data class MerchantStore(val merchantId: String? = null, val isManageMode: Boolean = false) : Screen

    @Serializable
    data object Gold : Screen
}


enum class BottomTab(val route: String, val labelKey: String) {
    CHATS("chats", "chats"),
    PROJECTS("projects", "projects"),
    BUSINESS("business", "business"),
    TRANSACTIONS("transactions", "transactions"),
    PROFILE("profile", "profile")
}
