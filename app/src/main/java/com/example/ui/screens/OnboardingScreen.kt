package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.RepeatUserSummary
import com.example.data.repository.AuthOutcome
import com.example.data.repository.OtpRequestOutcome
import com.example.data.repository.OtpVerifyOutcome
import com.example.ui.components.UserAppPortal

/**
 * OnboardingScreen - Serves as the server-backed authentication gateway housing UserAppPortal.
 * Enforces mandatory server OTP for registration, phone login, untrusted devices, and recovery,
 * while permitting 30-day trusted-device biometric/PIN unlock and offline local-cache unlock.
 */
@Composable
fun OnboardingScreen(
    repeatUser: RepeatUserSummary? = null,
    allHiddenProfiles: List<HiddenProfileEntity> = emptyList(),
    onTrustedOnlineLogin: suspend (phone: String) -> AuthOutcome,
    onOfflineCacheUnlock: suspend (phone: String) -> Boolean,
    onLoginWithPassword: suspend (emailOrPhone: String, password: String, channel: String) -> AuthOutcome,
    onRegisterAccount: suspend (name: String, email: String, phone: String, password: String, role: String, channel: String) -> AuthOutcome,
    onRequestOtp: suspend (purpose: String, channel: String, phone: String?, preAuthToken: String?) -> OtpRequestOutcome,
    onVerifyOtp: suspend (challengeId: String, code: String, phone: String?, preAuthToken: String?, trustDevice: Boolean) -> OtpVerifyOutcome,
    onRequestRecoveryOtp: suspend (phone: String, channel: String) -> OtpRequestOutcome,
    onConfirmRecovery: suspend (challengeId: String, code: String, newPassword: String) -> AuthOutcome,
    onAuthenticatedComplete: () -> Unit,
    onSwitchProfile: ((HiddenProfileEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    UserAppPortal(
        repeatUser = repeatUser,
        allHiddenProfiles = allHiddenProfiles,
        onTrustedOnlineLogin = onTrustedOnlineLogin,
        onOfflineCacheUnlock = onOfflineCacheUnlock,
        onLoginWithPassword = onLoginWithPassword,
        onRegisterAccount = onRegisterAccount,
        onRequestOtp = onRequestOtp,
        onVerifyOtp = onVerifyOtp,
        onRequestRecoveryOtp = onRequestRecoveryOtp,
        onConfirmRecovery = onConfirmRecovery,
        onAuthenticatedComplete = onAuthenticatedComplete,
        onSwitchProfile = onSwitchProfile,
        modifier = modifier
    )
}
