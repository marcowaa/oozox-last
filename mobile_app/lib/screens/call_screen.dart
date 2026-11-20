import 'package:flutter/material.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import 'package:provider/provider.dart';

import '../state/call_controller.dart';

/// Fullscreen active call view handling media renderers and user controls.
class CallScreen extends StatefulWidget {
  const CallScreen({super.key});

  @override
  State<CallScreen> createState() => _CallScreenState();
}

class _CallScreenState extends State<CallScreen> {
  final RTCVideoRenderer _localRenderer = RTCVideoRenderer();
  final RTCVideoRenderer _remoteRenderer = RTCVideoRenderer();
  bool _renderersInitialized = false;

  @override
  void initState() {
    super.initState();
    _initRenderers();
  }

  Future<void> _initRenderers() async {
    await _localRenderer.initialize();
    await _remoteRenderer.initialize();
    if (mounted) {
      setState(() {
        _renderersInitialized = true;
      });
    }
  }

  @override
  void dispose() {
    _localRenderer.dispose();
    _remoteRenderer.dispose();
    super.dispose();
  }

  void _syncStreams(CallController controller) {
    if (!_renderersInitialized) return;

    if (_remoteRenderer.srcObject != controller.remoteStream) {
      _remoteRenderer.srcObject = controller.remoteStream;
    }
    if (_localRenderer.srcObject != controller.localStream) {
      _localRenderer.srcObject = controller.localStream;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<CallController>(
      builder: (context, controller, _) {
        _syncStreams(controller);

        return PopScope(
          canPop: false,
          onPopInvokedWithResult: (didPop, result) async {
            if (didPop) return;
            await controller.hangUp();
            if (context.mounted) {
              Navigator.of(context).pop();
            }
          },
          child: Scaffold(
            backgroundColor: Colors.black,
            body: SafeArea(
              child: _buildBody(context, controller),
            ),
          ),
        );
      },
    );
  }

  Widget _buildBody(BuildContext context, CallController controller) {
    switch (controller.status) {
      case CallStatus.requestingMatch:
      case CallStatus.waiting:
        final country = controller.currentCountry ?? 'any';
        return _buildCenteredStatus(
          context: context,
          icon: const CircularProgressIndicator(color: Colors.tealAccent),
          title: 'Searching in $country...',
          subtitle: 'Waiting in matchmaking queue',
          onAction: () async {
            await controller.hangUp();
            if (context.mounted) Navigator.of(context).pop();
          },
          actionLabel: 'Cancel',
        );

      case CallStatus.connecting:
        final peer = controller.remoteUserId ?? 'peer';
        return _buildCenteredStatus(
          context: context,
          icon: const CircularProgressIndicator(color: Colors.amberAccent),
          title: 'Connecting to $peer...',
          subtitle: 'Negotiating SDP & ICE candidates',
          onAction: () async {
            await controller.hangUp();
            if (context.mounted) Navigator.of(context).pop();
          },
          actionLabel: 'Cancel',
        );

      case CallStatus.connected:
        return _buildConnectedView(context, controller);

      case CallStatus.error:
        return _buildCenteredStatus(
          context: context,
          icon: const Icon(Icons.error_outline, size: 64, color: Colors.redAccent),
          title: 'Connection Error',
          subtitle: controller.errorMessage ?? 'An error occurred during call setup',
          onAction: () {
            Navigator.of(context).pop();
          },
          actionLabel: 'Back',
        );

      case CallStatus.idle:
      case CallStatus.ended:
        return _buildCenteredStatus(
          context: context,
          icon: const Icon(Icons.call_end, size: 64, color: Colors.white54),
          title: 'Call ended',
          subtitle: 'Media session terminated cleanly',
          onAction: () {
            Navigator.of(context).pop();
          },
          actionLabel: 'Back',
        );
    }
  }

  Widget _buildCenteredStatus({
    required BuildContext context,
    required Widget icon,
    required String title,
    required String subtitle,
    required VoidCallback onAction,
    required String actionLabel,
  }) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            icon,
            const SizedBox(height: 24),
            Text(
              title,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                  ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              subtitle,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Colors.white70,
                  ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 36),
            FilledButton.tonal(
              onPressed: onAction,
              child: Text(actionLabel),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConnectedView(BuildContext context, CallController controller) {
    return Stack(
      children: [
        // 1. Fullscreen Remote Video Feed
        Positioned.fill(
          child: _renderersInitialized && controller.remoteStream != null
              ? RTCVideoView(
                  _remoteRenderer,
                  objectFit: RTCVideoViewObjectFit.RTCVideoViewObjectFitContain,
                )
              : Container(
                  color: Colors.black87,
                  child: const Center(
                    child: Icon(
                      Icons.person,
                      size: 96,
                      color: Colors.white24,
                    ),
                  ),
                ),
        ),

        // 2. Picture-in-Picture Local Video Feed (Top-Right)
        if (_renderersInitialized && controller.localStream != null)
          Positioned(
            top: 16,
            right: 16,
            child: SizedBox(
              width: 120,
              height: 160,
              child: Container(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.white38, width: 1.5),
                  boxShadow: const [
                    BoxShadow(color: Colors.black45, blurRadius: 8),
                  ],
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: RTCVideoView(
                    _localRenderer,
                    mirror: true,
                    objectFit: RTCVideoViewObjectFit.RTCVideoViewObjectFitCover,
                  ),
                ),
              ),
            ),
          ),

        // 3. User Identification Header
        Positioned(
          top: 16,
          left: 16,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: Colors.black54,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.circle, color: Colors.greenAccent, size: 10),
                const SizedBox(width: 8),
                Text(
                  controller.remoteUserId ?? 'Connected',
                  style: const TextStyle(color: Colors.white, fontSize: 13),
                ),
              ],
            ),
          ),
        ),

        // 4. Bottom Action Control Bar
        Positioned(
          bottom: 24,
          left: 16,
          right: 16,
          child: Center(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.black.withAlpha(180),
                borderRadius: BorderRadius.circular(36),
                border: Border.all(color: Colors.white12),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Mute Toggle
                  IconButton(
                    iconSize: 28,
                    color: controller.isMuted ? Colors.redAccent : Colors.white,
                    icon: Icon(controller.isMuted ? Icons.mic_off : Icons.mic),
                    onPressed: () => controller.toggleMute(),
                  ),
                  const SizedBox(width: 12),

                  // Camera Toggle
                  IconButton(
                    iconSize: 28,
                    color: controller.isCameraEnabled ? Colors.white : Colors.redAccent,
                    icon: Icon(controller.isCameraEnabled
                        ? Icons.videocam
                        : Icons.videocam_off),
                    onPressed: () => controller.toggleCamera(),
                  ),
                  const SizedBox(width: 12),

                  // Switch Camera
                  IconButton(
                    iconSize: 28,
                    color: Colors.white,
                    icon: const Icon(Icons.cameraswitch_rounded),
                    onPressed: () => controller.switchCamera(),
                  ),
                  const SizedBox(width: 16),

                  // End Call (Red)
                  IconButton(
                    style: IconButton.styleFrom(
                      backgroundColor: Colors.redAccent,
                    ),
                    iconSize: 28,
                    color: Colors.white,
                    icon: const Icon(Icons.call_end),
                    onPressed: () async {
                      await controller.hangUp();
                      if (context.mounted) {
                        Navigator.of(context).pop();
                      }
                    },
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}
