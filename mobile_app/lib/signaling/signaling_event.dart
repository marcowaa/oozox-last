enum SignalingConnectionState {
  connecting,
  connected,
  reconnecting,
  disconnected,
}

sealed class SignalingEvent {
  const SignalingEvent();

  factory SignalingEvent.fromJson(Map<String, dynamic> json) {
    final type = json['type'] as String?;
    switch (type) {
      case 'match_found':
        final payload = json['payload'] as Map<String, dynamic>? ?? {};
        return MatchFoundEvent(
          peerId: payload['peer_id'] as String? ?? '',
          isPolite: payload['is_polite'] as bool? ?? false,
        );
      case 'error':
        return ErrorEvent(
          code: json['code'] as String? ?? 'ERROR',
          message: json['message'] as String? ?? json['error'] as String? ?? 'Unknown error',
        );
      case 'signal':
        return SignalEvent(
          fromUserId: json['from'] as String? ?? json['from_user_id'] as String? ?? '',
          payload: json['payload'] as Map<String, dynamic>? ?? json,
        );
      default:
        // Default to SignalEvent for arbitrary signaling payloads
        return SignalEvent(
          fromUserId: json['from'] as String? ?? '',
          payload: json,
        );
    }
  }
}

final class MatchFoundEvent extends SignalingEvent {
  final String peerId;
  final bool isPolite;

  const MatchFoundEvent({
    required this.peerId,
    required this.isPolite,
  });

  @override
  String toString() => 'MatchFoundEvent(peerId: $peerId, isPolite: $isPolite)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is MatchFoundEvent &&
          runtimeType == other.runtimeType &&
          peerId == other.peerId &&
          isPolite == other.isPolite;

  @override
  int get hashCode => peerId.hashCode ^ isPolite.hashCode;
}

final class SignalEvent extends SignalingEvent {
  final String fromUserId;
  final Map<String, dynamic> payload;

  const SignalEvent({
    required this.fromUserId,
    required this.payload,
  });

  @override
  String toString() => 'SignalEvent(fromUserId: $fromUserId, payload: $payload)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is SignalEvent &&
          runtimeType == other.runtimeType &&
          fromUserId == other.fromUserId;

  @override
  int get hashCode => fromUserId.hashCode ^ payload.hashCode;
}

final class ErrorEvent extends SignalingEvent {
  final String code;
  final String message;

  const ErrorEvent({
    required this.code,
    required this.message,
  });

  @override
  String toString() => 'ErrorEvent(code: $code, message: $message)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ErrorEvent &&
          runtimeType == other.runtimeType &&
          code == other.code &&
          message == other.message;

  @override
  int get hashCode => code.hashCode ^ message.hashCode;
}

final class ConnectionStateEvent extends SignalingEvent {
  final SignalingConnectionState state;

  const ConnectionStateEvent({
    required this.state,
  });

  @override
  String toString() => 'ConnectionStateEvent(state: $state)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ConnectionStateEvent &&
          runtimeType == other.runtimeType &&
          state == other.state;

  @override
  int get hashCode => state.hashCode;
}
