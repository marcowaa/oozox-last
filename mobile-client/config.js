import { Platform } from 'react-native';

/**
 * OOZOX Mobile Client Environment & Network Configuration
 * Supports Android Emulator, iOS Simulator, Physical Wi-Fi Devices, and Live Cloud Production.
 */

// 1. Defined Environments
const ENV = {
  // Production Live Cloud Server (e.g. VPS / AWS / Cloud Run / DigitalOcean)
  production: 'https://api.oozox.com/api',

  // Staging / QA Server
  staging: 'https://staging-api.oozox.com/api',

  // Local Physical Device Testing over Wi-Fi
  // Replace '192.168.1.100' with your host computer's local Wi-Fi IPv4 address
  localDevice: 'http://192.168.1.100:5000/api',

  // Emulator / Simulator Localhost Mapping
  development: Platform.select({
    android: 'http://10.0.2.2:5000/api', // Android Emulator Host Loopback
    ios: 'http://localhost:5000/api',     // iOS Simulator
    default: 'http://localhost:5000/api',
  }),
};

// 2. Active Environment Selector
// Options: 'development' | 'localDevice' | 'staging' | 'production'
export const ACTIVE_ENV = process.env.REACT_NATIVE_APP_ENV || 'development';

// 3. Dynamic Base URL Resolution
export const API_BASE_URL =
  process.env.REACT_NATIVE_API_BASE_URL || ENV[ACTIVE_ENV] || ENV.development;

export const APP_CONFIG = {
  TIMEOUT_MS: 15000,
  APP_NAME: 'OOZOX',
  VERSION: '1.0.0',
  ENV_NAME: ACTIVE_ENV,
  IS_PRODUCTION: ACTIVE_ENV === 'production',
};

export default {
  API_BASE_URL,
  ACTIVE_ENV,
  APP_CONFIG,
  ENV,
};
