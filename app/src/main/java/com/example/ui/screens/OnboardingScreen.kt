package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.RepeatUserSummary
import com.example.data.repository.AuthOutcome
import com.example.data.repository.OtpRequestOutcome
import com.example.data.repository.OtpVerifyOutcome
import com.example.ui.components.UserAppPortal
import com.example.ui.navigation.PostOtpGatewayDecision

/**
 * OnboardingScreen - Classic Phone-Only OTP Gateway housing UserAppPortal.
 * Enforces mandatory server OTP for new/returning phones and untrusted devices,
 * while permitting 30-day trusted-device biometric/PIN unlock and offline local-cache unlock.
 */
@Composable
fun OnboardingScreen(
    repeatUser: RepeatUserSummary? = null,
    allHiddenProfiles: List<HiddenProfileEntity> = emptyList(),
    onTrustedOnlineLogin: suspend (phone: String) -> AuthOutcome,
    onOfflineCacheUnlock: suspend (phone: String) -> Boolean,
    onRequestOtp: suspend (purpose: String, channel: String, phone: String?, preAuthToken: String?) -> OtpRequestOutcome,
    onVerifyOtp: suspend (challengeId: String, code: String, phone: String?, preAuthToken: String?, trustDevice: Boolean) -> OtpVerifyOutcome,
    onCancelOtp: suspend (challengeId: String, phone: String?, preAuthToken: String?) -> Boolean = { _, _, _ -> true },
    onAuthenticatedComplete: (PostOtpGatewayDecision) -> Unit,
    onSwitchProfile: ((HiddenProfileEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    UserAppPortal(
        repeatUser = repeatUser,
        allHiddenProfiles = allHiddenProfiles,
        onTrustedOnlineLogin = onTrustedOnlineLogin,
        onOfflineCacheUnlock = onOfflineCacheUnlock,
        onRequestOtp = onRequestOtp,
        onVerifyOtp = onVerifyOtp,
        onCancelOtp = onCancelOtp,
        onAuthenticatedComplete = onAuthenticatedComplete,
        onSwitchProfile = onSwitchProfile,
        modifier = modifier
    )
}
