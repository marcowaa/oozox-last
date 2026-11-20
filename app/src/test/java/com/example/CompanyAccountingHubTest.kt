package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyAccountingRecordEntity
import com.example.ui.components.AccountingTaskbar
import com.example.ui.screens.DashboardContent
import com.example.ui.theme.GoldChatTheme
import com.example.util.SimpleExcelEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class CompanyAccountingHubTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun computeSummary_calculatesNetCashflowRevenueOutgoingAndDeficitAccurately() {
        val records = listOf(
            DailyAccountingRecordEntity(
                id = "rec_1",
                groupId = "grp_1",
                entryDate = "2026-11-16",
                dailyRevenue = 25000.0,
                incomingAmount = 5000.0,
                outgoingAmount = 4000.0,
                deficitAmount = 1000.0,
                complaintsCount = 1,
                staffId = "s_1",
                staffName = "محاسب 1",
                staffRole = "محاسب"
            ),
            DailyAccountingRecordEntity(
                id = "rec_2",
                groupId = "grp_1",
                entryDate = "2026-11-16",
                dailyRevenue = 10000.0,
                incomingAmount = 0.0,
                outgoingAmount = 2500.0,
                deficitAmount = 0.0,
                complaintsCount = 0,
                staffId = "s_2",
                staffName = "محاسب 2",
                staffRole = "محاسب"
            )
        )

        val summary = SimpleExcelEngine.computeSummary(records, initialCapital = 100000.0)

        assertEquals(35000.0, summary.totalRevenue, 0.01)
        assertEquals(5000.0, summary.totalIncoming, 0.01)
        assertEquals(6500.0, summary.totalOutgoing, 0.01)
        assertEquals(1000.0, summary.totalDeficit, 0.01)
        assertEquals(32500.0, summary.netCashflow, 0.01)
        assertEquals(1, summary.totalComplaints)
        assertEquals(2, summary.recordsCount)
    }

    @Test
    fun accountingTaskbar_switchesSectionsAndTriggersVoiceFab() {
        var selectedSection = -1
        var voiceClicked = false

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            GoldChatTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AccountingTaskbar(
                        activeSection = -1,
                        onSectionSelected = { selectedSection = it },
                        onVoiceRecordClick = { voiceClicked = true },
                        recordsCount = 12,
                        groupsCount = 4,
                        alertsCount = 2
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("accounting_tab_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("accounting_tab_ledger").assertIsDisplayed()
        composeTestRule.onNodeWithTag("accounting_tab_groups").assertIsDisplayed()
        composeTestRule.onNodeWithTag("accounting_tab_staff").assertIsDisplayed()
        composeTestRule.onNodeWithTag("floating_voice_recording_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("accounting_tab_ledger").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(50)
        assertEquals(1, selectedSection)

        composeTestRule.onNodeWithTag("accounting_tab_groups").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(50)
        assertEquals(2, selectedSection)

        composeTestRule.onNodeWithTag("accounting_tab_staff").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(50)
        assertEquals(3, selectedSection)

        composeTestRule.onNodeWithTag("floating_voice_recording_btn").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.mainClock.advanceTimeBy(50)
        assertTrue(voiceClicked)
    }

    @Test
    fun dashboardContent_displaysHeroKpiAndDispatchesQuickActions() {
        val summary = SimpleExcelEngine.computeSummary(
            records = listOf(
                DailyAccountingRecordEntity(
                    id = "r1",
                    groupId = "g1",
                    entryDate = "2026-11-16",
                    dailyRevenue = 18000.0,
                    incomingAmount = 2000.0,
                    outgoingAmount = 3000.0,
                    deficitAmount = 500.0,
                    staffId = "s1",
                    staffName = "أحمد",
                    staffRole = "أمين خزينة"
                )
            ),
            initialCapital = 50000.0
        )

        var quickAddTriggered = false
        var quickVoiceTriggered = false
        var navigatedSection = -99

        composeTestRule.setContent {
            GoldChatTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    DashboardContent(
                        summary = summary,
                        balance = 50000.0 + summary.netCashflow,
                        initialCapital = 50000.0,
                        todayRevenueAndIn = 20000.0,
                        todayExpenseAndOut = 3500.0,
                        pendingComplaintsCount = 2,
                        latestDeficitDate = "2026-11-16",
                        groupsCount = 3,
                        staffCount = 5,
                        hiddenProfilesCount = 2,
                        onQuickAddRecord = { quickAddTriggered = true },
                        onOpenVoiceAssistant = { quickVoiceTriggered = true },
                        onNavigate = { navigatedSection = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("accounting_hero_treasury_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("accounting_dashboard_lazy_column")
            .performScrollToNode(hasTestTag("quick_action_add_record"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quick_action_add_record").performClick()
        composeTestRule.waitForIdle()
        assertTrue(quickAddTriggered)

        composeTestRule.onNodeWithTag("quick_action_voice").performClick()
        composeTestRule.waitForIdle()
        assertTrue(quickVoiceTriggered)

        composeTestRule.onNodeWithTag("accounting_dashboard_lazy_column")
            .performScrollToNode(hasTestTag("dashboard_card_vaults"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dashboard_card_vaults").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, navigatedSection)
    }

    @Test
    fun circularCounterBadge_rendersStrictSquare1to1DimensionsForSingleDoubleAndLargeCounts() {
        composeTestRule.setContent {
            GoldChatTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.layout.Column {
                        com.example.ui.components.CircularCounterBadge(
                            count = 5,
                            containerColor = androidx.compose.ui.graphics.Color.Red,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.testTag("test_badge_single")
                        )
                        com.example.ui.components.CircularCounterBadge(
                            count = 42,
                            containerColor = androidx.compose.ui.graphics.Color.Red,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.testTag("test_badge_double")
                        )
                        com.example.ui.components.CircularCounterBadge(
                            count = 120,
                            containerColor = androidx.compose.ui.graphics.Color.Red,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.testTag("test_badge_large")
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        val singleNode = composeTestRule.onNodeWithTag("test_badge_single").fetchSemanticsNode()
        val doubleNode = composeTestRule.onNodeWithTag("test_badge_double").fetchSemanticsNode()
        val largeNode = composeTestRule.onNodeWithTag("test_badge_large").fetchSemanticsNode()

        // Verify mathematical 1:1 circular ratio: width must strictly equal height
        assertEquals("Single digit badge must have width == height", singleNode.size.width, singleNode.size.height)
        assertEquals("Double digit badge must have width == height", doubleNode.size.width, doubleNode.size.height)
        assertEquals("Large 99+ badge must have width == height", largeNode.size.width, largeNode.size.height)

        assertTrue(singleNode.size.width > 0)
        assertTrue(doubleNode.size.width >= singleNode.size.width)
        assertTrue(largeNode.size.width >= doubleNode.size.width)
    }

    @Test
    fun interpretCommand_interpretsLiveFiguresFeatureExplanationsAndRoutingAccurately() {
        val records = listOf(
            DailyAccountingRecordEntity(
                id = "r_live",
                groupId = "g1",
                entryDate = "2026-11-16",
                dailyRevenue = 50000.0,
                incomingAmount = 10000.0,
                outgoingAmount = 8000.0,
                deficitAmount = 2000.0,
                staffId = "s1",
                staffName = "طارق",
                staffRole = "مدير حسابات"
            )
        )

        // 1. Balance/deficit query returns live financial figures
        val balanceCmd = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "كم رصيدي اليوم في الخزينة",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ARABIC
        )
        assertTrue(balanceCmd is com.example.util.AccountingVoiceCommand.BalanceQuery)
        val balanceQuery = balanceCmd as com.example.util.AccountingVoiceCommand.BalanceQuery
        assertTrue(balanceQuery.details.contains("عجز") || balanceQuery.answer.isNotBlank())

        val deficitCmd = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "موقف العجز والشكاوى",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ARABIC
        )
        assertTrue(deficitCmd is com.example.util.AccountingVoiceCommand.DeficitQuery)
        val deficitQuery = deficitCmd as com.example.util.AccountingVoiceCommand.DeficitQuery
        assertTrue(deficitQuery.answer.contains("عجز") || deficitQuery.totalDeficit > 0.0)

        // 2. Explanations of features
        val explainCmd = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "اشرح لي الخزائن السرية والتشفير",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ARABIC
        )
        assertTrue(explainCmd is com.example.util.AccountingVoiceCommand.ExplainOrAnswer)
        val explanation = explainCmd as com.example.util.AccountingVoiceCommand.ExplainOrAnswer
        assertTrue(explanation.answer.isNotBlank())
        assertTrue(explanation.title.contains("الخزائن"))

        // 3. Navigation to hub section
        val navLedgerCmd = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "افتح دفتر اليومية المحاسبي",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ARABIC
        )
        assertTrue(navLedgerCmd is com.example.util.AccountingVoiceCommand.SectionNavigation)
        assertEquals(1, (navLedgerCmd as com.example.util.AccountingVoiceCommand.SectionNavigation).sectionIndex)

        // 4. Navigation to top-level app destination
        val navGoldCmd = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "افتح قسم الذهب والمعادن",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ARABIC
        )
        assertTrue(navGoldCmd is com.example.util.AccountingVoiceCommand.OpenAppScreen)
        assertEquals(com.example.util.AppNavDestination.GOLD, (navGoldCmd as com.example.util.AccountingVoiceCommand.OpenAppScreen).destination)

        // 5. Navigation to hub modal
        val disputesCmd = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "افتح شكاوى الفواتير",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ARABIC
        )
        assertTrue(disputesCmd is com.example.util.AccountingVoiceCommand.OpenHubModal)
        assertEquals(com.example.util.HubModalType.INVOICE_COMPLAINTS, (disputesCmd as com.example.util.AccountingVoiceCommand.OpenHubModal).modalType)
    }

    @Test
    fun accountingI18n_mirrorsCleanlyBetweenArabicAndEnglish() {
        assertEquals("المركز المحاسبي", com.example.util.AccountingI18n.get("hub_title", com.example.util.AppLanguage.ARABIC))
        assertEquals("Accounting Hub", com.example.util.AccountingI18n.get("hub_title", com.example.util.AppLanguage.ENGLISH))

        assertEquals("صافي الخزينة", com.example.util.AccountingI18n.get("net_treasury", com.example.util.AppLanguage.ARABIC))
        assertEquals("Net Treasury", com.example.util.AccountingI18n.get("net_treasury", com.example.util.AppLanguage.ENGLISH))

        assertEquals("إغلاق القائمة", com.example.util.AccountingI18n.get("close_drawer", com.example.util.AppLanguage.ARABIC))
        assertEquals("Close Menu", com.example.util.AccountingI18n.get("close_drawer", com.example.util.AppLanguage.ENGLISH))
    }

    @Test
    fun accountingI18n_coversNewExecutiveKeysInArabicAndEnglish() {
        val newKeys = listOf(
            "refresh",
            "encrypting",
            "backup_passcode",
            "retro_date_custom",
            "retro_deduct",
            "retro_add",
            "retro_impact",
            "deficit_entries_count",
            "alert_admin",
            "net_filtered",
            "view_attachment",
            "adherence_rate",
            "total_hours",
            "accept_deduction",
            "reject_with_proof",
            "staff_reply_reason",
            "attach_proof_image",
            "attach_proof_video"
        )
        newKeys.forEach { key ->
            val arVal = com.example.util.AccountingI18n.get(key, com.example.util.AppLanguage.ARABIC)
            val enVal = com.example.util.AccountingI18n.get(key, com.example.util.AppLanguage.ENGLISH)
            assertTrue("Missing or fallback Arabic translation for $key", arVal.isNotBlank() && arVal != key)
            assertTrue("Missing or fallback English translation for $key", enVal.isNotBlank() && enVal != key)
        }
    }

    @Test
    fun interpretCommand_supportsEnglishBalanceExplanationsAndRouting() {
        val records = listOf(
            DailyAccountingRecordEntity(
                id = "r_en",
                groupId = "g1",
                entryDate = "2026-11-16",
                dailyRevenue = 40000.0,
                incomingAmount = 5000.0,
                outgoingAmount = 12000.0,
                deficitAmount = 1500.0,
                staffId = "s1",
                staffName = "Omar",
                staffRole = "Controller"
            )
        )

        val balanceCmd = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "What is my balance?",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ENGLISH
        )
        assertTrue(balanceCmd is com.example.util.AccountingVoiceCommand.BalanceQuery)
        val balanceQuery = balanceCmd as com.example.util.AccountingVoiceCommand.BalanceQuery
        assertTrue(balanceQuery.details.contains("Inflow") && balanceQuery.details.contains("Deficit"))

        val explainVaults = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "Explain Secret Vaults",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ENGLISH
        )
        assertTrue(explainVaults is com.example.util.AccountingVoiceCommand.ExplainOrAnswer)
        val explainResp = explainVaults as com.example.util.AccountingVoiceCommand.ExplainOrAnswer
        assertTrue(explainResp.title.contains("Vaults"))
        assertTrue(explainResp.suggestedAction is com.example.util.SuggestedAssistantAction.OpenHubSection)

        val openJournal = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "Open Journal",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ENGLISH
        )
        assertTrue(openJournal is com.example.util.AccountingVoiceCommand.SectionNavigation)
        assertEquals(1, (openJournal as com.example.util.AccountingVoiceCommand.SectionNavigation).sectionIndex)

        val openGold = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "Open Gold",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ENGLISH
        )
        assertTrue(openGold is com.example.util.AccountingVoiceCommand.OpenAppScreen)
        assertEquals(com.example.util.AppNavDestination.GOLD, (openGold as com.example.util.AccountingVoiceCommand.OpenAppScreen).destination)

        val openDispute = com.example.util.AccountingSpeechToTextEngine.interpretCommand(
            rawText = "Open invoice dispute",
            records = records,
            groups = emptyList(),
            language = com.example.util.AppLanguage.ENGLISH
        )
        assertTrue(openDispute is com.example.util.AccountingVoiceCommand.OpenHubModal)
        assertEquals(com.example.util.HubModalType.INVOICE_COMPLAINTS, (openDispute as com.example.util.AccountingVoiceCommand.OpenHubModal).modalType)
    }

    @Test
    fun editorialHeader_mirrorsStartEdgeDrawerButtonAcrossRtlAndLtr() {
        var rtlClicks = 0
        var ltrClicks = 0

        composeTestRule.setContent {
            GoldChatTheme {
                androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
                    ) {
                        com.example.ui.components.OozoxEditorialHeader(
                            title = "المركز المحاسبي",
                            onOpenDrawer = { rtlClicks++ },
                            drawerButtonTestTag = "drawer_btn_rtl",
                            modifier = Modifier.testTag("header_rtl")
                        )
                    }
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                    ) {
                        com.example.ui.components.OozoxEditorialHeader(
                            title = "Accounting Hub",
                            onOpenDrawer = { ltrClicks++ },
                            drawerButtonTestTag = "drawer_btn_ltr",
                            modifier = Modifier.testTag("header_ltr")
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        val rtlHeaderBounds = composeTestRule.onNodeWithTag("header_rtl").fetchSemanticsNode().boundsInRoot
        val rtlDrawerBounds = composeTestRule.onNodeWithTag("drawer_btn_rtl").fetchSemanticsNode().boundsInRoot
        val ltrHeaderBounds = composeTestRule.onNodeWithTag("header_ltr").fetchSemanticsNode().boundsInRoot
        val ltrDrawerBounds = composeTestRule.onNodeWithTag("drawer_btn_ltr").fetchSemanticsNode().boundsInRoot

        // In RTL, Start edge sits on the right half of the header; in LTR, Start edge sits on the left half.
        assertTrue("RTL drawer button must sit on the right Start edge", rtlDrawerBounds.center.x > rtlHeaderBounds.center.x)
        assertTrue("LTR drawer button must sit on the left Start edge", ltrDrawerBounds.center.x < ltrHeaderBounds.center.x)

        composeTestRule.onNodeWithTag("drawer_btn_rtl").performClick()
        composeTestRule.onNodeWithTag("drawer_btn_ltr").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, rtlClicks)
        assertEquals(1, ltrClicks)
    }
}
