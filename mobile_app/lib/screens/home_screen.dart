import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/app_config.dart';
import '../services/location_service.dart';
import '../state/app_controller.dart';
import '../state/call_controller.dart';
import 'call_screen.dart';

/// Landing screen for entering matchmaking details and configuring the signaling server.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _userIdController;
  late final TextEditingController _countryController;
  late final TextEditingController _serverUrlController;
  bool _isDetectingLocation = false;

  @override
  void initState() {
    super.initState();
    _userIdController = TextEditingController(text: 'user_${DateTime.now().millisecondsSinceEpoch % 10000}');
    _countryController = TextEditingController(text: 'EG');
    _serverUrlController = TextEditingController(text: AppConfig.signalingBaseUrl);

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        final appController = context.read<AppController>();
        appController.addLocationContextListener(_onLocationContextChanged);
      }
    });
  }

  void _onLocationContextChanged(GeographicContext? context) {
    if (context != null && mounted) {
      setState(() {
        _countryController.text = context.countryCode;
      });
    }
  }

  @override
  void dispose() {
    try {
      context.read<AppController>().removeLocationContextListener(_onLocationContextChanged);
    } catch (_) {}
    _userIdController.dispose();
    _countryController.dispose();
    _serverUrlController.dispose();
    super.dispose();
  }

  Future<void> _detectCountryFromLocation() async {
    setState(() {
      _isDetectingLocation = true;
    });

    try {
      final geoContext = await LocationService.requestGeographicContext(
        context: context,
        showRationale: true,
      );

      if (!mounted) return;

      if (geoContext != null) {
        setState(() {
          _countryController.text = geoContext.countryCode;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.check_circle_outline, color: Colors.tealAccent),
                const SizedBox(width: 8),
                Text('Detected country: ${geoContext.countryCode}'),
              ],
            ),
            duration: const Duration(seconds: 2),
            behavior: SnackBarBehavior.floating,
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Location permission was not granted. You can enter your country manually.'),
            duration: Duration(seconds: 3),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isDetectingLocation = false;
        });
      }
    }
  }

  Future<void> _startMatching() async {
    if (!_formKey.currentState!.validate()) return;

    final userId = _userIdController.text.trim();
    final country = _countryController.text.trim().toUpperCase();
    final serverUrl = _serverUrlController.text.trim();

    // 1. Persist configured signaling endpoint
    await AppConfig.setSignalingBaseUrl(serverUrl);

    if (!mounted) return;

    // 2. Request match
    final controller = context.read<CallController>();
    await controller.requestMatch(userId: userId, country: country);

    if (!mounted) return;

    // 3. Navigate to CallScreen
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => const CallScreen(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Ayer'),
        centerTitle: true,
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Icon(
                    Icons.video_call_rounded,
                    size: 72,
                    color: Colors.tealAccent,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Direct 1:1 Video Match',
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Connect with peers securely over WebRTC Unified Plan',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Colors.white70,
                        ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 32),
                  TextFormField(
                    controller: _userIdController,
                    decoration: const InputDecoration(
                      labelText: 'User ID',
                      prefixIcon: Icon(Icons.person_outline),
                      border: OutlineInputBorder(),
                    ),
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'User ID is required' : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _countryController,
                    decoration: InputDecoration(
                      labelText: 'Country Code (e.g. EG, US, SA)',
                      prefixIcon: const Icon(Icons.public),
                      suffixIcon: _isDetectingLocation
                          ? const Padding(
                              padding: EdgeInsets.all(12.0),
                              child: SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.tealAccent,
                                ),
                              ),
                            )
                          : IconButton(
                              icon: const Icon(Icons.my_location),
                              tooltip: 'Auto-detect country from location',
                              onPressed: _detectCountryFromLocation,
                            ),
                      border: const OutlineInputBorder(),
                      helperText: 'Tap GPS icon to auto-detect country on-demand',
                    ),
                    textCapitalization: TextCapitalization.characters,
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Country is required' : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _serverUrlController,
                    decoration: const InputDecoration(
                      labelText: 'Signaling Base URL',
                      prefixIcon: Icon(Icons.dns_outlined),
                      border: OutlineInputBorder(),
                      helperText: 'Default: http://10.0.2.2:8080 (Emulator) or IP',
                    ),
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Server URL is required' : null,
                  ),
                  const SizedBox(height: 28),
                  FilledButton.icon(
                    onPressed: _startMatching,
                    icon: const Icon(Icons.play_arrow_rounded),
                    label: const Padding(
                      padding: EdgeInsets.symmetric(vertical: 14.0),
                      child: Text(
                        'Start Matching',
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),
                  _buildRecommendationsSection(context),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildRecommendationsSection(BuildContext context) {
    return Consumer<AppController>(
      builder: (context, appController, _) {
        final isAuth = appController.isLocationAuthorized;
        final hasPrompted = appController.hasPromptedLocationDialog;
        final geoContext = appController.currentLocationContext;
        final recommendations = appController.projectRecommendations;

        return Container(
          padding: const EdgeInsets.all(16.0),
          decoration: BoxDecoration(
            color: Colors.white.withAlpha(10),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: isAuth ? Colors.tealAccent.withAlpha(80) : Colors.white24,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.travel_explore_rounded,
                    color: isAuth ? Colors.tealAccent : Colors.white70,
                    size: 22,
                  ),
                  const SizedBox(width: 8),
                  const Expanded(
                    child: Text(
                      'Project Recommendations',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  if (isAuth && geoContext != null)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.tealAccent.withAlpha(30),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        'Region: ${geoContext.countryCode}',
                        style: const TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: Colors.tealAccent,
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                isAuth
                    ? 'Projects personalized based on your authorized location context.'
                    : hasPrompted
                        ? 'Showing global projects. (Location authorization was declined).'
                        : 'Authorize location context on first attempt to personalize recommendations.',
                style: const TextStyle(fontSize: 12, color: Colors.white70),
              ),
              if (!isAuth && !hasPrompted) ...[
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: () => appController.requestLocationAuthorization(context),
                  icon: const Icon(Icons.security, size: 16),
                  label: const Text('Authorize Location Context'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.tealAccent,
                    side: const BorderSide(color: Colors.tealAccent),
                  ),
                ),
              ],
              if (appController.isLoadingLocation) ...[
                const SizedBox(height: 16),
                const Center(
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.tealAccent,
                  ),
                ),
              ] else if (recommendations.isNotEmpty) ...[
                const SizedBox(height: 14),
                ...recommendations.map(
                  (project) => Container(
                    margin: const EdgeInsets.only(bottom: 8.0),
                    padding: const EdgeInsets.all(12.0),
                    decoration: BoxDecoration(
                      color: Colors.black26,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: Colors.white10),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                project.title,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w600,
                                  fontSize: 13,
                                ),
                              ),
                            ),
                            Text(
                              '${(project.relevancyScore * 100).toInt()}% match',
                              style: const TextStyle(
                                fontSize: 11,
                                color: Colors.tealAccent,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          project.description,
                          style: const TextStyle(fontSize: 11, color: Colors.white60),
                        ),
                        const SizedBox(height: 6),
                        Wrap(
                          spacing: 4,
                          runSpacing: 4,
                          children: project.tags
                              .map(
                                (tag) => Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: Colors.white.withAlpha(15),
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Text(
                                    '#$tag',
                                    style: const TextStyle(fontSize: 10, color: Colors.white70),
                                  ),
                                ),
                              )
                              .toList(),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}
