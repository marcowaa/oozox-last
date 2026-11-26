package com.example.util

import android.content.Context
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * 1-on-1 P2P WebRTC PeerConnection Manager backed by stream-webrtc-android (org.webrtc).
 * Manages EglBase, Hardware Audio/Video tracks, Coturn ICE Servers, SDP Offer/Answer, and ICE exchange.
 */
class WebRtcPeerManager(
    private val context: Context,
    private val isVideoCall: Boolean,
    private val iceServers: List<IceServerConfig>,
    private val onLocalIceCandidate: (IceCandidateData) -> Unit,
    private val onConnectionStateChanged: (PeerConnection.PeerConnectionState) -> Unit
) {
    companion object {
        private const val TAG = "WebRtcPeerManager"
        @Volatile
        private var factoryInitialized = false
    }

    val eglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private val pendingRemoteIce = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false

    init {
        initFactory()
        createPeerConnection()
        initLocalMedia()
    }

    private fun initFactory() {
        if (!factoryInitialized) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )
            factoryInitialized = true
        }

        val audioModule = JavaAudioDeviceModule.builder(context.applicationContext)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
        audioModule.release()
    }

    private fun createPeerConnection() {
        val factory = peerConnectionFactory ?: return
        val rtcIceList = iceServers.mapNotNull { cfg ->
            if (cfg.urls.isEmpty()) return@mapNotNull null
            PeerConnection.IceServer.builder(cfg.urls).apply {
                if (!cfg.username.isNullOrBlank()) setUsername(cfg.username)
                if (!cfg.credential.isNullOrBlank()) setPassword(cfg.credential)
            }.createIceServer()
        }

        val rtcConfig = PeerConnection.RTCConfiguration(rtcIceList).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    onLocalIceCandidate(
                        IceCandidateData(
                            sdpMid = candidate.sdpMid,
                            sdpMLineIndex = candidate.sdpMLineIndex,
                            sdpCandidate = candidate.sdp
                        )
                    )
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {
                val track = stream?.videoTracks?.firstOrNull() ?: return
                attachRemoteTrack(track)
            }

            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track() as? VideoTrack ?: return
                attachRemoteTrack(track)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                if (newState != null) {
                    onConnectionStateChanged(newState)
                }
            }
        })
    }

    private fun initLocalMedia() {
        val factory = peerConnectionFactory ?: return
        val pc = peerConnection ?: return

        val audioConstraints = MediaConstraints()
        audioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("OOZOX_AUDIO_0", audioSource).apply {
            setEnabled(true)
            pc.addTrack(this, listOf("OOZOX_STREAM"))
        }

        if (isVideoCall) {
            try {
                val enumerator = Camera2Enumerator(context.applicationContext)
                val frontCam = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                    ?: enumerator.deviceNames.firstOrNull()
                if (frontCam != null) {
                    val capturer = enumerator.createCapturer(frontCam, null)
                    if (capturer != null) {
                        videoCapturer = capturer
                        surfaceTextureHelper = SurfaceTextureHelper.create("OozoxCaptureThread", eglBase.eglBaseContext)
                        videoSource = factory.createVideoSource(capturer.isScreencast)
                        capturer.initialize(surfaceTextureHelper, context.applicationContext, videoSource?.capturerObserver)
                        capturer.startCapture(720, 1280, 30)
                        localVideoTrack = factory.createVideoTrack("OOZOX_VIDEO_0", videoSource).apply {
                            setEnabled(true)
                            localRenderer?.let { addSink(it) }
                            pc.addTrack(this, listOf("OOZOX_STREAM"))
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Camera initialization fallback (audio-only active): ${e.message}")
            }
        }
    }

    private fun attachRemoteTrack(track: VideoTrack) {
        remoteVideoTrack = track
        remoteRenderer?.let { renderer ->
            try {
                track.addSink(renderer)
            } catch (_: Exception) {
            }
        }
    }

    fun attachRenderers(localView: SurfaceViewRenderer?, remoteView: SurfaceViewRenderer?) {
        if (localView != null && localView != localRenderer) {
            localRenderer = localView
            try {
                localView.init(eglBase.eglBaseContext, null)
                localView.setMirror(true)
                localVideoTrack?.addSink(localView)
            } catch (_: Exception) {
            }
        }
        if (remoteView != null && remoteView != remoteRenderer) {
            remoteRenderer = remoteView
            try {
                remoteView.init(eglBase.eglBaseContext, null)
                remoteView.setMirror(false)
                remoteVideoTrack?.addSink(remoteView)
            } catch (_: Exception) {
            }
        }
    }

    fun createOffer(onOfferSdp: (String) -> Unit, onError: (String) -> Unit = {}) {
        val pc = peerConnection ?: return onError("PeerConnection unavailable")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
        }
        pc.createOffer(object : SdpObserverAdapter("createOffer") {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return onError("Empty SDP offer")
                pc.setLocalDescription(object : SdpObserverAdapter("setLocalOffer") {
                    override fun onSetSuccess() {
                        onOfferSdp(desc.description)
                    }
                    override fun onSetFailure(err: String?) {
                        onError(err ?: "setLocalDescription failed")
                    }
                }, desc)
            }
            override fun onCreateFailure(err: String?) {
                onError(err ?: "createOffer failed")
            }
        }, constraints)
    }

    fun handleRemoteOfferAndCreateAnswer(
        offerSdp: String,
        onAnswerSdp: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val pc = peerConnection ?: return onError("PeerConnection unavailable")
        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        pc.setRemoteDescription(object : SdpObserverAdapter("setRemoteOffer") {
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                drainPendingIce(pc)
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
                }
                pc.createAnswer(object : SdpObserverAdapter("createAnswer") {
                    override fun onCreateSuccess(ans: SessionDescription?) {
                        if (ans == null) return onError("Empty SDP answer")
                        pc.setLocalDescription(object : SdpObserverAdapter("setLocalAnswer") {
                            override fun onSetSuccess() {
                                onAnswerSdp(ans.description)
                            }
                            override fun onSetFailure(err: String?) {
                                onError(err ?: "setLocalDescription failed")
                            }
                        }, ans)
                    }
                    override fun onCreateFailure(err: String?) {
                        onError(err ?: "createAnswer failed")
                    }
                }, constraints)
            }
            override fun onSetFailure(err: String?) {
                onError(err ?: "setRemoteDescription failed")
            }
        }, remoteDesc)
    }

    fun handleRemoteAnswer(answerSdp: String, onError: (String) -> Unit = {}) {
        val pc = peerConnection ?: return
        val remoteDesc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        pc.setRemoteDescription(object : SdpObserverAdapter("setRemoteAnswer") {
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                drainPendingIce(pc)
            }
            override fun onSetFailure(err: String?) {
                onError(err ?: "setRemoteAnswer failed")
            }
        }, remoteDesc)
    }

    fun addRemoteIceCandidate(data: IceCandidateData) {
        val candidate = IceCandidate(data.sdpMid ?: "0", data.sdpMLineIndex, data.sdpCandidate)
        val pc = peerConnection ?: return
        if (isRemoteDescriptionSet) {
            pc.addIceCandidate(candidate)
        } else {
            pendingRemoteIce.add(candidate)
        }
    }

    private fun drainPendingIce(pc: PeerConnection) {
        if (pendingRemoteIce.isEmpty()) return
        val copy = pendingRemoteIce.toList()
        pendingRemoteIce.clear()
        copy.forEach { pc.addIceCandidate(it) }
    }

    fun setMicrophoneMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        try {
            videoCapturer?.switchCamera(null)
        } catch (_: Exception) {
        }
    }

    fun dispose() {
        try {
            localRenderer?.let { localVideoTrack?.removeSink(it) }
            remoteRenderer?.let { remoteVideoTrack?.removeSink(it) }
            localRenderer?.release()
            remoteRenderer?.release()
        } catch (_: Exception) {
        }
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (_: Exception) {
        }
        try {
            surfaceTextureHelper?.dispose()
            localVideoTrack?.dispose()
            videoSource?.dispose()
            localAudioTrack?.dispose()
            audioSource?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnectionFactory?.dispose()
            eglBase.release()
        } catch (_: Exception) {
        }
        peerConnection = null
        peerConnectionFactory = null
    }

    private open class SdpObserverAdapter(private val label: String) : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(err: String?) {
            Log.w(TAG, "$label create failure: $err")
        }
        override fun onSetFailure(err: String?) {
            Log.w(TAG, "$label set failure: $err")
        }
    }
}
