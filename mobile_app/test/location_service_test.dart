import 'package:flutter_test/flutter_test.dart';
import 'package:mobile_app/services/location_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('LocationService & Privacy Best Practice Tests', () {
    test('1. GeographicContext holds countryCode and optional regionName', () {
      const geo = GeographicContext(countryCode: 'EG', regionName: 'ar');
      expect(geo.countryCode, equals('EG'));
      expect(geo.regionName, equals('ar'));
      expect(geo.toString(), contains('countryCode: EG'));
    });

    test('2. LocationPermissionStatus enum covers all runtime outcomes', () {
      expect(LocationPermissionStatus.values.length, equals(4));
      expect(LocationPermissionStatus.values, contains(LocationPermissionStatus.granted));
      expect(LocationPermissionStatus.values, contains(LocationPermissionStatus.denied));
      expect(LocationPermissionStatus.values, contains(LocationPermissionStatus.permanentlyDenied));
      expect(LocationPermissionStatus.values, contains(LocationPermissionStatus.serviceDisabled));
    });

    test('3. checkAndRequestPermission gracefully handles mock/test environment without crashing', () async {
      final status = await LocationService.checkAndRequestPermission();
      // In unit test environment without registered mock channel, it safely falls back to granted or denied
      expect(status, isA<LocationPermissionStatus>());
    });

    test('4. resolveGeographicContext returns valid fallback country code', () async {
      final context = await LocationService.resolveGeographicContext();
      expect(context, isNotNull);
      expect(context!.countryCode, isNotEmpty);
      expect(context.countryCode.length, inInclusiveRange(2, 3));
    });
  });
}
