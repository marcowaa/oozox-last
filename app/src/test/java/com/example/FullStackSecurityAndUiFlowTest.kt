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
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.FinancialComplaintEntity
import com.example.data.model.GoldPricesModel
import com.example.data.model.GoldPurchaseEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectType
import com.example.data.model.ShiftHandoverEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.screens.GoldScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.theme.GoldChatTheme
import com.example.util.ApiCreateInvoiceRequest
import com.example.util.ApiInvoiceItemDto
import com.example.util.ApiLoginRequest
import com.example.util.ApiUpdateInvoiceStatusRequest
import com.example.util.DynamicBackendFormulaProcessor
import com.example.util.ForensicHandoverAnalysisEngine
import com.example.util.HiddenAccountSecurityEngine
import com.example.util.RetrofitClient
import com.example.util.RoomLocalAES256Engine
import com.example.util.SecurityValidator
import com.example.util.SimpleExcelEngine
import com.example.util.SmartAccountantEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class FullStackSecurityAndUiFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private var localServerSocket: ServerSocket? = null
    private val serverRunning = AtomicBoolean(false)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getInstance(context)
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
        RetrofitClient.setAuthToken(null)
        RetrofitClient.configureBaseUrl("https://api.oozox.com/api/")
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
                        var prev = -1
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
                            prev = cur
                        }

                        val headerText = headerBuilder.toString()
                        val lines = headerText.split("\r\n").filter { it.isNotBlank() }
                        if (lines.isEmpty()) return@use

                        val requestParts = lines.first().split(" ")
                        val method = requestParts.getOrElse(0) { "GET" }
                        val rawPath = requestParts.getOrElse(1) { "/" }
                        val path = rawPath.substringBefore("?")

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
                            500 -> "Internal Server Error"
                            else -> "Status"
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
    fun retrofitClient_injectsBearerToken_parsesBackendEnvelopes_andHandlesErrorsSafely() = runBlocking {
        val capturedAuthHeaders = CopyOnWriteArrayList<String>()

        val port = startEmbeddedHttpServer { method, path, headers, reqBody ->
            capturedAuthHeaders.add(headers["authorization"].orEmpty())
            when {
                method == "GET" && path == "/api/health" -> 200 to
                    """{"success":true,"message":"OK","data":{"status":"OK","uptime":42.5}}"""

                method == "POST" && path == "/api/auth/login" -> {
                    assertTrue(reqBody.contains("merchant@oozox.com"))
                    200 to """
                        {
                          "success": true,
                          "message": "Authenticated",
                          "data": {
                            "user": {"id":"u_101","name":"تاجر الذهب","email":"merchant@oozox.com","phone":"01012345678","role":"MERCHANT"},
                            "token": "jwt_signed_token_xyz_123"
                          }
                        }
                    """.trimIndent()
                }

                method == "GET" && path == "/api/invoices" -> 200 to """
                    {
                      "success": true,
                      "message": "Fetched",
                      "data": {
                        "invoices": [
                          {"_id":"inv_1","invoiceNumber":"INV-2026-001","subTotal":10000,"taxAmount":1400,"discount":0,"totalAmount":11400,"status":"PENDING","paymentMethod":"INSTAPAY","dueDate":"2026-12-01"}
                        ],
                        "pagination": {"total":1,"page":1,"pages":1,"limit":20}
                      }
                    }
                """.trimIndent()

                method == "POST" && path == "/api/invoices" -> 201 to """
                    {
                      "success": true,
                      "message": "Created",
                      "data": {
                        "invoice": {"_id":"inv_2","invoiceNumber":"INV-2026-002","subTotal":5000,"taxAmount":0,"discount":0,"totalAmount":5000,"status":"PENDING","paymentMethod":"CASH","dueDate":"2026-12-05"}
                      }
                    }
                """.trimIndent()

                method == "PATCH" && path.endsWith("/status") -> 200 to """
                    {
                      "success": true,
                      "message": "Updated",
                      "data": {
                        "invoice": {"_id":"inv_1","invoiceNumber":"INV-2026-001","subTotal":10000,"taxAmount":1400,"discount":0,"totalAmount":11400,"status":"PAID","paymentMethod":"INSTAPAY","dueDate":"2026-12-01"}
                      }
                    }
                """.trimIndent()

                else -> 500 to """{"success":false,"message":"Simulated server fault"}"""
            }
        }

        RetrofitClient.configureBaseUrl("http://127.0.0.1:$port/api")
        val api = RetrofitClient.apiService()

        // 1. Before login: no active session, no Authorization header leaked
        assertFalse(RetrofitClient.hasActiveSession())
        val healthResp = api.getHealth()
        assertTrue(healthResp.isSuccessful)
        assertEquals("OK", healthResp.body()?.data?.status)
        assertEquals("", capturedAuthHeaders.last())

        // 2. Login & persist Bearer token
        val loginResp = api.login(ApiLoginRequest("merchant@oozox.com", "StrongPass!1"))
        assertTrue(loginResp.isSuccessful)
        val token = loginResp.body()?.data?.token.orEmpty()
        assertEquals("jwt_signed_token_xyz_123", token)
        RetrofitClient.setAuthToken(token)
        assertTrue(RetrofitClient.hasActiveSession())

        // 3. Subsequent requests carry Bearer token cleanly
        val listResp = api.getInvoices(page = 1, limit = 20)
        assertTrue(listResp.isSuccessful)
        assertEquals("Bearer jwt_signed_token_xyz_123", capturedAuthHeaders.last())
        assertEquals("INV-2026-001", listResp.body()?.data?.invoices?.firstOrNull()?.invoiceNumber)

        val createdResp = api.createInvoice(
            ApiCreateInvoiceRequest(
                receiverId = "u_202",
                items = listOf(ApiInvoiceItemDto("سبيكة ذهب 24", 1.0, 5000.0, 24)),
                dueDate = "2026-12-05"
            )
        )
        assertEquals(201, createdResp.code())
        assertEquals("INV-2026-002", createdResp.body()?.data?.invoice?.invoiceNumber)

        val patchResp = api.updateInvoiceStatus("inv_1", ApiUpdateInvoiceStatusRequest("PAID"))
        assertTrue(patchResp.isSuccessful)
        assertEquals("PAID", patchResp.body()?.data?.invoice?.status)

        // 4. Deleting hits 500 path and returns graceful non-successful response without throwing
        val errorResp = api.deleteInvoice("inv_1")
        assertFalse(errorResp.isSuccessful)
        assertEquals(500, errorResp.code())
    }

    @Test
    fun aes256GcmEngine_rejectsWrongKey_andDetectsCiphertextTampering() {
        val secretText = "عهدة خزينة القاهرة: 450,000 جنيه و 120 جرام ذهب 21"
        val ownerKey = RoomLocalAES256Engine.deriveKeyForProfile("Pin_Strong_9981", "profile_1", context)
        val wrongKey = RoomLocalAES256Engine.deriveKeyForProfile("Wrong_Pin_0000", "profile_1", context)

        val encrypted = RoomLocalAES256Engine.encrypt(secretText, ownerKey)
        assertTrue(RoomLocalAES256Engine.isEncrypted(encrypted))
        assertNotEquals(secretText, encrypted)

        // Valid key decrypts cleanly
        assertEquals(secretText, RoomLocalAES256Engine.decrypt(encrypted, ownerKey))

        // Wrong key fails GCM authentication tag and never leaks plaintext
        val wrongDecrypted = RoomLocalAES256Engine.decrypt(encrypted, wrongKey)
        assertNotEquals(secretText, wrongDecrypted)
        assertTrue(wrongDecrypted.contains("مشفر"))

        // Tampered ciphertext (flip last characters) also fails GCM authentication tag
        val tampered = encrypted.dropLast(4) + "AAAA]"
        val tamperedDecrypted = RoomLocalAES256Engine.decrypt(tampered, ownerKey)
        assertNotEquals(secretText, tamperedDecrypted)
        assertTrue(tamperedDecrypted.contains("مشفر"))
    }

    @Test
    fun hiddenAccountSecurity_saltedHash_triggers_andAntiInjectionSafeguards() {
        val salt = HiddenAccountSecurityEngine.generateSalt()
        val passwordHash = HiddenAccountSecurityEngine.hashPassword("Vault#2026", salt)
        val account = HiddenSubAccountEntity(
            id = "sub_1",
            username = "vault_boss",
            displayName = "حساب الإدارة السري",
            passwordHash = passwordHash,
            passwordSalt = salt,
            rawSecretTrigger = "*9090#"
        )

        // Valid secret & salted password verification
        assertTrue(HiddenAccountSecurityEngine.verifyPassword("*9090#", account))
        assertTrue(HiddenAccountSecurityEngine.verifyPassword("Vault#2026", account))
        assertFalse(HiddenAccountSecurityEngine.verifyPassword("wrong_pass", account))

        // Search bar trigger patterns
        assertEquals(
            account.id,
            HiddenAccountSecurityEngine.evaluateSearchTrigger("vault_boss:Vault#2026", listOf(account))?.id
        )
        assertEquals(
            account.id,
            HiddenAccountSecurityEngine.evaluateSearchTrigger("@vault_boss#Vault#2026", listOf(account))?.id
        )
        assertNull(HiddenAccountSecurityEngine.evaluateSearchTrigger("vault_boss:bad", listOf(account)))

        // SQL injection & XSS sanitization
        assertTrue(SecurityValidator.isPotentiallyMalicious("' OR '1'='1"))
        assertTrue(SecurityValidator.isPotentiallyMalicious("SELECT * FROM users UNION SELECT 1,2"))
        assertTrue(SecurityValidator.isPotentiallyMalicious("<script>alert('xss')</script>"))
        assertFalse(SecurityValidator.isPotentiallyMalicious("فاتورة مبيعات عيار 21 شركة النيل"))

        val cleaned = SecurityValidator.sanitizeInput("  عميل <script>evil()</script> موثوق\u0000  ")
        assertFalse(cleaned.contains("<script>"))
        assertFalse(cleaned.contains("\u0000"))

        // Brute-force RateLimiter locks after maxAttempts failures
        val limiter = SecurityValidator.RateLimiter(maxAttempts = 3, lockDurationMillis = 60_000L)
        assertFalse(limiter.isLocked())
        limiter.recordFailure()
        limiter.recordFailure()
        assertFalse(limiter.isLocked())
        limiter.recordFailure()
        assertTrue(limiter.isLocked())
        assertTrue(limiter.getRemainingLockSeconds() > 0)
        limiter.recordSuccess()
        assertFalse(limiter.isLocked())
    }

    @Test
    fun forensicHandoverAndFormulaProcessor_evaluateAccurately() = runBlocking {
        val complaintNoHandover = FinancialComplaintEntity(
            id = "comp_${UUID.randomUUID().toString().take(8)}",
            groupId = "grp_forensic",
            submittedByStaffId = "mgr_1",
            submittedByStaffName = "المدير العام",
            submittedByStaffRole = "مدير",
            title = "عجز يومية مسائية",
            details = "فرق عجز 1200 ج.م بالفاتورة INV-991",
            targetDate = "2099-01-01",
            amount = 1200.0,
            accusedEmployeeId = "emp_out_1",
            accusedEmployeeName = "أحمد كاشير",
            invoiceNumber = "INV-991"
        )
        db.financialComplaintDao().insertComplaint(complaintNoHandover)

        val resultNoHandover = ForensicHandoverAnalysisEngine.analyzeComplaintHandoverLiability(
            context, complaintNoHandover
        )
        assertFalse(resultNoHandover.shiftHandoverFound)
        assertEquals("OUTGOING_STAFF_SOLE_LIABILITY", resultNoHandover.liabilityType)
        assertEquals(1200.0, resultNoHandover.primaryDeductionAmount, 0.01)
        assertEquals(0.0, resultNoHandover.receiverDeductionAmount, 0.01)

        // Now record a confirmed blind-accepted shift handover on 2099-01-02 without reporting discrepancy
        val handoverId = "ho_${UUID.randomUUID().toString().take(8)}"
        db.shiftHandoverDao().insertShiftHandover(
            ShiftHandoverEntity(
                id = handoverId,
                cashierId = "emp_out_2",
                cashierName = "كريم مسائي",
                loginTime = 1_000_000L,
                logoutTime = 2_000_000L,
                totalInvoicesCount = 12,
                totalRevenueEGP = 15000.0,
                totalPaidEGP = 14000.0,
                totalIncomingTransactions = 14000.0,
                totalOutgoingTransactions = 0.0,
                productsAddedCount = 0,
                productsSoldCount = 10,
                expectedCash = 14000.0,
                actualCash = 14000.0,
                shiftDateText = "2099-01-02",
                receivedByCashierId = "emp_in_2",
                receivedByCashierName = "سعيد صباحي",
                isReceived = true,
                acceptanceType = "CONFIRMED_CLEAN",
                cashDrawerConfirmed = true,
                physicalStockChecked = true
            )
        )

        val complaintConfirmed = FinancialComplaintEntity(
            id = "comp_${UUID.randomUUID().toString().take(8)}",
            groupId = "grp_forensic",
            submittedByStaffId = "mgr_1",
            submittedByStaffName = "المدير العام",
            submittedByStaffRole = "مدير",
            title = "عجز بعد تسليم شفت مؤكد",
            details = "عجز 800 ج.م في الفاتورة INV-992",
            targetDate = "2099-01-02",
            amount = 800.0,
            accusedEmployeeId = "emp_out_2",
            accusedEmployeeName = "كريم مسائي",
            invoiceNumber = "INV-992"
        )
        db.financialComplaintDao().insertComplaint(complaintConfirmed)

        val jointResult = ForensicHandoverAnalysisEngine.analyzeComplaintHandoverLiability(
            context, complaintConfirmed
        )
        assertTrue(jointResult.shiftHandoverFound)
        assertEquals("سعيد صباحي", jointResult.receiverName)
        assertTrue(jointResult.receiverDeductionAmount > 0.0 || jointResult.primaryDeductionAmount > 0.0)

        val compiledField = DynamicBackendFormulaProcessor.compileNaturalLanguageToFormula(
            prompt = "حساب الصافي بعد خصم 14% ضريبة",
            targetFieldKey = "net_after_tax",
            targetFieldName = "الصافي بعد الضريبة",
            templateId = "tpl_1"
        )
        assertEquals("net_after_tax", compiledField.fieldKey)
        assertTrue(compiledField.generatedFormulaExpression.orEmpty().isNotBlank())
    }

    @Test
    fun highScale_snapshotSummaryAndGlobalSearch_completesFastAndAccurately() {
        val records = List(2_000) { i ->
            DailyAccountingRecordEntity(
                id = "scale_rec_$i",
                groupId = "grp_${i % 10}",
                entryDate = "2026-11-${(i % 28 + 1).toString().padStart(2, '0')}",
                dailyRevenue = 1000.0,
                incomingAmount = 200.0,
                outgoingAmount = 300.0,
                deficitAmount = if (i % 100 == 0) 50.0 else 0.0,
                complaintsCount = if (i % 200 == 0) 1 else 0,
                staffId = "s_${i % 15}",
                staffName = "محاسب ${i % 15}",
                staffRole = "محاسب"
            )
        }
        val invoices = List(500) { i ->
            InvoiceEntity(
                id = "inv_scale_$i",
                invoiceNumber = "INV-$i",
                title = "فاتورة بيع رقم $i",
                senderId = "m_1",
                senderName = "مؤسسة الذهب",
                receiverId = "c_$i",
                receiverName = if (i == 42) "عميل النخبة المميز" else "عميل رقم $i",
                totalAmount = 5000.0,
                paidAmount = if (i % 2 == 0) 5000.0 else 1000.0,
                status = if (i % 2 == 0) InvoiceStatus.PAID.name else InvoiceStatus.PARTIAL.name
            )
        }

        val elapsed = measureTimeMillis {
            val summary = SimpleExcelEngine.computeSummary(records, initialCapital = 500_000.0)
            assertEquals(2_000_000.0, summary.totalRevenue, 0.01)
            assertEquals(400_000.0, summary.totalIncoming, 0.01)
            assertEquals(600_000.0, summary.totalOutgoing, 0.01)
            assertEquals(1_000.0, summary.totalDeficit, 0.01)

            val snapshot = SmartAccountantEngine.GlobalAccountingSnapshot(
                invoices = invoices,
                dailyRecords = records
            )
            val searchResult = SmartAccountantEngine.searchGlobalRecords("النخبة المميز", snapshot)
            assertEquals(1, searchResult.matchedInvoices.size)
            assertEquals("INV-42", searchResult.matchedInvoices.first().invoiceNumber)

            val answer = SmartAccountantEngine.askGlobalAccountant("الفواتير المتبقية غير المدفوعة", snapshot)
            assertTrue(answer.isNotBlank())
        }

        assertTrue("2k records + 500 invoices snapshot search must complete < 1500ms (was ${elapsed}ms)", elapsed < 1500)
    }

    @Test
    fun composeUiCuj_projectsAndGoldScreens_renderInLightDarkAndRtl_andSupportInteractions() {
        var openedProjectId: String? = null
        var createProjectClicked = false

        val sampleGroups = listOf(
            WorkGroupEntity(id = "wg_1", name = "مجموعة النيل للمقاولات", ownerId = "u_1")
        )
        val sampleProjects = listOf(
            ProjectEntity(
                id = "proj_cuj_1",
                workGroupId = "wg_1",
                name = "برج العاصمة الإدارية",
                type = ProjectType.CONSTRUCTION.name,
                estimatedCapital = 2500000.0,
                totalIncome = 950000.0,
                totalExpenses = 410000.0
            )
        )

        // CUJ 1: ProjectsScreen under Dark Theme + RTL layout + project search & click
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                GoldChatTheme(darkTheme = true) {
                    ProjectsScreen(
                        workGroups = sampleGroups,
                        projects = sampleProjects,
                        onProjectClick = { openedProjectId = it },
                        onCreateProjectClick = { createProjectClicked = true }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("projects_search_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("برج العاصمة الإدارية").assertIsDisplayed()
        composeTestRule.onNodeWithTag("projects_search_bar").performTextInput("العاصمة")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("برج العاصمة الإدارية").performClick()
        composeTestRule.waitForIdle()
        assertEquals("proj_cuj_1", openedProjectId)
        assertFalse(createProjectClicked)
    }

    @Test
    fun composeUiCuj_goldScreen_rendersInLightRtlWithBullionRates() {
        var quickAddClicked = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                GoldChatTheme(darkTheme = false) {
                    GoldScreen(
                        goldPrices = GoldPricesModel(karat21 = 3850.0, karat24 = 4400.0),
                        purchases = listOf(
                            GoldPurchaseEntity(
                                id = "gp_1",
                                buyerId = "u_buyer",
                                extractedWeight = 10.0,
                                karat = "21",
                                pricePerGram = 3850.0,
                                currentPricePerGram = 3900.0,
                                manufacturingCost = 85.0,
                                totalPaid = 39350.0,
                                currentValue = 39000.0,
                                manufacturer = "سويس جولد"
                            )
                        ),
                        currentUser = null,
                        onUpdatePricesClick = {},
                        onAddPurchaseClick = { quickAddClicked = true },
                        onSubmitTraderRequest = { _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("الذهب والمصوغات").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_gold_open_drawer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_gold_add_quick").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_gold_add_quick").performClick()
        composeTestRule.waitForIdle()
        assertTrue(quickAddClicked)
    }
}
