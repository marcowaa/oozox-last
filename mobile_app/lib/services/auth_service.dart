import 'dart:convert';
import 'dart:developer' as developer;

import 'package:http/http.dart' as http;

/// Service responsible for acquiring development authentication tokens from the backend.
class AuthService {
  /// Requests a valid development JWT for [userId] from the signaling backend.
  /// Returns null upon timeout, network error, or server refusal.
  static Future<String?> fetchToken({
    required String baseUrl,
    required String userId,
  }) async {
    final uri = Uri.parse('$baseUrl/auth/token');
    try {
      final response = await http
          .post(
            uri,
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'user_id': userId.trim()}),
          )
          .timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        final token = data['token'] as String?;
        if (token != null && token.isNotEmpty) {
          return token;
        }
        developer.log(
          'Token response contained empty or missing token: ${response.body}',
          name: 'AuthService',
        );
      } else {
        developer.log(
          'Auth token endpoint returned status ${response.statusCode}: ${response.body}',
          name: 'AuthService',
        );
      }
    } catch (e) {
      developer.log(
        'Failed to fetch auth token for $userId: $e',
        name: 'AuthService',
      );
    }
    return null;
  }
}
