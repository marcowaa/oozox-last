import 'package:flutter_webrtc/flutter_webrtc.dart';

/// Contract definition for WebRTC peer connection management, media control,
/// and signaling exchange abstractions.
abstract class RTCServiceInterface {
  /// Initializes the peer connection, configures event handlers, and sets the polite peer role.
  Future<void> initialize({required bool isPolite});

  /// Initiates negotiation by creating a local offer description.
  Future<void> createOffer();

  /// Processes an incoming remote SDP offer with collision resolution (Perfect Negotiation).
  Future<void> handleRemoteOffer(String sdp);

  /// Processes an incoming remote SDP answer to complete negotiation.
  Future<void> handleRemoteAnswer(String sdp);

  /// Adds a received remote ICE candidate to the peer connection.
  Future<void> addIceCandidate(Map<String, dynamic> candidate);

  /// Captures local media (audio/video) and attaches tracks to the peer connection.
  Future<void> setLocalMedia({bool audio = true, bool video = true});

  /// Mutes or unmutes local microphone track(s).
  Future<void> setMuted(bool muted);

  /// Enables or disables local camera video track(s).
  Future<void> setCameraEnabled(bool enabled);

  /// Switches between front and rear cameras.
  Future<void> switchCamera();

  /// Triggers an ICE restart to re-establish connectivity upon network changes or failure.
  Future<void> restartIce();

  /// Releases peer connection, media streams, tracks, and internal stream controllers.
  Future<void> dispose();

  /// Stream emitting generated local SDP descriptions (offers and answers as JSON strings).
  Stream<String> get onLocalSdp;

  /// Stream emitting locally gathered ICE candidates.
  Stream<Map<String, dynamic>> get onLocalIceCandidate;

  /// Stream emitting remote media stream when received from the peer.
  Stream<MediaStream> get onRemoteStream;

  /// Stream emitting peer connection state changes.
  Stream<RTCPeerConnectionState> get onConnectionState;

  /// Stream emitting ICE connection state changes.
  Stream<RTCIceConnectionState> get onIceConnectionState;
}
