import 'package:mobile_app/state/call_controller.dart';
import 'package:test/test.dart';

void main() {
  group('CallController Unit Tests', () {
    late CallController controller;

    setUp(() {
      controller = CallController();
    });

    tearDown(() {
      controller.dispose();
    });

    test('1. CallController instantiates cleanly without crash', () {
      expect(controller.status, equals(CallStatus.idle));
      expect(controller.currentUserId, isNull);
      expect(controller.currentCountry, isNull);
      expect(controller.remoteUserId, isNull);
      expect(controller.isMuted, isFalse);
      expect(controller.isCameraEnabled, isTrue);
      expect(controller.isFrontCamera, isTrue);
      expect(controller.localStream, isNull);
      expect(controller.remoteStream, isNull);
    });

    test('2. Media toggles without active call do not throw exceptions', () async {
      await expectLater(controller.toggleMute(), completes);
      expect(controller.isMuted, isTrue);
      await expectLater(controller.toggleMute(), completes);
      expect(controller.isMuted, isFalse);

      await expectLater(controller.toggleCamera(), completes);
      expect(controller.isCameraEnabled, isFalse);
      await expectLater(controller.toggleCamera(), completes);
      expect(controller.isCameraEnabled, isTrue);

      await expectLater(controller.switchCamera(), completes);
      expect(controller.isFrontCamera, isFalse);
    });

    test('3. hangUp on empty or idle controller completes cleanly without exception', () async {
      await expectLater(controller.hangUp(), completes);
      expect(controller.status, equals(CallStatus.ended));
    });
  });
}
