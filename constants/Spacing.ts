/**
 * Design Token System: Geometric 8pt Modular Scale
 * All dimensions (margins, paddings, gaps, and radii) strictly adhere to
 * multiples of the 4px micro-grid and 8px base-grid.
 */

export const Spacing = {
  none: 0,
  xxs: 4,   // 0.5x
  xs: 8,    // 1.0x
  sm: 12,   // 1.5x
  md: 16,   // 2.0x
  lg: 24,   // 3.0x
  xl: 32,   // 4.0x
  xxl: 48,  // 6.0x
  xxxl: 64, // 8.0x
} as const;

export const BorderRadius = {
  none: 0,
  xs: 4,    // 0.5x
  sm: 8,    // 1.0x
  md: 12,   // 1.5x
  lg: 16,   // 2.0x
  xl: 24,   // 3.0x
  xxl: 32,  // 4.0x
  full: 9999,
} as const;

export const Gap = {
  none: 0,
  xxs: 4,   // 0.5x
  xs: 8,    // 1.0x
  sm: 12,   // 1.5x
  md: 16,   // 2.0x
  lg: 24,   // 3.0x
  xl: 32,   // 4.0x
  xxl: 48,  // 6.0x
} as const;

export const TouchTarget = {
  min: 48, // Accessible minimum interactive component dimension (WCAG 2.5.5)
} as const;

export const IconSize = {
  xs: 12,
  sm: 16,
  md: 24,
  lg: 32,
  xl: 48,
} as const;

export type SpacingKey = keyof typeof Spacing;
export type BorderRadiusKey = keyof typeof BorderRadius;
export type GapKey = keyof typeof Gap;
export type IconSizeKey = keyof typeof IconSize;
