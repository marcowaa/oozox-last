import 'dart:async';
import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter_webrtc/flutter_webrtc.dart';

import '../signaling/signaling_client.dart';
import '../signaling/signaling_event.dart';
import 'webrtc_manager.dart';

/// Bridges [WebRTCManager] and [SignalingClient] to coordinate offer/answer
/// exchange and ICE candidate trickle for peer-to-peer WebRTC sessions.
class WebRTCSignalingBridge {
  final WebRTCManager webrtcManager;
  final SignalingClient signalingClient;
  final String remoteUserId;

  StreamSubscription? _sdpSub;
  StreamSubscription? _iceSub;
  StreamSubscription? _eventsSub;
  bool _disposed = false;

  WebRTCSignalingBridge({
    required this.webrtcManager,
    required this.signalingClient,
    required this.remoteUserId,
  });

  /// Forwarded remote media stream from [webrtcManager].
  Stream<MediaStream> get onRemoteStream => webrtcManager.onRemoteStream;

  /// Forwarded peer connection state stream from [webrtcManager].
  Stream<RTCPeerConnectionState> get onConnectionState =>
      webrtcManager.onConnectionState;

  /// Whether the bridge has been disposed/hung up.
  bool get isDisposed => _disposed;

  /// Starts the WebRTC session by initializing peer connection, local media,
  /// and wire listeners for SDP and ICE candidates.
  ///
  /// If [isPolite] is `false` (the impolite / caller peer), this automatically
  /// initiates the session by creating and setting the initial offer.
  Future<void> start({required bool isPolite, required bool withVideo}) async {
    if (_disposed) {
      throw StateError('Cannot start a disposed WebRTCSignalingBridge');
    }

    // 1. Initialize peer connection with polite/impolite role
    await webrtcManager.initialize(isPolite: isPolite);

    // 2. Listen for local SDP (offers/answers) and transmit via signaling
    _sdpSub = webrtcManager.onLocalSdp.listen((sdpJson) async {
      if (_disposed) return;
      try {
        final decodedSdp = jsonDecode(sdpJson) as Map<String, dynamic>;
        await signalingClient.sendSignal(remoteUserId, {
          'kind': 'sdp',
          'sdp': decodedSdp,
        });
      } catch (e) {
        developer.log('Failed to send local SDP signal: $e',
            name: 'WebRTCSignalingBridge');
      }
    });

    // 3. Listen for locally gathered ICE candidates and send to peer
    _iceSub = webrtcManager.onLocalIceCandidate.listen((candidate) async {
      if (_disposed) return;
      try {
        await signalingClient.sendSignal(remoteUserId, {
          'kind': 'ice',
          'candidate': candidate,
        });
      } catch (e) {
        developer.log('Failed to send local ICE signal: $e',
            name: 'WebRTCSignalingBridge');
      }
    });

    // 4. Listen for incoming signaling events from the signaling server
    _eventsSub = signalingClient.events.listen((event) async {
      if (_disposed) return;
      if (event is! SignalEvent) return;

      // Ensure signal is from the intended remote peer if sender is identified
      if (event.fromUserId.isNotEmpty && event.fromUserId != remoteUserId) {
        return;
      }

      final payload = event.payload;
      final kind = payload['kind'] as String?;

      if (kind == 'sdp') {
        final sdpMap = payload['sdp'] as Map<String, dynamic>?;
        if (sdpMap != null) {
          final type = sdpMap['type'] as String?;
          final sdpStr = sdpMap['sdp'] as String?;
          if (type == 'offer' && sdpStr != null) {
            await webrtcManager.handleRemoteOffer(sdpStr);
          } else if (type == 'answer' && sdpStr != null) {
            await webrtcManager.handleRemoteAnswer(sdpStr);
          }
        }
      } else if (kind == 'ice') {
        final candidate = payload['candidate'] as Map<String, dynamic>?;
        if (candidate != null) {
          await webrtcManager.addIceCandidate(candidate);
        }
      }
    });

    // 5. Configure local audio and video media tracks (may trigger onRenegotiationNeeded)
    await webrtcManager.setLocalMedia(audio: true, video: withVideo);

    // 6. If impolite peer, initiate call by creating initial offer
    if (!isPolite) {
      await webrtcManager.createOffer();
    }
  }

  /// Terminates the call, cancels all stream subscriptions, and releases
  /// WebRTC resources.
  Future<void> hangUp() async {
    _disposed = true;

    await _sdpSub?.cancel();
    _sdpSub = null;

    await _iceSub?.cancel();
    _iceSub = null;

    await _eventsSub?.cancel();
    _eventsSub = null;

    await webrtcManager.dispose();
  }
}
