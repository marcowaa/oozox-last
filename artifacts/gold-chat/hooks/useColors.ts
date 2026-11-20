// Updated to use the flat Colors export from constants/colors.ts
import { Colors } from '@/constants/colors';

/**
 * Returns the design tokens for the current color scheme.
 * The app uses a fixed gold/brown theme (light only).
 */
export function useColors() {
  return Colors;
}
