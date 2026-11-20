import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'signaling_event.dart';

typedef WebSocketConnector = Future<WebSocket> Function(Uri uri);

class SignalingClient {
  final String baseUrl;
  final String jwtToken;
  final String userId;

  final List<Duration> _backoffSchedule;
  final WebSocketConnector _connector;

  WebSocket? _socket;
  StreamSubscription? _socketSub;
  Timer? _reconnectTimer;

  bool _isClosed = false;
  bool _isConnecting = false;
  int _reconnectIndex = 0;
  SignalingConnectionState _currentState = SignalingConnectionState.disconnected;

  final StreamController<SignalingEvent> _eventController =
      StreamController<SignalingEvent>.broadcast();

  static const List<Duration> defaultBackoffSchedule = [
    Duration(seconds: 1),
    Duration(seconds: 2),
    Duration(seconds: 4),
    Duration(seconds: 8),
    Duration(seconds: 16),
    Duration(seconds: 30),
  ];

  SignalingClient({
    required this.baseUrl,
    required this.jwtToken,
    required this.userId,
    List<Duration>? backoffSchedule,
    WebSocketConnector? connector,
  })  : _backoffSchedule = backoffSchedule ?? defaultBackoffSchedule,
        _connector = connector ?? _defaultConnect;

  static Future<WebSocket> _defaultConnect(Uri uri) {
    return WebSocket.connect(uri.toString());
  }

  Stream<SignalingEvent> get events => _eventController.stream;

  SignalingConnectionState get state => _currentState;

  Uri _buildWsUri() {
    var uri = Uri.parse(baseUrl);
    String scheme = uri.scheme;
    if (scheme == 'http') {
      scheme = 'ws';
    } else if (scheme == 'https') {
      scheme = 'wss';
    } else if (scheme.isEmpty) {
      scheme = 'ws';
    }

    var path = uri.path;
    if (path.isEmpty || path == '/') {
      path = '/ws';
    } else if (!path.endsWith('/ws')) {
      path = path.endsWith('/') ? '${path}ws' : '$path/ws';
    }

    final queryParams = Map<String, String>.from(uri.queryParameters);
    queryParams['token'] = jwtToken;

    return uri.replace(
      scheme: scheme,
      path: path,
      queryParameters: queryParams,
    );
  }

  Future<void> connect() async {
    if (_isClosed) {
      _isClosed = false;
    }
    if (_currentState == SignalingConnectionState.connected || _isConnecting) {
      return;
    }

    _isConnecting = true;
    _updateState(
      _reconnectIndex == 0
          ? SignalingConnectionState.connecting
          : SignalingConnectionState.reconnecting,
    );

    try {
      final wsUri = _buildWsUri();
      final socket = await _connector(wsUri);

      if (_isClosed) {
        await socket.close();
        _updateState(SignalingConnectionState.disconnected);
        _isConnecting = false;
        return;
      }

      _socket = socket;
      // Configure native WebSocket ping frames every 25 seconds
      _socket?.pingInterval = const Duration(seconds: 25);

      _reconnectIndex = 0;
      _isConnecting = false;
      _updateState(SignalingConnectionState.connected);

      _socketSub = _socket!.listen(
        _onMessage,
        onError: _onError,
        onDone: _onDone,
        cancelOnError: true,
      );
    } catch (e) {
      _isConnecting = false;
      _eventController.add(
        ErrorEvent(code: 'CONNECTION_FAILED', message: e.toString()),
      );
      if (!_isClosed) {
        _scheduleReconnect();
      } else {
        _updateState(SignalingConnectionState.disconnected);
      }
    }
  }

  void _onMessage(dynamic raw) {
    try {
      final String text;
      if (raw is String) {
        text = raw;
      } else if (raw is List<int>) {
        text = utf8.decode(raw);
      } else {
        text = raw.toString();
      }

      final dynamic decoded = jsonDecode(text);
      if (decoded is Map<String, dynamic>) {
        final event = SignalingEvent.fromJson(decoded);
        _eventController.add(event);
      }
    } catch (e) {
      _eventController.add(
        ErrorEvent(code: 'MESSAGE_PARSE_ERROR', message: e.toString()),
      );
    }
  }

  void _onError(dynamic error) {
    _eventController.add(
      ErrorEvent(code: 'WS_ERROR', message: error.toString()),
    );
    _cleanupSocket();
    if (!_isClosed) {
      _scheduleReconnect();
    }
  }

  void _onDone() {
    _cleanupSocket();
    if (!_isClosed) {
      _scheduleReconnect();
    } else {
      _updateState(SignalingConnectionState.disconnected);
    }
  }

  void _scheduleReconnect() {
    if (_isClosed) return;

    _updateState(SignalingConnectionState.reconnecting);

    final delay = _reconnectIndex < _backoffSchedule.length
        ? _backoffSchedule[_reconnectIndex]
        : _backoffSchedule.last;

    _reconnectIndex = (_reconnectIndex + 1).clamp(0, _backoffSchedule.length - 1);

    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(delay, () {
      if (!_isClosed) {
        connect();
      }
    });
  }

  Future<void> sendSignal(String targetUserId, Map<String, dynamic> payload) async {
    if (_socket == null || _currentState != SignalingConnectionState.connected) {
      throw StateError('Cannot send signal: WebSocket is not connected');
    }

    final message = {
      'target': targetUserId,
      'from': userId,
      'type': 'signal',
      'payload': payload,
    };

    _socket!.add(jsonEncode(message));
  }

  Future<void> disconnect() async {
    _isClosed = true;
    _isConnecting = false;

    _reconnectTimer?.cancel();
    _reconnectTimer = null;

    await _cleanupSocket();

    _reconnectIndex = 0;
    _updateState(SignalingConnectionState.disconnected);
  }

  Future<void> _cleanupSocket() async {
    await _socketSub?.cancel();
    _socketSub = null;

    try {
      await _socket?.close();
    } catch (_) {}
    _socket = null;
  }

  void _updateState(SignalingConnectionState newState) {
    if (_currentState == newState) return;
    _currentState = newState;
    _eventController.add(ConnectionStateEvent(state: newState));
  }

  Future<void> dispose() async {
    await disconnect();
    await _eventController.close();
  }
}
