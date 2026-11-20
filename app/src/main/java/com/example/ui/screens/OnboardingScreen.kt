package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.RepeatUserSummary
import com.example.data.model.UserType
import com.example.ui.components.UserAppPortal

/**
 * OnboardingScreen - Serves as the authentication gateway housing the UserAppPortal.
 * Provides Biometric Authentication (Fingerprint / Face Unlock) for repeat users and multi-channel login.
 */
@Composable
fun OnboardingScreen(
    savedPhone: String? = null,
    repeatUser: RepeatUserSummary? = null,
    allHiddenProfiles: List<HiddenProfileEntity> = emptyList(),
    onLoginSuccess: (name: String, phone: String, username: String, type: UserType) -> Unit,
    onCreateSubAccountDirectly: ((name: String, pin: String, company: String, role: String, cap: Double, silent: Boolean) -> Unit)? = null,
    onSwitchProfile: ((HiddenProfileEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    UserAppPortal(
        repeatUser = repeatUser,
        allHiddenProfiles = allHiddenProfiles,
        onLoginSuccess = onLoginSuccess,
        onSwitchProfile = onSwitchProfile,
        onCreateSubAccountDirectly = onCreateSubAccountDirectly,
        modifier = modifier
    )
}
