import 'package:flutter_test/flutter_test.dart';
import 'package:mobile_app/services/location_service.dart';
import 'package:mobile_app/state/app_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('AppController & Location Context Listener Tests', () {
    late AppController controller;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      controller = AppController();
      await controller.init();
    });

    tearDown(() {
      controller.dispose();
    });

    test('1. AppController initializes cleanly with first-attempt flags set to false', () {
      expect(controller.isInitialized, isTrue);
      expect(controller.hasPromptedLocationDialog, isFalse);
      expect(controller.isLocationAuthorized, isFalse);
      expect(controller.currentLocationContext, isNull);
      expect(controller.projectRecommendations, isNotEmpty);
      expect(controller.projectRecommendations.first.region, equals('Global'));
    });

    test('2. Location Context listener receives emitted location context updates', () async {
      GeographicContext? capturedContext;
      var listenerCallCount = 0;

      void listener(GeographicContext? ctx) {
        capturedContext = ctx;
        listenerCallCount++;
      }

      controller.addLocationContextListener(listener);

      // Trigger location context refresh via authorization setter
      await controller.setLocationAuthorization(true);

      expect(listenerCallCount, greaterThan(0));
      expect(capturedContext, isNotNull);
      expect(controller.currentLocationContext, equals(capturedContext));

      // Remove listener and ensure it is no longer notified
      controller.removeLocationContextListener(listener);
      final currentCallCount = listenerCallCount;

      await controller.setLocationAuthorization(false);
      expect(listenerCallCount, equals(currentCallCount));
    });

    test('3. Project recommendations are personalized based on authorized location context', () async {
      await controller.setLocationAuthorization(true);

      expect(controller.isLocationAuthorized, isTrue);
      expect(controller.projectRecommendations, isNotEmpty);

      // Recommendations should reflect local regional nodes or fallback
      final rec = controller.projectRecommendations.first;
      expect(rec.title, isNotEmpty);
      expect(rec.tags, isNotEmpty);
      expect(rec.relevancyScore, inInclusiveRange(0.0, 1.0));
    });

    test('4. Authorization dialog state persists to ensure it only prompts on the first attempt', () async {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool(AppController.keyHasPromptedLocationDialog, true);
      await prefs.setBool(AppController.keyIsLocationAuthorized, false);

      final secondController = AppController();
      await secondController.init();

      expect(secondController.hasPromptedLocationDialog, isTrue);
      expect(secondController.isLocationAuthorized, isFalse);

      secondController.dispose();
    });
  });
}
