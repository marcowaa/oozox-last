package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.GoldChatRepository
import com.example.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackendSyncAndIntegrityTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: GoldChatRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getInstance(context)
        repository = GoldChatRepository(context)
    }

    @Test
    fun databaseV26_opensCleanly_andIntegrityCheckHealthy() = runBlocking {
        val version = db.openHelper.readableDatabase.version
        assertEquals(28, version)

        val report = DatabaseIntegrityChecker.performIntegrityCheck(context)
        assertEquals(28, report.currentVersion)
        assertTrue(report.isVersionValid)
        assertTrue(report.isQuickCheckOk)
        assertEquals(IntegrityStatus.HEALTHY, report.status)
    }

    @Test
    fun autoProcessHashtagFromMessage_ignoresCasualChat_andSyncsExplicitHashtagInvoice() = runBlocking {
        val chatId = "chat_auto_${UUID.randomUUID().toString().take(8)}"
        val chat = ChatEntity(
            id = chatId,
            contactId = "client_${UUID.randomUUID().toString().take(6)}",
            contactName = "عميل اختبار الهاشتاج",
            contactType = UserType.BUSINESSMAN.name
        )
        db.chatDao().insertChat(chat)

        // Casual message without # must NOT create any accounting entry or invoice
        val casualRes = repository.autoProcessHashtagFromMessage(
            chatId = chatId,
            content = "مرحباً تم تحويل 5000 جنيه للمراجعة فقط",
            senderId = "u_me",
            senderName = "المهندس محمود"
        )
        assertNull(casualRes)
        assertTrue(db.invoiceDao().getInvoicesByChatIdSync(chatId).isEmpty())

        // Message with explicit #hashtag + amount MUST sync Hashtag + Invoice + Encrypted DailyRecord
        val taggedRes = repository.autoProcessHashtagFromMessage(
            chatId = chatId,
            content = "توريد خامات #فاتورة 5000",
            senderId = "u_me",
            senderName = "المهندس محمود"
        )
        assertNotNull(taggedRes)

        val invoices = db.invoiceDao().getInvoicesByChatIdSync(chatId)
        assertEquals(1, invoices.size)
        val inv = invoices.first()
        assertEquals(5000.0, inv.totalAmount, 0.01)
        assertEquals(InvoiceStatus.PENDING.name, inv.status)
        assertEquals("فاتورة", inv.hashtagTag)

        val hashtags = db.chatAccountDao().getHashtagsForChatSync(chatId)
        assertTrue(hashtags.any { it.tag == "فاتورة" })

        val dailyRecords = repository.allDailyAccountingRecords.first()
        assertTrue(dailyRecords.any { it.groupId == chatId && it.subGroupId == "#فاتورة" })
    }

    @Test
    fun recordChatHashtagAndPayInvoiceWithSync_updatesInvoiceProjectHashtagAndAuditLogs() = runBlocking {
        val projectId = "proj_sync_${UUID.randomUUID().toString().take(8)}"
        val chatId = "chat_sync_${UUID.randomUUID().toString().take(8)}"
        val clientId = "client_${UUID.randomUUID().toString().take(6)}"

        db.projectDao().insertProject(
            ProjectEntity(
                id = projectId,
                workGroupId = "wg_sync",
                name = "برج النيل التجاري",
                type = ProjectType.CONSTRUCTION.name,
                estimatedCapital = 100000.0,
                totalIncome = 0.0,
                totalExpenses = 0.0
            )
        )
        db.chatDao().insertChat(
            ChatEntity(
                id = chatId,
                contactId = clientId,
                contactName = "مؤسسة الأفق",
                contactType = UserType.BUSINESSMAN.name
            )
        )

        // Step 1: Record DEBIT invoice from chat linked to project + external business flag
        val syncResult = repository.recordChatHashtagOperation(
            chatId = chatId,
            tag = "تشطيبات",
            entryType = HashtagEntryType.INVOICE,
            description = "دفعة مقاولات مرحلة أولى",
            amount = 10000.0,
            direction = HashtagEntryDirection.DEBIT,
            actorId = "u_owner",
            actorName = "المهندس محمود",
            projectId = projectId,
            projectName = "برج النيل التجاري",
            paymentMethod = "كاش",
            isExternalBusiness = true
        )

        val createdInv = syncResult.invoice
        assertNotNull(createdInv)
        assertEquals(InvoiceStatus.PENDING.name, createdInv!!.status)
        assertEquals(10000.0, createdInv.totalAmount, 0.01)
        assertTrue(createdInv.isExternalBusiness)
        assertEquals(10000.0, syncResult.remainingBalance, 0.01)
        assertFalse(syncResult.isSettled)

        // Verify project expense incremented
        val projAfterInv = db.projectDao().getProjectById(projectId)!!
        assertEquals(10000.0, projAfterInv.totalExpenses, 0.01)

        // Step 2: Partial payment 4000 -> PARTIAL
        val partialInv = repository.payInvoiceWithSync(
            invoiceId = createdInv.id,
            paymentAmount = 4000.0,
            paymentMethod = "تحويل بنكي",
            actorId = "u_owner",
            actorName = "المهندس محمود",
            notes = "دفعة أولى"
        )
        assertEquals(InvoiceStatus.PARTIAL.name, partialInv.status)
        assertEquals(4000.0, partialInv.paidAmount, 0.01)

        val projAfterPartial = db.projectDao().getProjectById(projectId)!!
        assertEquals(4000.0, projAfterPartial.totalIncome, 0.01)

        // Hashtag still active with 6000 remaining
        val hashAfterPartial = db.chatAccountDao().getHashtagByIdSync(syncResult.hashtagAccount.id)!!
        assertEquals(HashtagStatus.ACTIVE.name, hashAfterPartial.status)

        // Step 3: Final payment 6000 -> PAID + Hashtag SETTLED
        val paidInv = repository.payInvoiceWithSync(
            invoiceId = createdInv.id,
            paymentAmount = 6000.0,
            paymentMethod = "كاش",
            actorId = "u_owner",
            actorName = "المهندس محمود",
            notes = "تصفية كاملة"
        )
        assertEquals(InvoiceStatus.PAID.name, paidInv.status)
        assertEquals(10000.0, paidInv.paidAmount, 0.01)

        val hashSettled = db.chatAccountDao().getHashtagByIdSync(syncResult.hashtagAccount.id)!!
        assertEquals(HashtagStatus.SETTLED.name, hashSettled.status)

        val projFinal = db.projectDao().getProjectById(projectId)!!
        assertEquals(10000.0, projFinal.totalIncome, 0.01)

        val auditLogs = db.projectAuditLogDao().getAuditLogsForProject(projectId).first()
        assertTrue(auditLogs.size >= 3)

        val ledgerEntries = db.immutableLedgerDao().getAllLedgerEntries().first()
        assertTrue(ledgerEntries.any { it.notes?.contains(createdInv.invoiceNumber) == true })

        val messages = repository.getMessagesForChat(chatId).first()
        assertTrue(messages.any { it.type == MessageType.INVOICE.name && it.content.contains("#تشطيبات") })
        assertTrue(messages.any { it.type == MessageType.PAYMENT.name && it.content.contains(createdInv.invoiceNumber) })
    }

    @Test
    fun smartExcelSheet_persistsAndRestoresViaRoomDao() = runBlocking {
        val projectId = "proj_excel_${UUID.randomUUID().toString().take(8)}"
        val template = SmartExcelTemplateEngine.getTemplateForActivity(ProjectActivityType.CONSTRUCTION)
        val rows = listOf(
            SmartSpreadsheetRow(
                id = "row_1",
                rowNumber = 1,
                title = "توريد أسمنت",
                category = "مواد بناء",
                values = mutableMapOf(
                    "quantity" to 10.0,
                    "unit_price" to 2500.0,
                    "expenses" to 1500.0,
                    "revenue" to 32000.0
                ),
                calculatedResults = mutableMapOf("total_cost" to 26500.0, "net_profit" to 5500.0),
                notes = "تشغيل أول"
            )
        )

        repository.saveSmartExcelSheet(projectId, template, rows)
        val (loadedTpl, loadedRows) = repository.loadSmartExcelSheet(projectId, ProjectType.CONSTRUCTION.name)

        assertEquals(template.templateName, loadedTpl.templateName)
        assertEquals(1, loadedRows.size)
        assertEquals("توريد أسمنت", loadedRows.first().title)
        assertEquals(10.0, (loadedRows.first().values["quantity"] as? Number)?.toDouble() ?: 0.0, 0.01)
        assertEquals("تشغيل أول", loadedRows.first().notes)
    }

    @Test
    fun simpleExcelEngine_unifiedBusinessAccountingAndCsvExport_computesAndWritesBomCsv() {
        val projects = listOf(
            ProjectEntity(
                id = "p1",
                workGroupId = "wg1",
                name = "مشروع التجمع",
                type = ProjectType.CONSTRUCTION.name,
                estimatedCapital = 200000.0,
                totalIncome = 80000.0,
                totalExpenses = 30000.0
            )
        )
        val invoices = listOf(
            InvoiceEntity(
                id = "i1",
                invoiceNumber = "INV-101",
                title = "تشغيل خارجي",
                category = InvoiceCategory.GENERAL.name,
                senderId = "me",
                senderName = "محمود",
                receiverId = "c1",
                receiverName = "شركة النور",
                totalAmount = 20000.0,
                paidAmount = 5000.0,
                status = InvoiceStatus.PARTIAL.name,
                isExternalBusiness = true,
                sourceType = "EXTERNAL",
                hashtagTag = "مقاولات"
            )
        )
        val hashtags = listOf(
            HashtagAccountEntity(id = "h1", chatId = "ch1", accountId = "a1", tag = "مقاولات", creatorId = "me", status = HashtagStatus.ACTIVE.name)
        )
        val hashEntries = listOf(
            HashtagEntryEntity(id = "e1", hashtagId = "h1", accountId = "a1", type = HashtagEntryType.INVOICE.name, description = "أعمال", amount = 20000.0, direction = HashtagEntryDirection.DEBIT.name, createdBy = "me"),
            HashtagEntryEntity(id = "e2", hashtagId = "h1", accountId = "a1", type = HashtagEntryType.PAYMENT.name, description = "دفعة", amount = 5000.0, direction = HashtagEntryDirection.CREDIT.name, createdBy = "me")
        )
        val dailyRecords = listOf(
            DailyAccountingRecordEntity(id = "d1", groupId = "p1", entryDate = "2026-01-01", staffId = "me", staffName = "محمود", staffRole = "مدير", dailyRevenue = 10000.0, incomingAmount = 2000.0, outgoingAmount = 3000.0)
        )

        val summary = SimpleExcelEngine.calculateUnifiedBusinessAccounting(
            projects = projects,
            invoices = invoices,
            hashtagAccounts = hashtags,
            hashtagEntries = hashEntries,
            dailyRecords = dailyRecords
        )

        assertEquals(80000.0, summary.projectsIncome, 0.01)
        assertEquals(30000.0, summary.projectsExpenses, 0.01)
        assertEquals(20000.0, summary.invoicesTotalBilled, 0.01)
        assertEquals(5000.0, summary.invoicesPaidTotal, 0.01)
        assertEquals(15000.0, summary.invoicesOutstanding, 0.01)
        assertEquals(20000.0, summary.externalBusinessTotal, 0.01)
        assertEquals(15000.0, summary.hashtagsNetReceivable, 0.01)
        // combinedInflow = 80000 + 5000 + 10000 + 2000 = 97000; combinedOutflow = 30000 + 3000 = 33000; net = 64000
        assertEquals(97000.0, summary.combinedTotalInflow, 0.01)
        assertEquals(33000.0, summary.combinedTotalOutflow, 0.01)
        assertEquals(64000.0, summary.combinedNetPosition, 0.01)
        assertEquals(1, summary.unpaidInvoicesCount)

        val csvFile = SimpleExcelEngine.exportUnifiedBusinessWorkbookCsv(
            context = context,
            companyTitle = "مؤسسة الفقي",
            projects = projects,
            invoices = invoices,
            hashtagAccounts = hashtags,
            hashtagEntries = hashEntries,
            dailyRecords = dailyRecords
        )
        assertNotNull(csvFile)
        assertTrue(csvFile!!.exists())
        val bytes = csvFile.readBytes()
        assertTrue(bytes.size > 50)
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
        val text = csvFile.readText(Charsets.UTF_8)
        assertTrue(text.contains("INV-101"))
        assertTrue(text.contains("مشروع التجمع"))
        assertTrue(text.contains("#مقاولات"))
    }

    @Test
    fun smartAccountantEngine_globalSearchAndQuestions_returnsGroundedData() {
        val snapshot = SmartAccountantEngine.GlobalAccountingSnapshot(
            invoices = listOf(
                InvoiceEntity(
                    id = "i_ai",
                    invoiceNumber = "INV-990",
                    title = "توريد سبائك",
                    category = InvoiceCategory.GOLD_DEAL.name,
                    senderId = "me",
                    senderName = "محمود",
                    receiverId = "c_gold",
                    receiverName = "شركة الصاغة المتحدة",
                    totalAmount = 75000.0,
                    paidAmount = 25000.0,
                    status = InvoiceStatus.PARTIAL.name,
                    hashtagTag = "ذهب21",
                    isExternalBusiness = true
                )
            ),
            projects = listOf(
                ProjectEntity(
                    id = "p_ai",
                    workGroupId = "wg_ai",
                    name = "مول العاصمة",
                    type = ProjectType.CONSTRUCTION.name,
                    totalIncome = 120000.0,
                    totalExpenses = 45000.0
                )
            ),
            hashtagAccounts = listOf(
                HashtagAccountEntity(id = "h_ai", chatId = "c_ai", accountId = "a_ai", tag = "ذهب21", creatorId = "me")
            )
        )

        val searchRes = SmartAccountantEngine.searchGlobalRecords("ابحث عن ذهب21", snapshot)
        assertEquals(1, searchRes.matchedInvoices.size)
        assertEquals(1, searchRes.matchedHashtags.size)

        val unpaidAnswer = SmartAccountantEngine.askGlobalAccountant("ما هي الفواتير غير مدفوعة المتبقية؟", snapshot)
        assertTrue(unpaidAnswer.contains("INV-990"))
        assertTrue(unpaidAnswer.contains(GoldUtils.formatCurrency(50000.0)))

        val searchAnswer = SmartAccountantEngine.askGlobalAccountant("ابحث عن مول العاصمة", snapshot)
        assertTrue(searchAnswer.contains("مول العاصمة"))
    }

    @Test
    fun clientCardRenderer_generatesMutualPromoCardPngAndSocialCaption() {
        val user = UserEntity(
            id = "u_card",
            name = "م. محمود الفقي",
            phone = "01012345678",
            username = "mahmoud_pro",
            type = UserType.BUSINESSMAN.name,
            jobTitle = "مدير مشروعات ومقاولات",
            isVerified = true,
            skills = "إدارة مشروعات\nمحاسبة وتوريدات"
        )
        val config = ClientCardStyleConfig(
            displayName = user.name,
            roleTitle = "مدير مشروعات ومقاولات",
            bioText = "خبرة في إدارة الأعمال والتوريدات الموثقة",
            storeText = "مجموعة الفقي الهندسية",
            phoneText = user.phone
        )

        val caption = ClientCardRenderer.buildSocialCaption(
            config = config,
            username = user.username,
            trustScore = 98,
            reliabilitySummary = "سجل التزام 98%",
            projectsCount = 4,
            completedDealsCount = 12,
            skills = user.skills
        )
        assertTrue(caption.contains("OOZOX"))
        assertTrue(caption.contains("https://oozox.com/card/mahmoud_pro"))
        assertTrue(caption.contains("https://oozox.com/download?card=mahmoud_pro"))
        assertTrue(caption.contains("4 مشاريع"))

        val png = ClientCardRenderer.renderClientCardPngFile(
            context = context,
            user = user,
            config = config,
            trustScore = 98,
            reliabilitySummary = "سجل التزام 98%",
            projectsCount = 4,
            completedDealsCount = 12,
            skills = user.skills
        )
        assertNotNull(png)
        assertTrue(png!!.exists())
        assertTrue(png.length() > 1024L)
    }
}
