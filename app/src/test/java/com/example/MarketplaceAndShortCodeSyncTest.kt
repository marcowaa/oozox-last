package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.HashtagEntryDirection
import com.example.data.model.HashtagEntryType
import com.example.data.model.InvoiceStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserType
import com.example.data.repository.GoldChatRepository
import com.example.util.ClientCardRenderer
import com.example.util.ClientCardStyleConfig
import com.example.util.MerchantFieldCatalog
import com.example.util.ShortCodeGenerator
import com.example.util.SmartAccountantEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarketplaceAndShortCodeSyncTest {

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
    fun shortCodeGenerator_producesUnique8CharHyphenFreeCodesAndExtractsFromChat() {
        val generatedInvoices = (1..25).map { ShortCodeGenerator.generateInvoiceCode() }.toSet()
        val generatedOps = (1..25).map { ShortCodeGenerator.generateOperationCode() }.toSet()

        assertEquals(25, generatedInvoices.size)
        assertEquals(25, generatedOps.size)

        generatedInvoices.forEach { code ->
            assertEquals(8, code.length)
            assertFalse(code.contains('-'))
            assertTrue(code.startsWith("OX"))
            assertTrue(ShortCodeGenerator.CODE_REGEX.matches(code))
        }

        generatedOps.forEach { code ->
            assertEquals(8, code.length)
            assertFalse(code.contains('-'))
            assertTrue(code.startsWith("TX"))
            assertTrue(ShortCodeGenerator.CODE_REGEX.matches(code))
        }

        val chatText = "تم إنشاء فاتورة رقم OX7K9M2A والعملية TX4B8N2Q ضمن #توريد"
        val extracted = ShortCodeGenerator.extractAllCodes(chatText)
        assertEquals(listOf("OX7K9M2A", "TX4B8N2Q"), extracted)
    }

    @Test
    fun merchantFieldCatalog_supportsCustomFieldsWithAutoSuggestionsAcrossIndustries() {
        assertTrue(MerchantFieldCatalog.defaultFields.size >= 8)
        assertTrue(MerchantFieldCatalog.defaultFields.any { it.id == "contracting" })
        assertTrue(MerchantFieldCatalog.defaultFields.any { it.id == "tech_software" })

        val customName = "صيانة سيارات هايبرد"
        val created = MerchantFieldCatalog.addCustomField(context, customName)
        assertTrue(created.isCustom)
        assertEquals(customName, created.nameAr)
        assertTrue(created.suggestedJobTitle.contains(customName))
        assertTrue(created.suggestedBio.contains(customName))
        assertTrue(created.suggestedSkills.isNotEmpty())
        assertTrue(created.ratingCriteria.first.contains(customName))

        val loaded = MerchantFieldCatalog.loadAllFields(context)
        assertTrue(loaded.any { it.nameAr == customName })

        MerchantFieldCatalog.setSelectedField(context, "merchant_custom_1", customName)
        assertEquals(customName, MerchantFieldCatalog.getSelectedField(context, "merchant_custom_1"))
    }

    @Test
    fun repositoryAndAiAgent_createAndLookup8CharShortCodeInvoiceWithCardAvatar() = runBlocking {
        val chatId = "chat_short_${UUID.randomUUID().toString().take(8)}"
        db.chatDao().insertChat(
            ChatEntity(
                id = chatId,
                contactId = "c_short",
                contactName = "مؤسسة الفجر",
                contactType = UserType.BUSINESSMAN.name
            )
        )

        val res = repository.recordChatHashtagOperation(
            chatId = chatId,
            tag = "مقاولات",
            entryType = HashtagEntryType.INVOICE,
            description = "دفعة تشغيل",
            amount = 18500.0,
            direction = HashtagEntryDirection.DEBIT,
            actorId = "u_me",
            actorName = "أحمد",
            isExternalBusiness = true
        )

        val inv = res.invoice!!
        assertEquals(8, inv.invoiceNumber.length)
        assertFalse(inv.invoiceNumber.contains('-'))
        assertTrue(inv.invoiceNumber.startsWith("OX"))

        val lookedUp = repository.getInvoiceByNumberOrIdSync(inv.invoiceNumber)
        assertNotNull(lookedUp)
        assertEquals(inv.id, lookedUp!!.id)

        val snapshot = SmartAccountantEngine.GlobalAccountingSnapshot(
            invoices = listOf(inv),
            hashtagAccounts = listOf(res.hashtagAccount)
        )
        val aiReply = SmartAccountantEngine.askGlobalAccountant("ابحث عن ${inv.invoiceNumber}", snapshot)
        assertTrue(aiReply.contains(inv.invoiceNumber))
        assertTrue(aiReply.contains("مؤسسة الفجر"))

        // Render card PNG with a real avatar image file
        val avatarFile = File(context.cacheDir, "test_avatar.png")
        val avatarBmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        FileOutputStream(avatarFile).use { out ->
            avatarBmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        avatarBmp.recycle()

        val user = UserEntity(
            id = "u_store_avatar",
            name = "م. طارق",
            phone = "01055566677",
            username = "tarek_pro",
            avatarUri = Uri.fromFile(avatarFile).toString(),
            traderShopName = "مركز الهايبرد المعتمد",
            jobTitle = "صيانة سيارات هايبرد"
        )
        val png = ClientCardRenderer.renderClientCardPngFile(
            context = context,
            user = user,
            config = ClientCardStyleConfig(
                displayName = "مركز الهايبرد المعتمد",
                roleTitle = "صيانة سيارات هايبرد",
                bioText = "فحص كمبيوتر وبطاريات هايبرد",
                storeText = "مركز الهايبرد",
                phoneText = user.phone
            ),
            trustScore = 99,
            reliabilitySummary = "موثوق على OOZOX"
        )
        assertNotNull(png)
        assertTrue(png!!.exists() && png.length() > 1024L)
    }

    @Test
    fun chatFolderPrefs_supportsRenameDeleteAndRestoreForBuiltInAndCustomGroups() {
        val initial = com.example.util.ChatFolderPrefs.loadTopGroups(context)
        assertTrue(initial.any { it.id == com.example.util.ChatFolderPrefs.KEY_UNREAD && it.label == "غير مقروءة" && !it.isHidden })
        assertTrue(initial.any { it.id == com.example.util.ChatFolderPrefs.KEY_GOLD_TRADER && it.label == "تجار ذهب" && !it.isHidden })

        // Rename built-in group
        val renamed = com.example.util.ChatFolderPrefs.renameTopGroup(
            context,
            com.example.util.ChatFolderPrefs.KEY_GOLD_TRADER,
            "عملاء VIP"
        )
        assertEquals(
            "عملاء VIP",
            renamed.first { it.id == com.example.util.ChatFolderPrefs.KEY_GOLD_TRADER }.label
        )

        // Delete (hide) built-in group
        val deleted = com.example.util.ChatFolderPrefs.deleteTopGroup(
            context,
            com.example.util.ChatFolderPrefs.KEY_ARCHIVED
        )
        assertTrue(deleted.first { it.id == com.example.util.ChatFolderPrefs.KEY_ARCHIVED }.isHidden)

        // Restore built-in defaults
        val restored = com.example.util.ChatFolderPrefs.restoreBuiltInDefaults(context)
        assertEquals(
            "تجار ذهب",
            restored.first { it.id == com.example.util.ChatFolderPrefs.KEY_GOLD_TRADER }.label
        )
        assertFalse(restored.first { it.id == com.example.util.ChatFolderPrefs.KEY_ARCHIVED }.isHidden)
    }
}
