import 'package:mobile_app/signaling/signaling_client.dart';
import 'package:mobile_app/webrtc/webrtc_manager.dart';
import 'package:mobile_app/webrtc/webrtc_signaling_bridge.dart';
import 'package:test/test.dart';

void main() {
  group('WebRTCSignalingBridge Unit Tests', () {
    late WebRTCManager webrtcManager;
    late SignalingClient signalingClient;
    late WebRTCSignalingBridge bridge;

    setUp(() {
      webrtcManager = WebRTCManager(iceServers: []);
      signalingClient = SignalingClient(
        baseUrl: 'http://127.0.0.1:8080',
        jwtToken: 'test-token',
        userId: 'alice',
      );
      bridge = WebRTCSignalingBridge(
        webrtcManager: webrtcManager,
        signalingClient: signalingClient,
        remoteUserId: 'bob',
      );
    });

    tearDown(() async {
      await bridge.hangUp();
      await signalingClient.dispose();
    });

    test('1. Bridge instantiates cleanly with correct fields', () {
      expect(bridge.webrtcManager, equals(webrtcManager));
      expect(bridge.signalingClient, equals(signalingClient));
      expect(bridge.remoteUserId, equals('bob'));
      expect(bridge.isDisposed, isFalse);
    });

    test('2. Streams are forwarded from webrtcManager', () {
      expect(bridge.onRemoteStream, isNotNull);
      expect(bridge.onConnectionState, isNotNull);
    });

    test('3. hangUp marks bridge as disposed and closes resources', () async {
      expect(bridge.isDisposed, isFalse);
      await expectLater(bridge.hangUp(), completes);
      expect(bridge.isDisposed, isTrue);

      // Verify that calling start on disposed bridge throws StateError
      await expectLater(
        bridge.start(isPolite: true, withVideo: false),
        throwsStateError,
      );
    });
  });
}
