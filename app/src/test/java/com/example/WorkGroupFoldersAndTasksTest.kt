package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.ContactEntity
import com.example.data.model.GroupMemberEntity
import com.example.data.model.MessageEntity
import com.example.data.model.ProjectMemberEntity
import com.example.data.model.ProjectMemberInput
import com.example.data.model.ProjectMemberRole
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.ProjectType
import com.example.data.model.UserEntity
import com.example.data.model.UserType
import com.example.data.model.WorkGroupEntity
import com.example.data.repository.GoldChatRepository
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.theme.GoldChatTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class WorkGroupFoldersAndTasksTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: GoldChatRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        context = app
        db = AppDatabase.getInstance(context)
        repository = GoldChatRepository(context)
    }

    private fun pumpMainLooperUntil(maxAttempts: Int = 50, sleepMs: Long = 50L, condition: () -> Boolean) {
        var attempts = 0
        while (attempts < maxAttempts) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(sleepMs)
            shadowOf(Looper.getMainLooper()).idle()
            attempts++
        }
    }

    @Test
    fun folderGrouping_preservesIndividualChatFlag_neverMutatesToGroupChat() = runBlocking {
        val c1Id = "indiv_folder_1_${UUID.randomUUID().toString().take(6)}"
        val c2Id = "indiv_folder_2_${UUID.randomUUID().toString().take(6)}"
        db.chatDao().insertChat(
            ChatEntity(
                id = c1Id,
                contactId = "u_1",
                contactName = "أحمد للتوريدات",
                isGroup = false
            )
        )
        db.chatDao().insertChat(
            ChatEntity(
                id = c2Id,
                contactId = "u_2",
                contactName = "مكتب النخبة",
                isGroup = false
            )
        )

        repository.createGroupChatFolder("مجلد الموردين", listOf(c1Id, c2Id))

        val updated1 = db.chatDao().getChatById(c1Id)!!
        val updated2 = db.chatDao().getChatById(c2Id)!!

        assertEquals("مجلد الموردين", updated1.groupName)
        assertEquals("مجلد الموردين", updated2.groupName)
        assertFalse("Individual chat grouped into folder must keep isGroup=false", updated1.isGroup)
        assertFalse("Individual chat grouped into folder must keep isGroup=false", updated2.isGroup)
    }

    @Test
    fun companyWithDepartments_autoCreatesGroupChats_andPropagatesManagerPermissions() = runBlocking {
        val viewModel = MainViewModel(app)
        val owner = UserEntity(
            id = "owner_mgr_1",
            name = "م. محمود المدير",
            phone = "01012345678",
            username = "mahmoud_ceo",
            type = UserType.BUSINESSMAN.name
        )
        db.userDao().insertUser(owner)
        shadowOf(Looper.getMainLooper()).idle()

        var parentGroupId = ""
        viewModel.createAccountingCompanyGroup(
            name = "مجموعة النيل القابضة",
            companyName = "شركة النيل الذهبية",
            initialCapital = 500000.0,
            departments = listOf("المبيعات", "المخازن", "الحسابات"),
            onCreated = { id -> parentGroupId = id }
        )

        pumpMainLooperUntil { parentGroupId.isNotBlank() }
        Thread.sleep(200)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Company group should be created", parentGroupId.isNotBlank())

        val parentChat = db.chatDao().getChatByGroupId(parentGroupId)
        assertNotNull("Company parent must auto-create a real group chat", parentChat)
        assertTrue("Company group chat must have isGroup=true", parentChat!!.isGroup)

        val subGroups = db.workGroupDao().getSubGroupsSync(parentGroupId)
        assertEquals(3, subGroups.size)

        subGroups.forEach { dept ->
            val deptChat = db.chatDao().getChatByGroupId(dept.id)
            assertNotNull("Department '${dept.name}' must have a real group chat", deptChat)
            assertTrue("Department group chat must have isGroup=true", deptChat!!.isGroup)

            val members = db.workGroupDao().getGroupMembersSync(dept.id)
            assertTrue(
                "Manager must have admin permissions in department '${dept.name}'",
                members.any { it.isGroupAdmin }
            )
        }
    }

    @Test
    fun projectCreation_autoCreatesWorkGroupChat_andAddsMembers() = runBlocking {
        val viewModel = MainViewModel(app)
        val owner = UserEntity(
            id = "owner_proj_1",
            name = "م. يحيى المالك",
            phone = "01055566677",
            username = "yahya_owner",
            type = UserType.BUSINESSMAN.name
        )
        db.userDao().insertUser(owner)
        shadowOf(Looper.getMainLooper()).idle()

        val uniqueProjName = "برج الأفق_${UUID.randomUUID().toString().take(5)}"
        viewModel.createWorkGroupAndProject(
            groupName = "فريق $uniqueProjName",
            groupDesc = "تشطيبات ومقاولات",
            projectName = uniqueProjName,
            capital = 250000.0,
            projectType = ProjectType.CONSTRUCTION,
            coManagerPhone = "01099887766",
            coManagerName = "م. طارق التنفيذي",
            membersInfo = "@كريم الموقع (01122334455) - مهندس موقع | @سامي المشتريات (01233445566) - مسؤول توريد"
        )

        var createdProject = db.projectDao().getAllProjects().first().firstOrNull { it.name == uniqueProjName }
        pumpMainLooperUntil {
            createdProject = runBlocking { db.projectDao().getAllProjects().first().firstOrNull { it.name == uniqueProjName } }
            val p = createdProject ?: return@pumpMainLooperUntil false
            runBlocking {
                db.chatDao().getChatByGroupId(p.workGroupId) != null &&
                    db.workGroupDao().getGroupMembersSync(p.workGroupId).size >= 3
            }
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull("Project should be persisted", createdProject)
        val groupChat = db.chatDao().getChatByGroupId(createdProject!!.workGroupId)
        assertNotNull("Project workGroup must auto-create group chat", groupChat)
        assertTrue(groupChat!!.isGroup)

        val wgMembers = db.workGroupDao().getGroupMembersSync(createdProject!!.workGroupId)
        assertTrue("Co-manager must be in WorkGroup members", wgMembers.any { it.username.contains("طارق") && it.isGroupAdmin })
        assertTrue("Parsed member كريم must be added to WorkGroup", wgMembers.any { it.username.contains("كريم") })
        assertTrue("Parsed member سامي must be added to WorkGroup", wgMembers.any { it.username.contains("سامي") })
    }

    @Test
    fun insertProjectTask_withTargetChat_postsFieldTaskMessageCardInGroupChat() = runBlocking {
        val viewModel = MainViewModel(app)
        val wgId = "wg_task_${UUID.randomUUID().toString().take(6)}"
        val wg = WorkGroupEntity(id = wgId, name = "فريق التنفيذ", ownerId = "me")
        db.workGroupDao().insertWorkGroup(wg)
        val chat = repository.ensureWorkGroupChat(wg, displayTitle = "فريق التنفيذ")

        val task = ProjectTaskEntity(
            id = "ft_chat_${UUID.randomUUID().toString().take(6)}",
            projectId = "proj_t1",
            projectName = "برج العاصمة",
            workGroupId = wgId,
            title = "تركيب واجهات الزجاج",
            description = "تنفيذ واجهة المدخل الرئيسي",
            assignedMembersList = "@أحمد_حسن",
            assignedToName = "@أحمد_حسن"
        )
        viewModel.insertProjectTask(task, targetChatId = chat.id)

        var msgs = db.messageDao().getAllMessagesForChatSync(chat.id)
        pumpMainLooperUntil {
            msgs = runBlocking { db.messageDao().getAllMessagesForChatSync(chat.id) }
            msgs.any { it.type == "FIELD_TASK" }
        }

        val taskMsg = msgs.firstOrNull { it.type == "FIELD_TASK" }
        assertNotNull("Group chat must contain FIELD_TASK message", taskMsg)
        val decrypted = com.example.util.RoomLocalAES256Engine.decryptMessage(taskMsg!!, null, context)
        assertTrue("Expected FIELD_TASK token, was: ${decrypted.content}", decrypted.content.contains("[FIELD_TASK:${task.id}]"))
    }

    @Test
    fun chatListScreen_showsIndividualChatWithFolderChip_andNotAsGroup() {
        val folderChat = ChatEntity(
            id = "c_folder_ui_1",
            contactId = "u_ui_1",
            contactName = "عمر الصايغ",
            isGroup = false,
            groupName = "كبار العملاء",
            lastMessage = "تم استلام الدفعة"
        )

        composeTestRule.setContent {
            GoldChatTheme {
                ChatListScreen(
                    chats = listOf(folderChat),
                    onChatClick = {},
                    onNewChatClick = {},
                    onOpenAIAccountant = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("عمر الصايغ").assertIsDisplayed()
        // Folder label appears both as top filter chip and as badge inside the individual chat row
        composeTestRule.onAllNodesWithText("كبار العملاء").assertCountEquals(2)
        composeTestRule.onNodeWithText("مجموعة").assertDoesNotExist()
    }

    @Test
    fun chatDetailScreen_rendersFieldTaskCard_plusAssignTask_andFixedThemeDialogButtons() {
        val wg = WorkGroupEntity(id = "wg_ui_1", name = "قسم المبيعات", ownerId = "u_me")
        val groupChat = ChatEntity(
            id = "wg_chat_wg_ui_1",
            contactId = wg.id,
            contactName = "قسم المبيعات",
            isGroup = true,
            groupId = wg.id
        )
        val task = ProjectTaskEntity(
            id = "task_ui_1",
            projectId = "proj_ui_1",
            projectName = "مشروع الواحة",
            workGroupId = wg.id,
            title = "جرد الفاترين اليومي",
            description = "مطابقة الأوزان والباركود",
            assignedMembersList = "@أحمد_حسن",
            assignedToName = "@أحمد_حسن"
        )
        val messages = listOf(
            MessageEntity(
                id = "msg_task_1",
                chatId = groupChat.id,
                senderId = "u_me",
                senderName = "المدير",
                type = "FIELD_TASK",
                content = "[FIELD_TASK:${task.id}]"
            )
        )

        composeTestRule.setContent {
            GoldChatTheme {
                ChatDetailScreen(
                    chat = groupChat,
                    messages = messages,
                    currentUser = UserEntity(id = "u_me", name = "المدير", phone = "010000", username = "admin"),
                    workGroups = listOf(wg),
                    allGroupMembers = listOf(
                        GroupMemberEntity(
                            id = "gm_1",
                            groupId = wg.id,
                            userId = "u_me",
                            username = "المدير",
                            phone = "010000",
                            roleLabel = "مدير القسم",
                            isGroupAdmin = true
                        )
                    ),
                    allContacts = listOf(
                        ContactEntity(id = "c1", name = "أحمد_حسن", phone = "011111")
                    ),
                    projectTasks = listOf(task),
                    accounts = emptyList(),
                    hashtags = emptyList(),
                    allEntries = emptyList(),
                    onBackClick = {},
                    onSendMessage = { _, _ -> },
                    onCreateAccount = {},
                    onCreateHashtagAccount = { _, _, _ -> },
                    onAddEntry = { _, _, _, _, _, _ -> },
                    onOpenGoldDeal = {},
                    onOpenAIAccountant = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify hierarchy bar + interactive task card inside group chat
        composeTestRule.onNodeWithTag("workgroup_hierarchy_strip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_field_task_card_${task.id}", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_task_proof_btn_${task.id}", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_task_verify_btn_${task.id}", useUnmergedTree = true).assertIsDisplayed()

        // Verify @mention suggestions appear when typing '@'
        composeTestRule.onNodeWithTag("chat_message_input").performTextInput("@")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_mention_suggestions_strip").assertIsDisplayed()

        // Verify + bottom sheet contains "إسناد مهمة"
        composeTestRule.onNodeWithTag("chat_attachment_plus_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_plus_assign_task_btn").assertIsDisplayed()
    }

    @Test
    fun chatThemeDialog_keepsLivePreviewAndActionButtonsFixed_withScrollableOptions() {
        val sampleChat = ChatEntity(
            id = "chat_theme_test",
            contactId = "u_theme",
            contactName = "خالد الصايغ",
            isGroup = false
        )

        composeTestRule.setContent {
            GoldChatTheme {
                ChatDetailScreen(
                    chat = sampleChat,
                    messages = emptyList(),
                    accounts = emptyList(),
                    hashtags = emptyList(),
                    allEntries = emptyList(),
                    onBackClick = {},
                    onSendMessage = { _, _ -> },
                    onCreateAccount = {},
                    onCreateHashtagAccount = { _, _, _ -> },
                    onAddEntry = { _, _, _, _, _, _ -> },
                    onOpenGoldDeal = {},
                    onOpenAIAccountant = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("chat_messenger_info_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("المظهر والألوان")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("chat_theme_live_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_theme_options_scroll").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_theme_reset_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_theme_cancel_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_theme_save_btn").assertIsDisplayed()
    }

    @Test
    fun viewModel_addMembersToGroupChat_persistsContactsAndManualMember_andSupportsAdminToggleAndRemoval() = runBlocking {
        val viewModel = MainViewModel(app)
        val owner = UserEntity(
            id = "owner_add_1",
            name = "م. حسام المدير",
            phone = "01011223344",
            username = "hossam_mgr",
            type = UserType.BUSINESSMAN.name
        )
        db.userDao().insertUser(owner)
        val contact = ContactEntity(
            id = "c_add_${UUID.randomUUID().toString().take(6)}",
            name = "عماد المحاسب",
            phone = "01222334455"
        )
        db.contactDao().insertContact(contact)

        var createdChatId = ""
        viewModel.createStandaloneWorkGroup(
            groupName = "مجموعة الحسابات",
            description = "مراجعة يومية",
            selectedContacts = emptyList(),
            onCreated = { cid -> createdChatId = cid }
        )

        pumpMainLooperUntil { createdChatId.isNotBlank() }
        assertTrue("Group chat should be created", createdChatId.isNotBlank())
        val groupChat = db.chatDao().getChatById(createdChatId)!!
        val wgId = groupChat.groupId!!

        var addedCount = 0
        viewModel.addMembersToGroupChat(
            chatId = createdChatId,
            groupId = wgId,
            contacts = listOf(contact),
            manualName = "رامي المشرف",
            manualPhone = "01199887766",
            roleLabel = "مشرف تنفيذ",
            isGroupAdmin = false,
            onComplete = { count -> addedCount = count }
        )

        pumpMainLooperUntil {
            runBlocking { db.workGroupDao().getGroupMembersSync(wgId).size >= 3 }
        }
        assertEquals(2, addedCount)

        val members = db.workGroupDao().getGroupMembersSync(wgId)
        assertTrue("Selected contact must be persisted", members.any { it.username == "عماد المحاسب" && it.phone == "01222334455" })
        val manualMember = members.firstOrNull { it.username == "رامي المشرف" }
        assertNotNull("Manual member must be persisted", manualMember)
        assertEquals("مشرف تنفيذ", manualMember!!.roleLabel)

        val msgs = db.messageDao().getAllMessagesForChatSync(createdChatId)
            .map { com.example.util.RoomLocalAES256Engine.decryptMessage(it, null, context) }
        assertTrue(
            "System announcement message must list newly added members",
            msgs.any { it.type == "SYSTEM" && it.content.contains("@عماد المحاسب") && it.content.contains("@رامي المشرف") }
        )

        viewModel.toggleGroupMemberAdmin(manualMember)
        pumpMainLooperUntil {
            runBlocking {
                db.workGroupDao().getGroupMembersSync(wgId).firstOrNull { it.id == manualMember.id }?.isGroupAdmin == true
            }
        }
        assertTrue(db.workGroupDao().getGroupMembersSync(wgId).first { it.id == manualMember.id }.isGroupAdmin)

        viewModel.removeGroupMemberFromChat(createdChatId, manualMember)
        pumpMainLooperUntil {
            runBlocking { db.workGroupDao().getGroupMembersSync(wgId).none { it.id == manualMember.id } }
        }
        assertTrue(db.workGroupDao().getGroupMembersSync(wgId).none { it.id == manualMember.id })
    }

    @Test
    fun chatDetailScreen_addMemberFlows_renderAndSubmitSelectedContactsAndManualMember() {
        val wg = WorkGroupEntity(id = "wg_add_ui", name = "فريق التحصيل", ownerId = "u_me")
        val groupChat = ChatEntity(
            id = "wg_chat_wg_add_ui",
            contactId = wg.id,
            contactName = "فريق التحصيل",
            isGroup = true,
            groupId = wg.id
        )
        val c1 = ContactEntity(id = "contact_sarah", name = "سارة المحاسبة", phone = "01077788899")
        var capturedContacts: List<ContactEntity> = emptyList()
        var capturedManualName = ""
        var capturedManualPhone = ""

        composeTestRule.setContent {
            GoldChatTheme {
                ChatDetailScreen(
                    chat = groupChat,
                    messages = emptyList(),
                    currentUser = UserEntity(id = "u_me", name = "المدير", phone = "010000", username = "admin"),
                    workGroups = listOf(wg),
                    allGroupMembers = listOf(
                        GroupMemberEntity(
                            id = "gm_owner",
                            groupId = wg.id,
                            userId = "u_me",
                            username = "المدير",
                            phone = "010000",
                            roleLabel = "مدير المجموعة",
                            isGroupAdmin = true
                        )
                    ),
                    allContacts = listOf(c1),
                    accounts = emptyList(),
                    hashtags = emptyList(),
                    allEntries = emptyList(),
                    onBackClick = {},
                    onSendMessage = { _, _ -> },
                    onCreateAccount = {},
                    onCreateHashtagAccount = { _, _, _ -> },
                    onAddEntry = { _, _, _, _, _, _ -> },
                    onOpenGoldDeal = {},
                    onOpenAIAccountant = {},
                    onAddGroupMembers = { contacts, manualName, manualPhone, _, _ ->
                        capturedContacts = contacts
                        capturedManualName = manualName
                        capturedManualPhone = manualPhone
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Header strip shows member count & "+ إضافة عضو" button
        composeTestRule.onNodeWithTag("chat_header_members_count_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_header_add_member_btn").assertIsDisplayed()

        // + Bottom sheet also includes "إضافة أعضاء للمجموعة"
        composeTestRule.onNodeWithTag("chat_attachment_plus_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_plus_add_member_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_plus_add_member_btn")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeTestRule.waitForIdle()

        // Dialog opens on Add tab; select contact + enter manual member
        composeTestRule.onNodeWithTag("group_members_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("group_add_contact_item_${c1.id}")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        composeTestRule.onNodeWithTag("group_add_manual_name_input").performTextInput("وليد الموزع")
        composeTestRule.onNodeWithTag("group_add_manual_phone_input").performTextInput("01511223344")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("group_members_confirm_add_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(c1), capturedContacts)
        assertEquals("وليد الموزع", capturedManualName)
        assertEquals("01511223344", capturedManualPhone)
    }

    @Test
    fun projectCreation_withGranularMemberRolesAndPermissions_autoCreatesChatAndSyncsBothTables() = runBlocking {
        val viewModel = MainViewModel(app)
        val owner = UserEntity(
            id = "owner_granular_1",
            name = "م. عصام المالك",
            phone = "01011112233",
            username = "essam_owner",
            type = UserType.BUSINESSMAN.name
        )
        db.userDao().insertUser(owner)
        shadowOf(Looper.getMainLooper()).idle()

        val uniqueProjName = "مشروع الأبراج_${UUID.randomUUID().toString().take(5)}"
        val member1 = ProjectMemberInput(
            name = "م. خالد الفني",
            phone = "01122339900",
            roleTask = "مراجعة واعتماد المخططات",
            role = ProjectMemberRole.AUDITOR,
            isGroupAdmin = false,
            canPostAccounting = false,
            canEditRecords = true,
            canExportReports = true,
            canMessageDirectly = true
        )
        val member2 = ProjectMemberInput(
            name = "أ. هاني المالي",
            phone = "01233448877",
            roleTask = "محاسب تكاليف",
            role = ProjectMemberRole.FINANCIAL_PARTNER,
            isGroupAdmin = false,
            canPostAccounting = true,
            canEditRecords = true,
            canExportReports = true,
            canMessageDirectly = true
        )

        viewModel.createWorkGroupAndProject(
            groupName = "", // Blank -> should automatically resolve to clean project name!
            groupDesc = "تشييد وبناء متكامل",
            projectName = uniqueProjName,
            capital = 600000.0,
            projectType = ProjectType.CONSTRUCTION,
            coManagerPhone = "01088776655",
            coManagerName = "م. نادر التنفيذي",
            memberInputs = listOf(member1, member2)
        )

        var createdProject = db.projectDao().getAllProjects().first().firstOrNull { it.name == uniqueProjName }
        pumpMainLooperUntil {
            createdProject = runBlocking { db.projectDao().getAllProjects().first().firstOrNull { it.name == uniqueProjName } }
            val p = createdProject ?: return@pumpMainLooperUntil false
            runBlocking {
                db.chatDao().getChatByGroupId(p.workGroupId) != null &&
                    db.workGroupDao().getGroupMembersSync(p.workGroupId).size >= 4 &&
                    db.projectMemberDao().getMembersForProjectSync(p.id).size >= 3
            }
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull("Project must be created", createdProject)
        val p = createdProject!!
        val groupChat = db.chatDao().getChatByGroupId(p.workGroupId)
        assertNotNull("Chat group must be auto-created with project name", groupChat)
        assertEquals(uniqueProjName, groupChat!!.contactName)
        assertTrue(groupChat.isGroup)

        val projectMembers = db.projectMemberDao().getMembersForProjectSync(p.id)
        val groupMembers = db.workGroupDao().getGroupMembersSync(p.workGroupId)

        // Verify ProjectMember records
        val pmKhaled = projectMembers.firstOrNull { it.userName == "م. خالد الفني" }
        assertNotNull(pmKhaled)
        assertEquals(ProjectMemberRole.AUDITOR.name, pmKhaled!!.role)
        assertEquals("مراجعة واعتماد المخططات", pmKhaled.assignedTask)

        val pmHany = projectMembers.firstOrNull { it.userName == "أ. هاني المالي" }
        assertNotNull(pmHany)
        assertEquals(ProjectMemberRole.FINANCIAL_PARTNER.name, pmHany!!.role)

        // Verify GroupMember matching records and permissions
        val gmKhaled = groupMembers.firstOrNull { it.username == "م. خالد الفني" }
        assertNotNull(gmKhaled)
        assertFalse(gmKhaled!!.isGroupAdmin)
        assertFalse(gmKhaled.canPostAccounting)
        assertTrue(gmKhaled.canEditRecords)
        assertTrue(gmKhaled.canExportReports)

        val gmHany = groupMembers.firstOrNull { it.username == "أ. هاني المالي" }
        assertNotNull(gmHany)
        assertTrue(gmHany!!.canPostAccounting)
        assertTrue(gmHany.canEditRecords)

        // Verify Co-Manager admin permissions in both
        val gmNader = groupMembers.firstOrNull { it.username.contains("نادر") }
        assertNotNull(gmNader)
        assertTrue(gmNader!!.isGroupAdmin)

        // Verify Welcome message posted to group chat
        val msgs = db.messageDao().getAllMessagesForChatSync(groupChat.id)
            .map { com.example.util.RoomLocalAES256Engine.decryptMessage(it, null, context) }
        assertTrue("Welcome message must list added members", msgs.any { it.type == "SYSTEM" && it.content.contains("خالد") && it.content.contains("هاني") })
    }

    @Test
    fun projectWorkspace_bidirectionalDataSharing_andAccountingSync() = runBlocking {
        val viewModel = MainViewModel(app)
        val owner = UserEntity(
            id = "owner_sync_1",
            name = "م. فؤاد المدير",
            phone = "01022334455",
            username = "fouad_admin",
            type = UserType.BUSINESSMAN.name
        )
        db.userDao().insertUser(owner)

        val projId = "proj_sync_${UUID.randomUUID().toString().take(6)}"
        val wgId = "wg_sync_${UUID.randomUUID().toString().take(6)}"
        val wg = WorkGroupEntity(id = wgId, name = "مشروع اللؤلؤة", ownerId = owner.id)
        db.workGroupDao().insertWorkGroup(wg)
        val chat = repository.ensureWorkGroupChat(wg, displayTitle = "مشروع اللؤلؤة")

        val project = com.example.data.model.ProjectEntity(
            id = projId,
            name = "مشروع اللؤلؤة",
            workGroupId = wgId,
            estimatedCapital = 500000.0,
            type = "CONSTRUCTION",
            location = "الجيزة"
        )
        db.projectDao().insertProject(project)

        // 1. Add project member via ViewModel and verify synced to GroupMember
        viewModel.addProjectMember(
            projectId = projId,
            userName = "م. ماجد الموقع",
            userPhone = "01099881122",
            role = ProjectMemberRole.PROCUREMENT_OFFICER,
            task = "إشراف أعمال صب الخرسانة والتوريد",
            isGroupAdmin = false,
            canPostAccounting = false,
            canEditRecords = true,
            canExportReports = true,
            canMessageDirectly = true
        )

        pumpMainLooperUntil {
            runBlocking {
                db.projectMemberDao().getMembersForProjectSync(projId).any { it.userName == "م. ماجد الموقع" } &&
                    db.workGroupDao().getGroupMembersSync(wgId).any { it.username == "م. ماجد الموقع" }
            }
        }
        val gmMajed = db.workGroupDao().getGroupMembersSync(wgId).first { it.username == "م. ماجد الموقع" }
        assertEquals("إشراف أعمال صب الخرسانة والتوريد", gmMajed.roleLabel)
        assertTrue(gmMajed.canEditRecords)

        // 2. Add accounting entry and verify project balances and chat system message
        viewModel.addProjectAccountingEntry(
            projectId = projId,
            projectName = "مشروع اللؤلؤة",
            title = "دفعة حديد تسليح",
            amount = 45000.0,
            isIncome = false,
            accountCategory = "مواد خام"
        )

        var chatMsgs: List<MessageEntity> = emptyList()
        pumpMainLooperUntil {
            val updatedP = runBlocking { db.projectDao().getProjectById(projId) }
            chatMsgs = runBlocking {
                db.messageDao().getAllMessagesForChatSync(chat.id)
                    .map { com.example.util.RoomLocalAES256Engine.decryptMessage(it, null, context) }
            }
            updatedP != null && updatedP.totalExpenses == 45000.0 && chatMsgs.any { it.content.contains("دفعة حديد تسليح") }
        }

        val updatedP = db.projectDao().getProjectById(projId)!!
        assertEquals(45000.0, updatedP.totalExpenses, 0.001)

        assertTrue(
            "Chat must receive financial transaction message, actual contents: ${chatMsgs.map { it.content }}",
            chatMsgs.any { it.content.contains("دفعة حديد تسليح") }
        )
    }
}
