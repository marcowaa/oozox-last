package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.repository.AuthOutcome
import com.example.data.repository.AuthRepository
import com.example.data.repository.OtpRequestOutcome
import com.example.data.repository.OtpVerifyOutcome
import com.example.ui.components.OtpDeliveryChannel
import com.example.ui.components.OtpResendUiResult
import com.example.ui.components.OtpVerifyUiResult
import com.example.ui.components.WhatsAppTelegramOtpView
import com.example.ui.theme.GoldChatTheme
import com.example.util.AppLanguage
import com.example.util.BackupResult
import com.example.util.CrossDeviceTransferEngine
import com.example.util.DeviceTrustManager
import com.example.util.LocalAppLanguage
import com.example.util.RetrofitClient
import com.example.util.SecureBackupVaultEngine
import com.example.util.SecureCredentialVault
import com.example.util.Translations
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class AuthOtpDeviceTrustTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private var localServerSocket: ServerSocket? = null
    private val serverRunning = AtomicBoolean(false)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Translations.init(context)
        SecureCredentialVault.clearAll(context)
        DeviceTrustManager.resetPinAttempts(context)
        RetrofitClient.setAuthToken(null)
        RetrofitClient.enableNetworkBodyLogs = false
    }

    @After
    fun tearDown() {
        serverRunning.set(false)
        try {
            localServerSocket?.close()
        } catch (_: Exception) {}
        localServerSocket = null
        SecureCredentialVault.clearAll(context)
        DeviceTrustManager.resetPinAttempts(context)
        RetrofitClient.setAuthToken(null)
        RetrofitClient.configureBaseUrl("https://api.oozox.com/api/")
    }

    private fun isoAfterDays(days: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L))
    }

    private fun startEmbeddedHttpServer(
        handler: (method: String, path: String, headers: Map<String, String>, body: String) -> Pair<Int, String>
    ): Int {
        val server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        localServerSocket = server
        serverRunning.set(true)

        thread(isDaemon = true) {
            while (serverRunning.get() && !server.isClosed) {
                val socket = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                thread(isDaemon = true) {
                    socket.use { sock ->
                        val input = BufferedInputStream(sock.getInputStream())
                        val headerBuilder = StringBuilder()
                        var cur: Int
                        while (input.read().also { cur = it } != -1) {
                            headerBuilder.append(cur.toChar())
                            val len = headerBuilder.length
                            if (len >= 4 &&
                                headerBuilder[len - 4] == '\r' &&
                                headerBuilder[len - 3] == '\n' &&
                                headerBuilder[len - 2] == '\r' &&
                                headerBuilder[len - 1] == '\n'
                            ) {
                                break
                            }
                        }
                        val lines = headerBuilder.toString().split("\r\n").filter { it.isNotBlank() }
                        if (lines.isEmpty()) return@use

                        val requestParts = lines.first().split(" ")
                        val method = requestParts.getOrElse(0) { "GET" }
                        val path = requestParts.getOrElse(1) { "/" }.substringBefore("?")

                        val headers = mutableMapOf<String, String>()
                        for (i in 1 until lines.size) {
                            val idx = lines[i].indexOf(':')
                            if (idx > 0) {
                                headers[lines[i].substring(0, idx).trim().lowercase()] =
                                    lines[i].substring(idx + 1).trim()
                            }
                        }

                        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                        val reqBody = if (contentLength > 0) {
                            val buf = ByteArray(contentLength)
                            var offset = 0
                            while (offset < contentLength) {
                                val r = input.read(buf, offset, contentLength - offset)
                                if (r == -1) break
                                offset += r
                            }
                            String(buf, 0, offset, StandardCharsets.UTF_8)
                        } else ""

                        val (statusCode, respJson) = handler(method, path, headers, reqBody)
                        val respBytes = respJson.toByteArray(StandardCharsets.UTF_8)
                        val statusText = when (statusCode) {
                            200 -> "OK"
                            201 -> "Created"
                            401 -> "Unauthorized"
                            423 -> "Locked"
                            429 -> "Too Many Requests"
                            else -> "Error"
                        }
                        val httpResponse = (
                            "HTTP/1.1 $statusCode $statusText\r\n" +
                                "Content-Type: application/json; charset=utf-8\r\n" +
                                "Content-Length: ${respBytes.size}\r\n" +
                                "Connection: close\r\n\r\n"
                            ).toByteArray(StandardCharsets.US_ASCII) + respBytes

                        sock.getOutputStream().apply {
                            write(httpResponse)
                            flush()
                        }
                    }
                }
            }
        }
        return server.localPort
    }

    @Test
    fun credentialVault_persistsSessionAndTrustSeparately() {
        val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
        assertTrue(deviceId.startsWith("dev_"))
        assertEquals(deviceId, SecureCredentialVault.getOrCreateDeviceId(context))

        SecureCredentialVault.saveSessionTokens(
            context = context,
            accessToken = "jwt_access_abc",
            refreshToken = "jwt_refresh_xyz",
            sessionId = "sess_1"
        )
        SecureCredentialVault.saveServerDeviceTrust(
            context = context,
            phoneE164 = "+201012345678",
            userId = "usr_1",
            deviceTrustToken = "trust_tok_999",
            trustedUntilEpochMs = System.currentTimeMillis() + 86_400_000L,
            hardwareFingerprint = "fp_hash"
        )

        assertEquals("jwt_access_abc", SecureCredentialVault.getAccessToken(context))
        assertEquals("jwt_refresh_xyz", SecureCredentialVault.getRefreshToken(context))
        assertEquals("sess_1", SecureCredentialVault.getSessionId(context))
        assertEquals("trust_tok_999", SecureCredentialVault.getServerDeviceTrustToken(context))

        // Logging out without revoking device preserves server trust token for trusted biometric re-login
        SecureCredentialVault.clearSessionOnly(context)
        assertNull(SecureCredentialVault.getAccessToken(context))
        assertNull(SecureCredentialVault.getRefreshToken(context))
        assertEquals("trust_tok_999", SecureCredentialVault.getServerDeviceTrustToken(context))

        SecureCredentialVault.clearAll(context)
        assertNull(SecureCredentialVault.getServerDeviceTrustToken(context))
    }

    @Test
    @Suppress("DEPRECATION")
    fun deviceTrustManager_rejectsLocalMarkBypass_validatesServerTokenAndLockout() {
        val phone = "+201012345678"
        DeviceTrustManager.markDeviceTrusted(context, phone)
        assertFalse(DeviceTrustManager.isDeviceTrustedForPhone(context, phone))
        assertFalse(DeviceTrustManager.isOfflineCacheAccessAllowed(context, phone))

        // Store expired trust -> rejected
        DeviceTrustManager.storeServerDeviceTrust(
            context = context,
            phone = phone,
            userId = "usr_1",
            deviceTrustToken = "tok_expired",
            trustedUntilIso = "2020-01-01T00:00:00.000Z"
        )
        assertFalse(DeviceTrustManager.isDeviceTrustedForPhone(context, phone))
        assertFalse(DeviceTrustManager.isOfflineCacheAccessAllowed(context, phone))

        // Store valid 30-day trust -> accepted only for matching phone
        DeviceTrustManager.storeServerDeviceTrust(
            context = context,
            phone = phone,
            userId = "usr_1",
            deviceTrustToken = "tok_valid_30d",
            trustedUntilIso = isoAfterDays(30)
        )
        assertTrue(DeviceTrustManager.isDeviceTrustedForPhone(context, phone))
        assertTrue(DeviceTrustManager.isDeviceTrustedForPhone(context, "01012345678"))
        assertTrue(DeviceTrustManager.isOfflineCacheAccessAllowed(context, phone))
        assertFalse(DeviceTrustManager.isDeviceTrustedForPhone(context, "+201199999999"))

        // PIN failed attempt lockout after 5 failures
        repeat(4) {
            val left = DeviceTrustManager.recordFailedPinAttempt(context)
            assertTrue(left > 0)
            assertEquals(0, DeviceTrustManager.getRemainingLockoutSeconds(context))
        }
        val fifth = DeviceTrustManager.recordFailedPinAttempt(context)
        assertEquals(0, fifth)
        assertTrue(DeviceTrustManager.getRemainingLockoutSeconds(context) > 0)
        DeviceTrustManager.resetPinAttempts(context)
        assertEquals(0, DeviceTrustManager.getRemainingLockoutSeconds(context))
    }

    @Test
    fun authRepository_endToEndPhoneOnlyOtp_deviceTrust_andTransferMetadata() = runBlocking {
        val futureIso = isoAfterDays(30)
        val recordedHeaders = CopyOnWriteArrayList<Map<String, String>>()
        val currentDeviceId = SecureCredentialVault.getOrCreateDeviceId(context)

        val port = startEmbeddedHttpServer { method, path, headers, reqBody ->
            recordedHeaders.add(headers)
            when {
                method == "POST" && path == "/api/auth/otp/request" -> 200 to """
                    {
                      "success": true,
                      "message": "Switched to WhatsApp",
                      "data": {
                        "challengeId": "ch_login_2",
                        "activeChannel": "WHATSAPP",
                        "attemptedChannels": ["TELEGRAM", "WHATSAPP"],
                        "cooldownSeconds": 60,
                        "maxAttempts": 5,
                        "maskedPhone": "+2010****5678",
                        "isExistingUser": true
                      }
                    }
                """.trimIndent()

                method == "POST" && path == "/api/auth/otp/verify" -> 200 to """
                    {
                      "success": true,
                      "message": "Verified",
                      "data": {
                        "user": {
                          "id": "usr_101",
                          "name": "سالم العتيبي",
                          "email": "salem@oozox.com",
                          "phone": "+201012345678",
                          "phoneE164": "+201012345678",
                          "role": "MERCHANT",
                          "isVerified": true
                        },
                        "token": "jwt_full_session_1",
                        "refreshToken": "ref_1",
                        "sessionId": "sess_101",
                        "isExistingUser": true,
                        "hasOtherActiveDevice": true,
                        "otherActiveDevices": [
                          {
                            "sessionId": "sess_old_1",
                            "deviceId": "dev_old_pixel",
                            "deviceName": "Pixel 7",
                            "platform": "ANDROID"
                          }
                        ],
                        "backupSummary": {
                          "count": 1,
                          "latestBackupId": "bak_001",
                          "latestTotalRecords": 42,
                          "latestSizeBytes": 8192
                        },
                        "trustedDevice": {
                          "deviceId": "$currentDeviceId",
                          "deviceTrustToken": "srv_trust_30d_abc",
                          "trustedUntil": "$futureIso",
                          "trustDays": 30
                        }
                      }
                    }
                """.trimIndent()

                method == "POST" && path == "/api/auth/trusted-login" && reqBody.contains("srv_trust_30d_abc") -> 200 to """
                    {
                      "success": true,
                      "message": "Trusted biometric login",
                      "data": {
                        "user": {
                          "id": "usr_101",
                          "name": "سالم العتيبي",
                          "email": "salem@oozox.com",
                          "phone": "+201012345678",
                          "phoneE164": "+201012345678",
                          "role": "MERCHANT",
                          "isVerified": true
                        },
                        "token": "jwt_full_session_2",
                        "refreshToken": "ref_2",
                        "sessionId": "sess_102",
                        "isExistingUser": true,
                        "hasOtherActiveDevice": false,
                        "trustedDevice": {
                          "deviceId": "$currentDeviceId",
                          "deviceTrustToken": "srv_trust_30d_abc",
                          "trustedUntil": "$futureIso",
                          "trustDays": 30
                        }
                      }
                    }
                """.trimIndent()

                method == "GET" && path == "/api/auth/devices" -> 200 to """
                    {
                      "success": true,
                      "message": "OK",
                      "data": {
                        "devices": [
                          {
                            "deviceId": "$currentDeviceId",
                            "deviceName": "Pixel 8 Pro",
                            "platform": "ANDROID",
                            "trustedUntil": "$futureIso",
                            "isActive": true,
                            "isCurrent": true
                          }
                        ]
                      }
                    }
                """.trimIndent()

                method == "DELETE" && path == "/api/auth/devices/$currentDeviceId" -> 200 to """
                    {"success": true, "message": "تم إلغاء الجهاز"}
                """.trimIndent()

                else -> 401 to """{"success":false,"code":"OTP_REQUIRED","requiresOtp":true,"message":"Device untrusted"}"""
            }
        }

        RetrofitClient.configureBaseUrl("http://127.0.0.1:$port/api")

        // 1. Phone-only OTP request with channel switching
        val otpReq = AuthRepository.requestOtp(context, purpose = "LOGIN", channel = "WHATSAPP", phone = "+201012345678")
        assertTrue(otpReq is OtpRequestOutcome.Sent)
        val reqData = (otpReq as OtpRequestOutcome.Sent).data
        assertEquals("WHATSAPP", reqData.activeChannel)
        assertTrue(reqData.isExistingUser)
        assertNull(SecureCredentialVault.getAccessToken(context))

        // 2. Verify OTP issues JWT + 30-day server device trust + post-OTP transfer/backup signals
        val verifyRes = AuthRepository.verifyOtp(context, challengeId = "ch_login_2", code = "123456", phone = "+201012345678")
        assertTrue(verifyRes is OtpVerifyOutcome.Verified)
        val verifiedData = (verifyRes as OtpVerifyOutcome.Verified).data
        assertTrue(verifiedData.isExistingUser)
        assertTrue(verifiedData.hasOtherActiveDevice)
        assertEquals("Pixel 7", verifiedData.otherActiveDevices.firstOrNull()?.deviceName)
        assertEquals(1, verifiedData.backupSummary?.count)
        assertEquals("jwt_full_session_1", SecureCredentialVault.getAccessToken(context))
        assertTrue(DeviceTrustManager.isDeviceTrustedForPhone(context, "+201012345678"))
        assertTrue(recordedHeaders.any { it["x-device-id"] == currentDeviceId && it["x-platform"] == "ANDROID" })

        // 3. Trusted device biometric login succeeds with server trust token
        SecureCredentialVault.clearSessionOnly(context)
        val trustedLogin = AuthRepository.trustedDeviceLogin(context, "+201012345678")
        assertTrue(trustedLogin is AuthOutcome.Authenticated)
        assertEquals("jwt_full_session_2", SecureCredentialVault.getAccessToken(context))

        // 4. List & revoke trusted devices
        val devicesRes = AuthRepository.listTrustedDevices(context)
        assertTrue(devicesRes.isSuccess)
        assertEquals(1, devicesRes.getOrNull()?.size)

        val revokeRes = AuthRepository.revokeTrustedDevice(context, currentDeviceId)
        assertTrue(revokeRes.isSuccess)
        assertNull(SecureCredentialVault.getAccessToken(context))
        assertFalse(DeviceTrustManager.isDeviceTrustedForPhone(context, "+201012345678"))

        // 5. 429 RTC monthly budget error parsing
        val parsed429 = AuthRepository.parseErrorBody(
            """{"success":false,"code":"RTC_MONTHLY_BUDGET_EXCEEDED","message":"تم بلوغ الحد الشهري للمكالمات"}""",
            "fallback"
        )
        assertEquals("RTC_MONTHLY_BUDGET_EXCEEDED", parsed429.code)
        assertTrue(parsed429.message.contains("الحد الشهري"))
    }

    @Test
    fun crossDeviceTransferEngine_ecdhHkdfAesGcm_andSasSymmetry() {
        val aliceKeys = CrossDeviceTransferEngine.generateEphemeralKeyPair()
        val bobKeys = CrossDeviceTransferEngine.generateEphemeralKeyPair()
        val alicePub = CrossDeviceTransferEngine.encodePublicKeyBase64(aliceKeys.public)
        val bobPub = CrossDeviceTransferEngine.encodePublicKeyBase64(bobKeys.public)
        val transferId = "xfer_test_001"

        val aliceAes = CrossDeviceTransferEngine.deriveAesSessionKey(aliceKeys.private, bobPub, transferId)
        val bobAes = CrossDeviceTransferEngine.deriveAesSessionKey(bobKeys.private, alicePub, transferId)
        assertTrue(aliceAes.contentEquals(bobAes))

        val sasAlice = CrossDeviceTransferEngine.computeSasCode(aliceAes, transferId, alicePub, bobPub)
        val sasBob = CrossDeviceTransferEngine.computeSasCode(bobAes, transferId, bobPub, alicePub)
        assertEquals(6, sasAlice.length)
        assertEquals(sasAlice, sasBob)

        val secretPayload = "بيانات المحادثات والوسائط 24K — OOZOX E2EE".toByteArray(StandardCharsets.UTF_8)
        val (cipherB64, ivB64) = CrossDeviceTransferEngine.encryptAesGcm(aliceAes, secretPayload, "$transferId|0")
        val decrypted = CrossDeviceTransferEngine.decryptAesGcm(bobAes, cipherB64, ivB64, "$transferId|0")
        assertEquals(String(secretPayload, StandardCharsets.UTF_8), String(decrypted, StandardCharsets.UTF_8))
    }

    @Test
    fun vaultV3_localExportInspectAndRestore_preservesDataAndSkipsLosslessly() = runBlocking {
        val db = AppDatabase.getInstance(context)
        db.chatDao().insertChat(
            ChatEntity(
                id = "chat_v3_test",
                contactId = "+201012345678",
                contactName = "مورد الذهب الرئيسي",
                lastMessage = "تم استلام السبيكة"
            )
        )
        db.messageDao().insertMessage(
            MessageEntity(
                id = "msg_v3_test",
                chatId = "chat_v3_test",
                senderId = "usr_101",
                senderName = "أنا",
                content = "تم استلام السبيكة #ذهب_21",
                timestamp = 1764100000000L
            )
        )

        val exportRes = SecureBackupVaultEngine.createEncryptedBackup(
            context = context,
            db = db,
            currentUserPhone = "+201012345678",
            encryptionPassphrase = "VaultPass#2026",
            isGoogleDriveCloudExport = false
        )
        assertTrue(exportRes is BackupResult.Success)
        val exportedFile = java.io.File((exportRes as BackupResult.Success).filePath!!)
        val envelopeText = exportedFile.readText(StandardCharsets.UTF_8)

        val preview = SecureBackupVaultEngine.inspectBackupMetadata(envelopeText, "+201012345678")
        assertTrue(preview.isValid)
        assertEquals(3, preview.version)
        assertTrue(preview.isPhoneMatched)
        assertTrue(preview.chatsCount >= 1)
        assertTrue(preview.messagesCount >= 1)

        // Wrong owner phone must be rejected cleanly
        val wrongOwnerRestore = SecureBackupVaultEngine.restoreEncryptedBackup(
            context = context,
            db = db,
            backupFileContent = envelopeText,
            currentUserPhone = "+201199999999",
            enteredPassphrase = "VaultPass#2026"
        )
        assertTrue(wrongOwnerRestore is BackupResult.Error)

        // Valid owner + passphrase restores cleanly without throwing
        val validRestore = SecureBackupVaultEngine.restoreEncryptedBackup(
            context = context,
            db = db,
            backupFileContent = envelopeText,
            currentUserPhone = "+201012345678",
            enteredPassphrase = "VaultPass#2026"
        )
        assertTrue(validRestore is BackupResult.Success)
        assertNotNull(db.chatDao().getChatById("chat_v3_test"))
    }

    @Test
    fun otpView_ordersTelegramFirst_handlesFallbackSwitch_andLockoutFromServer() {
        var lastVerifiedCode: String? = null
        var lastSwitchedChannel: OtpDeliveryChannel? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAppLanguage provides AppLanguage.ARABIC,
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                GoldChatTheme {
                    WhatsAppTelegramOtpView(
                        phoneNumber = "+201012345678",
                        initialChannel = OtpDeliveryChannel.TELEGRAM,
                        initialCooldownSeconds = 0,
                        fallbackNotice = null,
                        onVerifyOtpCode = { code ->
                            lastVerifiedCode = code
                            OtpVerifyUiResult.Error(
                                message = "تم قفل المحاولات مؤقتًا",
                                remainingAttempts = 0,
                                lockRemainingSeconds = 120
                            )
                        },
                        onBackClick = {},
                        onResendRequested = { ch ->
                            lastSwitchedChannel = ch
                            OtpResendUiResult.Sent(
                                activeChannel = ch,
                                cooldownSeconds = 60,
                                notice = "تم التبديل إلى واتساب وإلغاء الرمز السابق"
                            )
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        // Verify all 3 channels exist in priority order
        assertEquals(
            listOf(OtpDeliveryChannel.TELEGRAM, OtpDeliveryChannel.WHATSAPP, OtpDeliveryChannel.SMS),
            OtpDeliveryChannel.entries
        )
        composeTestRule.onNodeWithTag("otp_channel_locked_notice").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_otp_resend_now").assertIsDisplayed()

        // Resend on active channel and verify callback
        composeTestRule.onNodeWithTag("btn_otp_resend_now").performClick()
        composeTestRule.waitForIdle()
        assertEquals(OtpDeliveryChannel.TELEGRAM, lastSwitchedChannel)

        // Enter 6 digits and trigger server error lockout
        composeTestRule.onNodeWithTag("otp_hidden_input").performTextInput("654321")
        composeTestRule.waitForIdle()
        assertEquals("654321", lastVerifiedCode)
        composeTestRule.onNodeWithTag("otp_lockout_banner").assertIsDisplayed()
    }
}
