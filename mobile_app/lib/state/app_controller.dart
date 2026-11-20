import 'dart:async';
import 'dart:developer' as developer;

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../services/location_service.dart';

/// Represents a recommended project tailored to the user's geographic context.
class ProjectRecommendation {
  final String id;
  final String title;
  final String description;
  final String region;
  final List<String> tags;
  final double relevancyScore;

  const ProjectRecommendation({
    required this.id,
    required this.title,
    required this.description,
    required this.region,
    required this.tags,
    this.relevancyScore = 1.0,
  });

  @override
  String toString() => 'ProjectRecommendation(id: $id, title: $title, region: $region)';
}

/// Controller managing application-level state, location context listeners,
/// and location-personalized project recommendations.
///
/// Ensures compliance with user authorization and privacy:
/// - Strictly waits for explicit user authorization before accessing device location.
/// - Guarantees the authorization dialog appears ONLY on the first attempt (persisted via SharedPreferences).
/// - Broadcasts location context updates through a dedicated 'Location Context' listener.
class AppController extends ChangeNotifier {
  static const String keyHasPromptedLocationDialog = 'app_has_prompted_location_dialog';
  static const String keyIsLocationAuthorized = 'app_is_location_authorized';

  bool _isInitialized = false;
  bool _hasPromptedLocationDialog = false;
  bool _isLocationAuthorized = false;
  bool _isLoadingLocation = false;

  GeographicContext? _currentLocationContext;
  List<ProjectRecommendation> _projectRecommendations = [];

  final List<void Function(GeographicContext? context)> _locationContextListeners = [];
  final StreamController<GeographicContext?> _locationContextStream =
      StreamController<GeographicContext?>.broadcast();

  // -------------------------------------------------------------
  // Getters
  // -------------------------------------------------------------
  bool get isInitialized => _isInitialized;
  bool get hasPromptedLocationDialog => _hasPromptedLocationDialog;
  bool get isLocationAuthorized => _isLocationAuthorized;
  bool get isLoadingLocation => _isLoadingLocation;
  GeographicContext? get currentLocationContext => _currentLocationContext;
  List<ProjectRecommendation> get projectRecommendations => List.unmodifiable(_projectRecommendations);
  Stream<GeographicContext?> get locationContextStream => _locationContextStream.stream;

  // -------------------------------------------------------------
  // Initialization
  // -------------------------------------------------------------
  /// Loads persisted authorization flags and sets up initial recommendations.
  Future<void> init() async {
    if (_isInitialized) return;

    try {
      final prefs = await SharedPreferences.getInstance();
      _hasPromptedLocationDialog = prefs.getBool(keyHasPromptedLocationDialog) ?? false;
      _isLocationAuthorized = prefs.getBool(keyIsLocationAuthorized) ?? false;
    } catch (e) {
      developer.log('Failed to read SharedPreferences: $e', name: 'AppController');
    }

    _isInitialized = true;

    // If user previously granted authorization, access device location and personalize recommendations
    if (_isLocationAuthorized) {
      await refreshLocationContext();
    } else {
      // Default non-personalized recommendations without accessing device location
      _projectRecommendations = _generateRecommendationsForRegion('GLOBAL');
    }

    notifyListeners();
  }

  // -------------------------------------------------------------
  // Location Context Listeners
  // -------------------------------------------------------------
  /// Registers a callback listener for Location Context updates.
  void addLocationContextListener(void Function(GeographicContext? context) listener) {
    if (!_locationContextListeners.contains(listener)) {
      _locationContextListeners.add(listener);
    }
  }

  /// Removes a registered Location Context listener.
  void removeLocationContextListener(void Function(GeographicContext? context) listener) {
    _locationContextListeners.remove(listener);
  }

  /// Emits location context updates to registered listeners and streams.
  void _notifyLocationContextChanged(GeographicContext? context) {
    _currentLocationContext = context;
    _locationContextStream.add(context);

    for (final listener in List.of(_locationContextListeners)) {
      try {
        listener(context);
      } catch (e) {
        developer.log('Error in LocationContext listener: $e', name: 'AppController');
      }
    }
  }

  // -------------------------------------------------------------
  // First-Attempt Authorization Dialog & Access
  // -------------------------------------------------------------
  /// Prompts the user for location authorization to personalize project recommendations.
  ///
  /// Enforces the requirement that the authorization dialog ONLY appears on the
  /// first attempt unless [force] is explicitly set to true.
  Future<bool> requestLocationAuthorization(BuildContext context, {bool force = false}) async {
    // 1. If already prompted and not forced, respect past decision and DO NOT show dialog again
    if (_hasPromptedLocationDialog && !force) {
      developer.log(
        'Location authorization dialog already appeared on first attempt. Skipping dialog.',
        name: 'AppController',
      );
      if (_isLocationAuthorized && _currentLocationContext == null) {
        await refreshLocationContext();
      }
      return _isLocationAuthorized;
    }

    // 2. First-attempt prompt dialog
    final authorized = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        icon: const Icon(Icons.travel_explore_rounded, size: 40, color: Colors.tealAccent),
        title: const Text('Personalize Projects'),
        content: const Text(
          'Would you like to authorize access to your device location context?\n\n'
          'This allows Ayer to personalize project recommendations, discover low-latency '
          'regional peers, and optimize WebRTC media routing.\n\n'
          'Your exact GPS coordinates are never collected or shared.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Not Now'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Authorize'),
          ),
        ],
      ),
    ) ?? false;

    // 3. Mark that dialog has appeared on first attempt and persist
    _hasPromptedLocationDialog = true;
    _isLocationAuthorized = authorized;

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool(keyHasPromptedLocationDialog, true);
      await prefs.setBool(keyIsLocationAuthorized, authorized);
    } catch (e) {
      developer.log('Failed to persist location authorization flags: $e', name: 'AppController');
    }

    // 4. Access location only if user gave explicit authorization
    if (authorized) {
      await refreshLocationContext();
    } else {
      _projectRecommendations = _generateRecommendationsForRegion('GLOBAL');
      notifyListeners();
    }

    return authorized;
  }

  // -------------------------------------------------------------
  // Access Device Location & Personalize Recommendations
  // -------------------------------------------------------------
  /// Accesses device location context after verifying user authorization.
  Future<void> refreshLocationContext() async {
    if (!_isLocationAuthorized) {
      developer.log('Access blocked: User has not authorized location access.', name: 'AppController');
      return;
    }

    _isLoadingLocation = true;
    notifyListeners();

    try {
      final geoContext = await LocationService.resolveGeographicContext();
      _notifyLocationContextChanged(geoContext);

      // Personalize recommendations using resolved location context
      final country = geoContext?.countryCode ?? 'GLOBAL';
      _projectRecommendations = _generateRecommendationsForRegion(country);
    } catch (e) {
      developer.log('Error refreshing location context: $e', name: 'AppController');
      _projectRecommendations = _generateRecommendationsForRegion('GLOBAL');
    } finally {
      _isLoadingLocation = false;
      notifyListeners();
    }
  }

  /// Manually set or reset authorization (e.g. from settings screen).
  Future<void> setLocationAuthorization(bool authorized) async {
    _isLocationAuthorized = authorized;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool(keyIsLocationAuthorized, authorized);
    } catch (_) {}

    if (authorized) {
      await refreshLocationContext();
    } else {
      _notifyLocationContextChanged(null);
      _projectRecommendations = _generateRecommendationsForRegion('GLOBAL');
      notifyListeners();
    }
  }

  // -------------------------------------------------------------
  // Recommendation Engine
  // -------------------------------------------------------------
  List<ProjectRecommendation> _generateRecommendationsForRegion(String region) {
    switch (region.toUpperCase()) {
      case 'EG':
      case 'SA':
      case 'AE':
      case 'MENA':
        return [
          const ProjectRecommendation(
            id: 'proj_mena_01',
            title: 'Cairo Regional Low-Latency Gateway',
            description: 'High-throughput WebRTC TURN/STUN cluster serving the Nile & Gulf regions.',
            region: 'MENA',
            tags: ['WebRTC', 'Low-Latency', 'TURN'],
            relevancyScore: 0.98,
          ),
          const ProjectRecommendation(
            id: 'proj_mena_02',
            title: 'Alexandria Edge Relay Network',
            description: 'Submarine cable interconnect for cross-continental peer connections.',
            region: 'MENA',
            tags: ['Edge', 'Routing', 'Submarine'],
            relevancyScore: 0.94,
          ),
          const ProjectRecommendation(
            id: 'proj_mena_03',
            title: 'Arabian Peninsula Peer Mesh',
            description: 'Decentralized mesh topology for direct 1:1 encrypted audio/video streaming.',
            region: 'MENA',
            tags: ['Mesh', 'Encrypted', 'P2P'],
            relevancyScore: 0.89,
          ),
        ];

      case 'US':
      case 'CA':
      case 'NA':
        return [
          const ProjectRecommendation(
            id: 'proj_na_01',
            title: 'Silicon Valley Fast-Path Exchange',
            description: 'Sub-5ms media relay network optimized for North American west-coast peers.',
            region: 'North America',
            tags: ['High-Speed', 'WebRTC', 'Edge'],
            relevancyScore: 0.97,
          ),
          const ProjectRecommendation(
            id: 'proj_na_02',
            title: 'Atlantic Fiber Peer Bridge',
            description: 'Cross-Atlantic direct signaling tunnel connecting US-East with Europe.',
            region: 'North America',
            tags: ['Fiber', 'Signaling', 'Cross-Region'],
            relevancyScore: 0.93,
          ),
        ];

      case 'DE':
      case 'FR':
      case 'GB':
      case 'EU':
        return [
          const ProjectRecommendation(
            id: 'proj_eu_01',
            title: 'Frankfurt Core Privacy Relay',
            description: 'GDPR-compliant WebRTC media reflector with end-to-end encrypted packet forwarding.',
            region: 'Europe',
            tags: ['GDPR', 'Privacy', 'Reflector'],
            relevancyScore: 0.96,
          ),
          const ProjectRecommendation(
            id: 'proj_eu_02',
            title: 'London Fiber Exchange Node',
            description: 'Ultra-low jitter peering point with direct tier-1 transit connections.',
            region: 'Europe',
            tags: ['Zero-Jitter', 'Tier-1', 'Peering'],
            relevancyScore: 0.91,
          ),
        ];

      default:
        return [
          const ProjectRecommendation(
            id: 'proj_glob_01',
            title: 'Global Unified Mesh Protocol',
            description: 'Decentralized multi-region WebRTC coordinator connecting peers worldwide.',
            region: 'Global',
            tags: ['Universal', 'P2P', 'Open-Source'],
            relevancyScore: 0.85,
          ),
          const ProjectRecommendation(
            id: 'proj_glob_02',
            title: 'Distributed ICE Candidate Analyzer',
            description: 'Autonomous STUN/TURN fallback optimizer for restrictive NAT traversal.',
            region: 'Global',
            tags: ['ICE', 'NAT-Traversal', 'Resilient'],
            relevancyScore: 0.82,
          ),
        ];
    }
  }

  @override
  void dispose() {
    _locationContextListeners.clear();
    _locationContextStream.close();
    super.dispose();
  }
}
