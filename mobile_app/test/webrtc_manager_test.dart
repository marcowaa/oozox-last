import 'package:mobile_app/webrtc/webrtc_manager.dart';
import 'package:test/test.dart';

void main() {
  group('WebRTCManager Unit Tests', () {
    test('WebRTCManager instantiates without throwing', () {
      final iceServers = [
        {'urls': 'stun:stun.l.google.com:19302'},
        {
          'urls': 'turn:turn.ayer.app:3478?transport=udp',
          'username': '12345678:alice',
          'credential': 'secret-credential',
        },
      ];

      final manager = WebRTCManager(iceServers: iceServers);
      expect(manager, isNotNull);
      expect(manager.iceServers, equals(iceServers));
    });

    test('dispose on manager without initialize does not throw and closes stream controllers', () async {
      final manager = WebRTCManager(iceServers: []);
      await expectLater(manager.dispose(), completes);

      // Verify streams are closed by checking that listeners receive done event
      expect(manager.onLocalSdp.isEmpty, completion(isTrue));
      expect(manager.onLocalIceCandidate.isEmpty, completion(isTrue));
      expect(manager.onRemoteStream.isEmpty, completion(isTrue));
      expect(manager.onConnectionState.isEmpty, completion(isTrue));
      expect(manager.onIceConnectionState.isEmpty, completion(isTrue));
    });

    test('setMuted on manager without local stream does not throw', () async {
      final manager = WebRTCManager(iceServers: []);
      await expectLater(manager.setMuted(true), completes);
      await expectLater(manager.setMuted(false), completes);
      await manager.dispose();
    });

    test('setCameraEnabled on manager without local stream does not throw', () async {
      final manager = WebRTCManager(iceServers: []);
      await expectLater(manager.setCameraEnabled(false), completes);
      await expectLater(manager.setCameraEnabled(true), completes);
      await manager.dispose();
    });

    test('switchCamera on manager without local stream does not throw', () async {
      final manager = WebRTCManager(iceServers: []);
      await expectLater(manager.switchCamera(), completes);
      await manager.dispose();
    });
  });
}
