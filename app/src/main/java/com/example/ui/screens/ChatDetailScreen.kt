package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatAccountEntity
import com.example.data.model.ChatEntity
import com.example.data.model.GoldPricesModel
import com.example.data.model.GroupMemberEntity
import com.example.data.model.HashtagAccountEntity
import com.example.data.model.HashtagEntryDirection
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.HashtagEntryType
import com.example.data.model.InvoiceEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.WorkGroupEntity
import com.example.data.model.ContactEntity
import com.example.ui.components.AdminVerifyTaskDialog
import com.example.ui.components.CallOverlayDialog
import com.example.ui.components.CallType
import com.example.ui.components.CameraVideoNoteDialog
import com.example.ui.components.ClearingHouseDialog
import com.example.ui.components.CoverPhotoEditorDialog
import com.example.ui.components.CreatePhysicalFieldTaskDialog
import com.example.ui.components.GroupMembersAndAddDialog
import com.example.ui.components.ImageFrameAspectMode
import com.example.ui.components.MediaChooserKind
import com.example.ui.components.MediaPreviewItem
import com.example.ui.components.MediaPreviewModal
import com.example.ui.components.MediaSourceChooserCard
import com.example.ui.components.VideoEditorDialog
import com.example.ui.components.VoiceAccountingDialog
import com.example.ui.components.VoiceDictationAssistantDialog
import com.example.ui.components.VoiceDictationMode
import com.example.ui.components.WorkerSubmitProofDialog
import com.example.ui.spatial.ChatSpatialMode
import com.example.ui.spatial.spatialAmbientBackground
import com.example.ui.theme.GoldPrimary
import com.example.ui.viewmodel.TranslatedMessageUiState
import com.example.util.AudioPlayerManager
import com.example.util.AudioRecorderManager
import com.example.util.BlockManager
import com.example.util.DocumentInfo
import com.example.util.FileUtils
import com.example.util.GoldUtils
import com.example.util.ReportExporter
import com.example.util.SmartVoiceAccountingParser
import com.example.util.SpeechToTextManager
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class UnifiedVoiceDragMode {
    RECORD_UP,
    DICTATE_DOWN,
    CANCEL_LEFT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: ChatEntity?,
    messages: List<MessageEntity>,
    isInitialLoading: Boolean = false,
    isLoadingOlderMessages: Boolean = false,
    hasMoreOlderMessages: Boolean = false,
    totalMessagesCount: Int = messages.size,
    onLoadOlderMessages: () -> Unit = {},
    onExportFullChatPdf: (() -> Unit)? = null,
    accounts: List<ChatAccountEntity>,
    hashtags: List<HashtagAccountEntity>,
    allEntries: List<HashtagEntryEntity>,
    allTransactions: List<TransactionEntity> = emptyList(),
    goldPrices: GoldPricesModel = GoldUtils.DEFAULT_GOLD_PRICES,
    currentUser: UserEntity? = null,
    contactUser: UserEntity? = null,
    availableChats: List<ChatEntity> = emptyList(),
    translatedMessages: Map<String, TranslatedMessageUiState> = emptyMap(),
    onTranslateMessage: (MessageEntity) -> Unit = {},
    onDismissTranslation: (String) -> Unit = {},
    onBackClick: () -> Unit,
    onSendMessage: (String, String) -> Unit,
    onForwardMessage: (targetChatId: String, content: String, type: String) -> Unit = { _, _, _ -> },
    onToggleReaction: (messageId: String, currentReaction: String?, emoji: String) -> Unit = { _, _, _ -> },
    onCreateAccount: (String) -> Unit,
    onCreateHashtagAccount: (chatId: String, accountId: String, tag: String) -> Unit,
    onAddEntry: (String, String, HashtagEntryType, String, Double, HashtagEntryDirection) -> Unit,
    onOpenGoldDeal: () -> Unit,
    onOpenAIAccountant: () -> Unit,
    onOpenVoiceBusinessAgent: () -> Unit = onOpenAIAccountant,
    onExecuteClearing: (netGold24k: Double, netSilver999: Double, netCash: Double, metalLoss: Double, notes: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateToProfile: (contactId: String) -> Unit = {},
    translationFontSize: String = "medium",
    translationColorStyle: String = "gold",
    onUpdateTranslationFontSize: (String) -> Unit = {},
    onUpdateTranslationColorStyle: (String) -> Unit = {},
    allProjects: List<ProjectEntity> = emptyList(),
    allInvoices: List<InvoiceEntity> = emptyList(),
    invoicesForChat: List<InvoiceEntity> = emptyList(),
    onPayInvoice: ((invoiceId: String, amount: Double, method: String, notes: String?) -> Unit)? = null,
    onRecordHashtagOperation: ((tag: String, entryType: HashtagEntryType, description: String, amount: Double, direction: HashtagEntryDirection, projectId: String?, projectName: String?, paymentMethod: String, isExternalBusiness: Boolean) -> Unit)? = null,
    workGroups: List<WorkGroupEntity> = emptyList(),
    allGroupMembers: List<GroupMemberEntity> = emptyList(),
    allContacts: List<ContactEntity> = emptyList(),
    projectTasks: List<ProjectTaskEntity> = emptyList(),
    onInsertFieldTask: (ProjectTaskEntity) -> Unit = {},
    onSubmitTaskProof: (taskId: String, uri: String?, type: String, notes: String?, userId: String?, userName: String?) -> Unit = { _, _, _, _, _, _ -> },
    onConfirmAndArchiveTask: (taskId: String, amount: Double, notes: String?) -> Unit = { _, _, _ -> },
    onOpenGroupChatById: (String) -> Unit = {},
    onAddGroupMembers: (contacts: List<ContactEntity>, manualName: String, manualPhone: String, roleLabel: String, isGroupAdmin: Boolean) -> Unit = { _, _, _, _, _ -> },
    onRemoveGroupMember: (GroupMemberEntity) -> Unit = {},
    onToggleGroupMemberAdmin: (GroupMemberEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val ttsHelper = remember { TextToSpeechHelper(context) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    var activeSpatialMode by remember { mutableStateOf(ChatSpatialMode.COMMUNICATION) }
    var isVoiceCaptureActive by remember { mutableStateOf(false) }
    var showAttachmentActionsSheet by remember { mutableStateOf(false) }
    var showVoiceDictationDialog by remember { mutableStateOf(false) }
    var activeDictationMode by remember { mutableStateOf(VoiceDictationMode.INVOICE) }
    var selectedMessageForForward by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForOptions by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForReaction by remember { mutableStateOf<MessageEntity?>(null) }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var forwardSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showChatSettingsDialog by remember { mutableStateOf(false) }
    var showContactProfileModal by remember { mutableStateOf(false) }
    var showMessengerOptionsSheet by remember { mutableStateOf(false) }
    var messageZoomScale by rememberSaveable { mutableFloatStateOf(1f) }
    var lastZoomInteractionMs by remember { mutableLongStateOf(0L) }
    var showZoomPill by remember { mutableStateOf(false) }
    LaunchedEffect(lastZoomInteractionMs) {
        if (lastZoomInteractionMs > 0L) {
            showZoomPill = true
            delay(1700)
            showZoomPill = false
        }
    }
    var customChatBg by remember { mutableStateOf("default") }
    var customFontColorStyle by remember { mutableStateOf("auto") }
    var customBubbleStyle by remember { mutableStateOf("auto") }
    var customChatBgColor by remember { mutableStateOf<Color?>(null) }
    var customBubbleColor by remember { mutableStateOf<Color?>(null) }
    var customFontColor by remember { mutableStateOf<Color?>(null) }
    var isUserBlocked by remember(chat?.contactId) {
        mutableStateOf(chat?.contactId?.let { BlockManager.isUserBlocked(context, it) } ?: false)
    }
    var callPrivacyBlockedReason by remember { mutableStateOf<String?>(null) }
    var isTopModesRailExpanded by remember { mutableStateOf(false) }

    val densityForIme = LocalDensity.current
    val isImeOpen = WindowInsets.ime.getBottom(densityForIme) > with(densityForIme) { 80.dp.toPx() }

    var isActionButtonCameraMode by remember { mutableStateOf(false) }
    var showCameraVideoNoteDialog by remember { mutableStateOf(false) }

    var isInlineRecordingAudio by remember { mutableStateOf(false) }
    var isInlineRecordCancelZone by remember { mutableStateOf(false) }
    var inlineRecordSeconds by remember { mutableIntStateOf(0) }
    var inlineRecordAmp by remember { mutableFloatStateOf(0.25f) }
    var voiceDraftPath by remember { mutableStateOf<String?>(null) }
    var voiceDraftDurationSec by remember { mutableIntStateOf(0) }
    var isDraftAudioPlaying by remember { mutableStateOf(false) }
    var draftAudioProgress by remember { mutableFloatStateOf(0f) }

    var isHoldRecording by remember { mutableStateOf(false) }
    var holdLiveTranscript by remember { mutableStateOf("") }
    var holdAmpLevel by remember { mutableFloatStateOf(0.4f) }
    val holdSpeechManager = remember { SpeechToTextManager(context) }

    var isInlineDictating by remember { mutableStateOf(false) }
    var dictationLanguage by remember { mutableStateOf("ar-EG") }
    var dictationBaseText by remember { mutableStateOf("") }
    var dictationLiveText by remember { mutableStateOf("") }
    var isStartingDictationAfterPermission by remember { mutableStateOf(false) }
    val inlineSpeechManager = remember { SpeechToTextManager(context) }
    val inlineSpeechRms by inlineSpeechManager.rmsLevel.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            holdSpeechManager.destroy()
            inlineSpeechManager.destroy()
            ttsHelper.shutdown()
            if (isInlineRecordingAudio) {
                AudioRecorderManager.cancelRecording()
            }
            if (isDraftAudioPlaying) {
                AudioPlayerManager.stop()
            }
        }
    }

    LaunchedEffect(isInlineRecordingAudio) {
        if (isInlineRecordingAudio) {
            val startedMs = System.currentTimeMillis()
            while (isInlineRecordingAudio) {
                val maxAmp = AudioRecorderManager.getMaxAmplitude()
                inlineRecordAmp = (maxAmp / 25000f).coerceIn(0.18f, 1.0f)
                inlineRecordSeconds = ((System.currentTimeMillis() - startedMs) / 1000L).toInt().coerceAtLeast(0)
                delay(75L)
            }
        }
    }

    LaunchedEffect(isHoldRecording) {
        while (isHoldRecording) {
            val maxAmp = AudioRecorderManager.getMaxAmplitude()
            holdAmpLevel = (maxAmp / 25000f).coerceIn(0.2f, 1.0f)
            delay(70L)
        }
    }

    val mediaScope = rememberCoroutineScope()
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImageFrameUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoEditUri by remember { mutableStateOf<Uri?>(null) }
    var activeMediaChooserKind by remember { mutableStateOf<MediaChooserKind?>(null) }
    var pendingCameraPermissionKind by remember { mutableStateOf<MediaChooserKind?>(null) }
    var isProcessingMedia by remember { mutableStateOf(false) }
    var mediaProcessProgress by remember { mutableFloatStateOf(0f) }
    var attachedDocumentInfo by remember { mutableStateOf<DocumentInfo?>(null) }
    var activeMediaPreview by remember { mutableStateOf<MediaPreviewItem?>(null) }
    var showCameraPermissionRationale by remember { mutableStateOf(false) }
    var showMicPermissionRationale by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val rawUri = tempPhotoUri
        if (success && rawUri != null) {
            mediaScope.launch {
                isProcessingMedia = true
                mediaProcessProgress = 0.1f
                val processed = FileUtils.smartProcessAndStoreMedia(
                    context = context,
                    sourceUri = rawUri,
                    isVideo = false,
                    targetFolder = "chat_images"
                ) { prog -> mediaProcessProgress = prog }
                isProcessingMedia = false
                capturedImageUri = processed?.uri?.let { Uri.parse(it) } ?: rawUri
                attachedDocumentInfo = null
            }
        }
    }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val rawUri = tempVideoUri
        if (success && rawUri != null) {
            attachedDocumentInfo = null
            pendingVideoEditUri = rawUri
        }
    }

    val photoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            mediaScope.launch {
                isProcessingMedia = true
                mediaProcessProgress = 0.08f
                val processed = FileUtils.smartProcessAndStoreMedia(
                    context = context,
                    sourceUri = uri,
                    isVideo = false,
                    targetFolder = "chat_images"
                ) { prog -> mediaProcessProgress = prog }
                isProcessingMedia = false
                capturedImageUri = processed?.uri?.let { Uri.parse(it) } ?: uri
                attachedDocumentInfo = null
            }
        }
    }

    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedDocumentInfo = null
            pendingVideoEditUri = uri
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = FileUtils.getFileNameAndSize(context, uri)
            val mime = FileUtils.getMimeType(context, uri, name)
            val isImg = FileUtils.isImageNameOrMime(name, mime)
            val isVid = FileUtils.isVideoNameOrMime(name, mime)
            if (isVid) {
                attachedDocumentInfo = null
                pendingVideoEditUri = uri
            } else {
                mediaScope.launch {
                    isProcessingMedia = true
                    mediaProcessProgress = 0.1f
                    if (isImg) {
                        val processed = FileUtils.smartProcessAndStoreMedia(
                            context = context,
                            sourceUri = uri,
                            isVideo = false,
                            targetFolder = "chat_images"
                        ) { prog -> mediaProcessProgress = prog }
                        capturedImageUri = processed?.uri?.let { Uri.parse(it) } ?: uri
                        attachedDocumentInfo = null
                    } else {
                        mediaProcessProgress = 0.45f
                        val persistentUri = withContext(Dispatchers.IO) {
                            FileUtils.copyUriToLocalStorage(context, uri, name)
                        }
                        mediaProcessProgress = 1f
                        attachedDocumentInfo = DocumentInfo(
                            uri = persistentUri,
                            fileName = name,
                            fileSize = size,
                            mimeType = mime,
                            isImage = false
                        )
                        capturedImageUri = null
                    }
                    isProcessingMedia = false
                }
            }
        }
    }

    val launchCameraCapture: () -> Unit = {
        try {
            val imagesDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val imageFile = File(imagesDir, "IMG_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val launchVideoCameraCapture: () -> Unit = {
        try {
            val videosDir = File(context.cacheDir, "camera_videos").apply { mkdirs() }
            val videoFile = File(videosDir, "VID_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile)
            tempVideoUri = uri
            videoCaptureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val videoNotePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.CAMERA] == true) {
            showCameraVideoNoteDialog = true
        } else {
            showCameraPermissionRationale = true
        }
    }

    val launchFrontVideoNote: () -> Unit = {
        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasCam) {
            showCameraVideoNoteDialog = true
        } else {
            videoNotePermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val targetKind = pendingCameraPermissionKind
        pendingCameraPermissionKind = null
        if (isGranted) {
            if (targetKind == MediaChooserKind.VIDEO) launchVideoCameraCapture() else launchCameraCapture()
        } else {
            showCameraPermissionRationale = true
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val rawRecognized = spokenMatches?.firstOrNull()?.trim().orEmpty()
            val recognizedText = SmartVoiceAccountingParser.normalizeSpokenHashtags(rawRecognized)
            if (recognizedText.isNotBlank()) {
                textInput = if (textInput.isNotBlank()) "${textInput.trim()} $recognizedText" else recognizedText
            }
        }
    }

    var startInlineDictationInternalRef by remember { mutableStateOf<(() -> Unit)?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isStartingDictationAfterPermission) {
                isStartingDictationAfterPermission = false
                startInlineDictationInternalRef?.invoke()
            }
        } else {
            isStartingDictationAfterPermission = false
            showMicPermissionRationale = true
        }
    }

    val startInlineDictationInternal: () -> Unit = {
        if (isInlineDictating) {
            inlineSpeechManager.stopListening()
            isInlineDictating = false
            dictationLiveText = ""
        } else {
            dictationBaseText = textInput
            dictationLiveText = ""
            isInlineDictating = true
            if (inlineSpeechManager.isRecognitionAvailable()) {
                inlineSpeechManager.startListening(
                    languageCode = dictationLanguage,
                    onPartial = { partial ->
                        val norm = SmartVoiceAccountingParser.normalizeSpokenHashtags(partial)
                        if (norm.isNotBlank()) {
                            dictationLiveText = norm
                            textInput = if (dictationBaseText.isNotBlank()) "${dictationBaseText.trim()} $norm" else norm
                        }
                    },
                    onFinal = { finalText ->
                        val norm = SmartVoiceAccountingParser.normalizeSpokenHashtags(finalText)
                        if (norm.isNotBlank()) {
                            dictationLiveText = ""
                            textInput = if (dictationBaseText.isNotBlank()) "${dictationBaseText.trim()} $norm" else norm
                        }
                        isInlineDictating = false
                    }
                )
            } else {
                isInlineDictating = false
                try {
                    speechRecognizerLauncher.launch(inlineSpeechManager.createSpeechIntent(dictationLanguage))
                } catch (e: Exception) {
                    Toast.makeText(context, "التعرف على الصوت غير متاح على هذا الجهاز", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    startInlineDictationInternalRef = startInlineDictationInternal

    val onCameraTrigger: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraCapture()
        } else {
            pendingCameraPermissionKind = MediaChooserKind.PHOTO
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onVideoCameraTrigger: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchVideoCameraCapture()
        } else {
            pendingCameraPermissionKind = MediaChooserKind.VIDEO
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var showLedgerSheet by remember { mutableStateOf(false) }
    var showNewAccountDialog by remember { mutableStateOf(false) }
    var showNewEntryDialog by remember { mutableStateOf(false) }
    var showClearingDialog by remember { mutableStateOf(false) }
    var showVoiceAccountingDialog by remember { mutableStateOf(false) }
    var isAutoTranslateEnabled by rememberSaveable { mutableStateOf(true) }
    var translateMyMessagesToo by rememberSaveable { mutableStateOf(false) }
    var activeCallType by remember { mutableStateOf<CallType?>(null) }
    var showZoxCallBlockedDialog by remember { mutableStateOf(false) }
    val isZoxAdminChat = remember(chat) { chat?.contactType == "ADMIN" }
    var selectedHashtagIdForEntry by remember { mutableStateOf<String?>(null) }
    var selectedAccountIdForEntry by remember { mutableStateOf<String?>(null) }

    var chatStreamFilter by rememberSaveable(chat?.id) { mutableStateOf("ALL") }
    var showChatSearchBar by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var showQuickHashtagSheet by remember { mutableStateOf(false) }
    var quickHashtagInitialTag by remember { mutableStateOf<String?>(null) }
    var quickPayInvoiceTarget by remember { mutableStateOf<InvoiceEntity?>(null) }
    var inspectedHashtagTag by remember { mutableStateOf<String?>(null) }
    var inspectedCode by remember { mutableStateOf<String?>(null) }
    var showCreateTaskInChatDialog by remember { mutableStateOf(false) }
    var showGroupMembersDialog by remember { mutableStateOf(false) }
    var groupMembersInitialTab by remember { mutableIntStateOf(0) }
    var chatTaskPendingProof by remember { mutableStateOf<ProjectTaskEntity?>(null) }
    var chatTaskPendingVerify by remember { mutableStateOf<ProjectTaskEntity?>(null) }

    val linkedWorkGroup = remember(chat, workGroups) {
        if (chat == null) null
        else workGroups.firstOrNull { wg ->
            (chat.groupId != null && wg.id == chat.groupId) || wg.id == chat.contactId || chat.id == "chat_wg_${wg.id}"
        }
    }
    val linkedProject = remember(linkedWorkGroup, allProjects) {
        if (linkedWorkGroup == null) null else allProjects.firstOrNull { it.workGroupId == linkedWorkGroup.id }
    }
    val parentCompanyGroup = remember(linkedWorkGroup, workGroups) {
        val pId = linkedWorkGroup?.parentGroupId
        if (pId.isNullOrBlank()) null else workGroups.firstOrNull { it.id == pId }
    }
    val subDepartmentGroups = remember(linkedWorkGroup, parentCompanyGroup, workGroups) {
        val rootId = parentCompanyGroup?.id ?: linkedWorkGroup?.id
        if (rootId.isNullOrBlank()) emptyList()
        else workGroups.filter { it.parentGroupId == rootId && it.id != linkedWorkGroup?.id }
    }
    val activeGroupMembers = remember(linkedWorkGroup, allGroupMembers) {
        if (linkedWorkGroup == null) emptyList() else allGroupMembers.filter { it.groupId == linkedWorkGroup.id }
    }
    val isCurrentUserGroupManager = remember(linkedWorkGroup, activeGroupMembers, currentUser) {
        linkedWorkGroup != null && (
            linkedWorkGroup.ownerId == currentUser?.id ||
                activeGroupMembers.any {
                    it.isGroupAdmin && (
                        it.userId == currentUser?.id ||
                            (!currentUser?.phone.isNullOrBlank() && it.phone == currentUser?.phone)
                        )
                }
            )
    }
    val mentionCandidates = remember(activeGroupMembers, linkedProject, allContacts, chat) {
        val candidates = linkedSetOf<String>()
        activeGroupMembers.forEach { m ->
            val handle = m.username.trim().removePrefix("@")
            if (handle.isNotBlank()) candidates.add("@$handle - ${m.roleLabel}")
        }
        linkedProject?.coManagerName?.takeIf { it.isNotBlank() }?.let { co ->
            candidates.add("@${co.removePrefix("@")} - مدير تنفيذي")
        }
        linkedProject?.membersInfo?.split("|")?.map { it.trim() }?.filter { it.isNotBlank() }?.forEach {
            candidates.add(if (it.startsWith("@")) it else "@$it")
        }
        if (chat != null && !chat.isGroup && chat.contactName.isNotBlank()) {
            candidates.add("@${chat.contactName.removePrefix("@")}")
        }
        allContacts.take(15).forEach { c ->
            if (c.name.isNotBlank()) candidates.add("@${c.name.removePrefix("@")}")
        }
        candidates.toList()
    }

    val listState = rememberLazyListState()
    val filteredMessages = remember(messages, chatStreamFilter, chatSearchQuery) {
        val q = chatSearchQuery.trim()
        messages.filter { msg ->
            val isFinanceMsg = msg.type == MessageType.INVOICE.name ||
                msg.type == MessageType.PAYMENT.name ||
                msg.type == MessageType.HASHTAG.name ||
                msg.type == MessageType.GOLD_PURCHASE.name ||
                msg.content.contains("#")
            val passesStream = when (chatStreamFilter) {
                "CHAT_ONLY" -> !isFinanceMsg
                "FINANCE_ONLY" -> isFinanceMsg
                else -> true
            }
            val passesSearch = q.isBlank() ||
                msg.content.contains(q, ignoreCase = true) ||
                msg.senderName.contains(q, ignoreCase = true)
            passesStream && passesSearch
        }
    }
    val messagesNewestFirst = remember(filteredMessages) { filteredMessages.asReversed() }

    val firstUnreadIndex = remember(filteredMessages, currentUser, chat?.contactName) {
        filteredMessages.indexOfFirst { msg ->
            val isMe = if (currentUser != null) {
                msg.senderId == currentUser.id || msg.senderName == currentUser.name
            } else {
                msg.senderName != chat?.contactName
            }
            !isMe && msg.status != MessageStatus.READ.name
        }
    }
    val firstUnreadReversedIndex = remember(firstUnreadIndex, filteredMessages.size) {
        if (firstUnreadIndex != -1 && filteredMessages.isNotEmpty()) (filteredMessages.lastIndex - firstUnreadIndex) else -1
    }

    var initialScrollDone by remember(chat?.id) { mutableStateOf(false) }
    var lastNewestMessageId by remember(chat?.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(messagesNewestFirst.firstOrNull()?.id, chat?.id) {
        val newestId = messagesNewestFirst.firstOrNull()?.id
        if (messagesNewestFirst.isNotEmpty()) {
            try {
                if (!initialScrollDone) {
                    if (firstUnreadReversedIndex in 5 until messagesNewestFirst.size) {
                        listState.scrollToItem(firstUnreadReversedIndex)
                    }
                    initialScrollDone = true
                    lastNewestMessageId = newestId
                } else if (newestId != null && newestId != lastNewestMessageId) {
                    if (listState.firstVisibleItemIndex <= 2) {
                        listState.animateScrollToItem(0)
                    }
                    lastNewestMessageId = newestId
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(listState, hasMoreOlderMessages, isLoadingOlderMessages, messagesNewestFirst.size) {
        if (!hasMoreOlderMessages || isLoadingOlderMessages || messagesNewestFirst.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (messagesNewestFirst.size - 6)
        }
            .distinctUntilChanged()
            .collect { nearTopOfHistory ->
                if (nearTopOfHistory && hasMoreOlderMessages && !isLoadingOlderMessages) {
                    onLoadOlderMessages()
                }
            }
    }

    if (chat == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .spatialAmbientBackground()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(50.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
                        }
                        Text(
                            text = "محادثة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = GoldPrimary)
                        Text(
                            text = "جاري تحميل المحادثة...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
        return
    }

    val customBgModifier = when {
        customChatBgColor != null -> Modifier.background(customChatBgColor!!)
        customChatBg == "gold" -> Modifier.background(Brush.verticalGradient(listOf(Color(0xFF1E1B10), Color(0xFF0F0E0A))))
        customChatBg == "navy" -> Modifier.background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617))))
        customChatBg == "light" -> Modifier.background(Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))))
        else -> Modifier.background(MaterialTheme.colorScheme.background)
    }

    val animatedZoomScale by animateFloatAsState(
        targetValue = messageZoomScale,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chat_zoom_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(customBgModifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatDetailTopBar(
                chat = chat,
                contactUser = contactUser,
                isZoxAdminChat = isZoxAdminChat,
                activeGroupMembers = activeGroupMembers,
                isCurrentUserGroupManager = isCurrentUserGroupManager,
                parentCompanyGroup = parentCompanyGroup,
                subDepartmentGroups = subDepartmentGroups,
                invoicesForChat = invoicesForChat,
                hashtags = hashtags,
                chatStreamFilter = chatStreamFilter,
                showChatSearchBar = showChatSearchBar,
                chatSearchQuery = chatSearchQuery,
                activeSpatialMode = activeSpatialMode,
                isTopModesRailExpanded = isTopModesRailExpanded,
                onBackClick = onBackClick,
                onShowMessengerOptions = { showMessengerOptionsSheet = true },
                onCallAttempt = { activeCallType = it },
                onCallPrivacyBlocked = { callPrivacyBlockedReason = it },
                onZoxCallBlocked = { showZoxCallBlockedDialog = true },
                onStreamFilterChange = { chatStreamFilter = it },
                onOpenQuickHashtag = { showQuickHashtagSheet = true },
                onToggleSearchBar = {
                    showChatSearchBar = !showChatSearchBar
                    if (!showChatSearchBar) chatSearchQuery = ""
                },
                onSearchQueryChange = { chatSearchQuery = it },
                onOpenLedgerSheet = { showLedgerSheet = true },
                onOpenGroupMembersDialog = { initialTab ->
                    groupMembersInitialTab = initialTab
                    showGroupMembersDialog = true
                },
                onOpenCreateTaskDialog = { showCreateTaskInChatDialog = true },
                onOpenGroupChatById = onOpenGroupChatById,
                onSelectSpatialMode = { mode ->
                    activeSpatialMode = mode
                    isTopModesRailExpanded = false
                }
            )

            ChatMessageStreamSection(
                chat = chat,
                messagesNewestFirst = messagesNewestFirst,
                firstUnreadIndex = firstUnreadIndex,
                firstUnreadReversedIndex = firstUnreadReversedIndex,
                isInitialLoading = isInitialLoading,
                isLoadingOlderMessages = isLoadingOlderMessages,
                hasMoreOlderMessages = hasMoreOlderMessages,
                onLoadOlderMessages = onLoadOlderMessages,
                listState = listState,
                isImeOpen = isImeOpen,
                messageZoomScale = messageZoomScale,
                animatedZoomScale = animatedZoomScale,
                showZoomPill = showZoomPill,
                onZoomChange = { nextZoom ->
                    messageZoomScale = nextZoom
                    lastZoomInteractionMs = System.currentTimeMillis()
                },
                onResetZoom = { messageZoomScale = 1f },
                currentUser = currentUser,
                customFontColorStyle = customFontColorStyle,
                customBubbleStyle = customBubbleStyle,
                customFontColor = customFontColor,
                customBubbleColor = customBubbleColor,
                isAutoTranslateEnabled = isAutoTranslateEnabled,
                translateMyMessagesToo = translateMyMessagesToo,
                translationFontSize = translationFontSize,
                translationColorStyle = translationColorStyle,
                translatedMessages = translatedMessages,
                invoicesForChat = invoicesForChat,
                onQuickPayInvoice = if (onPayInvoice != null) { inv -> quickPayInvoiceTarget = inv } else null,
                onHashtagClick = { tag -> inspectedHashtagTag = tag },
                onCodeClick = { code -> inspectedCode = code },
                onTranslateMessage = onTranslateMessage,
                ttsHelper = ttsHelper,
                speakingMessageId = speakingMessageId,
                onSpeakingMessageIdChange = { speakingMessageId = it },
                onDismissTranslation = onDismissTranslation,
                onAvatarClick = { showMessengerOptionsSheet = true },
                onImageClick = { previewItem -> activeMediaPreview = previewItem },
                onOptionsClick = { selectedMessageForOptions = it },
                onReactionClick = { selectedMessageForReaction = it },
                onForwardClick = { selectedMessageForForward = it },
                onReply = { replyingToMessage = it },
                projectTasks = projectTasks,
                onWorkerSubmitTaskProof = { task -> chatTaskPendingProof = task },
                onAdminVerifyTask = { task -> chatTaskPendingVerify = task },
                onMentionClick = { mention ->
                    val cleanMention = "@${mention.removePrefix("@").trim()}"
                    textInput = if (textInput.isBlank()) "$cleanMention " else "${textInput.trimEnd()} $cleanMention "
                },
                onSendMessage = onSendMessage,
                forwardSuccessMessage = forwardSuccessMessage,
                onDismissForwardSuccess = { forwardSuccessMessage = null }
            )

            ChatDetailBottomComposer(
                textInput = textInput,
                onTextInputChange = { textInput = it },
                isVoiceCaptureActive = isVoiceCaptureActive,
                onCancelVoiceCapture = { isVoiceCaptureActive = false },
                isInlineDictating = isInlineDictating,
                dictationLanguage = dictationLanguage,
                dictationBaseText = dictationBaseText,
                dictationLiveText = dictationLiveText,
                inlineSpeechManager = inlineSpeechManager,
                inlineSpeechRms = inlineSpeechRms,
                onDictationLanguageToggle = { dictationLanguage = it },
                onDictationLiveTextChange = { dictationLiveText = it },
                onInlineDictatingChange = { isInlineDictating = it },
                isHoldRecording = isHoldRecording,
                holdLiveTranscript = holdLiveTranscript,
                holdAmpLevel = holdAmpLevel,
                attachedDocumentInfo = attachedDocumentInfo,
                onClearAttachedDocument = { attachedDocumentInfo = null },
                isProcessingMedia = isProcessingMedia,
                mediaProcessProgress = mediaProcessProgress,
                capturedImageUri = capturedImageUri,
                onClearCapturedImage = { capturedImageUri = null },
                replyingToMessage = replyingToMessage,
                onClearReplyingTo = { replyingToMessage = null },
                mentionCandidates = mentionCandidates,
                isInlineRecordingAudio = isInlineRecordingAudio,
                onInlineRecordingAudioChange = { isInlineRecordingAudio = it },
                isInlineRecordCancelZone = isInlineRecordCancelZone,
                onInlineRecordCancelZoneChange = { isInlineRecordCancelZone = it },
                inlineRecordSeconds = inlineRecordSeconds,
                onInlineRecordSecondsChange = { inlineRecordSeconds = it },
                inlineRecordAmp = inlineRecordAmp,
                voiceDraftPath = voiceDraftPath,
                onVoiceDraftPathChange = { voiceDraftPath = it },
                voiceDraftDurationSec = voiceDraftDurationSec,
                onVoiceDraftDurationChange = { voiceDraftDurationSec = it },
                isDraftAudioPlaying = isDraftAudioPlaying,
                onDraftAudioPlayingChange = { isDraftAudioPlaying = it },
                draftAudioProgress = draftAudioProgress,
                onDraftAudioProgressChange = { draftAudioProgress = it },
                isActionButtonCameraMode = isActionButtonCameraMode,
                onToggleActionButtonCameraMode = { isActionButtonCameraMode = !isActionButtonCameraMode },
                isImeOpen = isImeOpen,
                currentUser = currentUser,
                onOpenAttachmentSheet = { showAttachmentActionsSheet = true },
                onOpenVideoEditor = { pendingVideoEditUri = it },
                onOpenImageFrameEditor = { pendingImageFrameUri = it },
                onOpenMediaPreview = { activeMediaPreview = it },
                onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onLaunchFrontVideoNote = launchFrontVideoNote,
                onSendMessage = onSendMessage
            )
        }
    }

    if (showAttachmentActionsSheet) {
        ChatAttachmentActionsSheet(
            isGroup = chat.isGroup,
            onDismiss = { showAttachmentActionsSheet = false },
            onPickDocument = {
                try {
                    documentPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                } catch (_: Exception) {
                }
            },
            onPickMediaKind = { activeMediaChooserKind = it },
            onOpenGoldDeal = onOpenGoldDeal,
            onOpenVoiceBusinessAgent = onOpenVoiceBusinessAgent,
            onOpenVoiceAccounting = { showVoiceAccountingDialog = true },
            onOpenVoiceDictation = { mode ->
                activeDictationMode = mode
                showVoiceDictationDialog = true
            },
            onOpenQuickHashtag = { showQuickHashtagSheet = true },
            onOpenCreateTask = { showCreateTaskInChatDialog = true },
            onOpenAddGroupMembers = {
                groupMembersInitialTab = 1
                showGroupMembersDialog = true
            }
        )
    }

    if (showLedgerSheet) {
        ChatLedgerSheet(
            chatId = chat.id,
            accounts = accounts,
            hashtags = hashtags,
            allEntries = allEntries,
            onDismiss = { showLedgerSheet = false },
            onOpenNewAccount = { showNewAccountDialog = true },
            onOpenNewEntry = { hId, aId ->
                selectedHashtagIdForEntry = hId
                selectedAccountIdForEntry = aId
                showNewEntryDialog = true
            },
            onCreateHashtagAccount = onCreateHashtagAccount
        )
    }

    if (showNewAccountDialog) {
        ChatNewAccountDialog(
            onDismiss = { showNewAccountDialog = false },
            onCreateAccount = { accName ->
                onCreateAccount(accName)
                showNewAccountDialog = false
            }
        )
    }

    if (showNewEntryDialog && selectedHashtagIdForEntry != null && selectedAccountIdForEntry != null) {
        ChatNewEntryDialog(
            onDismiss = { showNewEntryDialog = false },
            onSaveEntry = { entryType, desc, amt, dir ->
                onAddEntry(selectedHashtagIdForEntry!!, selectedAccountIdForEntry!!, entryType, desc, amt, dir)
                showNewEntryDialog = false
            }
        )
    }

    if (showClearingDialog) {
        ClearingHouseDialog(
            contactName = chat.contactName,
            contactId = chat.contactId,
            transactions = allTransactions,
            onExecuteSettlement = { netGold, netSilver, netCash, loss, notes ->
                onExecuteClearing(netGold, netSilver, netCash, loss, notes)
                showClearingDialog = false
            },
            onDismiss = { showClearingDialog = false }
        )
    }

    if (activeCallType != null) {
        CallOverlayDialog(
            callType = activeCallType!!,
            contactName = chat.contactName,
            contactAvatarUri = null,
            isTrader = chat.contactType == com.example.data.model.UserType.GOLD_TRADER.name,
            onDismiss = { activeCallType = null },
            onSendMessageNotice = { notice, type -> onSendMessage(notice, type) }
        )
    }

    if (callPrivacyBlockedReason != null) {
        ChatCallPrivacyBlockedDialog(
            reason = callPrivacyBlockedReason!!,
            onDismiss = { callPrivacyBlockedReason = null }
        )
    }

    if (showZoxCallBlockedDialog) {
        ChatZoxCallBlockedDialog(onDismiss = { showZoxCallBlockedDialog = false })
    }

    if (showCameraPermissionRationale) {
        ChatPermissionRationaleDialog(
            icon = Icons.Default.PhotoCamera,
            title = "إذن استخدام الكاميرا",
            body = "يتطلب التقاط صور الفواتير والذهب ومرفقات المحادثة إذن الوصول إلى كاميرا الهاتف. يرجى منح الإذن للمتابعة.",
            onDismiss = { showCameraPermissionRationale = false },
            onGrant = {
                showCameraPermissionRationale = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    if (showMicPermissionRationale) {
        ChatPermissionRationaleDialog(
            icon = Icons.Default.Mic,
            title = "إذن استخدام الميكروفون ️",
            body = "يتطلب تسجيل وإرسال الرسائل والملاحظات الصوتية المباشرة في المحادثة إذن الوصول إلى الميكروفون. يرجى منح الإذن للمتابعة.",
            onDismiss = { showMicPermissionRationale = false },
            onGrant = {
                showMicPermissionRationale = false
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }

    if (showVoiceDictationDialog) {
        VoiceDictationAssistantDialog(
            initialMode = activeDictationMode,
            contactName = chat.contactName,
            onDismiss = { showVoiceDictationDialog = false },
            onInsertText = { formatted ->
                textInput = if (textInput.isNotBlank()) "${textInput.trim()}\n\n$formatted" else formatted
            },
            onDirectSend = { content, msgType -> onSendMessage(content, msgType) }
        )
    }

    if (showVoiceAccountingDialog) {
        VoiceAccountingDialog(
            contactName = chat.contactName,
            existingHashtags = hashtags.map { it.tag.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct(),
            onDismiss = { showVoiceAccountingDialog = false },
            onInsertEntry = { record, _ ->
                val cleanTag = record.suggestedTag.trim().removePrefix("#").ifBlank { "فاتورة" }
                if (onRecordHashtagOperation != null && record.amount > 0.0) {
                    val isCredit = record.direction.equals("CREDIT", ignoreCase = true)
                    val entryType = if (isCredit) HashtagEntryType.PAYMENT else HashtagEntryType.INVOICE
                    val dir = if (isCredit) HashtagEntryDirection.CREDIT else HashtagEntryDirection.DEBIT
                    onRecordHashtagOperation(
                        cleanTag,
                        entryType,
                        record.notes.ifBlank { record.rawSpokenText }.ifBlank { record.spokenConfirmationText },
                        record.amount,
                        dir,
                        null,
                        null,
                        "كاش",
                        false
                    )
                } else {
                    onSendMessage(record.formattedHashtagMessage, MessageType.HASHTAG.name)
                }
            }
        )
    }

    if (showCameraVideoNoteDialog) {
        CameraVideoNoteDialog(
            onDismiss = { showCameraVideoNoteDialog = false },
            onSendVideo = { videoUri, caption ->
                val payload = if (caption.isNotBlank()) "$videoUri||$caption" else videoUri
                onSendMessage(payload, MessageType.IMAGE.name)
                showCameraVideoNoteDialog = false
            }
        )
    }

    activeMediaChooserKind?.let { chooserKind ->
        MediaSourceChooserCard(
            kind = chooserKind,
            onCameraClick = {
                activeMediaChooserKind = null
                if (chooserKind == MediaChooserKind.PHOTO) onCameraTrigger() else onVideoCameraTrigger()
            },
            onGalleryClick = {
                activeMediaChooserKind = null
                if (chooserKind == MediaChooserKind.PHOTO) {
                    photoGalleryLauncher.launch("image/*")
                } else {
                    videoGalleryLauncher.launch("video/*")
                }
            },
            onDismiss = { activeMediaChooserKind = null }
        )
    }

    pendingImageFrameUri?.let { editUri ->
        CoverPhotoEditorDialog(
            imageUri = editUri,
            frameMode = ImageFrameAspectMode.PRODUCT_PHOTO,
            title = "تعديل الصورة",
            onSaveCroppedCover = { savedPath ->
                capturedImageUri = Uri.fromFile(File(savedPath))
                pendingImageFrameUri = null
            },
            onDismiss = { pendingImageFrameUri = null }
        )
    }

    pendingVideoEditUri?.let { editVideoUri ->
        VideoEditorDialog(
            videoUri = editVideoUri,
            initialCaption = textInput,
            showCaptionField = true,
            targetFolder = "chat_videos",
            onConfirmSave = { savedUri, _, editedCaption ->
                capturedImageUri = savedUri
                attachedDocumentInfo = null
                if (editedCaption.isNotBlank()) {
                    textInput = editedCaption
                }
                pendingVideoEditUri = null
            },
            onDismiss = { pendingVideoEditUri = null }
        )
    }

    if (activeMediaPreview != null) {
        MediaPreviewModal(
            item = activeMediaPreview!!,
            onDismiss = { activeMediaPreview = null },
            onForward = {
                val preview = activeMediaPreview
                if (preview != null) {
                    val foundMsg = messages.find { it.content.contains(preview.uri) }
                    selectedMessageForForward = foundMsg ?: MessageEntity(
                        id = UUID.randomUUID().toString(),
                        chatId = chat.id,
                        senderId = currentUser?.id ?: "me",
                        senderName = currentUser?.name ?: "أنا",
                        type = MessageType.IMAGE.name,
                        content = preview.uri + if (preview.caption.isNotBlank()) "||${preview.caption}" else "",
                        timestamp = System.currentTimeMillis()
                    )
                }
                activeMediaPreview = null
            }
        )
    }

    if (selectedMessageForOptions != null || selectedMessageForReaction != null) {
        val targetMsg = selectedMessageForOptions ?: selectedMessageForReaction!!
        ChatMessageOptionsDialog(
            targetMsg = targetMsg,
            onDismiss = {
                selectedMessageForOptions = null
                selectedMessageForReaction = null
            },
            onToggleReaction = onToggleReaction,
            onReply = { replyingToMessage = it },
            onForward = { selectedMessageForForward = it },
            onCopiedNotice = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
            onTranslate = onTranslateMessage
        )
    }

    if (showChatSettingsDialog) {
        ChatDetailThemeSettingsDialog(
            chat = chat,
            customChatBg = customChatBg,
            customChatBgColor = customChatBgColor,
            customBubbleStyle = customBubbleStyle,
            customBubbleColor = customBubbleColor,
            customFontColorStyle = customFontColorStyle,
            customFontColor = customFontColor,
            translationFontSize = translationFontSize,
            translationColorStyle = translationColorStyle,
            isUserBlocked = isUserBlocked,
            onDismiss = { showChatSettingsDialog = false },
            onBlockedChange = { isUserBlocked = it },
            onSaveTheme = { bgStyle, bgColor, bubbleStyle, bubbleColor, fontStyle, fontColor, trFontSize, trColorStyle ->
                customChatBg = bgStyle
                customChatBgColor = bgColor
                customBubbleStyle = bubbleStyle
                customBubbleColor = bubbleColor
                customFontColorStyle = fontStyle
                customFontColor = fontColor
                onUpdateTranslationFontSize(trFontSize)
                onUpdateTranslationColorStyle(trColorStyle)
                showChatSettingsDialog = false
            }
        )
    }

    if (showContactProfileModal) {
        ChatContactProfileDialog(
            chat = chat,
            contactUser = contactUser,
            onDismiss = { showContactProfileModal = false },
            onStartCall = { activeCallType = it },
            onNavigateToProfile = onNavigateToProfile,
            onOpenChatSettings = { showChatSettingsDialog = true }
        )
    }

    if (selectedMessageForForward != null) {
        ForwardMessageDialog(
            messageToForward = selectedMessageForForward!!,
            currentChatId = chat.id,
            availableChats = availableChats,
            onDismiss = { selectedMessageForForward = null },
            onForwardToChat = { targetChat ->
                val msg = selectedMessageForForward!!
                onForwardMessage(targetChat.id, msg.content, msg.type)
                forwardSuccessMessage = "تمت إعادة توجيه الرسالة بنجاح إلى ${targetChat.contactName}"
                selectedMessageForForward = null
            }
        )
    }

    if (showMessengerOptionsSheet) {
        ChatMessengerOptionsSheet(
            chat = chat,
            contactUser = contactUser,
            isZoxAdminChat = isZoxAdminChat,
            activeGroupMembersCount = activeGroupMembers.size,
            accountsCount = accounts.size,
            messagesDisplayCount = if (totalMessagesCount > 0) totalMessagesCount else messages.size,
            activeSpatialMode = activeSpatialMode,
            isAutoTranslateEnabled = isAutoTranslateEnabled,
            translateMyMessagesToo = translateMyMessagesToo,
            isUserBlocked = isUserBlocked,
            onDismiss = { showMessengerOptionsSheet = false },
            onCallAttempt = { activeCallType = it },
            onCallPrivacyBlocked = { callPrivacyBlockedReason = it },
            onZoxCallBlocked = { showZoxCallBlockedDialog = true },
            onNavigateToProfile = onNavigateToProfile,
            onOpenContactProfile = { showContactProfileModal = true },
            onBackAfterBubble = onBackClick,
            onOpenGroupMembers = {
                groupMembersInitialTab = 0
                showGroupMembersDialog = true
            },
            onOpenChatSettings = { showChatSettingsDialog = true },
            onToggleTopModesRail = { isTopModesRailExpanded = !isTopModesRailExpanded },
            onAutoTranslateChange = { isAutoTranslateEnabled = it },
            onTranslateMyMessagesChange = { translateMyMessagesToo = it },
            onOpenGoldDeal = onOpenGoldDeal,
            onOpenLedgerSheet = { showLedgerSheet = true },
            onOpenClearingDialog = { showClearingDialog = true },
            onExportPdf = {
                onExportFullChatPdf?.invoke() ?: ReportExporter.exportChatConversationToPdf(context, chat, messages)
            },
            onOpenVoiceBusinessAgent = onOpenVoiceBusinessAgent,
            onOpenAIAccountant = onOpenAIAccountant
        )
    }

    if (showQuickHashtagSheet) {
        ChatQuickHashtagDialog(
            hashtags = hashtags,
            allProjects = allProjects,
            initialTag = quickHashtagInitialTag,
            onRecordHashtagOperation = onRecordHashtagOperation,
            onSendFallbackMessage = onSendMessage,
            onDismiss = {
                showQuickHashtagSheet = false
                quickHashtagInitialTag = null
            }
        )
    }

    val targetPayInv = quickPayInvoiceTarget
    if (targetPayInv != null && onPayInvoice != null) {
        ChatQuickPayInvoiceDialog(
            targetPayInv = targetPayInv,
            onDismiss = { quickPayInvoiceTarget = null },
            onPayInvoice = onPayInvoice
        )
    }

    val activeInspectedTag = inspectedHashtagTag
    if (activeInspectedTag != null) {
        ChatHashtagInspectorDialog(
            inspectedTag = activeInspectedTag,
            hashtags = hashtags,
            allEntries = allEntries,
            invoicesForChat = invoicesForChat,
            allInvoices = allInvoices,
            onDismiss = { inspectedHashtagTag = null },
            onInspectInvoiceCode = { code -> inspectedCode = code },
            onOpenNewEntryForTag = { tag ->
                quickHashtagInitialTag = tag
                showQuickHashtagSheet = true
            }
        )
    }

    val activeInspectedCode = inspectedCode
    if (activeInspectedCode != null) {
        ChatCodeInspectorDialog(
            inspectedCode = activeInspectedCode,
            invoicesForChat = invoicesForChat,
            allInvoices = allInvoices,
            allTransactions = allTransactions,
            canPayInvoices = onPayInvoice != null,
            onDismiss = { inspectedCode = null },
            onPayInvoiceTarget = { matchedInvoice -> quickPayInvoiceTarget = matchedInvoice }
        )
    }

    if (showCreateTaskInChatDialog) {
        CreatePhysicalFieldTaskDialog(
            projectId = linkedProject?.id ?: "wg_${linkedWorkGroup?.id ?: chat.id}",
            projectName = linkedProject?.name ?: linkedWorkGroup?.name ?: chat.contactName,
            workGroupId = linkedWorkGroup?.id ?: chat.groupId,
            availableMembers = mentionCandidates,
            onDismiss = { showCreateTaskInChatDialog = false },
            onCreateTask = { task ->
                onInsertFieldTask(task)
                showCreateTaskInChatDialog = false
            }
        )
    }

    if (showGroupMembersDialog && chat.isGroup) {
        GroupMembersAndAddDialog(
            groupTitle = linkedWorkGroup?.name ?: chat.contactName,
            ownerId = linkedWorkGroup?.ownerId,
            members = activeGroupMembers,
            contacts = allContacts,
            isCurrentUserManager = isCurrentUserGroupManager || activeGroupMembers.isEmpty(),
            initialTab = groupMembersInitialTab,
            onDismiss = { showGroupMembersDialog = false },
            onAddMembers = { chosenContacts, manualName, manualPhone, roleLabel, isAdmin ->
                onAddGroupMembers(chosenContacts, manualName, manualPhone, roleLabel, isAdmin)
                showGroupMembersDialog = false
            },
            onToggleAdmin = onToggleGroupMemberAdmin,
            onRemoveMember = onRemoveGroupMember
        )
    }

    chatTaskPendingProof?.let { targetTask ->
        WorkerSubmitProofDialog(
            task = targetTask,
            currentUserName = currentUser?.name ?: "الموظف المكلف",
            currentUserId = currentUser?.id ?: "worker_1",
            onDismiss = { chatTaskPendingProof = null },
            onSubmitProof = { uri, type, notes, uid, uname ->
                onSubmitTaskProof(targetTask.id, uri, type, notes, uid, uname)
                chatTaskPendingProof = null
            }
        )
    }

    chatTaskPendingVerify?.let { targetTask ->
        AdminVerifyTaskDialog(
            task = targetTask,
            onDismiss = { chatTaskPendingVerify = null },
            onConfirmAndArchive = { amount, notes ->
                onConfirmAndArchiveTask(targetTask.id, amount, notes)
                chatTaskPendingVerify = null
            }
        )
    }
}
