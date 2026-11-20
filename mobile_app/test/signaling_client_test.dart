import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:mobile_app/signaling/signaling_client.dart';
import 'package:mobile_app/signaling/signaling_event.dart';
import 'package:test/test.dart';

void main() {
  group('SignalingClient Tests', () {
    late HttpServer server;
    late StreamController<WebSocket> serverSockets;

    setUp(() async {
      serverSockets = StreamController<WebSocket>.broadcast();
      server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      server.transform(WebSocketTransformer()).listen((socket) {
        serverSockets.add(socket);
      });
    });

    tearDown(() async {
      await serverSockets.close();
      await server.close(force: true);
    });

    test('1. Connects and sends signal correctly formatted', () async {
      final client = SignalingClient(
        baseUrl: 'http://127.0.0.1:${server.port}',
        jwtToken: 'valid-jwt-token-xyz',
        userId: 'alice',
      );

      final socketFuture = serverSockets.stream.first;
      await client.connect();
      final serverSocket = await socketFuture;

      final messageCompleter = Completer<Map<String, dynamic>>();
      serverSocket.listen((data) {
        final decoded = jsonDecode(data as String) as Map<String, dynamic>;
        messageCompleter.complete(decoded);
      });

      await client.sendSignal('bob', {'sdp': 'v=0...', 'type': 'offer'});
      final received = await messageCompleter.future.timeout(const Duration(seconds: 2));

      expect(received['target'], equals('bob'));
      expect(received['from'], equals('alice'));
      expect(received['type'], equals('signal'));
      expect(received['payload'], equals({'sdp': 'v=0...', 'type': 'offer'}));

      await client.disconnect();
    });

    test('2. Receiving match_found produces MatchFoundEvent', () async {
      final client = SignalingClient(
        baseUrl: 'http://127.0.0.1:${server.port}',
        jwtToken: 'valid-jwt-token-xyz',
        userId: 'alice',
      );

      final socketFuture = serverSockets.stream.first;
      await client.connect();
      final serverSocket = await socketFuture;

      final eventFuture = client.events.where((e) => e is MatchFoundEvent).first;

      final matchPayload = {
        'type': 'match_found',
        'payload': {
          'peer_id': 'bob',
          'is_polite': true,
        }
      };
      serverSocket.add(jsonEncode(matchPayload));

      final event = await eventFuture.timeout(const Duration(seconds: 2)) as MatchFoundEvent;
      expect(event.peerId, equals('bob'));
      expect(event.isPolite, isTrue);

      await client.disconnect();
    });

    test('3. Reconnects with exponential backoff on connection drop', () async {
      final fastSchedule = [
        const Duration(milliseconds: 50),
        const Duration(milliseconds: 100),
      ];

      final client = SignalingClient(
        baseUrl: 'http://127.0.0.1:${server.port}',
        jwtToken: 'valid-jwt-token-xyz',
        userId: 'alice',
        backoffSchedule: fastSchedule,
      );

      final states = <SignalingConnectionState>[];
      final sub = client.events.where((e) => e is ConnectionStateEvent).listen((e) {
        states.add((e as ConnectionStateEvent).state);
      });

      // First connection
      final firstSocketFuture = serverSockets.stream.first;
      await client.connect();
      final firstSocket = await firstSocketFuture;
      expect(client.state, equals(SignalingConnectionState.connected));

      // Drop the first connection from server side
      final secondSocketFuture = serverSockets.stream.first;
      final reconnectedFuture = client.events
          .where((e) => e is ConnectionStateEvent && e.state == SignalingConnectionState.connected)
          .first;
      await firstSocket.close();

      // Client should automatically reconnect using the backoff schedule
      final secondSocket = await secondSocketFuture.timeout(const Duration(seconds: 3));
      expect(secondSocket, isNotNull);

      await reconnectedFuture.timeout(const Duration(seconds: 3));
      expect(client.state, equals(SignalingConnectionState.connected));

      expect(states, contains(SignalingConnectionState.reconnecting));
      expect(states, contains(SignalingConnectionState.connected));

      await sub.cancel();
      await client.disconnect();
    });

    test('4. Disconnect cleans up resources and prevents automatic reconnection', () async {
      final client = SignalingClient(
        baseUrl: 'http://127.0.0.1:${server.port}',
        jwtToken: 'valid-jwt-token-xyz',
        userId: 'alice',
        backoffSchedule: [const Duration(milliseconds: 50)],
      );

      final socketFuture = serverSockets.stream.first;
      await client.connect();
      await socketFuture;

      await client.disconnect();
      expect(client.state, equals(SignalingConnectionState.disconnected));

      // Attempting to send signal after disconnect throws StateError
      await expectLater(
        client.sendSignal('bob', {'test': 'data'}),
        throwsStateError,
      );

      // Verify no reconnection happens after explicit disconnect
      await Future.delayed(const Duration(milliseconds: 150));
      expect(client.state, equals(SignalingConnectionState.disconnected));
    });

    test('5. Receiving signal produces SignalEvent with fromUserId and payload', () async {
      final client = SignalingClient(
        baseUrl: 'http://127.0.0.1:${server.port}',
        jwtToken: 'valid-jwt-token-xyz',
        userId: 'alice',
      );

      final socketFuture = serverSockets.stream.first;
      await client.connect();
      final serverSocket = await socketFuture;

      final eventFuture = client.events.where((e) => e is SignalEvent).first;

      final signalPayload = {
        'type': 'signal',
        'from': 'bob',
        'payload': {
          'type': 'offer',
          'sdp': 'v=0...',
        },
      };
      serverSocket.add(jsonEncode(signalPayload));

      final event = await eventFuture.timeout(const Duration(seconds: 2)) as SignalEvent;
      expect(event.fromUserId, equals('bob'));
      expect(event.payload['type'], equals('offer'));
      expect(event.payload['sdp'], equals('v=0...'));

      await client.disconnect();
    });

    test('6. Receiving error message produces ErrorEvent', () async {
      final client = SignalingClient(
        baseUrl: 'http://127.0.0.1:${server.port}',
        jwtToken: 'valid-jwt-token-xyz',
        userId: 'alice',
      );

      final socketFuture = serverSockets.stream.first;
      await client.connect();
      final serverSocket = await socketFuture;

      final eventFuture = client.events.where((e) => e is ErrorEvent).first;

      final errorPayload = {
        'type': 'error',
        'code': 'ROOM_FULL',
        'message': 'Cannot join room: room is full',
      };
      serverSocket.add(jsonEncode(errorPayload));

      final event = await eventFuture.timeout(const Duration(seconds: 2)) as ErrorEvent;
      expect(event.code, equals('ROOM_FULL'));
      expect(event.message, equals('Cannot join room: room is full'));

      await client.disconnect();
    });
  });
}
