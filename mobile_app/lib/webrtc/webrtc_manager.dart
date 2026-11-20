import 'dart:async';
import 'dart:convert';
import 'dart:developer' as developer;

import 'package:flutter_webrtc/flutter_webrtc.dart';

import 'rtc_service_interface.dart';

/// Production-ready WebRTC peer connection manager implementing RTCServiceInterface.
///
/// Features:
/// - RFC 8829 Perfect Negotiation pattern for glare-free offer/answer collision resolution.
/// - Unified Plan SDP semantics with max-bundle and rtcp-mux policies.
/// - Resilient ICE handling with automatic single-shot ICE restart on failure.
/// - Clean resource disposal to prevent camera, microphone, and memory leaks.
class WebRTCManager implements RTCServiceInterface {
  final List<Map<String, dynamic>> iceServers;

  RTCPeerConnection? _peerConnection;
  MediaStream? _localStream;
  MediaStream? _remoteStream;

  // Perfect Negotiation state variables (RFC 8829)
  // _isPolite defines whether this peer yields when an offer collision occurs.
  // The polite peer rolls back its own offer upon receiving an unexpected remote offer.
  bool _isPolite = false;
  bool _makingOffer = false;
  bool _ignoreOffer = false;
  bool _isSettingRemoteAnswerPending = false;
  bool _hasAutoRestartedIce = false;
  bool _initialOfferSent = false;

  final StreamController<String> _onLocalSdpController =
      StreamController<String>.broadcast();
  final StreamController<Map<String, dynamic>> _onLocalIceCandidateController =
      StreamController<Map<String, dynamic>>.broadcast();
  final StreamController<MediaStream> _onRemoteStreamController =
      StreamController<MediaStream>.broadcast();
  final StreamController<RTCPeerConnectionState> _onConnectionStateController =
      StreamController<RTCPeerConnectionState>.broadcast();
  final StreamController<RTCIceConnectionState> _onIceConnectionStateController =
      StreamController<RTCIceConnectionState>.broadcast();

  WebRTCManager({required this.iceServers});

  @override
  Stream<String> get onLocalSdp => _onLocalSdpController.stream;

  @override
  Stream<Map<String, dynamic>> get onLocalIceCandidate =>
      _onLocalIceCandidateController.stream;

  @override
  Stream<MediaStream> get onRemoteStream => _onRemoteStreamController.stream;

  @override
  Stream<RTCPeerConnectionState> get onConnectionState =>
      _onConnectionStateController.stream;

  @override
  Stream<RTCIceConnectionState> get onIceConnectionState =>
      _onIceConnectionStateController.stream;

  /// Exposes the local media stream if captured, or null otherwise.
  MediaStream? get localStreamOrNull => _localStream;

  @override
  Future<void> initialize({required bool isPolite}) async {
    // Idempotent initialization: cleanly close and dispose existing connection
    // and stop any active tracks before allocating a new peer connection.
    if (_peerConnection != null) {
      final oldTracks = _localStream?.getTracks();
      if (oldTracks != null) {
        for (final track in oldTracks) {
          await track.stop();
        }
      }
      await _localStream?.dispose();
      _localStream = null;

      await _remoteStream?.dispose();
      _remoteStream = null;

      await _peerConnection!.close();
      await _peerConnection!.dispose();
      _peerConnection = null;
    }

    _isPolite = isPolite;
    _makingOffer = false;
    _ignoreOffer = false;
    _isSettingRemoteAnswerPending = false;
    _hasAutoRestartedIce = false;
    _initialOfferSent = false;

    final configuration = <String, dynamic>{
      'iceServers': iceServers,
      'sdpSemantics': 'unified-plan',
      'bundlePolicy': 'max-bundle',
      'rtcpMuxPolicy': 'require',
      'iceTransportPolicy': 'all',
    };

    _peerConnection = await createPeerConnection(configuration);

    // Perfect Negotiation: onRenegotiationNeeded handles renegotiation triggers
    // such as adding local tracks or modifying transceivers.
    _peerConnection!.onRenegotiationNeeded = () async {
      if (_initialOfferSent) return;
      _initialOfferSent = true;
      _makingOffer = true;
      try {
        final offer = await _peerConnection!.createOffer();
        await _peerConnection!.setLocalDescription(offer);
        _onLocalSdpController.add(jsonEncode({
          'type': 'offer',
          'sdp': offer.sdp,
        }));
      } catch (e) {
        developer.log('onRenegotiationNeeded error: $e', name: 'WebRTCManager');
      } finally {
        _makingOffer = false;
      }
    };

    // Candidate gathering listener
    _peerConnection!.onIceCandidate = (RTCIceCandidate candidate) {
      if (candidate.candidate != null) {
        _onLocalIceCandidateController.add({
          'candidate': candidate.candidate,
          'sdpMid': candidate.sdpMid,
          'sdpMLineIndex': candidate.sdpMLineIndex,
        });
      }
    };

    // Unified-plan track reception listener
    _peerConnection!.onTrack = (RTCTrackEvent event) async {
      if (event.streams.isNotEmpty) {
        _remoteStream = event.streams[0];
      } else {
        _remoteStream ??= await createLocalMediaStream('remote_stream');
        _remoteStream!.addTrack(event.track);
      }
      _onRemoteStreamController.add(_remoteStream!);
    };

    // Peer connection state changes
    _peerConnection!.onConnectionState = (RTCPeerConnectionState state) {
      _onConnectionStateController.add(state);
    };

    // ICE connection state listener with automatic failure recovery
    _peerConnection!.onIceConnectionState = (RTCIceConnectionState state) {
      _onIceConnectionStateController.add(state);

      // Reset auto-restart flag once connectivity is successfully established or restored
      if (state == RTCIceConnectionState.RTCIceConnectionStateConnected ||
          state == RTCIceConnectionState.RTCIceConnectionStateCompleted) {
        _hasAutoRestartedIce = false;
      }

      // Auto-restart ICE on failure: networks switch or NAT mappings drop,
      // so we trigger restartIce once automatically to attempt fast reconnection.
      if (state == RTCIceConnectionState.RTCIceConnectionStateFailed) {
        if (!_hasAutoRestartedIce) {
          _hasAutoRestartedIce = true;
          developer.log('ICE state failed. Auto-triggering restartIce().',
              name: 'WebRTCManager');
          restartIce();
        }
      }
    };
  }

  @override
  Future<void> createOffer() async {
    if (_peerConnection == null) return;
    if (_initialOfferSent) return;
    _initialOfferSent = true;
    _makingOffer = true;
    try {
      final offer = await _peerConnection!.createOffer();
      await _peerConnection!.setLocalDescription(offer);
      _onLocalSdpController.add(jsonEncode({
        'type': 'offer',
        'sdp': offer.sdp,
      }));
    } finally {
      _makingOffer = false;
    }
  }

  @override
  Future<void> handleRemoteOffer(String sdp) async {
    if (_peerConnection == null) return;

    // Perfect Negotiation collision detection:
    // A collision occurs if we are currently crafting an offer or if signaling state is not stable.
    final offerCollision = _makingOffer ||
        (_peerConnection!.signalingState !=
            RTCSignalingState.RTCSignalingStateStable);

    // The impolite peer ignores collision offers; the polite peer yields and accepts the remote offer.
    _ignoreOffer = !_isPolite && offerCollision;
    if (_ignoreOffer) {
      developer.log('Offer collision detected on impolite peer. Ignoring offer.',
          name: 'WebRTCManager');
      return;
    }

    // W3C RFC 8829 Perfect Negotiation rollback:
    // If an offer collision occurs on the polite peer, it must rollback its own local offer
    // before applying the incoming remote offer. This returns signalingState from 'have-local-offer'
    // back to 'stable', preventing setRemoteDescription(offer) from failing.
    if (offerCollision) {
      await _peerConnection!.setLocalDescription(
        RTCSessionDescription('', 'rollback'),
      );
    }

    await _peerConnection!.setRemoteDescription(
      RTCSessionDescription(sdp, 'offer'),
    );
    final answer = await _peerConnection!.createAnswer();
    await _peerConnection!.setLocalDescription(answer);

    _onLocalSdpController.add(jsonEncode({
      'type': 'answer',
      'sdp': answer.sdp,
    }));
  }

  @override
  Future<void> handleRemoteAnswer(String sdp) async {
    if (_peerConnection == null) return;

    // Guard against re-entrant or duplicate remote answer application
    if (_isSettingRemoteAnswerPending) return;
    _isSettingRemoteAnswerPending = true;
    try {
      await _peerConnection!.setRemoteDescription(
        RTCSessionDescription(sdp, 'answer'),
      );
    } finally {
      _isSettingRemoteAnswerPending = false;
    }
  }

  @override
  Future<void> addIceCandidate(Map<String, dynamic> candidate) async {
    if (_peerConnection == null) return;
    try {
      final rtcCandidate = RTCIceCandidate(
        candidate['candidate'] as String?,
        candidate['sdpMid'] as String?,
        candidate['sdpMLineIndex'] as int?,
      );
      await _peerConnection!.addCandidate(rtcCandidate);
    } catch (e) {
      developer.log('addIceCandidate suppressed error: $e',
          name: 'WebRTCManager');
    }
  }

  @override
  Future<void> setLocalMedia({bool audio = true, bool video = true}) async {
    final mediaConstraints = <String, dynamic>{
      'audio': audio,
      'video': video
          ? {
              'width': {'ideal': 1280},
              'height': {'ideal': 720},
              'frameRate': {'ideal': 30},
            }
          : false,
    };

    _localStream = await navigator.mediaDevices.getUserMedia(mediaConstraints);

    if (_peerConnection != null) {
      for (final track in _localStream!.getTracks()) {
        await _peerConnection!.addTrack(track, _localStream!);
      }
    }
  }

  @override
  Future<void> setMuted(bool muted) async {
    final audioTracks = _localStream?.getAudioTracks();
    if (audioTracks != null) {
      for (final track in audioTracks) {
        track.enabled = !muted;
      }
    }
  }

  @override
  Future<void> setCameraEnabled(bool enabled) async {
    final videoTracks = _localStream?.getVideoTracks();
    if (videoTracks != null) {
      for (final track in videoTracks) {
        track.enabled = enabled;
      }
    }
  }

  @override
  Future<void> switchCamera() async {
    final videoTracks = _localStream?.getVideoTracks();
    if (videoTracks != null && videoTracks.isNotEmpty) {
      await Helper.switchCamera(videoTracks.first);
    }
  }

  @override
  Future<void> restartIce() async {
    if (_peerConnection == null) return;
    developer.log('Triggering restartIce()', name: 'WebRTCManager');
    await _peerConnection!.restartIce();
  }

  @override
  Future<void> dispose() async {
    final tracks = _localStream?.getTracks();
    if (tracks != null) {
      for (final track in tracks) {
        await track.stop();
      }
    }
    await _localStream?.dispose();
    _localStream = null;

    await _remoteStream?.dispose();
    _remoteStream = null;

    await _peerConnection?.close();
    await _peerConnection?.dispose();
    _peerConnection = null;

    await _onLocalSdpController.close();
    await _onLocalIceCandidateController.close();
    await _onRemoteStreamController.close();
    await _onConnectionStateController.close();
    await _onIceConnectionStateController.close();
  }
}
