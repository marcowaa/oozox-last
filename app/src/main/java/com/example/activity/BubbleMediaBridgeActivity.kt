package com.example.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.util.FileUtils
import com.example.util.MessengerBubbleManager
import java.io.File

/**
 * Translucent bridge activity launched strictly by the floating chat bubble service
 * so Camera, Video Capture, Photo Gallery, Video Gallery, and Document Pickers
 * work over other apps without hiding or stopping the floating overlay service.
 */
class BubbleMediaBridgeActivity : ComponentActivity() {

    private var chatId: String = ""
    private var actionKind: String = ACTION_PHOTO_GALLERY
    private var cameraOutputUri: Uri? = null

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchRequestedPickerOrCamera()
        } else {
            finishWithoutAnimation()
        }
    }

    private val requestMicPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        finishWithoutAnimation()
    }

    private val requestCallPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        finishWithoutAnimation()
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraOutputUri
        if (success && uri != null) {
            MessengerBubbleManager.deliverBubbleMediaResult(
                chatId = chatId,
                uri = uri,
                isVideo = false,
                isDocument = false
            )
        }
        finishWithoutAnimation()
    }

    private val captureVideoLauncher = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        val uri = cameraOutputUri
        if (success && uri != null) {
            MessengerBubbleManager.deliverBubbleMediaResult(
                chatId = chatId,
                uri = uri,
                isVideo = true,
                isDocument = false
            )
        }
        finishWithoutAnimation()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val isVid = FileUtils.isVideoUri(this, uri)
            MessengerBubbleManager.deliverBubbleMediaResult(
                chatId = chatId,
                uri = uri,
                isVideo = isVid,
                isDocument = false
            )
        }
        finishWithoutAnimation()
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            MessengerBubbleManager.deliverBubbleMediaResult(
                chatId = chatId,
                uri = uri,
                isVideo = true,
                isDocument = false
            )
        }
        finishWithoutAnimation()
    }

    private val pickDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val (name, size) = FileUtils.getFileNameAndSize(this, uri)
            val mime = FileUtils.getMimeType(this, uri, name)
            val copiedUri = FileUtils.copyUriToLocalStorage(this, uri, name)
            MessengerBubbleManager.deliverBubbleMediaResult(
                chatId = chatId,
                uri = copiedUri,
                isVideo = false,
                isDocument = true,
                fileName = name,
                fileSize = size,
                mimeType = mime
            )
        }
        finishWithoutAnimation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra(EXTRA_CHAT_ID).orEmpty()
        actionKind = intent.getStringExtra(EXTRA_ACTION) ?: ACTION_PHOTO_GALLERY

        when (actionKind) {
            ACTION_REQUEST_MIC -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    finishWithoutAnimation()
                } else {
                    requestMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            ACTION_REQUEST_CALL_PERMS -> {
                val needed = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                    .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
                if (needed.isEmpty()) {
                    finishWithoutAnimation()
                } else {
                    requestCallPermissionsLauncher.launch(needed.toTypedArray())
                }
            }
            ACTION_PHOTO_CAMERA, ACTION_VIDEO_CAMERA -> {
                val hasCam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (hasCam) {
                    launchRequestedPickerOrCamera()
                } else {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            else -> launchRequestedPickerOrCamera()
        }
    }

    private fun launchRequestedPickerOrCamera() {
        try {
            when (actionKind) {
                ACTION_PHOTO_CAMERA -> {
                    val file = File(cacheDir, "bubble_cam_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                    cameraOutputUri = uri
                    takePhotoLauncher.launch(uri)
                }
                ACTION_VIDEO_CAMERA -> {
                    val file = File(cacheDir, "bubble_vid_${System.currentTimeMillis()}.mp4")
                    val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                    cameraOutputUri = uri
                    captureVideoLauncher.launch(uri)
                }
                ACTION_PHOTO_GALLERY -> {
                    pickImageLauncher.launch("image/*")
                }
                ACTION_VIDEO_GALLERY -> {
                    pickVideoLauncher.launch("video/*")
                }
                ACTION_DOCUMENT -> {
                    pickDocumentLauncher.launch(arrayOf("*/*"))
                }
                else -> finishWithoutAnimation()
            }
        } catch (_: Exception) {
            finishWithoutAnimation()
        }
    }

    private fun finishWithoutAnimation() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_ACTION = "extra_bubble_media_action"
        const val EXTRA_CHAT_ID = "extra_bubble_chat_id"

        const val ACTION_PHOTO_CAMERA = "PHOTO_CAMERA"
        const val ACTION_PHOTO_GALLERY = "PHOTO_GALLERY"
        const val ACTION_VIDEO_CAMERA = "VIDEO_CAMERA"
        const val ACTION_VIDEO_GALLERY = "VIDEO_GALLERY"
        const val ACTION_DOCUMENT = "DOCUMENT"
        const val ACTION_REQUEST_MIC = "REQUEST_MIC"
        const val ACTION_REQUEST_CALL_PERMS = "REQUEST_CALL_PERMS"
    }
}
