package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.ui.components.AdminVerifyTaskDialog
import com.example.ui.components.CallOverlayDialog
import com.example.ui.components.CallType
import com.example.ui.components.CameraVideoNoteDialog
import com.example.ui.components.CoverPhotoEditorDialog
import com.example.ui.components.CreatePhysicalFieldTaskDialog
import com.example.ui.components.GroupMembersAndAddDialog
import com.example.ui.components.WorkerSubmitProofDialog
import com.example.ui.components.ImageFrameAspectMode
import com.example.ui.components.InlineChatVideoPlayer
import com.example.ui.components.MediaPreviewItem
import com.example.ui.components.MediaPreviewModal
import com.example.ui.components.MediaChooserKind
import com.example.ui.components.MediaSourceChooserCard
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.components.OozoxThumbnailImage
import com.example.ui.components.VideoEditorDialog
import com.example.ui.components.VoiceDictationAssistantDialog
import com.example.ui.components.VoiceDictationMode
import com.example.ui.spatial.*
import com.example.ui.theme.*
import com.example.util.AudioPlayerManager
import com.example.util.AudioRecorderManager
import com.example.util.BlockManager
import com.example.util.CallPrivacyManager
import com.example.util.ContentSafetyEngine
import com.example.util.DocumentInfo
import com.example.util.FileUtils
import com.example.util.GoldUtils
import com.example.util.HashtagUtils
import com.example.util.ShortCodeGenerator
import com.example.util.SmartVoiceAccountingParser
import com.example.util.SpeechToTextManager
import com.example.util.TextToSpeechHelper
import com.example.ui.viewmodel.TranslatedMessageUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

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
    var isContextPanelExpanded by remember { mutableStateOf(true) }
    var isVoiceCaptureActive by remember { mutableStateOf(false) }
    var showTopOptionsMenu by remember { mutableStateOf(false) }
    var showAttachmentActionsSheet by remember { mutableStateOf(false) }
    var showVoiceDictationDialog by remember { mutableStateOf(false) }
    var activeDictationMode by remember { mutableStateOf(VoiceDictationMode.INVOICE) }
    var selectedMessageForForward by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForOptions by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForReaction by remember { mutableStateOf<MessageEntity?>(null) }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var forwardSearchQuery by remember { mutableStateOf("") }
    var forwardSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showChatSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationTonePickerInChat by remember { mutableStateOf(false) }
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

    // Expandable Quick Tools Bar State
    var isQuickToolsExpanded by remember { mutableStateOf(false) }

    // Top Modes Expandable Button State (Screenshot item)
    var isTopModesRailExpanded by remember { mutableStateOf(false) }

    val densityForIme = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val isImeOpen = WindowInsets.ime.getBottom(densityForIme) > with(densityForIme) { 80.dp.toPx() }

    // Unified Voice / Camera Toggle & Live Video Note State
    var isActionButtonCameraMode by remember { mutableStateOf(false) }
    var isLiveCameraCapturing by remember { mutableStateOf(false) }
    var isContinuousCameraMode by remember { mutableStateOf(false) }
    var liveCameraCaptureStartTime by remember { mutableLongStateOf(0L) }
    var showCameraVideoNoteDialog by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Minimalist Inline Audio Recording & Draft Playback State (when IME closed)
    var isInlineRecordingAudio by remember { mutableStateOf(false) }
    var isInlineRecordCancelZone by remember { mutableStateOf(false) }
    var inlineRecordSeconds by remember { mutableIntStateOf(0) }
    var inlineRecordAmp by remember { mutableFloatStateOf(0.25f) }
    var voiceDraftPath by remember { mutableStateOf<String?>(null) }
    var voiceDraftDurationSec by remember { mutableIntStateOf(0) }
    var isDraftAudioPlaying by remember { mutableStateOf(false) }
    var draftAudioProgress by remember { mutableFloatStateOf(0f) }

    // Legacy / Quick Voice Button & Drag Gesture State
    var isUnifiedVoiceActive by remember { mutableStateOf(false) }
    var unifiedVoiceMode by remember { mutableStateOf(UnifiedVoiceDragMode.RECORD_UP) }
    var unifiedDragY by remember { mutableFloatStateOf(0f) }
    var unifiedDragX by remember { mutableFloatStateOf(0f) }

    // Hold-To-Record Voice-to-Text State
    var isHoldRecording by remember { mutableStateOf(false) }
    var holdRecordStartTime by remember { mutableLongStateOf(0L) }
    var holdLiveTranscript by remember { mutableStateOf("") }
    var holdAmpLevel by remember { mutableFloatStateOf(0.4f) }
    val holdSpeechManager = remember { SpeechToTextManager(context) }

    // Direct Inline Dictation (Voice-to-Text) State
    var isInlineDictating by remember { mutableStateOf(false) }
    var dictationLanguage by remember { mutableStateOf("ar-EG") }
    var dictationBaseText by remember { mutableStateOf("") }
    var dictationLiveText by remember { mutableStateOf("") }
    var isStartingDictationAfterPermission by remember { mutableStateOf(false) }
    val inlineSpeechManager = remember { SpeechToTextManager(context) }
    val inlineSpeechRms by inlineSpeechManager.rmsLevel.collectAsStateWithLifecycle()
    val inlineSpeechError by inlineSpeechManager.errorState.collectAsStateWithLifecycle()

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

    // Polling microphone amplitude & elapsed timer when holding inline audio recording
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

    // Polling microphone amplitude when holding to record
    LaunchedEffect(isHoldRecording) {
        while (isHoldRecording) {
            val maxAmp = AudioRecorderManager.getMaxAmplitude()
            val normalized = (maxAmp / 25000f).coerceIn(0.2f, 1.0f)
            holdAmpLevel = normalized
            delay(70L)
        }
    }

    // Camera & Media & Document Attachment State
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
                        val persistentUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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

    val genericFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
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
                        val persistentUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )
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
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                videoFile
            )
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
            if (targetKind == MediaChooserKind.VIDEO) {
                launchVideoCameraCapture()
            } else {
                launchCameraCapture()
            }
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

    val startInlineDictation: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            isStartingDictationAfterPermission = true
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startInlineDictationInternal()
        }
    }

    val onCameraTrigger: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraCapture()
        } else {
            pendingCameraPermissionKind = MediaChooserKind.PHOTO
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onVideoCameraTrigger: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchVideoCameraCapture()
        } else {
            pendingCameraPermissionKind = MediaChooserKind.VIDEO
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onMicTrigger: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            isVoiceCaptureActive = true
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var showLedgerSheet by remember { mutableStateOf(false) }
    var showNewAccountDialog by remember { mutableStateOf(false) }
    var showNewEntryDialog by remember { mutableStateOf(false) }
    var showClearingDialog by remember { mutableStateOf(false) }
    var showVoiceAccountingDialog by remember { mutableStateOf(false) }
    var isAutoTranslateEnabled by rememberSaveable { mutableStateOf(true) }
    var translateMyMessagesToo by rememberSaveable { mutableStateOf(false) }
    var showTranslateOptionsMenu by remember { mutableStateOf(false) }
    var activeCallType by remember { mutableStateOf<CallType?>(null) }
    var showZoxCallBlockedDialog by remember { mutableStateOf(false) }
    val isZoxAdminChat = remember(chat) {
        chat?.contactType == "ADMIN"
    }
    var selectedHashtagIdForEntry by remember { mutableStateOf<String?>(null) }
    
    var selectedAccountIdForEntry by remember { mutableStateOf<String?>(null) }

    var chatStreamFilter by rememberSaveable(chat?.id) { mutableStateOf("ALL") } // "ALL", "CHAT_ONLY", "FINANCE_ONLY"
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
            (chat.groupId != null && wg.id == chat.groupId) ||
                wg.id == chat.contactId ||
                chat.id == "chat_wg_${wg.id}"
        }
    }
    val linkedProject = remember(linkedWorkGroup, allProjects) {
        if (linkedWorkGroup == null) null
        else allProjects.firstOrNull { it.workGroupId == linkedWorkGroup.id }
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
        if (linkedWorkGroup == null) emptyList()
        else allGroupMembers.filter { it.groupId == linkedWorkGroup.id }
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
    val scope = rememberCoroutineScope()

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

    // حساب أول رسالة غير مقروءة قادمة من المستخدم الآخر
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

    // Auto Lazy-Load older messages when user scrolls upward toward the top of loaded history
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
            // Slim Facebook Messenger Style Header at Highest Safe Area directly below Status Bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(50.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showMessengerOptionsSheet = true }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Box(modifier = Modifier.size(36.dp)) {
                                OozoxAvatarImage(
                                    model = contactUser?.avatarUri,
                                    name = chat.contactName,
                                    size = 36.dp,
                                    isGroup = chat.isGroup,
                                    isTrader = chat.contactType == UserType.GOLD_TRADER.name,
                                    contentDescription = chat.contactName
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = chat.contactName,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (chat.contactVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "موثق",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = when {
                                        isZoxAdminChat -> "الإدارة الرسمية"
                                        chat.isGroup && activeGroupMembers.isNotEmpty() -> "مجموعة نشطة • ${activeGroupMembers.size} أعضاء"
                                        chat.isGroup -> "مجموعة نشطة"
                                        chat.contactType == UserType.GOLD_TRADER.name -> "نشط الآن • تاجر معتمد"
                                        else -> "نشط الآن"
                                    },
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isZoxAdminChat) Color(0xFF10B981) else GoldAccent
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val (allowed, reason) = CallPrivacyManager.checkCallAllowed(
                                    context = context,
                                    isContactSaved = !chat.isGroup,
                                    callerName = chat.contactName
                                )
                                if (!allowed) {
                                    callPrivacyBlockedReason = reason
                                } else if (isZoxAdminChat) {
                                    showZoxCallBlockedDialog = true
                                } else {
                                    activeCallType = CallType.AUDIO
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isZoxAdminChat) Icons.Default.PhoneDisabled else Icons.Default.Call,
                                contentDescription = "اتصال صوتي",
                                tint = if (isZoxAdminChat) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val (allowed, reason) = CallPrivacyManager.checkCallAllowed(
                                    context = context,
                                    isContactSaved = !chat.isGroup,
                                    callerName = chat.contactName
                                )
                                if (!allowed) {
                                    callPrivacyBlockedReason = reason
                                } else if (isZoxAdminChat) {
                                    showZoxCallBlockedDialog = true
                                } else {
                                    activeCallType = CallType.VIDEO
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isZoxAdminChat) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = "مكالمة فيديو",
                                tint = if (isZoxAdminChat) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else GoldPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        IconButton(
                            onClick = { showMessengerOptionsSheet = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("chat_messenger_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "خيارات ومعلومات المحادثة",
                                tint = GoldPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Stream Separation Bar + Search + Quick #Hashtag Trigger
                    val chatTotalBilled = remember(invoicesForChat) {
                        invoicesForChat.sumOf { it.totalAmount }
                    }
                    val chatTotalPaid = remember(invoicesForChat) {
                        invoicesForChat.sumOf { it.paidAmount }
                    }
                    val chatRemaining = remember(chatTotalBilled, chatTotalPaid) {
                        (chatTotalBilled - chatTotalPaid).coerceAtLeast(0.0)
                    }
                    val chatOpenInvoicesCount = remember(invoicesForChat) {
                        invoicesForChat.count { it.status != InvoiceStatus.PAID.name && (it.totalAmount - it.paidAmount) > 0.01 }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "ALL" to "الكل",
                            "CHAT_ONLY" to "الدردشة فقط",
                            "FINANCE_ONLY" to "الفواتير والهاشتاجات #"
                        ).forEach { (key, label) ->
                            val selected = chatStreamFilter == key
                            FilterChip(
                                selected = selected,
                                onClick = { chatStreamFilter = key },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary.copy(alpha = 0.16f),
                                    selectedLabelColor = GoldPrimary
                                ),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("chat_stream_filter_$key")
                            )
                        }

                        Surface(
                            onClick = { showQuickHashtagSheet = true },
                            shape = RoundedCornerShape(16.dp),
                            color = GoldPrimary.copy(alpha = 0.14f),
                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("chat_top_hashtag_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("#", color = GoldPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Text("فاتورة / هاشتاج", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        IconButton(
                            onClick = {
                                showChatSearchBar = !showChatSearchBar
                                if (!showChatSearchBar) chatSearchQuery = ""
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("chat_toggle_search_btn")
                        ) {
                            Icon(
                                imageVector = if (showChatSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "بحث في المحادثة",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showChatSearchBar,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = chatSearchQuery,
                            onValueChange = { chatSearchQuery = it },
                            placeholder = { Text("ابحث في الرسائل أو #...", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (chatSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { chatSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("chat_search_input")
                        )
                    }

                    if (invoicesForChat.isNotEmpty() || hashtags.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.6.dp, GoldPrimary.copy(alpha = 0.25f)),
                            onClick = { showLedgerSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("chat_client_ledger_strip")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "الفواتير: ${GoldUtils.formatCurrency(chatTotalBilled)}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "متبقي: ${GoldUtils.formatCurrency(chatRemaining)}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (chatRemaining > 0.01) StatusOrange else StatusGreen,
                                        maxLines = 1
                                    )
                                }
                                if (chatOpenInvoicesCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StatusOrange.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "$chatOpenInvoicesCount مفتوحة",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusOrange,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "الدفتر (${hashtags.size}#)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // WorkGroup / Company Hierarchy & Manager Task Quick Strip
            if (chat.isGroup) {
                Surface(
                    color = GoldPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(0.6.dp, GoldPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workgroup_hierarchy_strip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrentUserGroupManager) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoldPrimary.copy(alpha = 0.18f),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                                    Text("صلاحية المدير", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                groupMembersInitialTab = 1
                                showGroupMembersDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("chat_header_members_count_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = "أعضاء المجموعة", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${activeGroupMembers.size} أعضاء",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                groupMembersInitialTab = 0
                                showGroupMembersDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.14f),
                            border = BorderStroke(0.8.dp, Color(0xFF3B82F6).copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("chat_header_add_member_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(13.dp))
                                Text("إضافة عضو", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                            }
                        }

                        Surface(
                            onClick = { showCreateTaskInChatDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.14f),
                            border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("chat_header_assign_task_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                                Text("إسناد مهمة", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }

                        if (parentCompanyGroup != null) {
                            Surface(
                                onClick = { onOpenGroupChatById(parentCompanyGroup.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "الرئيسية: ${parentCompanyGroup.name}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        subDepartmentGroups.forEach { dept ->
                            Surface(
                                onClick = { onOpenGroupChatById(dept.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier.testTag("dept_chip_${dept.id}")
                            ) {
                                Text(
                                    text = "قسم: ${dept.name}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Smart Spatial Mode Rail (Collapsible from Messenger Options Sheet)
            AnimatedVisibility(
                visible = isTopModesRailExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = SpatialElevation.Level2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChatSpatialMode.entries.forEach { mode ->
                            val selected = activeSpatialMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    activeSpatialMode = mode
                                    isTopModesRailExpanded = false
                                },
                                label = { Text(mode.label, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (mode) {
                                        ChatSpatialMode.GOLD -> GoldDark
                                        ChatSpatialMode.FINANCIAL -> Color(0xFF1E293B)
                                        ChatSpatialMode.PROJECT -> Color(0xFF1E3A5F)
                                        ChatSpatialMode.AI_ACCOUNTANT -> Color(0xFF2E1065)
                                        ChatSpatialMode.COMMUNICATION -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            val isNetworkOnline by com.example.util.OfflineSyncManager.isNetworkOnline.collectAsStateWithLifecycle()
            val isSyncing by com.example.util.OfflineSyncManager.isSyncing.collectAsStateWithLifecycle()

            AnimatedVisibility(
                visible = !isNetworkOnline,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = Color(0xFFD97706).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .testTag("offline_chat_cache_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وضع عدم الاتصال — يُحفظ محلياً",
                                fontSize = 11.sp,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { com.example.util.OfflineSyncManager.triggerImmediateSync(context) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = if (isSyncing) "جاري المزامنة..." else "مزامنة",
                                fontSize = 10.sp,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Full-bleed Message Stream with Pinch-to-Zoom Reflow
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    if (zoomChange != 1f) {
                                        messageZoomScale = (messageZoomScale * zoomChange).coerceIn(0.75f, 1.85f)
                                        lastZoomInteractionMs = System.currentTimeMillis()
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                val streamMaxWidth = maxWidth
                val streamMaxHeight = maxHeight

                // Adjustable Bottom Cushion for Conversation Stream (ONLY active when keyboard is closed)
                val minBottomCushionDp = 12.dp
                val maxBottomCushionDp = (streamMaxHeight * 0.36f).coerceIn(135.dp, 280.dp)
                val defaultBottomCushionDp = (streamMaxHeight * 0.14f).coerceIn(58.dp, 108.dp)
                var userBottomCushionDp by rememberSaveable(chat.id) { mutableFloatStateOf(-1f) }
                val targetCushionDp = if (isImeOpen) {
                    4.dp
                } else if (userBottomCushionDp < 0f) {
                    defaultBottomCushionDp
                } else {
                    userBottomCushionDp.dp.coerceIn(minBottomCushionDp, maxBottomCushionDp)
                }
                val effectiveBottomCushionDp by animateDpAsState(
                    targetValue = targetCushionDp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "chat_bottom_cushion"
                )

                val chatCushionNestedScroll = remember(isImeOpen, streamMaxHeight, densityForIme) {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (isImeOpen || source != NestedScrollSource.UserInput) return Offset.Zero
                            if (available.y != 0f) {
                                val atVisualBottom = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 4
                                val deltaDp = with(densityForIme) { (-available.y).toDp().value }
                                val current = if (userBottomCushionDp < 0f) defaultBottomCushionDp.value else userBottomCushionDp
                                // Unconsumed upward drag lifts conversation higher (even in short chats); downward drag at bottom lowers it
                                if (available.y < 0f || (available.y > 0f && atVisualBottom)) {
                                    val next = (current + deltaDp).coerceIn(minBottomCushionDp.value, maxBottomCushionDp.value)
                                    val appliedPx = with(densityForIme) { (next - current).dp.toPx() }
                                    userBottomCushionDp = next
                                    return Offset(0f, -appliedPx)
                                }
                            }
                            return Offset.Zero
                        }
                    }
                }

                if (messagesNewestFirst.isEmpty() && isInitialLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
                    ) {
                        val shimmerWidths = listOf(0.55f, 0.72f, 0.42f, 0.65f, 0.48f, 0.76f)
                        shimmerWidths.forEachIndexed { idx, frac ->
                            val alignEnd = idx % 2 == 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (alignEnd) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(frac)
                                        .height(if (idx % 3 == 0) 46.dp else 36.dp)
                                ) {}
                            }
                        }
                    }
                } else if (messagesNewestFirst.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = "بداية المحادثة مع ${chat.contactName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "يمكنك إرسال رسائل أو توثيق صفقات الذهب وقيود #هاشتاج.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { onSendMessage("مرحباً، أود الاستفسار عن أسعار الذهب اليوم", MessageType.TEXT.name) },
                                    label = { Text("استفسار عن الذهب", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = { onSendMessage("السلام عليكم، تم مراجعة الحسابات والقيود", MessageType.TEXT.name) },
                                    label = { Text("مراجعة القيود", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(chatCushionNestedScroll),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Adjustable free bottom cushion (index 0 visually at bottom in reverseLayout)
                        item(key = "chat_bottom_free_space") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(effectiveBottomCushionDp)
                                    .then(
                                        if (!isImeOpen) {
                                            Modifier.pointerInput(streamMaxHeight) {
                                                detectVerticalDragGestures { _, dragAmount ->
                                                    val current = if (userBottomCushionDp < 0f) {
                                                        defaultBottomCushionDp.value
                                                    } else {
                                                        userBottomCushionDp
                                                    }
                                                    val deltaDp = with(densityForIme) { (-dragAmount).toDp().value }
                                                    userBottomCushionDp = (current + deltaDp).coerceIn(
                                                        minBottomCushionDp.value,
                                                        maxBottomCushionDp.value
                                                    )
                                                }
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .testTag("chat_bottom_free_space"),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (!isImeOpen && effectiveBottomCushionDp >= 24.dp) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .width(34.dp)
                                            .height(3.5.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f))
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = messagesNewestFirst,
                            key = { _, msg: MessageEntity ->
                                msg.id.ifBlank { "${msg.timestamp}_${msg.senderId}" }
                            }
                        ) { revIndex, msg ->
                            val isMe = if (currentUser != null) {
                                msg.senderId == currentUser.id || msg.senderName == currentUser.name
                            } else {
                                msg.senderName != chat.contactName
                            }

                            // In reverseLayout = true: revIndex + 1 is visually ABOVE (older), revIndex - 1 is visually BELOW (newer)
                            val olderMsg = messagesNewestFirst.getOrNull(revIndex + 1)
                            val newerMsg = messagesNewestFirst.getOrNull(revIndex - 1)
                            val showHourHeader = olderMsg == null || !GoldUtils.isSameHour(olderMsg.timestamp, msg.timestamp)
                            val belowShowsHourHeader = newerMsg != null && !GoldUtils.isSameHour(msg.timestamp, newerMsg.timestamp)

                            val olderIsMe = olderMsg?.let { p ->
                                if (currentUser != null) p.senderId == currentUser.id || p.senderName == currentUser.name
                                else p.senderName != chat.contactName
                            }
                            val newerIsMe = newerMsg?.let { n ->
                                if (currentUser != null) n.senderId == currentUser.id || n.senderName == currentUser.name
                                else n.senderName != chat.contactName
                            }

                            val sameSenderAbove = olderMsg != null && !showHourHeader && olderIsMe == isMe && (isMe || olderMsg.senderName == msg.senderName)
                            val sameSenderBelow = newerMsg != null && !belowShowsHourHeader && newerIsMe == isMe && (isMe || newerMsg.senderName == msg.senderName)

                            val maxBubbleWidth = (streamMaxWidth * (if (isMe) 0.78f else 0.74f)).coerceAtLeast(180.dp)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = when {
                                            showHourHeader -> 5.dp
                                            sameSenderAbove -> 1.5.dp
                                            else -> 5.dp
                                        },
                                        bottom = if (sameSenderBelow) 0.dp else 1.dp
                                    )
                            ) {
                                if (showHourHeader) {
                                    ChatHourlyTimestampHeader(
                                        timestamp = msg.timestamp,
                                        zoomScale = animatedZoomScale,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                    )
                                }

                                if (firstUnreadReversedIndex != -1 && revIndex == firstUnreadReversedIndex) {
                                    Surface(
                                        color = GoldPrimary.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MarkEmailUnread,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "رسائل غير مقروءة",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary
                                            )
                                        }
                                    }
                                }

                                ChatBubble(
                                    msg = msg,
                                    isMe = isMe,
                                    isFirstInGroup = !sameSenderAbove,
                                    isLastInGroup = !sameSenderBelow,
                                    zoomScale = animatedZoomScale,
                                    maxBubbleWidth = maxBubbleWidth,
                                    customFontColorStyle = customFontColorStyle,
                                    customBubbleStyle = customBubbleStyle,
                                    customFontColor = customFontColor,
                                    customBubbleColor = customBubbleColor,
                                    isAutoTranslateEnabled = isAutoTranslateEnabled,
                                    translateMyMessagesToo = translateMyMessagesToo,
                                    translationFontSize = translationFontSize,
                                    translationColorStyle = translationColorStyle,
                                    translation = translatedMessages[msg.id],
                                    invoicesForChat = invoicesForChat,
                                    onQuickPayInvoice = if (onPayInvoice != null) { inv -> quickPayInvoiceTarget = inv } else null,
                                    onHashtagClick = { clickedTag -> inspectedHashtagTag = clickedTag },
                                    onCodeClick = { clickedCode -> inspectedCode = clickedCode },
                                    onTranslateClick = { onTranslateMessage(msg) },
                                    onSpeakTranslation = { text, lang ->
                                        if (speakingMessageId == msg.id) {
                                            ttsHelper.stop()
                                            speakingMessageId = null
                                        } else {
                                            ttsHelper.stop()
                                            speakingMessageId = msg.id
                                            ttsHelper.speakText(
                                                text = text,
                                                languageCode = lang,
                                                onStart = { speakingMessageId = msg.id },
                                                onDone = { speakingMessageId = null }
                                            )
                                        }
                                    },
                                    onStopSpeaking = {
                                        ttsHelper.stop()
                                        speakingMessageId = null
                                    },
                                    isSpeakingTranslation = (speakingMessageId == msg.id),
                                    onDismissTranslation = { onDismissTranslation(msg.id) },
                                    onAvatarClick = {
                                        showMessengerOptionsSheet = true
                                    },
                                    onImageClick = { previewItem ->
                                        activeMediaPreview = previewItem
                                    },
                                    onOptionsClick = { selectedMessageForOptions = msg },
                                    onLongPress = { selectedMessageForReaction = msg },
                                    onReactionClick = { selectedMessageForReaction = msg },
                                    onForwardClick = { selectedMessageForForward = msg },
                                    onReply = { replyingToMessage = msg },
                                    projectTasks = projectTasks,
                                    onWorkerSubmitTaskProof = { task -> chatTaskPendingProof = task },
                                    onAdminVerifyTask = { task -> chatTaskPendingVerify = task },
                                    onMentionClick = { mention ->
                                        val cleanMention = "@${mention.removePrefix("@").trim()}"
                                        textInput = if (textInput.isBlank()) "$cleanMention " else "${textInput.trimEnd()} $cleanMention "
                                    }
                                )
                            }
                        }

                        if (isLoadingOlderMessages || hasMoreOlderMessages) {
                            item(key = "chat_history_lazy_loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        onClick = { if (!isLoadingOlderMessages) onLoadOlderMessages() },
                                        enabled = !isLoadingOlderMessages,
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier.testTag("chat_load_older_pill")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isLoadingOlderMessages) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 1.8.dp,
                                                    color = GoldPrimary
                                                )
                                                Text(
                                                    text = "جاري تحميل الرسائل الأقدم...",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = GoldPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "تحميل الرسائل الأقدم",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating scroll-to-bottom action button
                    val isScrolledUp = remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex > 2
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isScrolledUp.value,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 10.dp)
                    ) {
                        Surface(
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            },
                            color = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary,
                            shape = RoundedCornerShape(18.dp),
                            shadowElevation = 5.dp,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "التمرير للأسفل",
                                    tint = adaptiveOnPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (firstUnreadIndex != -1) "رسائل جديدة" else "الأسفل",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = adaptiveOnPrimary
                                )
                            }
                        }
                    }
                }

                // Active Zoom Level Pill (Pinch to zoom in/out, tap to reset 100%)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showZoomPill || abs(messageZoomScale - 1f) > 0.03f,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    Surface(
                        onClick = {
                            messageZoomScale = 1f
                            showZoomPill = false
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.testTag("chat_zoom_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${(animatedZoomScale * 100).roundToInt()}%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (abs(messageZoomScale - 1f) > 0.03f) {
                                Text(
                                    text = "إعادة ضبط",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Forward Message Success Banner
            AnimatedVisibility(visible = forwardSuccessMessage != null) {
                Surface(
                    color = StatusGreen.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(forwardSuccessMessage ?: "", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { forwardSuccessMessage = null }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Slim Messenger-style Bottom Composer
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    if (isVoiceCaptureActive) {
                        VoiceCaptureSpace(
                            onCancel = { isVoiceCaptureActive = false },
                            onSendVoiceNote = { duration, audioPath, transcript ->
                                val contentPayload = if (audioPath.isNotBlank()) {
                                    "$audioPath||$duration||$transcript"
                                } else {
                                    "تسجيل صوتي ($duration ثانية) - $transcript"
                                }
                                onSendMessage(contentPayload, MessageType.VOICE.name)
                                isVoiceCaptureActive = false
                            },
                            onSendAsText = { transcribedText ->
                                val tag = HashtagUtils.parseHashtag(transcribedText)
                                val msgType = if (tag != null) MessageType.HASHTAG.name else MessageType.TEXT.name
                                onSendMessage(transcribedText.trim(), msgType)
                                isVoiceCaptureActive = false
                            },
                            onInsertIntoInput = { transcribedText ->
                                textInput = transcribedText
                                isVoiceCaptureActive = false
                            }
                        )
                    } else {
                        // Inline Voice-to-Text Live Dictation Floating Banner
                        AnimatedVisibility(
                            visible = isInlineDictating,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            val pulseTransition = rememberInfiniteTransition(label = "inline_dict_pulse")
                            val pulseScale by pulseTransition.animateFloat(
                                initialValue = 0.92f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(450, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "inline_dict_scale"
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.5.dp, GoldPrimary),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .scale(pulseScale)
                                                    .clip(CircleShape)
                                                    .background(adaptiveGoldPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "جاري الاستماع",
                                                    tint = adaptiveOnPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "إملاء صوتي فوري (تحويل الصوت لنص) ️",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldPrimary
                                                )
                                                Text(
                                                    text = "تحدث لإدراج الكلمات مباشرة في صندوق الرسالة",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Language toggle
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = GoldPrimary.copy(alpha = 0.15f),
                                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.4f)),
                                                modifier = Modifier.clickable {
                                                    dictationLanguage = if (dictationLanguage == "ar-EG") "en-US" else "ar-EG"
                                                    inlineSpeechManager.stopListening()
                                                    inlineSpeechManager.startListening(
                                                        languageCode = dictationLanguage,
                                                        onPartial = { partial ->
                                                            if (partial.isNotBlank()) {
                                                                dictationLiveText = partial
                                                                textInput = if (dictationBaseText.isNotBlank()) "${dictationBaseText.trim()} $partial" else partial
                                                            }
                                                        },
                                                        onFinal = { finalText ->
                                                            if (finalText.isNotBlank()) {
                                                                dictationLiveText = ""
                                                                textInput = if (dictationBaseText.isNotBlank()) "${dictationBaseText.trim()} $finalText" else finalText
                                                            }
                                                            isInlineDictating = false
                                                        }
                                                    )
                                                }
                                            ) {
                                                Text(
                                                    text = if (dictationLanguage == "ar-EG") "عربي" else "EN",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            // Done Button
                                            Button(
                                                onClick = {
                                                    inlineSpeechManager.stopListening()
                                                    isInlineDictating = false
                                                    dictationLiveText = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = adaptiveGoldPrimary,
                                                    contentColor = adaptiveOnPrimary
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("تم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (dictationLiveText.isNotBlank()) dictationLiveText else "تحدث الآن... جاري التقاط صوتك وتحويله لنص عربي/إنجليزي تلقائياً",
                                                fontSize = 12.sp,
                                                fontWeight = if (dictationLiveText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (dictationLiveText.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            // Sound Level Bars
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                repeat(5) { i ->
                                                    val factor = ((i * 3) % 4 + 2) / 5f
                                                    val barH = (6 + (inlineSpeechRms * 16 * factor)).dp
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.5.dp)
                                                            .height(barH)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(GoldPrimary)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Hold-To-Record Live Transcription Floating Banner
                        AnimatedVisibility(
                            visible = isHoldRecording,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            val pulseTransition = rememberInfiniteTransition(label = "hold_pulse")
                            val pulseScale by pulseTransition.animateFloat(
                                initialValue = 0.96f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(450, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "hold_scale"
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.5.dp, GoldPrimary),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .scale(pulseScale)
                                                    .clip(CircleShape)
                                                    .background(StatusOrange),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "جاري تحويل الصوت إلى نص مباشرة",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldPrimary
                                                )
                                                Text(
                                                    text = "حرر للإرسال الفوري كرسالة نصية",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Live visual waveform
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            repeat(8) { i ->
                                                val factor = ((i * 7) % 5 + 3) / 8f
                                                val barH = (10 + (holdAmpLevel * 20 * factor)).dp
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(barH)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(GoldPrimary)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (holdLiveTranscript.isNotBlank()) holdLiveTranscript else "تحدث الآن... جاري الاستماع وتحويل الكلمات إلى نص",
                                            fontSize = 13.sp,
                                            fontWeight = if (holdLiveTranscript.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                            color = if (holdLiveTranscript.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // Attached Document Preview Row (if document attached)
                        AnimatedVisibility(
                            visible = attachedDocumentInfo != null,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            if (attachedDocumentInfo != null) {
                                val doc = attachedDocumentInfo!!
                                val isPdf = doc.fileName.endsWith(".pdf", true)
                                val isWord = doc.fileName.endsWith(".doc", true) || doc.fileName.endsWith(".docx", true)
                                val isExcel = doc.fileName.endsWith(".xls", true) || doc.fileName.endsWith(".xlsx", true) || doc.fileName.endsWith(".csv", true)
                                val isArchive = doc.fileName.endsWith(".zip", true) || doc.fileName.endsWith(".rar", true)
                                val docBadgeColor = when {
                                    isPdf -> Color(0xFFEF4444)
                                    isWord -> Color(0xFF2563EB)
                                    isExcel -> Color(0xFF10B981)
                                    isArchive -> Color(0xFFF59E0B)
                                    else -> GoldPrimary
                                }
                                val docIcon = when {
                                    isPdf -> Icons.Default.PictureAsPdf
                                    isWord -> Icons.Default.Description
                                    isExcel -> Icons.Default.TableChart
                                    isArchive -> Icons.Default.FolderZip
                                    else -> Icons.Default.InsertDriveFile
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, docBadgeColor.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                color = docBadgeColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = docIcon,
                                                        contentDescription = null,
                                                        tint = docBadgeColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = doc.fileName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = docBadgeColor.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = FileUtils.getFileExtension(doc.fileName).uppercase().ifBlank { "DOC" },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = docBadgeColor,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = FileUtils.formatFileSize(doc.fileSize),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { attachedDocumentInfo = null },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "إلغاء إرفاق المستند",
                                                tint = StatusRed
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Smart Media Processing Progress Bar
                        AnimatedVisibility(
                            visible = isProcessingMedia,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { mediaProcessProgress },
                                        modifier = Modifier.size(20.dp),
                                        color = adaptiveGoldPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                    LinearProgressIndicator(
                                        progress = { mediaProcessProgress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = adaptiveGoldPrimary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }

                        // Attached Image Preview Row (if photo/video captured)
                        AnimatedVisibility(
                            visible = capturedImageUri != null,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val isAttachedVideo = FileUtils.isVideoUri(context, capturedImageUri!!)
                                        OozoxThumbnailImage(
                                            model = capturedImageUri,
                                            width = 52.dp,
                                            height = 52.dp,
                                            shape = RoundedCornerShape(10.dp),
                                            contentScale = ContentScale.Fit,
                                            contentDescription = if (isAttachedVideo) "فيديو مرفق" else "صورة مرفقة",
                                            onClick = {
                                                activeMediaPreview = MediaPreviewItem(
                                                    uri = capturedImageUri.toString(),
                                                    caption = textInput,
                                                    senderName = currentUser?.name ?: "أنا",
                                                    timestamp = System.currentTimeMillis(),
                                                    isVideo = isAttachedVideo
                                                )
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isAttachedVideo) "فيديو جاهز" else "صورة كاملة",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveGoldPrimary
                                            )
                                            Text(
                                                text = "${(mediaProcessProgress * 100).roundToInt().coerceIn(0, 100)}%",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val isAttachedVideo = FileUtils.isVideoUri(context, capturedImageUri!!)
                                        if (isAttachedVideo) {
                                            IconButton(
                                                onClick = { pendingVideoEditUri = capturedImageUri },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = "تعديل الفيديو",
                                                    tint = adaptiveGoldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { pendingImageFrameUri = capturedImageUri },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CropFree,
                                                    contentDescription = "تعديل الصورة",
                                                    tint = adaptiveGoldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { capturedImageUri = null },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "حذف الصورة المرفقة",
                                                tint = StatusRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Replying to Message Floating Banner
                        AnimatedVisibility(
                            visible = replyingToMessage != null,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            if (replyingToMessage != null) {
                                val repMsg = replyingToMessage!!
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.5.dp)
                                                    .height(36.dp)
                                                    .background(GoldPrimary, RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "الرد على ${repMsg.senderName}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldPrimary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = when {
                                                        repMsg.type == MessageType.VOICE.name -> "تسجيل صوتي"
                                                        repMsg.type == MessageType.IMAGE.name -> "صورة ملتقطة"
                                                        repMsg.type == MessageType.DOCUMENT.name -> "مستند مرفق"
                                                        repMsg.type == MessageType.GOLD_PURCHASE.name -> "صفقة ذهب"
                                                        repMsg.type == MessageType.INVOICE.name -> "فاتورة معتمدة"
                                                        repMsg.type == MessageType.PAYMENT.name -> "إشعار سداد"
                                                        else -> if (repMsg.content.startsWith("↩️ رد على ") && repMsg.content.contains("\n\n")) {
                                                            repMsg.content.substringAfter("\n\n").take(80)
                                                        } else {
                                                            repMsg.content.take(80)
                                                        }
                                                    },
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { replyingToMessage = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "إلغاء الرد",
                                                tint = StatusRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val activeMentionMatch = remember(textInput) {
                            Regex("(?:^|\\s)@([\\u0600-\\u06FF\\w._-]*)$").find(textInput)
                        }
                        val filteredMentions = remember(activeMentionMatch, mentionCandidates) {
                            val q = activeMentionMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
                            if (activeMentionMatch == null) emptyList()
                            else mentionCandidates.filter { candidate ->
                                q.isBlank() || candidate.contains(q, ignoreCase = true)
                            }.take(6)
                        }

                        AnimatedVisibility(
                            visible = filteredMentions.isNotEmpty() && !isInlineRecordingAudio && voiceDraftPath == null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.45f)),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .testTag("chat_mention_suggestions_strip")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "@منشن:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                    filteredMentions.forEach { candidate ->
                                        val handleOnly = candidate.substringBefore(" - ").substringBefore(" (").trim()
                                        Surface(
                                            onClick = {
                                                val match = activeMentionMatch
                                                if (match != null) {
                                                    val prefix = textInput.substring(0, match.range.first)
                                                    val leadingSpace = if (match.value.startsWith(" ")) " " else ""
                                                    val cleanHandle = if (handleOnly.startsWith("@")) handleOnly else "@$handleOnly"
                                                    textInput = "$prefix$leadingSpace$cleanHandle "
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = candidate,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Unified Attachment & Tools Action Button (+) — houses #hashtag, voice, deals, media
                            IconButton(
                                onClick = { showAttachmentActionsSheet = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                    .testTag("chat_attachment_plus_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "إرفاق وأدوات",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            // Center Composer Slot: Live Audio Recording Bar OR Voice Draft Player OR Standard Input Pill
                            when {
                                isInlineRecordingAudio -> {
                                    // Minimalist clean live recording pill (ZERO explanatory text)
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = if (isInlineRecordCancelZone) {
                                            StatusRed.copy(alpha = 0.16f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isInlineRecordCancelZone) StatusRed else adaptiveGoldPrimary.copy(alpha = 0.55f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("inline_recording_pill")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isInlineRecordCancelZone) StatusRed else Color(0xFFEF4444))
                                            )
                                            Text(
                                                text = String.format(
                                                    Locale.US,
                                                    "%02d:%02d",
                                                    inlineRecordSeconds / 60,
                                                    inlineRecordSeconds % 60
                                                ),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isInlineRecordCancelZone) StatusRed else MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(22.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                repeat(14) { idx ->
                                                    val mod = ((idx * 5) % 6 + 3) / 8f
                                                    val h = (4f + inlineRecordAmp * 16f * mod).dp
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(h)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isInlineRecordCancelZone) StatusRed else adaptiveGoldPrimary
                                                            )
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "حذف التسجيل",
                                                tint = if (isInlineRecordCancelZone) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }

                                voiceDraftPath != null -> {
                                    // Minimalist clean Voice Draft Player IN PLACE OF text field (Play/Pause + Waveform + Timer + Delete)
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("voice_draft_player_pill")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (isDraftAudioPlaying) {
                                                        AudioPlayerManager.stop()
                                                        isDraftAudioPlaying = false
                                                    }
                                                    val path = voiceDraftPath
                                                    voiceDraftPath = null
                                                    voiceDraftDurationSec = 0
                                                    draftAudioProgress = 0f
                                                    if (!path.isNullOrBlank()) {
                                                        try { File(path).delete() } catch (_: Exception) {}
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .testTag("voice_draft_delete_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف التسجيل الصوتي",
                                                    tint = StatusRed,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(adaptiveGoldPrimary)
                                                    .clickable {
                                                        val path = voiceDraftPath ?: return@clickable
                                                        if (isDraftAudioPlaying) {
                                                            AudioPlayerManager.stop()
                                                            isDraftAudioPlaying = false
                                                        } else {
                                                            isDraftAudioPlaying = true
                                                            AudioPlayerManager.play(
                                                                context = context,
                                                                pathOrUri = path,
                                                                onProgress = { cur, tot ->
                                                                    if (tot > 0) {
                                                                        draftAudioProgress = (cur.toFloat() / tot.toFloat()).coerceIn(0f, 1f)
                                                                    }
                                                                },
                                                                onComplete = {
                                                                    isDraftAudioPlaying = false
                                                                    draftAudioProgress = 0f
                                                                }
                                                            )
                                                        }
                                                    }
                                                    .testTag("voice_draft_play_btn"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isDraftAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = if (isDraftAudioPlaying) "إيقاف" else "استماع",
                                                    tint = adaptiveOnPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(22.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val totalBars = 18
                                                repeat(totalBars) { idx ->
                                                    val barFrac = idx.toFloat() / totalBars.toFloat()
                                                    val barH = (5 + ((idx * 7) % 13)).dp
                                                    val passed = barFrac <= draftAudioProgress
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(barH)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (passed) adaptiveGoldPrimary else adaptiveGoldPrimary.copy(alpha = 0.3f)
                                                            )
                                                    )
                                                }
                                            }

                                            Text(
                                                text = String.format(
                                                    Locale.US,
                                                    "%d:%02d",
                                                    voiceDraftDurationSec / 60,
                                                    voiceDraftDurationSec % 60
                                                ),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    // Slim Messenger Rounded Pill Input Field
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 38.dp, max = 108.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (textInput.isEmpty()) {
                                                    Text(
                                                        text = when {
                                                            attachedDocumentInfo != null -> "تعليق..."
                                                            capturedImageUri != null -> "تعليق..."
                                                            else -> "رسالة..."
                                                        },
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                BasicTextField(
                                                    value = textInput,
                                                    onValueChange = { textInput = it },
                                                    textStyle = TextStyle(
                                                        fontSize = 14.5.sp,
                                                        lineHeight = 19.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    cursorBrush = SolidColor(GoldPrimary),
                                                    maxLines = 4,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("chat_message_input")
                                                )
                                            }
                                            if (textInput.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .clickable { textInput = "" },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "مسح النص",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Action End Button: Send OR Unified Mic <-> Camera Button
                            val hasContentToSend = textInput.isNotBlank() ||
                                attachedDocumentInfo != null ||
                                capturedImageUri != null ||
                                voiceDraftPath != null

                            if (hasContentToSend) {
                                IconButton(
                                    onClick = {
                                        if (voiceDraftPath != null) {
                                            if (isDraftAudioPlaying) {
                                                AudioPlayerManager.stop()
                                                isDraftAudioPlaying = false
                                            }
                                            val dur = voiceDraftDurationSec.coerceAtLeast(1)
                                            val payload = "${voiceDraftPath}||$dur||"
                                            onSendMessage(payload, MessageType.VOICE.name)
                                            voiceDraftPath = null
                                            voiceDraftDurationSec = 0
                                            draftAudioProgress = 0f
                                            replyingToMessage = null
                                        } else if (attachedDocumentInfo != null) {
                                            val doc = attachedDocumentInfo!!
                                            val payload = "${doc.uri}||${doc.fileName}||${doc.fileSize}||${textInput.trim()}"
                                            onSendMessage(payload, MessageType.DOCUMENT.name)
                                            attachedDocumentInfo = null
                                            replyingToMessage = null
                                            textInput = ""
                                        } else if (capturedImageUri != null) {
                                            val contentPayload = if (textInput.isNotBlank()) {
                                                "${capturedImageUri}||${textInput.trim()}"
                                            } else {
                                                capturedImageUri.toString()
                                            }
                                            onSendMessage(contentPayload, MessageType.IMAGE.name)
                                            capturedImageUri = null
                                            replyingToMessage = null
                                            textInput = ""
                                        } else if (textInput.isNotBlank()) {
                                            val sendText = textInput.trim()
                                            val finalContent = if (replyingToMessage != null) {
                                                val rep = replyingToMessage!!
                                                val repSnippet = when {
                                                    rep.type == MessageType.VOICE.name -> "تسجيل صوتي"
                                                    rep.type == MessageType.IMAGE.name -> "صورة"
                                                    rep.type == MessageType.DOCUMENT.name -> "مستند"
                                                    rep.type == MessageType.GOLD_PURCHASE.name -> "صفقة ذهب"
                                                    rep.type == MessageType.INVOICE.name -> "فاتورة"
                                                    rep.type == MessageType.PAYMENT.name -> "سداد مالي"
                                                    else -> {
                                                        val c = rep.content
                                                        if (c.startsWith("↩️ رد على ") && c.contains("\n\n")) c.substringAfter("\n\n").take(60) else c.take(60)
                                                    }
                                                }
                                                "↩️ رد على ${rep.senderName}:\n\"$repSnippet\"\n\n$sendText"
                                            } else {
                                                sendText
                                            }
                                            val tag = HashtagUtils.parseHashtag(sendText)
                                            val msgType = if (tag != null) MessageType.HASHTAG.name else MessageType.TEXT.name
                                            onSendMessage(finalContent, msgType)
                                            replyingToMessage = null
                                            textInput = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(adaptiveGoldPrimary)
                                        .testTag("chat_send_btn")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "إرسال",
                                        tint = adaptiveOnPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                val cancelThresholdPx = with(densityForIme) { 84.dp.toPx() }
                                val swipeDownCameraThresholdPx = with(densityForIme) { 22.dp.toPx() }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isInlineRecordingAudio && isInlineRecordCancelZone -> StatusRed
                                                isInlineRecordingAudio -> Color(0xFFEF4444)
                                                isActionButtonCameraMode -> Color(0xFF10B981)
                                                else -> GoldPrimary
                                            }
                                        )
                                        .pointerInput(isActionButtonCameraMode, isImeOpen) {
                                            if (isImeOpen) {
                                                detectTapGestures(
                                                    onTap = {
                                                        isActionButtonCameraMode = !isActionButtonCameraMode
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                )
                                            } else {
                                                awaitEachGesture {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    val touchSlop = viewConfiguration.touchSlop
                                                    var totalDx = 0f
                                                    var totalDy = 0f
                                                    var isDragging = false
                                                    var recordingStarted = false
                                                    var recStartMs = 0L
                                                    var cameraTriggered = false

                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id }
                                                            ?: event.changes.firstOrNull()
                                                            ?: break

                                                        if (!change.pressed) {
                                                            // Pointer released
                                                            if (recordingStarted) {
                                                                isInlineRecordingAudio = false
                                                                if (isInlineRecordCancelZone) {
                                                                    AudioRecorderManager.cancelRecording()
                                                                    isInlineRecordCancelZone = false
                                                                } else {
                                                                    val recorded = AudioRecorderManager.stopRecording()
                                                                    val heldMs = System.currentTimeMillis() - recStartMs
                                                                    if (recorded != null && recorded.exists() && heldMs >= 320L) {
                                                                        voiceDraftPath = recorded.absolutePath
                                                                        voiceDraftDurationSec = (heldMs / 1000L).toInt().coerceAtLeast(1)
                                                                        draftAudioProgress = 0f
                                                                        isDraftAudioPlaying = false
                                                                    } else if (recorded != null && recorded.exists()) {
                                                                        try { recorded.delete() } catch (_: Exception) {}
                                                                    }
                                                                }
                                                            } else if (!isDragging && !cameraTriggered) {
                                                                // Clean Tap -> Toggle Mic <-> Camera
                                                                isActionButtonCameraMode = !isActionButtonCameraMode
                                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                            break
                                                        }

                                                        val delta = change.position - change.previousPosition
                                                        totalDx += delta.x
                                                        totalDy += delta.y

                                                        if (!isDragging && kotlin.math.hypot(totalDx.toDouble(), totalDy.toDouble()) > touchSlop) {
                                                            isDragging = true
                                                            if (!isActionButtonCameraMode) {
                                                                val hasMicPerm = ContextCompat.checkSelfPermission(
                                                                    context,
                                                                    Manifest.permission.RECORD_AUDIO
                                                                ) == PackageManager.PERMISSION_GRANTED
                                                                if (!hasMicPerm) {
                                                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                                    break
                                                                }
                                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                val file = AudioRecorderManager.startRecording(context)
                                                                if (file != null) {
                                                                    recordingStarted = true
                                                                    recStartMs = System.currentTimeMillis()
                                                                    inlineRecordSeconds = 0
                                                                    isInlineRecordCancelZone = false
                                                                    isInlineRecordingAudio = true
                                                                }
                                                            }
                                                        }

                                                        if (isDragging) {
                                                            change.consume()
                                                            if (!isActionButtonCameraMode && recordingStarted) {
                                                                val cancelNow = kotlin.math.abs(totalDx) > cancelThresholdPx
                                                                if (cancelNow != isInlineRecordCancelZone) {
                                                                    isInlineRecordCancelZone = cancelNow
                                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                }
                                                            } else if (isActionButtonCameraMode && !cameraTriggered && totalDy > swipeDownCameraThresholdPx) {
                                                                cameraTriggered = true
                                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                launchFrontVideoNote()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("chat_mic_camera_toggle_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isInlineRecordingAudio && isInlineRecordCancelZone -> Icons.Default.Close
                                            isInlineRecordingAudio -> Icons.Default.Mic
                                            isActionButtonCameraMode -> Icons.Default.CameraAlt
                                            else -> Icons.Default.Mic
                                        },
                                        contentDescription = if (isActionButtonCameraMode) "كاميرا" else "تسجيل صوتي",
                                        tint = adaptiveOnPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Attachment & Tools Circular Icon-Only Modal Bottom Sheet (+)
    if (showAttachmentActionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentActionsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: Media, Files & Deals (Circular Icon-Only Buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Document / File
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            try {
                                documentPickerLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                genericFilePickerLauncher.launch("*/*")
                            }
                        },
                        shape = CircleShape,
                        color = Color(0xFF3B82F6).copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, Color(0xFF3B82F6).copy(alpha = 0.45f)),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "مستند أو ملف",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Add Photo (opens 2-option Camera / Gallery chooser card ONLY)
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            activeMediaChooserKind = MediaChooserKind.PHOTO
                        },
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .size(58.dp)
                            .testTag("chat_attach_photo_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "إضافة صورة",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Add Video (opens 2-option Camera / Gallery chooser card ONLY)
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            activeMediaChooserKind = MediaChooserKind.VIDEO
                        },
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .size(58.dp)
                            .testTag("chat_attach_video_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "إضافة فيديو",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Gold Deal
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            onOpenGoldDeal()
                        },
                        shape = CircleShape,
                        color = GoldPrimary.copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = "صفقة ذهب",
                                tint = GoldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Row 2: Voice & Smart Tools (Moved from above and beside +)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Business Agent
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            onOpenVoiceBusinessAgent()
                        },
                        shape = CircleShape,
                        color = OozoxTeal.copy(alpha = 0.18f),
                        border = BorderStroke(1.2.dp, OozoxTeal.copy(alpha = 0.55f)),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = "الوكيل الصوتي لإدارة الأعمال",
                                tint = OozoxTeal,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Smart Voice Accounting
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            showVoiceAccountingDialog = true
                        },
                        shape = CircleShape,
                        color = GoldPrimary.copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "قيد محاسبي صوتي",
                                tint = GoldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Voice Invoice Dictation
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            activeDictationMode = VoiceDictationMode.INVOICE
                            showVoiceDictationDialog = true
                        },
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.45f)),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "إملاء فاتورة",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Voice Notes / Free Dictation
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            activeDictationMode = VoiceDictationMode.PROJECT_NOTES
                            showVoiceDictationDialog = true
                        },
                        shape = CircleShape,
                        color = Color(0xFF8B5CF6).copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f)),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "إملاء ملاحظات",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        showAttachmentActionsSheet = false
                        quickHashtagInitialTag = null
                        showQuickHashtagSheet = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = GoldPrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("chat_quick_hashtag_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("#", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = GoldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إدراج هاشتاج أو فاتورة مالية", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
                    }
                }

                Surface(
                    onClick = {
                        showAttachmentActionsSheet = false
                        showCreateTaskInChatDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("chat_plus_assign_task_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentInd,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إسناد مهمة", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF10B981))
                    }
                }

                if (chat.isGroup) {
                    Surface(
                        onClick = {
                            showAttachmentActionsSheet = false
                            groupMembersInitialTab = 0
                            showGroupMembersDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.14f),
                        border = BorderStroke(1.2.dp, Color(0xFF3B82F6).copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("chat_plus_add_member_btn")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إضافة أعضاء للمجموعة", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF3B82F6))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Ledger Bottom Sheet
    if (showLedgerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLedgerSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "دفتر حسابات المحادثة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    Button(
                        onClick = { showNewAccountDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary)
                    ) {
                        Text("حساب جديد +", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (accounts.isEmpty()) {
                    Text(
                        text = "لا توجد حسابات مسجلة في هذه المحادثة بعد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            val accHashtags = hashtags.filter { it.accountId == acc.id }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(acc.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(acc.status, fontSize = 12.sp, color = GoldAccent)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    accHashtags.forEach { h ->
                                        val hEntries = allEntries.filter { it.hashtagId == h.id }
                                        val totals = HashtagUtils.calculateHashtagTotals(hEntries)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("#${h.tag}", color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "المتبقي: ${GoldUtils.formatCurrency(totals.remaining)}",
                                                color = if (totals.remaining > 0) StatusOrange else StatusGreen,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val firstHash = accHashtags.firstOrNull()
                                                if (firstHash != null) {
                                                    selectedHashtagIdForEntry = firstHash.id
                                                    selectedAccountIdForEntry = acc.id
                                                    showNewEntryDialog = true
                                                } else {
                                                    onCreateHashtagAccount(chat.id, acc.id, "عام")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = adaptiveGoldPrimary,
                                                contentColor = adaptiveOnPrimary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("+ فاتورة / دفع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Account Dialog
    if (showNewAccountDialog) {
        var accName by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = { showNewAccountDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إضافة حساب مالي جديد",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "إنشاء محفظة حسابات مستقلة داخل المحادثة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text("اسم الحساب (مثال: توريدات ذهب 2026)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            focusedLabelColor = GoldPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showNewAccountDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                if (accName.isNotBlank()) {
                                    onCreateAccount(accName.trim())
                                    showNewAccountDialog = false
                                }
                            },
                            enabled = accName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("إنشاء الحساب", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Create Entry Dialog
    if (showNewEntryDialog && selectedHashtagIdForEntry != null && selectedAccountIdForEntry != null) {
        var desc by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var entryType by remember { mutableStateOf(HashtagEntryType.INVOICE) }

        Dialog(
            onDismissRequest = { showNewEntryDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إضافة فاتورة / دفعة مالية",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تسجيل قيد مالي مباشر بالدفتر",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = entryType == HashtagEntryType.INVOICE,
                            onClick = { entryType = HashtagEntryType.INVOICE },
                            label = { Text("فاتورة (مدين)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = entryType == HashtagEntryType.PAYMENT,
                            onClick = { entryType = HashtagEntryType.PAYMENT },
                            label = { Text("دفعة (دائن)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("الوصف / بيان المعاملة") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            focusedLabelColor = GoldPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("المبلغ (بالجنيه المصري)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            focusedLabelColor = GoldPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showNewEntryDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                val amt = amountStr.toDoubleOrNull()
                                if (desc.isNotBlank() && amt != null && amt > 0) {
                                    val dir = if (entryType == HashtagEntryType.INVOICE) HashtagEntryDirection.DEBIT else HashtagEntryDirection.CREDIT
                                    onAddEntry(selectedHashtagIdForEntry!!, selectedAccountIdForEntry!!, entryType, desc.trim(), amt, dir)
                                    showNewEntryDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("حفظ المعاملة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showClearingDialog) {
        com.example.ui.components.ClearingHouseDialog(
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
            isTrader = chat.contactType == UserType.GOLD_TRADER.name,
            onDismiss = { activeCallType = null },
            onSendMessageNotice = { notice, type ->
                onSendMessage(notice, type)
            }
        )
    }

    // Call Privacy Blocked Dialog
    if (callPrivacyBlockedReason != null) {
        AlertDialog(
            onDismissRequest = { callPrivacyBlockedReason = null },
            icon = {
                Icon(Icons.Default.PhoneDisabled, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
            },
            title = {
                Text("خصوصية المكالمات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = callPrivacyBlockedReason!!,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { callPrivacyBlockedReason = null },
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary)
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ZOX Call Blocked Info Dialog
    if (showZoxCallBlockedDialog) {
        Dialog(
            onDismissRequest = { showZoxCallBlockedDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEF4444).copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneDisabled,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "المكالمات الواردة محظورة لحساب ZOX",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "حساب الإدارة العليا والدعم الفني (ZOX) مخصص للمراسلات النصية المشفرة وتوثيق السجلات والقيود المالية فقط لضمان سرية وحفظ العمليات والردود الرسمية المعتمدة.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "يتم الرد الفوري على استفساراتك وتوجيه طلباتك عبر الدردشة النصية المباشرة.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showZoxCallBlockedDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("إغلاق", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showZoxCallBlockedDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("مراسلة نصية", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Camera Permission Rationale Dialog
    if (showCameraPermissionRationale) {
        Dialog(
            onDismissRequest = { showCameraPermissionRationale = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "إذن استخدام الكاميرا",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "يتطلب التقاط صور الفواتير والذهب ومرفقات المحادثة إذن الوصول إلى كاميرا الهاتف. يرجى منح الإذن للمتابعة.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCameraPermissionRationale = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                showCameraPermissionRationale = false
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("منح الإذن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Microphone Permission Rationale Dialog
    if (showMicPermissionRationale) {
        Dialog(
            onDismissRequest = { showMicPermissionRationale = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "إذن استخدام الميكروفون ️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "يتطلب تسجيل وإرسال الرسائل والملاحظات الصوتية المباشرة في المحادثة إذن الوصول إلى الميكروفون. يرجى منح الإذن للمتابعة.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showMicPermissionRationale = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                showMicPermissionRationale = false
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("منح الإذن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Voice-to-Text Smart Dictation Dialog (Invoice / Project Notes / Freeform)
    if (showVoiceDictationDialog) {
        VoiceDictationAssistantDialog(
            initialMode = activeDictationMode,
            contactName = chat?.contactName ?: "",
            onDismiss = { showVoiceDictationDialog = false },
            onInsertText = { formatted ->
                textInput = if (textInput.isNotBlank()) "${textInput.trim()}\n\n$formatted" else formatted
            },
            onDirectSend = { content, msgType ->
                onSendMessage(content, msgType)
            }
        )
    }

    // Smart Voice Accounting & Pre-Commit Audit Dialog
    if (showVoiceAccountingDialog) {
        com.example.ui.components.VoiceAccountingDialog(
            contactName = chat?.contactName ?: "",
            existingHashtags = hashtags.map { it.tag.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct(),
            onDismiss = { showVoiceAccountingDialog = false },
            onInsertEntry = { record, report ->
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

    // Minimal 2-Option Media Source Chooser (Camera / Gallery ONLY)
    activeMediaChooserKind?.let { chooserKind ->
        MediaSourceChooserCard(
            kind = chooserKind,
            onCameraClick = {
                activeMediaChooserKind = null
                if (chooserKind == MediaChooserKind.PHOTO) {
                    onCameraTrigger()
                } else {
                    onVideoCameraTrigger()
                }
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

    // Optional Zoom-to-Frame & Rotate Editor for Attached Image
    pendingImageFrameUri?.let { editUri ->
        CoverPhotoEditorDialog(
            imageUri = editUri,
            frameMode = ImageFrameAspectMode.PRODUCT_PHOTO,
            title = "تعديل الصورة",
            onSaveCroppedCover = { savedPath ->
                capturedImageUri = Uri.fromFile(java.io.File(savedPath))
                pendingImageFrameUri = null
            },
            onDismiss = { pendingImageFrameUri = null }
        )
    }

    // Pro Video Editor (Trim / Mute / Caption)
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

    // Full Screen Photo & Video Preview Modal
    if (activeMediaPreview != null) {
        MediaPreviewModal(
            item = activeMediaPreview!!,
            onDismiss = { activeMediaPreview = null },
            onForward = {
                val preview = activeMediaPreview
                if (preview != null && chat != null) {
                    val foundMsg = messages.find { it.content.contains(preview.uri) }
                    if (foundMsg != null) {
                        selectedMessageForForward = foundMsg
                    } else {
                        selectedMessageForForward = MessageEntity(
                            id = UUID.randomUUID().toString(),
                            chatId = chat.id,
                            senderId = currentUser?.id ?: "me",
                            senderName = currentUser?.name ?: "أنا",
                            type = MessageType.IMAGE.name,
                            content = preview.uri + if (preview.caption.isNotBlank()) "||${preview.caption}" else "",
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
                activeMediaPreview = null
            }
        )
    }

    // Unified Compact Message Options & Reaction Dialog
    if (selectedMessageForOptions != null || selectedMessageForReaction != null) {
        val targetMsg = selectedMessageForOptions ?: selectedMessageForReaction!!
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val emojis = listOf("معتمد", "تم الاطلاع", "موافق", "قيد المراجعة")

        Dialog(
            onDismissRequest = {
                selectedMessageForOptions = null
                selectedMessageForReaction = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "خيارات الرسالة والتفاعل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                selectedMessageForOptions = null
                                selectedMessageForReaction = null
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Quick Reaction Emoji Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            emojis.forEach { emoji ->
                                val isSelected = targetMsg.reaction == emoji
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) GoldPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) GoldPrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onToggleReaction(targetMsg.id, targetMsg.reaction, emoji)
                                            selectedMessageForOptions = null
                                            selectedMessageForReaction = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Message preview snippet
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (targetMsg.type == MessageType.VOICE.name) "تسجيل صوتي"
                            else if (targetMsg.type == MessageType.IMAGE.name) "صورة مرفقة"
                            else targetMsg.content.take(80),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action Row (3 compact equal pills - NEVER OVERLAPPING)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reply Button
                        Surface(
                            color = adaptiveGoldPrimary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    val msg = targetMsg
                                    selectedMessageForOptions = null
                                    selectedMessageForReaction = null
                                    replyingToMessage = msg
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = adaptiveOnPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رد ↩️", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Forward Button
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    val msg = targetMsg
                                    selectedMessageForOptions = null
                                    selectedMessageForReaction = null
                                    selectedMessageForForward = msg
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("توجيه ↗️", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Copy Button
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(targetMsg.content))
                                    forwardSuccessMessage = "تم نسخ النص"
                                    selectedMessageForOptions = null
                                    selectedMessageForReaction = null
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    // Contextual Translation Action
                    if (targetMsg.type == MessageType.TEXT.name && targetMsg.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clickable {
                                    val msg = targetMsg
                                    selectedMessageForOptions = null
                                    selectedMessageForReaction = null
                                    onTranslateMessage(msg)
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ترجمة سياقية ذكية (Gemini AI)",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (!targetMsg.reaction.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                onToggleReaction(targetMsg.id, targetMsg.reaction, targetMsg.reaction)
                                selectedMessageForOptions = null
                                selectedMessageForReaction = null
                            },
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        ) {
                            Text("إزالة التفاعل (${targetMsg.reaction})", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Chat Settings Dialog (إعدادات وتخصيص ألوان المحادثة مع المعاينة المباشرة)
    if (showChatSettingsDialog && chat != null) {
        var draftChatBgStyle by remember(showChatSettingsDialog) { mutableStateOf(customChatBg) }
        var draftChatBgColor by remember(showChatSettingsDialog) { mutableStateOf(customChatBgColor) }

        var draftBubbleStyle by remember(showChatSettingsDialog) { mutableStateOf(customBubbleStyle) }
        var draftBubbleColor by remember(showChatSettingsDialog) { mutableStateOf(customBubbleColor) }

        var draftFontColorStyle by remember(showChatSettingsDialog) { mutableStateOf(customFontColorStyle) }
        var draftFontColor by remember(showChatSettingsDialog) { mutableStateOf(customFontColor) }

        var draftTranslationFontSize by remember(showChatSettingsDialog) { mutableStateOf(translationFontSize) }
        var draftTranslationColorStyle by remember(showChatSettingsDialog) { mutableStateOf(translationColorStyle) }

        var colorPickerTarget by remember { mutableStateOf<String?>(null) } // "bg", "bubble", "font"
        var customHexInput by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { showChatSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 540.dp)
                        .heightIn(max = 680.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Fixed Title Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تخصيص ألوان المحادثة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        draftChatBgStyle = "default"
                                        draftChatBgColor = null
                                        draftBubbleStyle = "auto"
                                        draftBubbleColor = null
                                        draftFontColorStyle = "auto"
                                        draftFontColor = null
                                        draftTranslationFontSize = "medium"
                                        draftTranslationColorStyle = "gold"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .testTag("chat_theme_reset_btn")
                                ) {
                                    Text("إعادة ضبط", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = GoldPrimary)
                                }
                                IconButton(
                                    onClick = { showChatSettingsDialog = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        HorizontalDivider()

                        // Fixed Live Preview Box (معاينة حية ثابتة أعلى الخيارات)
                    val effectivePreviewBg = when {
                        draftChatBgColor != null -> draftChatBgColor!!
                        draftChatBgStyle == "gold" -> Color(0xFF1E1B10)
                        draftChatBgStyle == "emerald" -> Color(0xFF062C22)
                        draftChatBgStyle == "navy" -> Color(0xFF0F172A)
                        draftChatBgStyle == "light" -> Color(0xFFF1F5F9)
                        else -> Color(0xFF0B0F19)
                    }
                    val effectivePreviewBubble = when {
                        draftBubbleColor != null -> draftBubbleColor!!
                        draftBubbleStyle == "gold_dark" -> GoldDark
                        draftBubbleStyle == "slate" -> Color(0xFF1E293B)
                        draftBubbleStyle == "emerald_glass" -> Color(0xFF064E3B)
                        draftBubbleStyle == "violet" -> Color(0xFF2E1065)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                    val effectivePreviewFont = when {
                        draftFontColor != null -> draftFontColor!!
                        draftFontColorStyle == "gold" -> GoldLight
                        draftFontColorStyle == "white" -> Color.White
                        draftFontColorStyle == "emerald" -> Color(0xFF10B981)
                        draftFontColorStyle == "platinum" -> Color(0xFFE2E8F0)
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_theme_live_preview"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(effectivePreviewBg)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "معاينة شاشة المحادثة المباشرة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = effectivePreviewFont.copy(alpha = 0.9f)
                                )
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "تحديث فوري",
                                        fontSize = 10.sp,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Sample Incoming Message
                            Surface(
                                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 2.dp),
                                color = effectivePreviewBubble,
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text(
                                    text = "مرحباً بك! هذه معاينة مباشرة لألوان خلفية الشاشة، الرسائل، ولون النص",
                                    color = effectivePreviewFont,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            // Live Translation Subtitle Preview
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1B2028) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .padding(start = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(7.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ترجمة سياقية (معاينة التباين)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "مرحباً! هذه معاينة حية لخط ولون الترجمة المتباين والمريح للعين.",
                                        color = com.example.util.TranslationStyleHelper.resolveColor(draftTranslationColorStyle, MaterialTheme.colorScheme.background.luminance() < 0.5f),
                                        fontSize = com.example.util.TranslationStyleHelper.resolveFontSize(draftTranslationFontSize).sp,
                                        lineHeight = com.example.util.TranslationStyleHelper.resolveLineHeight(draftTranslationFontSize).sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Sample Outgoing Message
                            Surface(
                                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 2.dp, bottomStart = 14.dp),
                                color = effectivePreviewBubble.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .align(Alignment.End)
                            ) {
                                Text(
                                    text = "ممتاز، التناسق والوضوح رائع جداً! اضغط 'حفظ' لتطبيق التعديلات",
                                    color = effectivePreviewFont,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Scrollable Middle Design Options
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .testTag("chat_theme_options_scroll"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                    //  SECTION 1: خلفية الشاشة (Screen Background Color Picker)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. خلفية المحادثة ️", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
                            OutlinedButton(
                                onClick = { colorPickerTarget = "bg" },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                            ) {
                                Text("+ لون خاص", fontSize = 11.sp, color = GoldPrimary)
                            }
                        }

                        // Preset chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "default" to "الافتراضي",
                                "gold" to "ذهبي",
                                "emerald" to "زمردي",
                                "navy" to "أزرق ليلي",
                                "light" to "️ فاتح"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = draftChatBgStyle == key && draftChatBgColor == null,
                                    onClick = {
                                        draftChatBgStyle = key
                                        draftChatBgColor = null
                                    },
                                    label = { Text(label, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = adaptiveGoldPrimary,
                                        selectedLabelColor = adaptiveOnPrimary
                                    )
                                )
                            }
                        }

                        // Background Color Pickers Grid (Color Circles)
                        Text("أو اختر لونك المفضل مباشرة (Color Picker):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Color(0xFF0B0F19),
                                Color(0xFF1E1B10),
                                Color(0xFF062C22),
                                Color(0xFF0F172A),
                                Color(0xFF18181B),
                                Color(0xFF2E1065),
                                Color(0xFF4C0519),
                                Color(0xFF3B1254),
                                Color(0xFF064E3B),
                                Color(0xFF0F766E),
                                Color(0xFF451A03),
                                Color(0xFFF1F5F9)
                            ).forEach { colorSwatch ->
                                val isSelected = draftChatBgColor == colorSwatch
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colorSwatch)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            draftChatBgColor = colorSwatch
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (colorSwatch.luminance() > 0.5f) Color.Black else GoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    //  SECTION 2: لون الرسائل (Message Bubble Color Picker)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. لون فقاعات الرسائل", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
                            OutlinedButton(
                                onClick = { colorPickerTarget = "bubble" },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                            ) {
                                Text("+ لون خاص", fontSize = 11.sp, color = GoldPrimary)
                            }
                        }

                        // Preset chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "auto" to "تلقائي",
                                "gold_dark" to "ذهبي داكن",
                                "slate" to "رمادي داكن",
                                "emerald_glass" to "زمردي",
                                "violet" to "بنفسجي"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = draftBubbleStyle == key && draftBubbleColor == null,
                                    onClick = {
                                        draftBubbleStyle = key
                                        draftBubbleColor = null
                                    },
                                    label = { Text(label, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = adaptiveGoldPrimary,
                                        selectedLabelColor = adaptiveOnPrimary
                                    )
                                )
                            }
                        }

                        // Bubble Color Swatches
                        Text("أو اختر لون فقاعات الرسائل:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                GoldDark,
                                Color(0xFF1E293B),
                                Color(0xFF064E3B),
                                Color(0xFF2E1065),
                                Color(0xFF831843),
                                Color(0xFF1E3A8A),
                                Color(0xFF78350F),
                                Color(0xFF334155),
                                Color(0xFF0F766E),
                                Color(0xFF4C0519),
                                Color(0xFF1C1917),
                                Color(0xFFF3F4F6)
                            ).forEach { colorSwatch ->
                                val isSelected = draftBubbleColor == colorSwatch
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colorSwatch)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            draftBubbleColor = colorSwatch
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (colorSwatch.luminance() > 0.5f) Color.Black else GoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // ️ SECTION 3: لون الخط (Font Text Color Picker)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("3. لون خط النصوص ️", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
                            OutlinedButton(
                                onClick = { colorPickerTarget = "font" },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                            ) {
                                Text("+ لون خاص", fontSize = 11.sp, color = GoldPrimary)
                            }
                        }

                        // Preset chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "auto" to "تلقائي",
                                "gold" to "ذهبي",
                                "white" to "أبيض",
                                "emerald" to "أخضر",
                                "platinum" to "️ بلاتيني"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = draftFontColorStyle == key && draftFontColor == null,
                                    onClick = {
                                        draftFontColorStyle = key
                                        draftFontColor = null
                                    },
                                    label = { Text(label, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = adaptiveGoldPrimary,
                                        selectedLabelColor = adaptiveOnPrimary
                                    )
                                )
                            }
                        }

                        // Font Color Swatches
                        Text("أو اختر لون خط القراءة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                GoldLight,
                                Color.White,
                                Color(0xFF10B981),
                                Color(0xFFE2E8F0),
                                Color(0xFF38BDF8),
                                Color(0xFFF43F5E),
                                Color(0xFFF59E0B),
                                Color(0xFFA855F7),
                                Color(0xFF84CC16),
                                Color(0xFFFACC15),
                                Color(0xFF000000)
                            ).forEach { colorSwatch ->
                                val isSelected = draftFontColor == colorSwatch
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colorSwatch)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            draftFontColor = colorSwatch
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (colorSwatch.luminance() > 0.5f) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    //  SECTION 4: حظر المستخدم
                    Surface(
                        color = if (isUserBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isUserBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حظر هذا المستخدم",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isUserBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isUserBlocked) "المستخدم محظور حالياً (لا تظهر له أي علامة حظر لحفظ اللباقة)" else "حظر سري: لن تظهر له أي علامة حظر، وتُحجب تحديثات ملفك وصورتك عنه تلقائياً",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isUserBlocked,
                                onCheckedChange = { blocked ->
                                    isUserBlocked = blocked
                                    val targetId = chat?.contactId
                                    if (targetId != null) {
                                        if (blocked) {
                                            BlockManager.blockUser(context, targetId)
                                            android.widget.Toast.makeText(context, "تم تفعيل الحظر وحجب بياناتك وتحديثاتك", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            BlockManager.unblockUser(context, targetId)
                                            android.widget.Toast.makeText(context, "تم إلغاء الحظر", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }

                    // 🌟 SECTION: تخصيص خط ولون الترجمة السياقية AI (Translation Typography & Contrast)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تخصيص خط ولون الترجمة 🌐", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "تباين عالي وراحة للعين",
                                    fontSize = 10.sp,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Colors row
                        Text("لون خط الترجمة:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.example.util.TranslationStyleHelper.COLOR_OPTIONS.forEach { opt ->
                                val isSelected = draftTranslationColorStyle == opt.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable { draftTranslationColorStyle = opt.id }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(opt.previewColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = opt.titleAr,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Sizes row
                        Text("حجم خط الترجمة:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.example.util.TranslationStyleHelper.SIZE_OPTIONS.forEach { sz ->
                                val isSelected = draftTranslationFontSize == sz.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable { draftTranslationFontSize = sz.id }
                                ) {
                                    Text(
                                        text = sz.titleAr,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Message Notification Sound Section
                        val chatToneId = remember(showNotificationTonePickerInChat, showChatSettingsDialog) {
                            com.example.util.MessageNotificationSoundHelper.getSelectedTone(context)
                        }
                        val chatToneObj = remember(chatToneId) {
                            com.example.util.MessageNotificationSoundHelper.getToneById(chatToneId)
                        }
                        val isChatSoundOn = remember(showNotificationTonePickerInChat, showChatSettingsDialog) {
                            com.example.util.MessageNotificationSoundHelper.isSoundEnabled(context)
                        }

                        Text("نغمة تنبيه الرسائل الجديدة:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNotificationTonePickerInChat = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(chatToneObj.icon, fontSize = 18.sp)
                                    Column {
                                        Text(
                                            text = if (isChatSoundOn) chatToneObj.nameAr else "صامت (التنبيه الصوتي معطل)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChatSoundOn) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "انقر للتغيير أو الاستماع للنغمات المدمجة",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoldPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "تغيير 🎵",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    } // End Scrollable Middle Options Column

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Fixed Bottom Cancel & Save Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChatSettingsDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("chat_theme_cancel_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إلغاء", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                customChatBg = draftChatBgStyle
                                customChatBgColor = draftChatBgColor
                                customBubbleStyle = draftBubbleStyle
                                customBubbleColor = draftBubbleColor
                                customFontColorStyle = draftFontColorStyle
                                customFontColor = draftFontColor
                                onUpdateTranslationFontSize(draftTranslationFontSize)
                                onUpdateTranslationColorStyle(draftTranslationColorStyle)
                                showChatSettingsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("chat_theme_save_btn")
                        ) {
                            Text("حفظ", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                }
            }
        }

        // Custom Message Notification Tone Picker Dialog
        if (showNotificationTonePickerInChat) {
            com.example.ui.components.MessageNotificationToneDialog(
                onDismiss = { showNotificationTonePickerInChat = false }
            )
        }

        // Custom Color Picker Modal (Color Grid + Hex Input)
        if (colorPickerTarget != null) {
            val targetTitle = when (colorPickerTarget) {
                "bg" -> "خلفية الشاشة"
                "bubble" -> "فقاعات الرسائل"
                else -> "لون الخط"
            }
            Dialog(
                onDismissRequest = { colorPickerTarget = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اختيار لون خاص لـ ($targetTitle)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { colorPickerTarget = null },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }

                        Text("اختر لوناً من شبكة الألوان الخاصة:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        val extendedColors = listOf(
                            Color(0xFFFFD700), Color(0xFFD4AF37), Color(0xFF10B981), Color(0xFF0F766E),
                            Color(0xFF38BDF8), Color(0xFF1E3A8A), Color(0xFF8B5CF6), Color(0xFF4C1D95),
                            Color(0xFFEC4899), Color(0xFFEF4444), Color(0xFFFFFFFF), Color(0xFFE2E8F0),
                            Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF18181B), Color(0xFF000000)
                        )

                        // 4x4 Grid of colors
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            extendedColors.chunked(4).forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowColors.forEach { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape)
                                                .clickable {
                                                    when (colorPickerTarget) {
                                                        "bg" -> draftChatBgColor = c
                                                        "bubble" -> draftBubbleColor = c
                                                        "font" -> draftFontColor = c
                                                    }
                                                    colorPickerTarget = null
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Text("أو أدخل كود اللون (Hex Code):", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customHexInput,
                                onValueChange = { customHexInput = it },
                                placeholder = { Text("#FFD700", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Button(
                                onClick = {
                                    val parsed = parseHexColor(customHexInput)
                                    if (parsed != null) {
                                        when (colorPickerTarget) {
                                            "bg" -> draftChatBgColor = parsed
                                            "bubble" -> draftBubbleColor = parsed
                                            "font" -> draftFontColor = parsed
                                        }
                                        colorPickerTarget = null
                                        customHexInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("تطبيق", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Contact Profile Modal (بطاقة بيانات وخيارات الطرف الآخر)
    if (showContactProfileModal && chat != null) {
        val canViewPublicProfile = !chat.isGroup && (contactUser?.showSkillsPublicly != false)
        val hasDirectPhone = !chat.isGroup && !contactUser?.phone.isNullOrBlank()
        Dialog(
            onDismissRequest = { showContactProfileModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header row: Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (chat.isGroup) "معلومات المجموعة" else "بيانات جهة الاتصال",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { showContactProfileModal = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Avatar & Identity
                    OozoxAvatarImage(
                        model = contactUser?.avatarUri,
                        name = chat.contactName,
                        size = 76.dp,
                        isGroup = chat.isGroup,
                        isTrader = chat.contactType == UserType.GOLD_TRADER.name,
                        contentDescription = chat.contactName
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = chat.contactName,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (chat.contactVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "حساب موثق",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        val subtitleText = when {
                            chat.isGroup -> "مجموعة عمل"
                            !contactUser?.jobTitle.isNullOrBlank() -> contactUser?.jobTitle!!
                            chat.contactType == UserType.GOLD_TRADER.name -> "تاجر ذهب ومعادن"
                            else -> "حساب أعمال"
                        }
                        Text(
                            text = subtitleText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoldPrimary
                        )

                        if (!chat.isGroup && (!contactUser?.city.isNullOrBlank() || (contactUser?.totalReviewsCount ?: 0) > 0)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!contactUser?.city.isNullOrBlank()) {
                                    Text(
                                        text = contactUser?.city!!,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if ((contactUser?.totalReviewsCount ?: 0) > 0) {
                                    Text(
                                        text = "★ ${String.format(java.util.Locale.US, "%.1f", contactUser?.ratingScore ?: 0f)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Call Options Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "خيارات الاتصال",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Audio Call
                            Surface(
                                onClick = {
                                    showContactProfileModal = false
                                    activeCallType = CallType.AUDIO
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = "مكالمة صوتية",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "صوتية",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }

                            // Video Call
                            Surface(
                                onClick = {
                                    showContactProfileModal = false
                                    activeCallType = CallType.VIDEO
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = "مكالمة فيديو",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "فيديو",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Direct Phone Call (when phone is available)
                            if (hasDirectPhone) {
                                Surface(
                                    onClick = {
                                        showContactProfileModal = false
                                        try {
                                            val dialIntent = android.content.Intent(
                                                android.content.Intent.ACTION_DIAL,
                                                android.net.Uri.parse("tel:${contactUser?.phone}")
                                            )
                                            context.startActivity(dialIntent)
                                        } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PhoneForwarded,
                                            contentDescription = "اتصال هاتفي",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "هاتفي",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Public Profile Button (only when user allows showing it)
                    if (canViewPublicProfile) {
                        Button(
                            onClick = {
                                showContactProfileModal = false
                                onNavigateToProfile(chat.contactId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الصفحة الشخصية",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Chat Settings Button
                    OutlinedButton(
                        onClick = {
                            showContactProfileModal = false
                            showChatSettingsDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعدادات المحادثة والحظر",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Forward Message Dialog
    if (selectedMessageForForward != null && chat != null) {
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

    if (showMessengerOptionsSheet && chat != null) {
        val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val canViewPublicProfile = !chat.isGroup && (contactUser?.showSkillsPublicly != false)
        ModalBottomSheet(
            onDismissRequest = { showMessengerOptionsSheet = false },
            sheetState = optionsSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(modifier = Modifier.size(68.dp)) {
                    OozoxAvatarImage(
                        model = contactUser?.avatarUri,
                        name = chat.contactName,
                        size = 68.dp,
                        isGroup = chat.isGroup,
                        isTrader = chat.contactType == UserType.GOLD_TRADER.name,
                        contentDescription = chat.contactName
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chat.contactName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chat.contactVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "موثق",
                                tint = GoldPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Text(
                        text = when {
                            isZoxAdminChat -> "الإدارة الرسمية"
                            chat.isGroup -> "مجموعة عمل نشطة"
                            !contactUser?.jobTitle.isNullOrBlank() -> contactUser?.jobTitle!!
                            chat.contactType == UserType.GOLD_TRADER.name -> "تاجر ذهب ومعادن معتمد"
                            else -> "حساب أعمال"
                        },
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!chat.isGroup && (!contactUser?.city.isNullOrBlank() || (contactUser?.totalReviewsCount ?: 0) > 0)) {
                        val detailsLine = buildList {
                            contactUser?.city?.takeIf { it.isNotBlank() }?.let { add(it) }
                            if ((contactUser?.totalReviewsCount ?: 0) > 0) {
                                add("تقييم ${String.format(java.util.Locale.US, "%.1f", contactUser?.ratingScore ?: 0f)}")
                            }
                        }.joinToString(" • ")
                        if (detailsLine.isNotBlank()) {
                            Text(
                                text = detailsLine,
                                fontSize = 11.5.sp,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 4 Circular Messenger Quick Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MessengerQuickCircleAction(
                        icon = if (isZoxAdminChat) Icons.Default.PhoneDisabled else Icons.Default.Call,
                        label = "اتصال",
                        enabled = !isZoxAdminChat,
                        onClick = {
                            showMessengerOptionsSheet = false
                            val (allowed, reason) = CallPrivacyManager.checkCallAllowed(
                                context = context,
                                isContactSaved = !chat.isGroup,
                                callerName = chat.contactName
                            )
                            if (!allowed) {
                                callPrivacyBlockedReason = reason
                            } else if (isZoxAdminChat) {
                                showZoxCallBlockedDialog = true
                            } else {
                                activeCallType = CallType.AUDIO
                            }
                        }
                    )
                    MessengerQuickCircleAction(
                        icon = if (isZoxAdminChat) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        label = "فيديو",
                        enabled = !isZoxAdminChat,
                        onClick = {
                            showMessengerOptionsSheet = false
                            val (allowed, reason) = CallPrivacyManager.checkCallAllowed(
                                context = context,
                                isContactSaved = !chat.isGroup,
                                callerName = chat.contactName
                            )
                            if (!allowed) {
                                callPrivacyBlockedReason = reason
                            } else if (isZoxAdminChat) {
                                showZoxCallBlockedDialog = true
                            } else {
                                activeCallType = CallType.VIDEO
                            }
                        }
                    )
                    MessengerQuickCircleAction(
                        icon = Icons.Default.Person,
                        label = "الملف",
                        enabled = true,
                        onClick = {
                            showMessengerOptionsSheet = false
                            if (canViewPublicProfile) {
                                onNavigateToProfile(chat.contactId)
                            } else {
                                showContactProfileModal = true
                            }
                        }
                    )
                    MessengerQuickCircleAction(
                        icon = Icons.Default.BubbleChart,
                        label = "فقاعة",
                        enabled = true,
                        onClick = {
                            showMessengerOptionsSheet = false
                            com.example.util.MessengerBubbleManager.setBubbleFeatureEnabled(context, true)
                            com.example.util.MessengerBubbleManager.showBubble(
                                chatId = chat.id,
                                contactName = chat.contactName,
                                contactAvatarUri = contactUser?.avatarUri,
                                contactRole = if (chat.contactType == UserType.GOLD_TRADER.name) "تاجر ذهب معتمد" else "حساب أعمال",
                                messageText = chat.lastMessage ?: "",
                                autoExpand = true,
                                isOnline = true
                            )
                            onBackClick()
                        }
                    )
                }

                if (chat.isGroup) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "إدارة المجموعة والأعضاء",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = GoldPrimary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("messenger_options_group_members_row")
                        ) {
                            MessengerOptionItemRow(
                                icon = Icons.Default.GroupAdd,
                                title = "أعضاء المجموعة وإضافة أعضاء",
                                subtitle = "${activeGroupMembers.size} عضو حالي • إضافة أو إدارة الصلاحيات",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    groupMembersInitialTab = 0
                                    showGroupMembersDialog = true
                                }
                            )
                        }
                    }
                }

                // Group 1: Customization & Translation
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "التخصيص والترجمة",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MessengerOptionItemRow(
                                icon = Icons.Default.Palette,
                                title = "المظهر والألوان",
                                subtitle = "الخلفية، الفقاعات، ولون الخط",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    showChatSettingsDialog = true
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            MessengerOptionItemRow(
                                icon = Icons.Default.Tune,
                                title = "تصنيف المحادثة",
                                subtitle = activeSpatialMode.label,
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    isTopModesRailExpanded = !isTopModesRailExpanded
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            MessengerOptionSwitchRow(
                                icon = Icons.Default.Translate,
                                title = "ترجمة الطرف الآخر",
                                checked = isAutoTranslateEnabled,
                                onCheckedChange = { isAutoTranslateEnabled = it }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            MessengerOptionSwitchRow(
                                icon = Icons.Default.GTranslate,
                                title = "ترجمة رسائلي أيضاً",
                                checked = translateMyMessagesToo,
                                onCheckedChange = { translateMyMessagesToo = it }
                            )
                        }
                    }
                }

                // Group 2: Business & Trade
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "الأعمال والتجارة",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MessengerOptionItemRow(
                                icon = Icons.Default.MonetizationOn,
                                title = "صفقة ذهب مباشرة",
                                subtitle = "إدراج فاتورة أو عيار",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    onOpenGoldDeal()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            MessengerOptionItemRow(
                                icon = Icons.Default.Book,
                                title = "دفتر الحسابات والقيود",
                                subtitle = "${accounts.size} حسابات مالية",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    showLedgerSheet = true
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            MessengerOptionItemRow(
                                icon = Icons.Default.Balance,
                                title = "تقاص وتصفية الحساب",
                                subtitle = "تسوية الأرصدة والموازين",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    showClearingDialog = true
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            MessengerOptionItemRow(
                                icon = Icons.Default.PictureAsPdf,
                                title = "تصدير المحادثة PDF",
                                subtitle = "${if (totalMessagesCount > 0) totalMessagesCount else messages.size} رسالة",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    onExportFullChatPdf?.invoke() ?: com.example.util.ReportExporter.exportChatConversationToPdf(context, chat, messages)
                                }
                            )
                        }
                    }
                }

                // Group 3: Assistance & Privacy
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "المساعدة والخصوصية",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MessengerOptionItemRow(
                                icon = Icons.Default.RecordVoiceOver,
                                title = "الوكيل الصوتي للأعمال",
                                subtitle = "تسجيل وتنفيذ الأوامر",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    onOpenVoiceBusinessAgent()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            MessengerOptionItemRow(
                                icon = Icons.Default.Analytics,
                                title = "المحاسب المالي المتقدم",
                                subtitle = "مراجعة المعاملات والوفر",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    onOpenAIAccountant()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            MessengerOptionItemRow(
                                icon = Icons.Default.Security,
                                title = "إعدادات المحادثة والحظر",
                                subtitle = if (isUserBlocked) "محظور حالياً" else "إدارة الخصوصية والحظر",
                                onClick = {
                                    showMessengerOptionsSheet = false
                                    showContactProfileModal = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Easy Quick #Hashtag & Synchronized Invoice/Payment Modal (Sticky-Top Input + IME-Safe)
    if (showQuickHashtagSheet) {
        val defaultTags = listOf("فاتورة", "توريد", "دفعة", "سداد", "ذهب", "عمل_خارجي")
        val existingTags = remember(hashtags) { hashtags.map { it.tag.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct() }
        val suggestedTags = remember(existingTags, quickHashtagInitialTag) {
            (listOfNotNull(quickHashtagInitialTag?.removePrefix("#")) + existingTags + defaultTags)
                .filter { it.isNotBlank() }
                .distinct()
        }

        var selectedTag by remember(quickHashtagInitialTag) {
            mutableStateOf(quickHashtagInitialTag?.removePrefix("#") ?: suggestedTags.firstOrNull() ?: "فاتورة")
        }
        var customTag by remember { mutableStateOf("") }
        var entryType by remember { mutableStateOf(HashtagEntryType.INVOICE) }
        var description by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var selectedProjectId by remember { mutableStateOf<String?>(null) }
        var paymentMethod by remember { mutableStateOf("كاش") }
        var isExternalBusiness by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = {
                showQuickHashtagSheet = false
                quickHashtagInitialTag = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 640.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // PINNED / STICKY TOP INPUT AREA (Always stays fixed at top above keyboard)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قيد مالي وهاشتاج #${(customTag.ifBlank { selectedTag }).removePrefix("#")}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = GoldPrimary
                                )
                                IconButton(
                                    onClick = {
                                        showQuickHashtagSheet = false
                                        quickHashtagInitialTag = null
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    placeholder = { Text("5000", fontSize = 13.sp) },
                                    label = { Text("المبلغ", fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_hashtag_amount_input")
                                )
                                OutlinedTextField(
                                    value = customTag,
                                    onValueChange = { customTag = it.removePrefix("#") },
                                    placeholder = { Text("توريد", fontSize = 13.sp) },
                                    label = { Text("هاشتاج", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("بيان العملية", fontSize = 13.sp) },
                                label = { Text("الوصف", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_hashtag_desc_input")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Scrollable options below the pinned top box
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = entryType == HashtagEntryType.INVOICE,
                                    onClick = { entryType = HashtagEntryType.INVOICE },
                                    label = { Text("فاتورة (عليه)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = entryType == HashtagEntryType.PAYMENT,
                                    onClick = { entryType = HashtagEntryType.PAYMENT },
                                    label = { Text("سداد (دفع)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text("هاشتاجات سريعة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                suggestedTags.forEach { tagOption ->
                                    val active = customTag.isBlank() && selectedTag == tagOption
                                    FilterChip(
                                        selected = active,
                                        onClick = {
                                            selectedTag = tagOption
                                            customTag = ""
                                        },
                                        label = { Text("#$tagOption", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            if (allProjects.isNotEmpty()) {
                                Text("ربط بمشروع:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedProjectId == null,
                                        onClick = { selectedProjectId = null },
                                        label = { Text("بدون مشروع", fontSize = 11.sp) }
                                    )
                                    allProjects.take(12).forEach { proj ->
                                        FilterChip(
                                            selected = selectedProjectId == proj.id,
                                            onClick = { selectedProjectId = proj.id },
                                            label = { Text(proj.name, fontSize = 11.sp, maxLines = 1) }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("كاش", "تحويل بنكي", "إنستا باي").forEach { method ->
                                    FilterChip(
                                        selected = paymentMethod == method,
                                        onClick = { paymentMethod = method },
                                        label = { Text(method, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("عمل خارجي مستقل", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = isExternalBusiness,
                                    onCheckedChange = { isExternalBusiness = it }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // Equal-sized, symbol-free Cancel & Save Footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showQuickHashtagSheet = false
                                    quickHashtagInitialTag = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_hashtag_cancel_btn")
                            ) {
                                Text("إلغاء", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    val amt = amountStr.trim().toDoubleOrNull()
                                    val finalTag = (customTag.ifBlank { selectedTag }).trim().removePrefix("#").ifBlank { "فاتورة" }
                                    val finalDesc = description.trim().ifBlank {
                                        if (entryType == HashtagEntryType.INVOICE) "فاتورة #$finalTag" else "سداد #$finalTag"
                                    }
                                    if (amt != null && amt > 0.0) {
                                        val dir = if (entryType == HashtagEntryType.INVOICE) {
                                            HashtagEntryDirection.DEBIT
                                        } else {
                                            HashtagEntryDirection.CREDIT
                                        }
                                        val pickedProj = allProjects.find { it.id == selectedProjectId }
                                        if (onRecordHashtagOperation != null) {
                                            onRecordHashtagOperation(
                                                finalTag,
                                                entryType,
                                                finalDesc,
                                                amt,
                                                dir,
                                                pickedProj?.id,
                                                pickedProj?.name,
                                                paymentMethod,
                                                isExternalBusiness
                                            )
                                        } else {
                                            onSendMessage("$finalDesc بمبلغ ${GoldUtils.formatCurrency(amt)} #$finalTag", if (entryType == HashtagEntryType.INVOICE) MessageType.INVOICE.name else MessageType.PAYMENT.name)
                                        }
                                        showQuickHashtagSheet = false
                                        quickHashtagInitialTag = null
                                    } else {
                                        Toast.makeText(context, "أدخل مبلغاً صحيحاً أكبر من صفر", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = adaptiveGoldPrimary,
                                    contentColor = adaptiveOnPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("quick_hashtag_save_btn")
                            ) {
                                Text("حفظ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Inline Quick Invoice Payment Dialog triggered from Chat Bubble
    val targetPayInv = quickPayInvoiceTarget
    if (targetPayInv != null && onPayInvoice != null) {
        val remaining = (targetPayInv.totalAmount - targetPayInv.paidAmount).coerceAtLeast(0.0)
        var payAmountStr by remember(targetPayInv.id) { mutableStateOf(if (remaining > 0) remaining.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
        var payMethod by remember(targetPayInv.id) { mutableStateOf("كاش") }
        var payNotes by remember(targetPayInv.id) { mutableStateOf("") }

        Dialog(
            onDismissRequest = { quickPayInvoiceTarget = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تسجيل سداد فاتورة ${targetPayInv.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldPrimary
                        )
                        Text(
                            text = "المتبقي المستحق: ${GoldUtils.formatCurrency(remaining)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusOrange
                        )
                        OutlinedTextField(
                            value = payAmountStr,
                            onValueChange = { payAmountStr = it },
                            placeholder = { Text("مثال: 1500", fontSize = 13.sp) },
                            label = { Text("المبلغ المدفوع") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_pay_invoice_amount_input")
                        )
                        OutlinedTextField(
                            value = payNotes,
                            onValueChange = { payNotes = it },
                            placeholder = { Text("ملاحظات السداد", fontSize = 13.sp) },
                            label = { Text("ملاحظات (اختياري)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("كاش", "تحويل بنكي", "إنستا باي").forEach { method ->
                                FilterChip(
                                    selected = payMethod == method,
                                    onClick = { payMethod = method },
                                    label = { Text(method, fontSize = 11.5.sp) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { quickPayInvoiceTarget = null },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("إلغاء", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    val amt = payAmountStr.trim().toDoubleOrNull()
                                    if (amt != null && amt > 0.0) {
                                        onPayInvoice(targetPayInv.id, amt, payMethod, payNotes.trim().ifBlank { null })
                                        quickPayInvoiceTarget = null
                                    } else {
                                        Toast.makeText(context, "أدخل مبلغ سداد صحيح", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = adaptiveGoldPrimary,
                                    contentColor = adaptiveOnPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("حفظ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive #Hashtag Ledger & Linked Invoices Inspector Dialog
    val activeInspectedTag = inspectedHashtagTag
    if (activeInspectedTag != null) {
        val cleanTag = activeInspectedTag.removePrefix("#").trim()
        val matchedAccounts = remember(hashtags, cleanTag) {
            hashtags.filter { it.tag.removePrefix("#").trim().equals(cleanTag, ignoreCase = true) }
        }
        val accountIds = remember(matchedAccounts) { matchedAccounts.map { it.id }.toSet() }
        val tagEntries = remember(allEntries, accountIds) {
            allEntries.filter { it.hashtagId in accountIds }.sortedByDescending { it.createdAt }
        }
        val combinedInvoices = remember(invoicesForChat, allInvoices, cleanTag, accountIds) {
            (invoicesForChat + allInvoices)
                .distinctBy { it.id }
                .filter { inv ->
                    val invTag = inv.hashtagTag?.removePrefix("#")?.trim().orEmpty()
                    invTag.equals(cleanTag, ignoreCase = true) || (inv.hashtagId != null && inv.hashtagId in accountIds)
                }
                .sortedByDescending { it.createdAt }
        }
        val tagCredit = tagEntries.filter { it.direction == HashtagEntryDirection.CREDIT.name }.sumOf { it.amount }
        val tagDebit = tagEntries.filter { it.direction == HashtagEntryDirection.DEBIT.name }.sumOf { it.amount }
        val tagBalance = tagCredit - tagDebit

        Dialog(
            onDismissRequest = { inspectedHashtagTag = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hashtag_inspector_dialog")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#$cleanTag",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = adaptiveGoldPrimary
                                )
                                Text(
                                    text = "${tagEntries.size} عملية • ${combinedInvoices.size} فاتورة مرتبطة",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (tagBalance >= 0) StatusGreen.copy(alpha = 0.14f) else StatusRed.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "${if (tagBalance >= 0) "+" else ""}${GoldUtils.formatCurrency(tagBalance)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tagBalance >= 0) StatusGreen else StatusRed,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusGreen.copy(alpha = 0.10f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("له / دائن", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(tagCredit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusRed.copy(alpha = 0.10f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("عليه / مدين", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(tagDebit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 310.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (combinedInvoices.isNotEmpty()) {
                                Text(
                                    text = "الفواتير المرتبطة بالهاشتاج",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                combinedInvoices.forEach { inv ->
                                    val isPaid = inv.status == InvoiceStatus.PAID.name || inv.paidAmount >= inv.totalAmount
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                inspectedHashtagTag = null
                                                inspectedCode = inv.invoiceNumber
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${inv.invoiceNumber} • ${inv.title}",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = adaptiveGoldPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${inv.receiverName} • ${GoldUtils.formatCurrency(inv.totalAmount)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isPaid) StatusGreen.copy(alpha = 0.16f) else StatusOrange.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = if (isPaid) "مدفوعة" else "معلقة",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPaid) StatusGreen else StatusOrange,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (tagEntries.isNotEmpty()) {
                                Text(
                                    text = "حركات الدفتر والقيود",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                tagEntries.take(15).forEach { entry ->
                                    val isCreditEntry = entry.direction == HashtagEntryDirection.CREDIT.name
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.description.ifBlank { "قيد مالي" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (!entry.projectName.isNullOrBlank()) {
                                                    Text(
                                                        text = "مشروع: ${entry.projectName}",
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${if (isCreditEntry) "+" else "-"}${GoldUtils.formatCurrency(entry.amount)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCreditEntry) StatusGreen else StatusRed
                                            )
                                        }
                                    }
                                }
                            }

                            if (combinedInvoices.isEmpty() && tagEntries.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "لا توجد قيود أو فواتير مسجلة لهذا الهاشتاج حتى الآن، يمكنك إضافة أول قيد مباشرة.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { inspectedHashtagTag = null },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("إلغاء", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    inspectedHashtagTag = null
                                    quickHashtagInitialTag = cleanTag
                                    showQuickHashtagSheet = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = adaptiveGoldPrimary,
                                    contentColor = adaptiveOnPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hashtag_inspector_new_entry_btn")
                            ) {
                                Text("قيد جديد", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Clickable 8-Char Alphanumeric Invoice / Transaction Code Inspector Dialog
    val activeInspectedCode = inspectedCode
    if (activeInspectedCode != null) {
        val normCode = ShortCodeGenerator.normalize(activeInspectedCode)
        val matchedInvoice = remember(invoicesForChat, allInvoices, normCode) {
            (invoicesForChat + allInvoices)
                .distinctBy { it.id }
                .find {
                    ShortCodeGenerator.normalize(it.invoiceNumber) == normCode ||
                        ShortCodeGenerator.normalize(it.id) == normCode
                }
        }
        val matchedTx = remember(allTransactions, normCode) {
            allTransactions.find {
                ShortCodeGenerator.normalize(it.id) == normCode ||
                    (it.originalTransactionId != null && ShortCodeGenerator.normalize(it.originalTransactionId) == normCode)
            }
        }

        Dialog(
            onDismissRequest = { inspectedCode = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_inspector_dialog")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (matchedInvoice != null) "تفاصيل الفاتورة" else if (matchedTx != null) "تفاصيل العملية" else "سجل المعاملة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = normCode,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = adaptiveGoldPrimary
                                )
                            }
                            if (matchedInvoice != null) {
                                val isPaid = matchedInvoice.status == InvoiceStatus.PAID.name || matchedInvoice.paidAmount >= matchedInvoice.totalAmount
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPaid) StatusGreen.copy(alpha = 0.16f) else StatusOrange.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = if (isPaid) "مدفوعة" else "معلقة",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) StatusGreen else StatusOrange,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        when {
                            matchedInvoice != null -> {
                                val remaining = (matchedInvoice.totalAmount - matchedInvoice.paidAmount).coerceAtLeast(0.0)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(matchedInvoice.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("العميل: ${matchedInvoice.receiverName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("الإجمالي: ${GoldUtils.formatCurrency(matchedInvoice.totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("المدفوع: ${GoldUtils.formatCurrency(matchedInvoice.paidAmount)} • المتبقي: ${GoldUtils.formatCurrency(remaining)}", fontSize = 12.sp, color = if (remaining > 0.0) StatusOrange else StatusGreen)
                                        Text("وسيلة الدفع: ${matchedInvoice.paymentMethod}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (!matchedInvoice.hashtagTag.isNullOrBlank()) {
                                            Text("الهاشتاج: ${matchedInvoice.hashtagTag}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = adaptiveGoldPrimary)
                                        }
                                        if (!matchedInvoice.projectName.isNullOrBlank()) {
                                            Text("المشروع: ${matchedInvoice.projectName}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (!matchedInvoice.notes.isNullOrBlank()) {
                                            Text("ملاحظات: ${matchedInvoice.notes}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            matchedTx != null -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("${matchedTx.goldType} • ${matchedTx.goldKarat}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("الطرف: ${matchedTx.contactName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("الوزن: ${matchedTx.weight} جم • السعر الإجمالي: ${GoldUtils.formatCurrency(matchedTx.totalPrice)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        Text("الحالة: ${matchedTx.status}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (!matchedTx.notes.isNullOrBlank()) {
                                            Text("البيان: ${matchedTx.notes}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "المعرف $normCode غير مرتبط بفاتورة أو حركة مسجلة حالياً في قاعدة البيانات.",
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        val canPay = matchedInvoice != null && onPayInvoice != null && (matchedInvoice.totalAmount - matchedInvoice.paidAmount) > 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { inspectedCode = null },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("إلغاء", fontWeight = FontWeight.SemiBold)
                            }
                            if (canPay && matchedInvoice != null) {
                                Button(
                                    onClick = {
                                        inspectedCode = null
                                        quickPayInvoiceTarget = matchedInvoice
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = adaptiveGoldPrimary,
                                        contentColor = adaptiveOnPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("code_inspector_pay_btn")
                                ) {
                                    Text("سداد", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateTaskInChatDialog && chat != null) {
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

    if (showGroupMembersDialog && chat != null && chat.isGroup) {
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
            onToggleAdmin = { member -> onToggleGroupMemberAdmin(member) },
            onRemoveMember = { member -> onRemoveGroupMember(member) }
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

@Composable
private fun MessengerQuickCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun MessengerOptionItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MessengerOptionSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GoldPrimary
            )
        )
    }
}

@Composable
private fun ContextWorkspacePanel(
    mode: ChatSpatialMode,
    goldPrices: GoldPricesModel,
    accounts: List<ChatAccountEntity>,
    allEntries: List<HashtagEntryEntity>,
    onOpenLedgerSheet: () -> Unit,
    onOpenGoldDeal: () -> Unit,
    onOpenAIAccountant: () -> Unit
) {
    SpatialSurface(
        elevation = SpatialElevation.Level3,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = GoldPrimary.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (mode) {
                ChatSpatialMode.GOLD -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("لوحة متابعة أسعار الذهب اليوم", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                            Text("عيار 21: ${GoldUtils.formatCurrency(goldPrices.karat21)} | عيار 24: ${GoldUtils.formatCurrency(goldPrices.karat24)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onOpenGoldDeal,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("صفقة جديدة +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ChatSpatialMode.FINANCIAL -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الحساب المالي الجاري", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusGreen)
                            Text("${accounts.size} حسابات مالية مسجلة في المحادثة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onOpenLedgerSheet,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("فتح الدفتر", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                ChatSpatialMode.AI_ACCOUNTANT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مساعد المحاسب المتقدم", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFA78BFA))
                            Text("تحليل المعاملات واقتراح التوفير المالي تلقائياً", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onOpenAIAccountant,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تحليل المحادثة", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("محادثة نشطة", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("اسحب للتنقل بين الأوضاع", fontSize = 11.sp, color = GoldAccent)
                    }
                }
            }
        }
    }
}

/**
 * Unified ChatBubble component with modern, space-efficient design
 * and full compatibility with zoom scaling.
 */
@Composable
fun ChatBubble(
    msg: MessageEntity,
    isMe: Boolean,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    zoomScale: Float = 1f,
    maxBubbleWidth: Dp = 300.dp,
    customFontColorStyle: String = "auto",
    customBubbleStyle: String = "auto",
    customFontColor: Color? = null,
    customBubbleColor: Color? = null,
    isAutoTranslateEnabled: Boolean = true,
    translateMyMessagesToo: Boolean = false,
    translationFontSize: String = "medium",
    translationColorStyle: String = "gold",
    translation: TranslatedMessageUiState? = null,
    invoicesForChat: List<InvoiceEntity> = emptyList(),
    onQuickPayInvoice: ((InvoiceEntity) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onCodeClick: ((String) -> Unit)? = null,
    onTranslateClick: () -> Unit = {},
    onSpeakTranslation: (text: String, lang: String) -> Unit = { _, _ -> },
    onStopSpeaking: () -> Unit = {},
    isSpeakingTranslation: Boolean = false,
    onDismissTranslation: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onImageClick: (MediaPreviewItem) -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onReactionClick: () -> Unit = {},
    onForwardClick: () -> Unit = {},
    onReply: () -> Unit = {},
    projectTasks: List<ProjectTaskEntity> = emptyList(),
    onWorkerSubmitTaskProof: (ProjectTaskEntity) -> Unit = {},
    onAdminVerifyTask: (ProjectTaskEntity) -> Unit = {},
    onMentionClick: ((String) -> Unit)? = null
) {
    AdaptiveMessageBubble(
        msg = msg,
        isMe = isMe,
        isFirstInGroup = isFirstInGroup,
        isLastInGroup = isLastInGroup,
        zoomScale = zoomScale,
        maxBubbleWidth = maxBubbleWidth,
        customFontColorStyle = customFontColorStyle,
        customBubbleStyle = customBubbleStyle,
        customFontColor = customFontColor,
        customBubbleColor = customBubbleColor,
        isAutoTranslateEnabled = isAutoTranslateEnabled,
        translateMyMessagesToo = translateMyMessagesToo,
        translationFontSize = translationFontSize,
        translationColorStyle = translationColorStyle,
        translation = translation,
        invoicesForChat = invoicesForChat,
        onQuickPayInvoice = onQuickPayInvoice,
        onHashtagClick = onHashtagClick,
        onCodeClick = onCodeClick,
        onTranslateClick = onTranslateClick,
        onSpeakTranslation = onSpeakTranslation,
        onStopSpeaking = onStopSpeaking,
        isSpeakingTranslation = isSpeakingTranslation,
        onDismissTranslation = onDismissTranslation,
        onAvatarClick = onAvatarClick,
        onImageClick = onImageClick,
        onOptionsClick = onOptionsClick,
        onLongPress = onLongPress,
        onReactionClick = onReactionClick,
        onForwardClick = onForwardClick,
        onReply = onReply,
        projectTasks = projectTasks,
        onWorkerSubmitTaskProof = onWorkerSubmitTaskProof,
        onAdminVerifyTask = onAdminVerifyTask,
        onMentionClick = onMentionClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdaptiveMessageBubble(
    msg: MessageEntity,
    isMe: Boolean,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    zoomScale: Float = 1f,
    maxBubbleWidth: Dp = 300.dp,
    customFontColorStyle: String = "auto",
    customBubbleStyle: String = "auto",
    customFontColor: Color? = null,
    customBubbleColor: Color? = null,
    isAutoTranslateEnabled: Boolean = true,
    translateMyMessagesToo: Boolean = false,
    translationFontSize: String = "medium",
    translationColorStyle: String = "gold",
    translation: TranslatedMessageUiState? = null,
    invoicesForChat: List<InvoiceEntity> = emptyList(),
    onQuickPayInvoice: ((InvoiceEntity) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onCodeClick: ((String) -> Unit)? = null,
    onTranslateClick: () -> Unit = {},
    onSpeakTranslation: (text: String, lang: String) -> Unit = { _, _ -> },
    onStopSpeaking: () -> Unit = {},
    isSpeakingTranslation: Boolean = false,
    onDismissTranslation: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onImageClick: (MediaPreviewItem) -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onReactionClick: () -> Unit = {},
    onForwardClick: () -> Unit = {},
    onReply: () -> Unit = {},
    projectTasks: List<ProjectTaskEntity> = emptyList(),
    onWorkerSubmitTaskProof: (ProjectTaskEntity) -> Unit = {},
    onAdminVerifyTask: (ProjectTaskEntity) -> Unit = {},
    onMentionClick: ((String) -> Unit)? = null
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipe_offset"
    )
    val density = LocalDensity.current
    val replyThresholdPx = with(density) { 56.dp.toPx() }

    LaunchedEffect(msg.id, msg.content, isAutoTranslateEnabled, translateMyMessagesToo, isMe) {
        val shouldTranslateThisMessage = !isMe || translateMyMessagesToo
        if (isAutoTranslateEnabled && shouldTranslateThisMessage && msg.type == MessageType.TEXT.name && msg.content.isNotBlank() && translation == null) {
            val hasArabic = msg.content.any { it in '\u0600'..'\u06FF' }
            val hasLatin = msg.content.any { it in 'a'..'z' || it in 'A'..'Z' }
            if (hasLatin && !hasArabic) {
                onTranslateClick()
            }
        }
    }

    val isGoldPurchase = msg.type == MessageType.GOLD_PURCHASE.name
    val isInvoice = msg.type == MessageType.INVOICE.name
    val isPayment = msg.type == MessageType.PAYMENT.name
    val isVoice = msg.type == MessageType.VOICE.name
    val isDocument = msg.type == MessageType.DOCUMENT.name
    val isMissedCall = msg.type == MessageType.MISSED_CALL.name
    val isCall = msg.type == MessageType.CALL.name
    val isFieldTask = msg.type == "FIELD_TASK" || msg.content.trimStart().startsWith("[FIELD_TASK:")
    val isImage = msg.type == MessageType.IMAGE.name ||
            ((msg.content.startsWith("content://") || msg.content.startsWith("file://")) && !isDocument && !msg.content.contains(".pdf") && !msg.content.contains(".doc") && !msg.content.contains(".xls") && !msg.content.contains(".zip"))

    val innerCorner = if (isFirstInGroup && isLastInGroup) 16.dp else 6.dp
    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = 16.dp,
            bottomStart = 16.dp,
            topEnd = if (isFirstInGroup) 16.dp else innerCorner,
            bottomEnd = if (isLastInGroup) 5.dp else innerCorner
        )
    } else {
        RoundedCornerShape(
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            topStart = if (isFirstInGroup) 16.dp else innerCorner,
            bottomStart = if (isLastInGroup) 5.dp else innerCorner
        )
    }

    val hPadding = (7.5f * (0.88f + 0.12f * clampedZoom)).dp
    val vPadding = (3.2f * (0.88f + 0.12f * clampedZoom)).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 2.dp,
                vertical = if (!msg.reaction.isNullOrBlank()) 4.dp else 0.dp
            ),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            if (isLastInGroup) {
                Box(modifier = Modifier.clickable { onAvatarClick() }) {
                    OozoxAvatarImage(
                        model = null,
                        name = msg.senderName,
                        size = 30.dp,
                        isTrader = isGoldPurchase,
                        contentDescription = msg.senderName
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(30.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(msg.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.absoluteValue > replyThresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReply()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = offsetX + dragAmount
                            offsetX = if (isMe) {
                                newOffset.coerceIn(-140f, 40f)
                            } else {
                                newOffset.coerceIn(-40f, 140f)
                            }
                        }
                    )
                },
            contentAlignment = if (isMe) Alignment.BottomEnd else Alignment.BottomStart
        ) {
            Surface(
                shape = bubbleShape,
                color = when {
                    isMissedCall -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    isGoldPurchase -> GoldDark
                    isInvoice -> MaterialTheme.colorScheme.surfaceVariant
                    isPayment -> Color(0xFF064E3B)
                    customBubbleColor != null -> customBubbleColor
                    customBubbleStyle == "gold_dark" -> GoldDark
                    customBubbleStyle == "slate" -> Color(0xFF1E293B)
                    customBubbleStyle == "emerald_glass" -> Color(0xFF064E3B)
                    customBubbleStyle == "violet" -> Color(0xFF2E1065)
                    isImage -> if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    isMe -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = BorderStroke(
                    width = 0.8.dp,
                    color = when {
                        isGoldPurchase -> GoldPrimary
                        isInvoice -> Color(0xFF3B82F6).copy(alpha = 0.4f)
                        isPayment -> StatusGreen.copy(alpha = 0.5f)
                        isImage -> GoldPrimary.copy(alpha = 0.35f)
                        isMissedCall -> Color(0xFFEF4444).copy(alpha = 0.4f)
                        isMe -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                    }
                ),
                shadowElevation = if (isGoldPurchase || isInvoice || isImage) 3.dp else 0.5.dp,
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .combinedClickable(
                        onClick = { onOptionsClick() },
                        onLongClick = { onLongPress() }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = hPadding, vertical = vPadding)) {
                    val showSpecialBadge = isGoldPurchase || isInvoice || isPayment || isDocument
                    val showSenderLabel = !isMe && isFirstInGroup
                    if (showSenderLabel || showSpecialBadge) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            if (showSenderLabel) {
                                Text(
                                    text = msg.senderName,
                                    fontSize = (11.5f * clampedZoom).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGoldPurchase) GoldLight else GoldPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (showSpecialBadge) {
                                val badgeText = when {
                                    isGoldPurchase -> "صفقة ذهب"
                                    isInvoice -> "فاتورة معتمدة"
                                    isPayment -> "إشعار سداد"
                                    isDocument -> "مستند"
                                    else -> ""
                                }
                                val badgeColor = when {
                                    isGoldPurchase -> GoldPrimary
                                    isInvoice -> Color(0xFF60A5FA)
                                    isPayment -> StatusGreen
                                    isDocument -> Color(0xFF60A5FA)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                if (badgeText.isNotEmpty()) {
                                    Text(
                                        text = badgeText,
                                        fontSize = (10.5f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                    }

                    val safetyVerdict = remember(msg.content, msg.id, isImage, isDocument) {
                        val mediaUri = if (isImage || isDocument) msg.content.substringBefore("||") else null
                        ContentSafetyEngine.evaluate(
                            text = msg.content,
                            mediaUri = mediaUri,
                            isVideo = isImage && FileUtils.isVideoUriString(msg.content)
                        )
                    }

                    var isRevealed by rememberSaveable(msg.id) { mutableStateOf(false) }

                    if (!safetyVerdict.isSafe && !isRevealed) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF7F1D1D).copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "درع الأمان",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "درع الأمان: تم حجب محتوى غير لائق",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (12f * clampedZoom).sp,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = safetyVerdict.reason,
                                    fontSize = (11f * clampedZoom).sp,
                                    lineHeight = (15f * clampedZoom).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    onClick = { isRevealed = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "كشف المحتوى",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "كشف المحتوى على مسؤوليتي",
                                            fontSize = (11f * clampedZoom).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (!safetyVerdict.isSafe && isRevealed) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF7F1D1D).copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "محتوى حساس تم كشفه",
                                        fontSize = (10f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5)
                                    )
                                    Text(
                                        text = "إعادة الحجب",
                                        fontSize = (10f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight,
                                        modifier = Modifier.clickable { isRevealed = false }
                                    )
                                }
                            }
                        }

                        if (isDocument) {
                            val parts = msg.content.split("||")
                            val fileUri = parts.getOrNull(0) ?: ""
                            val fileName = parts.getOrNull(1) ?: "مستند_مرفق"
                            val fileSize = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                            val caption = if (parts.size > 3) parts[3] else ""

                            DocumentMessageItem(
                                fileUri = fileUri,
                                fileName = fileName,
                                fileSize = fileSize,
                                caption = caption,
                                isMe = isMe,
                                zoomScale = clampedZoom
                            )
                        } else if (isImage) {
                            val imageParts = msg.content.split("||", limit = 2)
                            val imageUri = imageParts[0].trim()
                            val caption = if (imageParts.size > 1) imageParts[1].trim() else ""
                            val isVideo = FileUtils.isVideoUriString(imageUri)

                            val previewItem = MediaPreviewItem(
                                uri = imageUri,
                                caption = caption,
                                senderName = msg.senderName,
                                timestamp = msg.timestamp,
                                isVideo = isVideo
                            )
                            val thumbWidth = (228f * clampedZoom.coerceAtMost(1.35f)).dp.coerceAtMost((maxBubbleWidth - 22.dp).coerceAtLeast(140.dp))
                            val thumbHeight = (182f * clampedZoom.coerceAtMost(1.35f)).dp

                            Column(
                                modifier = Modifier.padding(vertical = 2.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                if (isVideo) {
                                    InlineChatVideoPlayer(
                                        videoUri = imageUri,
                                        width = thumbWidth,
                                        height = thumbHeight,
                                        shape = RoundedCornerShape(12.dp),
                                        onExpandFullscreen = { onImageClick(previewItem) }
                                    )
                                } else {
                                    OozoxThumbnailImage(
                                        model = imageUri,
                                        width = thumbWidth,
                                        height = thumbHeight,
                                        shape = RoundedCornerShape(12.dp),
                                        contentScale = ContentScale.Fit,
                                        contentDescription = "صورة المحادثة",
                                        onClick = { onImageClick(previewItem) }
                                    )
                                }

                                if (caption.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = caption,
                                        fontSize = (13.5f * clampedZoom).sp,
                                        lineHeight = (18.5f * clampedZoom).sp,
                                        color = when {
                                            isMe -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        } else if (isVoice) {
                            VoiceMessagePlayer(
                                content = msg.content,
                                isMe = isMe,
                                zoomScale = clampedZoom
                            )
                        } else if (isMissedCall || isCall) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 3.dp, horizontal = 2.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isMissedCall) Color(0xFFEF4444).copy(alpha = 0.15f) else GoldPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isMissedCall) Icons.Default.PhoneMissed else Icons.Default.Call,
                                            contentDescription = null,
                                            tint = if (isMissedCall) Color(0xFFEF4444) else GoldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isMissedCall) "مكالمة فائتة" else "مكالمة صوتية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (14f * clampedZoom).sp,
                                        color = if (isMissedCall) Color(0xFFEF4444) else if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = GoldUtils.formatCallExactDateTime(msg.timestamp),
                                        fontSize = (11.5f * clampedZoom).sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            val textColor = when {
                                isGoldPurchase -> GoldLight
                                isPayment -> Color(0xFFD1FAE5)
                                customFontColor != null -> customFontColor
                                customFontColorStyle == "gold" -> GoldLight
                                customFontColorStyle == "white" -> Color.White
                                customFontColorStyle == "emerald" -> Color(0xFF10B981)
                                customFontColorStyle == "platinum" -> Color(0xFFE2E8F0)
                                isMe -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val uriHandler = LocalUriHandler.current
                            val hasRepliedQuote = msg.content.startsWith("↩️ رد على ") && msg.content.contains("\n\n")
                            val bodyToRender = if (hasRepliedQuote) msg.content.substringAfter("\n\n") else msg.content

                            if (hasRepliedQuote) {
                                val quoteHeader = msg.content.substringBefore("\n\n")
                                Surface(
                                    color = if (isMe) MaterialTheme.colorScheme.surface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(24.dp)
                                                .background(GoldPrimary, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = quoteHeader,
                                            fontSize = (11f * clampedZoom).sp,
                                            lineHeight = (15f * clampedZoom).sp,
                                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (isFieldTask) {
                                val taskTokenMatch = Regex("^\\[FIELD_TASK:([^\\]\\n]+)\\]\\s*").find(msg.content.trim())
                                val linkedTaskId = taskTokenMatch?.groupValues?.getOrNull(1)?.trim()
                                val linkedTask = remember(linkedTaskId, projectTasks) {
                                    if (linkedTaskId.isNullOrBlank()) null
                                    else projectTasks.firstOrNull { it.id == linkedTaskId }
                                }
                                val displayTaskBody = if (taskTokenMatch != null) {
                                    msg.content.trim().removePrefix(taskTokenMatch.value).trim()
                                } else {
                                    msg.content
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (linkedTask?.isArchived == true) StatusGreen.copy(alpha = 0.55f) else GoldPrimary.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .testTag("chat_field_task_card_${linkedTaskId ?: msg.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AssignmentInd,
                                                    contentDescription = null,
                                                    tint = GoldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = linkedTask?.title ?: "مهمة عمل",
                                                    fontSize = (13f * clampedZoom).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldPrimary,
                                                    maxLines = 2
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = when {
                                                    linkedTask?.isArchived == true -> StatusGreen.copy(alpha = 0.16f)
                                                    linkedTask?.isSubmittedForReview == true -> Color(0xFF3B82F6).copy(alpha = 0.16f)
                                                    else -> Color(0xFFF59E0B).copy(alpha = 0.16f)
                                                }
                                            ) {
                                                Text(
                                                    text = when {
                                                        linkedTask?.isArchived == true -> "معتمدة"
                                                        linkedTask?.isSubmittedForReview == true -> "بانتظار الاعتماد"
                                                        else -> linkedTask?.deadlineDurationText ?: "قيد التنفيذ"
                                                    },
                                                    fontSize = (10f * clampedZoom).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        linkedTask?.isArchived == true -> StatusGreen
                                                        linkedTask?.isSubmittedForReview == true -> Color(0xFF3B82F6)
                                                        else -> Color(0xFFF59E0B)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (displayTaskBody.isNotBlank()) {
                                            RichTextMessageText(
                                                text = displayTaskBody,
                                                textColor = MaterialTheme.colorScheme.onSurface,
                                                fontSize = (12.5f * clampedZoom).sp,
                                                lineHeight = (18f * clampedZoom).sp,
                                                onHashtagClick = onHashtagClick,
                                                onCodeClick = onCodeClick,
                                                onMentionClick = onMentionClick,
                                                onLinkClick = { linkUrl ->
                                                    try { uriHandler.openUri(linkUrl) } catch (_: Exception) {}
                                                }
                                            )
                                        }

                                        val assigneesText = linkedTask?.assignedMembersList?.ifBlank { linkedTask.assignedToName.orEmpty() }.orEmpty()
                                        if (assigneesText.isNotBlank() && !displayTaskBody.contains(assigneesText)) {
                                            RichTextMessageText(
                                                text = "المكلف: $assigneesText",
                                                textColor = Color(0xFF38BDF8),
                                                fontSize = (11.5f * clampedZoom).sp,
                                                lineHeight = (16f * clampedZoom).sp,
                                                onMentionClick = onMentionClick,
                                                onLinkClick = {}
                                            )
                                        }

                                        if (linkedTask != null && !linkedTask.isArchived) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (!linkedTask.isSubmittedForReview) {
                                                    OutlinedButton(
                                                        onClick = { onWorkerSubmitTaskProof(linkedTask) },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                            .testTag("chat_task_proof_btn_${linkedTask.id}")
                                                    ) {
                                                        Text("توثيق الإنجاز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Button(
                                                    onClick = { onAdminVerifyTask(linkedTask) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("chat_task_verify_btn_${linkedTask.id}")
                                                ) {
                                                    Text("اعتماد وأرشفة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                RichTextMessageText(
                                    text = bodyToRender,
                                    textColor = textColor,
                                    fontSize = (14.5f * clampedZoom).sp,
                                    lineHeight = (20f * clampedZoom).sp,
                                    onHashtagClick = { tag -> onHashtagClick?.invoke(tag) },
                                    onCodeClick = { code -> onCodeClick?.invoke(code) },
                                    onMentionClick = onMentionClick,
                                    onLinkClick = { linkUrl ->
                                        val isYt = linkUrl.contains("youtube.com") || linkUrl.contains("youtu.be")
                                        val isVidLink = isYt || FileUtils.isVideoUriString(linkUrl)
                                        val isImgLink = FileUtils.isImageNameOrMime(linkUrl.substringBefore('?'), "")
                                        val isMedia = isVidLink || isImgLink
                                        if (isMedia) {
                                            onImageClick(
                                                MediaPreviewItem(
                                                    uri = linkUrl,
                                                    caption = "رابط مرفق في المحادثة",
                                                    senderName = msg.senderName,
                                                    timestamp = msg.timestamp,
                                                    isVideo = isVidLink
                                                )
                                            )
                                        } else {
                                            try {
                                                uriHandler.openUri(linkUrl)
                                            } catch (_: Exception) { }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = translation != null,
                        enter = fadeIn(animationSpec = tween(durationMillis = 350, easing = EaseOutCubic)) +
                                expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 200, easing = EaseInCubic)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 200))
                    ) {
                        if (translation != null) {
                            Column {
                                Spacer(modifier = Modifier.height(5.dp))
                                ContextualTranslationBubbleCard(
                                    translation = translation,
                                    translationFontSize = translationFontSize,
                                    translationColorStyle = translationColorStyle,
                                    zoomScale = clampedZoom,
                                    isSpeaking = isSpeakingTranslation,
                                    onSpeak = onSpeakTranslation,
                                    onStopSpeak = onStopSpeaking,
                                    onDismiss = onDismissTranslation,
                                    isMe = isMe
                                )
                            }
                        }
                    }

                    val isHashtagOrFinance = isInvoice || isPayment || isGoldPurchase ||
                        msg.type == MessageType.HASHTAG.name || msg.content.contains("#")
                    if (isHashtagOrFinance && invoicesForChat.isNotEmpty()) {
                        val parsedTag = remember(msg.content) { HashtagUtils.parseHashtag(msg.content) }
                        val linkedInvoice = remember(invoicesForChat, parsedTag, msg.content, msg.timestamp) {
                            when {
                                !parsedTag.isNullOrBlank() -> invoicesForChat.firstOrNull { inv ->
                                    inv.hashtagTag?.equals(parsedTag, ignoreCase = true) == true
                                }
                                isInvoice -> invoicesForChat.firstOrNull { inv ->
                                    inv.status != InvoiceStatus.PAID.name &&
                                        (msg.content.contains(inv.invoiceNumber, ignoreCase = true) ||
                                            msg.content.contains(inv.notes ?: "", ignoreCase = true))
                                } ?: invoicesForChat.firstOrNull { it.status != InvoiceStatus.PAID.name }
                                else -> null
                            }
                        }
                        if (linkedInvoice != null) {
                            val remaining = (linkedInvoice.totalAmount - linkedInvoice.paidAmount).coerceAtLeast(0.0)
                            val isFullyPaid = linkedInvoice.status == InvoiceStatus.PAID.name || remaining <= 0.01
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isFullyPaid) StatusGreen.copy(alpha = 0.14f) else StatusOrange.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = if (isFullyPaid) "فاتورة مسددة" else "متبقي ${GoldUtils.formatCurrency(remaining)}",
                                        fontSize = (10f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFullyPaid) StatusGreen else StatusOrange,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                if (!isFullyPaid && onQuickPayInvoice != null) {
                                    Surface(
                                        onClick = { onQuickPayInvoice(linkedInvoice) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldPrimary.copy(alpha = 0.18f),
                                        border = BorderStroke(0.7.dp, GoldPrimary.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = "تسجيل سداد",
                                            fontSize = (10f * clampedZoom).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    var showMessageDetails by remember { mutableStateOf(false) }

                    // Compact Footer (Timestamp + Read Receipt) hugging content cleanly
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { showMessageDetails = !showMessageDetails }
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = GoldUtils.formatShortTime(msg.timestamp),
                            fontSize = (9.5f * clampedZoom).sp,
                            color = when {
                                isGoldPurchase -> GoldLight.copy(alpha = 0.7f)
                                isMe -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                            }
                        )
                        if (isMe) {
                            when (msg.status) {
                                MessageStatus.READ.name -> {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "تمت القراءة",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier
                                            .size((14f * (0.85f + 0.15f * clampedZoom)).dp)
                                            .testTag("read_receipt_read_${msg.id}")
                                    )
                                }
                                MessageStatus.DELIVERED.name -> {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "تم التسليم",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .size((14f * (0.85f + 0.15f * clampedZoom)).dp)
                                            .testTag("read_receipt_delivered_${msg.id}")
                                    )
                                }
                                MessageStatus.PENDING.name -> {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "محفوظ محلياً في Room (في انتظار الاتصال)",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier
                                            .size((12.5f * (0.85f + 0.15f * clampedZoom)).dp)
                                            .testTag("read_receipt_pending_${msg.id}")
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "تم الإرسال",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size((13f * (0.85f + 0.15f * clampedZoom)).dp)
                                            .testTag("read_receipt_sent_${msg.id}")
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Expandable Read Receipt & Quick Actions Details
                    AnimatedVisibility(
                        visible = showMessageDetails,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val statusLabel = when (msg.status) {
                                        MessageStatus.READ.name -> "تمت القراءة من المستلم"
                                        MessageStatus.DELIVERED.name -> "تم التسليم بنجاح"
                                        MessageStatus.PENDING.name -> "محفوظ محلياً في Room"
                                        else -> "تم الإرسال عبر الخادم"
                                    }
                                    val statusColor = when (msg.status) {
                                        MessageStatus.READ.name -> GoldPrimary
                                        MessageStatus.DELIVERED.name -> StatusGreen
                                        MessageStatus.PENDING.name -> Color(0xFFF59E0B)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(
                                        text = statusLabel,
                                        fontSize = (9.5f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    Text(
                                        text = GoldUtils.formatExactDateTime(msg.timestamp),
                                        fontSize = (9f * clampedZoom).sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (msg.type == MessageType.TEXT.name && msg.content.isNotBlank()) {
                                        Text(
                                            text = "ترجمة",
                                            fontSize = (10f * clampedZoom).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary,
                                            modifier = Modifier.clickable { onTranslateClick() }
                                        )
                                    }
                                    Text(
                                        text = "رد",
                                        fontSize = (10f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        modifier = Modifier.clickable { onReply() }
                                    )
                                    Text(
                                        text = "توجيه",
                                        fontSize = (10f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        modifier = Modifier.clickable { onForwardClick() }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!msg.reaction.isNullOrBlank()) {
                Surface(
                    onClick = onReactionClick,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.2.dp, GoldPrimary),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .offset(
                            x = if (isMe) (-6).dp else 6.dp,
                            y = 10.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg.reaction,
                            fontSize = (12f * clampedZoom).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageReactionDialog(
    message: MessageEntity,
    onDismiss: () -> Unit,
    onReactionSelect: (String) -> Unit,
    onReply: () -> Unit = {},
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onTranslate: () -> Unit = {}
) {
    val emojis = listOf("معتمد", "تم الاطلاع", "موافق", "قيد المراجعة")
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)), RoundedCornerShape(20.dp))
                    .padding(20.dp)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "التفاعل مع الرسالة",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Floating Reactions Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.5f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        emojis.forEach { emoji ->
                            val isSelected = message.reaction == emoji
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) adaptiveGoldPrimary.copy(alpha = 0.25f)
                                        else if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) adaptiveGoldPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onReactionSelect(emoji)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Message preview snippet
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (message.type == MessageType.VOICE.name) "تسجيل صوتي"
                            else if (message.type == MessageType.IMAGE.name) "صورة مرفقة"
                            else message.content.take(100),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons (Reply, Forward, Copy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReply,
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = adaptiveOnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رد", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onForward,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("توجيه", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ", fontSize = 12.sp)
                    }
                }

                // Contextual AI Translation Action
                if (message.type == MessageType.TEXT.name && message.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onTranslate()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ترجمة سياقية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }

                if (!message.reaction.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onReactionSelect(message.reaction) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إزالة التفاعل (${message.reaction})", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Contextual Translation Bubble Card:
 * Clean, lightweight, contextual translation rendered below the message text.
 * Powered by Gemini AI context analysis and Text-To-Speech audio pronunciation.
 */
@Composable
fun ContextualTranslationBubbleCard(
    translation: TranslatedMessageUiState,
    translationFontSize: String = "medium",
    translationColorStyle: String = "gold",
    zoomScale: Float = 1f,
    isSpeaking: Boolean,
    onSpeak: (String, String) -> Unit,
    onStopSpeak: () -> Unit,
    onDismiss: () -> Unit,
    isMe: Boolean
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val resolvedTextColor = com.example.util.TranslationStyleHelper.resolveColor(translationColorStyle, isDark)
    val resolvedFontSize = com.example.util.TranslationStyleHelper.resolveFontSize(translationFontSize) * clampedZoom
    val resolvedLineHeight = com.example.util.TranslationStyleHelper.resolveLineHeight(translationFontSize) * clampedZoom

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1B2028).copy(alpha = 0.95f) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "ترجمة سياقية",
                        fontSize = (10.5f * clampedZoom).sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    val langLabel = if (translation.targetLang == "ar") "العربية" else "English"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = langLabel,
                            fontSize = (9f * clampedZoom).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) GoldLight else GoldPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!translation.isLoading && translation.translatedText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isSpeaking) {
                                    onStopSpeak()
                                } else {
                                    onSpeak(translation.translatedText, translation.targetLang)
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "استماع صوتياً للترجمة",
                                tint = if (isSpeaking) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(AnnotatedString(translation.translatedText))
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ النص المترجم",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إخفاء الترجمة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Crossfade(
                targetState = translation.isLoading,
                animationSpec = tween(durationMillis = 350, easing = EaseOutCubic),
                label = "translation_text_crossfade"
            ) { isLoading ->
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = GoldPrimary
                        )
                        Text(
                            text = "جارٍ استخراج الترجمة وفهم سياق المحادثة...",
                            fontSize = (11f * clampedZoom).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = translation.translatedText,
                        fontSize = resolvedFontSize.sp,
                        lineHeight = resolvedLineHeight.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = resolvedTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceMessagePlayer(
    content: String,
    isMe: Boolean,
    zoomScale: Float = 1f
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var durationSec by remember { mutableIntStateOf(10) }
    var currentSec by remember { mutableIntStateOf(0) }
    var audioPath by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }

    LaunchedEffect(content) {
        if (content.contains("||")) {
            val parts = content.split("||")
            audioPath = parts.getOrNull(0)?.trim() ?: ""
            durationSec = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 10
            transcript = parts.getOrNull(2)?.trim() ?: ""
        } else {
            transcript = content
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isPlaying) {
                AudioPlayerManager.stop()
            }
        }
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play / Pause Circle Button
            IconButton(
                onClick = {
                    if (isPlaying) {
                        AudioPlayerManager.stop()
                        isPlaying = false
                    } else {
                        isPlaying = true
                        if (audioPath.isNotBlank() && File(audioPath).exists()) {
                            AudioPlayerManager.play(
                                context = context,
                                pathOrUri = audioPath,
                                onProgress = { cur, tot ->
                                    if (tot > 0) {
                                        currentProgress = cur.toFloat() / tot.toFloat()
                                        currentSec = cur / 1000
                                        durationSec = tot / 1000
                                    }
                                },
                                onComplete = {
                                    isPlaying = false
                                    currentProgress = 0f
                                    currentSec = 0
                                }
                            )
                        } else {
                            isPlaying = false
                            currentProgress = 0f
                            android.widget.Toast.makeText(context, "الملف الصوتي غير متوفر على الجهاز", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) StatusOrange else adaptiveGoldPrimary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل التسجيل الصوتي",
                    tint = if (isPlaying) Color.White else adaptiveOnPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Waveform & Progress
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(16) { index ->
                        val barHeight = (6 + ((index * 9) % 18)).dp
                        val isPassed = (index.toFloat() / 16f) <= currentProgress
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isPassed) GoldAccent else GoldPrimary.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                        fontSize = (10f * clampedZoom).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%02d:%02d", durationSec / 60, durationSec % 60),
                        fontSize = (10f * clampedZoom).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (transcript.isNotBlank() && !transcript.startsWith("/")) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transcript,
                fontSize = (11.5f * clampedZoom).sp,
                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = (15.5f * clampedZoom).sp
            )
        }
    }
}

@Composable
fun ForwardMessageDialog(
    messageToForward: MessageEntity,
    currentChatId: String,
    availableChats: List<ChatEntity>,
    onDismiss: () -> Unit,
    onForwardToChat: (targetChat: ChatEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredChats = remember(searchQuery, availableChats, currentChatId) {
        availableChats
            .filter { it.id != currentChatId }
            .filter {
                if (searchQuery.isBlank()) true
                else it.contactName.contains(searchQuery, ignoreCase = true) ||
                        it.contactType.contains(searchQuery, ignoreCase = true)
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        GoldPrimary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إعادة توجيه الرسالة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اختر جهة الاتصال لإرسال الرسالة إليها",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Message Preview Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (messageToForward.type) {
                                MessageType.VOICE.name -> Icons.Default.Mic
                                MessageType.IMAGE.name -> Icons.Default.Image
                                MessageType.GOLD_PURCHASE.name -> Icons.Default.MonetizationOn
                                else -> Icons.Default.ChatBubbleOutline
                            },
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (messageToForward.type) {
                                MessageType.VOICE.name -> "تسجيل صوتي"
                                MessageType.IMAGE.name -> "صورة مرفقة"
                                else -> messageToForward.content.take(80)
                            },
                            fontSize = 12.sp,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في المحادثات...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        focusedLabelColor = GoldPrimary
                    ),
                    singleLine = true
                )

                if (filteredChats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة" else "لا توجد محادثات أخرى متاحة لإعادة التوجيه",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(filteredChats, key = { it.id }) { targetChat ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onForwardToChat(targetChat) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OozoxAvatarImage(
                                            model = null,
                                            name = targetChat.contactName,
                                            size = 36.dp,
                                            isTrader = targetChat.contactType.contains("TRADER"),
                                            contentDescription = targetChat.contactName
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(targetChat.contactName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(targetChat.lastMessage?.take(30) ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Button(
                                        onClick = { onForwardToChat(targetChat) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = adaptiveGoldPrimary,
                                            contentColor = adaptiveOnPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("إرسال", color = adaptiveOnPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun DocumentMessageItem(
    fileUri: String,
    fileName: String,
    fileSize: Long,
    caption: String,
    isMe: Boolean,
    zoomScale: Float = 1f
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val context = LocalContext.current
    val isPdf = fileName.endsWith(".pdf", true)
    val isWord = fileName.endsWith(".doc", true) || fileName.endsWith(".docx", true)
    val isExcel = fileName.endsWith(".xls", true) || fileName.endsWith(".xlsx", true) || fileName.endsWith(".csv", true)
    val isArchive = fileName.endsWith(".zip", true) || fileName.endsWith(".rar", true)
    val badgeColor = when {
        isPdf -> Color(0xFFEF4444)
        isWord -> Color(0xFF2563EB)
        isExcel -> Color(0xFF10B981)
        isArchive -> Color(0xFFF59E0B)
        else -> GoldPrimary
    }
    val docIcon = when {
        isPdf -> Icons.Default.PictureAsPdf
        isWord -> Icons.Default.Description
        isExcel -> Icons.Default.TableChart
        isArchive -> Icons.Default.FolderZip
        else -> Icons.Default.InsertDriveFile
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = docIcon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = badgeColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = FileUtils.getFileExtension(fileName).uppercase().ifBlank { "FILE" },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            if (fileSize > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = FileUtils.formatFileSize(fileSize),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { FileUtils.openDocument(context, fileUri, fileName) },
                        colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "فتح المستند",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { FileUtils.shareDocument(context, fileUri, fileName) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة المستند",
                            tint = badgeColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        if (caption.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = caption,
                fontSize = (13f * clampedZoom).sp,
                lineHeight = (18f * clampedZoom).sp,
                color = when {
                    isMe -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun ChatHourlyTimestampHeader(
    timestamp: Long,
    zoomScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val headerText = remember(timestamp) {
        GoldUtils.formatHourlyTimestampHeader(timestamp)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            tonalElevation = 1.dp,
            modifier = Modifier.testTag("hourly_timestamp_header_${timestamp}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = headerText,
                    fontSize = (11f * clampedZoom).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun parseHexColor(hex: String): Color? {
    val cleanHex = hex.trim().removePrefix("#")
    if (cleanHex.length != 6 && cleanHex.length != 8) return null
    return try {
        val colorInt = if (cleanHex.length == 6) {
            0xFF000000.toInt() or cleanHex.toLong(16).toInt()
        } else {
            cleanHex.toLong(16).toInt()
        }
        Color(colorInt)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun RichTextMessageText(
    text: String,
    textColor: Color,
    fontSize: TextUnit = 14.5.sp,
    lineHeight: TextUnit = 20.sp,
    onHashtagClick: ((String) -> Unit)? = null,
    onCodeClick: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: (String) -> Unit
) {
    val annotatedString = remember(text, textColor) {
        buildAnnotatedString {
            val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)|(https?://[^\\s]+|app://[^\\s]+)")
            val hashtagRegex = Regex("#([\\u0600-\\u06FF\\w]+)")
            val mentionRegex = Regex("@([\\u0600-\\u06FF\\w._-]+)")
            val codeRegex = ShortCodeGenerator.CODE_REGEX

            var lastIndex = 0
            val matches = (linkRegex.findAll(text) + hashtagRegex.findAll(text) + mentionRegex.findAll(text) + codeRegex.findAll(text))
                .sortedBy { it.range.first }
                .toList()

            for (match in matches) {
                if (match.range.first >= lastIndex) {
                    val rawMatch = match.value
                    val isValidMentionStart = !rawMatch.startsWith("@") ||
                        match.range.first == 0 ||
                        text[match.range.first - 1].isWhitespace() ||
                        text[match.range.first - 1] in listOf(':', '،', ',', '|', '(', '[')

                    if (!isValidMentionStart) {
                        continue
                    }

                    append(text.substring(lastIndex, match.range.first))

                    when {
                        rawMatch.startsWith("[") -> {
                            val label = match.groupValues.getOrNull(1) ?: ""
                            val url = match.groupValues.getOrNull(2) ?: ""
                            pushStringAnnotation(tag = "URL", annotation = url)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(label)
                            pop()
                            pop()
                        }
                        rawMatch.startsWith("http") || rawMatch.startsWith("app://") -> {
                            pushStringAnnotation(tag = "URL", annotation = rawMatch)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(rawMatch)
                            pop()
                            pop()
                        }
                        rawMatch.startsWith("#") -> {
                            val cleanTag = rawMatch.removePrefix("#")
                            val isSpecialTag = rawMatch.contains("له") || rawMatch.contains("عليه") || rawMatch.contains("ذهب") || rawMatch.contains("سداد") || rawMatch.contains("مبيعات") || rawMatch.contains("فاتورة")
                            pushStringAnnotation(tag = "HASHTAG", annotation = cleanTag)
                            pushStyle(
                                SpanStyle(
                                    color = if (isSpecialTag) GoldAccent else GoldPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(rawMatch)
                            pop()
                            pop()
                        }
                        rawMatch.startsWith("@") -> {
                            val cleanMention = rawMatch.removePrefix("@")
                            pushStringAnnotation(tag = "MENTION", annotation = cleanMention)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            append(rawMatch)
                            pop()
                            pop()
                        }
                        codeRegex.matches(rawMatch) -> {
                            val normalizedCode = rawMatch.uppercase(Locale.US)
                            pushStringAnnotation(tag = "CODE", annotation = normalizedCode)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF0EA5E9),
                                    fontWeight = FontWeight.Black,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(normalizedCode)
                            pop()
                            pop()
                        }
                        else -> append(rawMatch)
                    }
                    lastIndex = match.range.last + 1
                }
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = textColor
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                    return@ClickableText
                }
            annotatedString.getStringAnnotations(tag = "HASHTAG", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onHashtagClick?.invoke(annotation.item)
                    return@ClickableText
                }
            annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick?.invoke(annotation.item)
                    return@ClickableText
                }
            annotatedString.getStringAnnotations(tag = "CODE", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onCodeClick?.invoke(annotation.item)
                }
        }
    )
}

