import 'dart:async';
import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter/foundation.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import '../services/auth_service.dart';
import '../services/turn_service.dart';
import '../signaling/signaling_client.dart';
import '../signaling/signaling_event.dart';
import '../webrtc/webrtc_manager.dart';
import '../webrtc/webrtc_signaling_bridge.dart';

/// States representing the lifecycle of matchmaking and WebRTC peer connection.
enum CallStatus {
  idle,
  requestingMatch,
  waiting,
  connecting,
  connected,
  ended,
  error,
}

/// State controller managing matchmaking, signaling events, and WebRTC call sessions.
class CallController extends ChangeNotifier {
  String? currentUserId;
  String? currentCountry;
  String? remoteUserId;
  CallStatus status = CallStatus.idle;
  String? errorMessage;
  bool isPolite = false;

  MediaStream? localStream;
  MediaStream? remoteStream;

  bool isMuted = false;
  bool isCameraEnabled = true;
  bool isFrontCamera = true;

  SignalingClient? _signalingClient;
  WebRTCManager? _webrtcManager;
  WebRTCSignalingBridge? _bridge;
  String? _jwtToken;

  StreamSubscription? _eventsSub;
  StreamSubscription? _remoteStreamSub;
  StreamSubscription? _connStateSub;

  /// Initiates matchmaking request with country filter and connects to signaling.
  Future<void> requestMatch({
    required String userId,
    required String country,
  }) async {
    currentUserId = userId.trim();
    currentCountry = country.trim().toUpperCase();
    errorMessage = null;
    status = CallStatus.requestingMatch;
    notifyListeners();

    try {
      // 1. Clean up any lingering previous session
      await _cleanup();

      // 2. Fetch authenticated JWT from backend
      _jwtToken = await AuthService.fetchToken(
        baseUrl: AppConfig.signalingBaseUrl,
        userId: currentUserId!,
      );

      if (_jwtToken == null) {
        status = CallStatus.error;
        errorMessage = 'Failed to obtain auth token';
        notifyListeners();
        return;
      }

      // 3. Connect to WebSocket signaling server
      _signalingClient = SignalingClient(
        baseUrl: AppConfig.signalingBaseUrl,
        jwtToken: _jwtToken!,
        userId: currentUserId!,
      );

      _eventsSub = _signalingClient!.events.listen((event) {
        if (event is MatchFoundEvent) {
          _startCall(peerId: event.peerId, isPolite: event.isPolite);
        } else if (event is ConnectionStateEvent) {
          if (event.state == SignalingConnectionState.disconnected &&
              status != CallStatus.idle &&
              status != CallStatus.ended) {
            status = CallStatus.error;
            errorMessage = 'Signaling disconnected';
            notifyListeners();
          }
        } else if (event is ErrorEvent) {
          status = CallStatus.error;
          errorMessage = event.message;
          notifyListeners();
        }
      });

      await _signalingClient!.connect();

      // 4. Send HTTP POST match request
      final matchUri = Uri.parse('${AppConfig.signalingBaseUrl}/match');
      final response = await http
          .post(
            matchUri,
            headers: {
              'Authorization': 'Bearer $_jwtToken',
              'Content-Type': 'application/json',
            },
            body: jsonEncode({
              'country': currentCountry,
            }),
          )
          .timeout(const Duration(seconds: 8));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        final matchStatus = data['status'] as String?;

        if (matchStatus == 'matched') {
          final peerId = data['peer_id'] as String? ?? '';
          final polite = currentUserId!.compareTo(peerId) < 0;
          await _startCall(peerId: peerId, isPolite: polite);
        } else if (matchStatus == 'waiting') {
          status = CallStatus.waiting;
          notifyListeners();
        } else {
          status = CallStatus.waiting;
          notifyListeners();
        }
      } else {
        status = CallStatus.error;
        errorMessage = 'Matchmaking request failed: ${response.statusCode}';
        notifyListeners();
      }
    } catch (e) {
      status = CallStatus.error;
      errorMessage = 'Error requesting match: $e';
      notifyListeners();
    }
  }

  /// Internal handler invoked when peer match is confirmed by the signaling backend.
  Future<void> _startCall({
    required String peerId,
    required bool isPolite,
  }) async {
    remoteUserId = peerId;
    this.isPolite = isPolite;
    status = CallStatus.connecting;
    notifyListeners();

    try {
      final iceServers = await TurnService.fetchIceServers(jwtToken: _jwtToken ?? '');
      _webrtcManager = WebRTCManager(iceServers: iceServers);

      _bridge = WebRTCSignalingBridge(
        webrtcManager: _webrtcManager!,
        signalingClient: _signalingClient!,
        remoteUserId: peerId,
      );

      _remoteStreamSub = _bridge!.onRemoteStream.listen((stream) {
        remoteStream = stream;
        notifyListeners();
      });

      _connStateSub = _bridge!.onConnectionState.listen((state) {
        if (state == RTCPeerConnectionState.RTCPeerConnectionStateConnected) {
          status = CallStatus.connected;
        } else if (state == RTCPeerConnectionState.RTCPeerConnectionStateFailed) {
          status = CallStatus.error;
          errorMessage = 'Connection failed';
        }
        notifyListeners();
      });

      await _bridge!.start(isPolite: isPolite, withVideo: true);
      localStream = _webrtcManager!.localStreamOrNull;
      notifyListeners();
    } catch (e) {
      developer.log('Failed to start call bridge: $e', name: 'CallController');
      status = CallStatus.error;
      errorMessage = 'Failed to establish call: $e';
      notifyListeners();
    }
  }

  /// Toggles microphone audio transmission.
  Future<void> toggleMute() async {
    await _webrtcManager?.setMuted(!isMuted);
    isMuted = !isMuted;
    notifyListeners();
  }

  /// Toggles video track transmission.
  Future<void> toggleCamera() async {
    await _webrtcManager?.setCameraEnabled(!isCameraEnabled);
    isCameraEnabled = !isCameraEnabled;
    notifyListeners();
  }

  /// Switches between front-facing and rear cameras.
  Future<void> switchCamera() async {
    await _webrtcManager?.switchCamera();
    isFrontCamera = !isFrontCamera;
    notifyListeners();
  }

  /// Terminates the current call and cleans up active sessions.
  Future<void> hangUp() async {
    await _bridge?.hangUp();
    await _cleanup();
    status = CallStatus.ended;
    notifyListeners();
  }

  Future<void> _cleanup() async {
    await _remoteStreamSub?.cancel();
    _remoteStreamSub = null;

    await _connStateSub?.cancel();
    _connStateSub = null;

    await _eventsSub?.cancel();
    _eventsSub = null;

    if (_signalingClient != null) {
      await _signalingClient!.dispose();
      _signalingClient = null;
    }

    _webrtcManager = null;
    _bridge = null;
    remoteStream = null;
    localStream = null;
  }

  @override
  void dispose() {
    _cleanup();
    super.dispose();
  }
}
