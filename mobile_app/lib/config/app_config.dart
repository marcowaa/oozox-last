import 'package:shared_preferences/shared_preferences.dart';

/// Centralized configuration management for the Ayer application.
class AppConfig {
  static const String _keySignalingBaseUrl = 'signaling_base_url';
  static const String defaultSignalingBaseUrl = 'http://10.0.2.2:8080';

  static SharedPreferences? _prefs;
  static String _signalingBaseUrl = defaultSignalingBaseUrl;

  /// Returns the configured signaling base URL.
  static String get signalingBaseUrl => _signalingBaseUrl;

  /// Updates and persists the signaling base URL.
  static Future<void> setSignalingBaseUrl(String url) async {
    _signalingBaseUrl = url.trim();
    if (_prefs != null) {
      await _prefs!.setString(_keySignalingBaseUrl, _signalingBaseUrl);
    }
  }

  /// Loads persisted configuration from SharedPreferences.
  /// Must be called before runApp in main().
  static Future<void> load() async {
    try {
      _prefs = await SharedPreferences.getInstance();
      final savedUrl = _prefs?.getString(_keySignalingBaseUrl);
      if (savedUrl != null && savedUrl.trim().isNotEmpty) {
        _signalingBaseUrl = savedUrl.trim();
      } else {
        _signalingBaseUrl = defaultSignalingBaseUrl;
      }
    } catch (_) {
      _signalingBaseUrl = defaultSignalingBaseUrl;
    }
  }
}
