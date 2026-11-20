import 'dart:convert';
import 'dart:developer' as developer;

import 'package:http/http.dart' as http;

import '../config/app_config.dart';

/// Fetches dynamic TURN/STUN server credentials from the Ayer backend.
class TurnService {
  static const List<Map<String, dynamic>> defaultIceServers = [
    {'urls': 'stun:stun.l.google.com:19302'},
  ];

  /// Fetches ephemeral TURN/STUN credentials from the backend signaling service.
  /// Falls back gracefully to public Google STUN upon network or service unavailability.
  static Future<List<Map<String, dynamic>>> fetchIceServers({
    required String jwtToken,
  }) async {
    final uri = Uri.parse('${AppConfig.signalingBaseUrl}/turn-credentials');
    try {
      final response = await http
          .post(
            uri,
            headers: {
              'Authorization': 'Bearer $jwtToken',
              'Content-Type': 'application/json',
            },
          )
          .timeout(const Duration(seconds: 4));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        final urls = data['urls'];
        final username = data['username'];
        final credential = data['credential'];

        final servers = <Map<String, dynamic>>[
          {'urls': 'stun:stun.l.google.com:19302'},
        ];

        if (urls is List) {
          for (final url in urls) {
            servers.add({
              'urls': url.toString(),
              if (username != null) 'username': username.toString(),
              if (credential != null) 'credential': credential.toString(),
            });
          }
        } else if (urls is String) {
          servers.add({
            'urls': urls,
            if (username != null) 'username': username.toString(),
            if (credential != null) 'credential': credential.toString(),
          });
        }

        return servers;
      } else {
        developer.log(
          'TURN credentials endpoint returned status ${response.statusCode}: ${response.body}',
          name: 'TurnService',
        );
      }
    } catch (e) {
      developer.log(
        'Failed to fetch TURN credentials, using fallback STUN: $e',
        name: 'TurnService',
      );
    }

    return List<Map<String, dynamic>>.from(defaultIceServers);
  }
}
