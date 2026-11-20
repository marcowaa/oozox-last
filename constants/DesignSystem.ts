/**
 * OOZOX Design System Tokens & Spatial Architecture
 * 
 * Implements a strict 8px harmonic grid system with a 4px micro-grid baseline.
 * Rule: 4, 8, 12, 16, 24, 32, 48 (and extended 64px for macro containers).
 * 
 * Adheres strictly to WCAG 2.5.5 touch target rules (min 48px) and Material 3
 * design specifications.
 */

// ============================================================================
// 1. SPATIAL GRID & SPACING TOKENS (4, 8, 12, 16, 24, 32, 48 Rule)
// ============================================================================

export const Spacing = {
  none: 0,
  /** 4px - Micro spacing, icon offsets, compact badges */
  xxs: 4,
  /** 8px - Base unit: inner padding, tight item gaps, micro margins */
  xs: 8,
  /** 12px - Sub-unit: chip padding, compact card insets */
  sm: 12,
  /** 16px - Standard container padding, standard list item gutter */
  md: 16,
  /** 24px - Section gutter, prominent card spacing, header margins */
  lg: 24,
  /** 32px - Module spacing, screen top/bottom hero spacing */
  xl: 32,
  /** 48px - Macro section divider, bottom navigation clearance */
  xxl: 48,
  /** 64px - Hero banners, modal margins, large desktop viewport gutters */
  xxxl: 64,
} as const;

export type SpacingToken = keyof typeof Spacing;
export type SpacingValue = (typeof Spacing)[SpacingToken];

// ============================================================================
// 2. BORDER RADIUS TOKENS (Harmonic geometric curvature)
// ============================================================================

export const BorderRadius = {
  none: 0,
  /** 4px - Micro tags, small progress bars */
  xs: 4,
  /** 8px - Standard buttons, input fields, badge pills */
  sm: 8,
  /** 12px - Compact cards, chips, dropdown menus */
  md: 12,
  /** 16px - Standard cards, dialogs, inset list containers */
  lg: 16,
  /** 24px - Floating action sheets, hero banners, modal dialogs */
  xl: 24,
  /** 32px - Large bottom sheets, navigation pill drawers */
  xxl: 32,
  /** 48px - Full circular avatars, interactive dials */
  xxxl: 48,
  /** 9999px - Perfect pill shape (capsules & circular badges) */
  full: 9999,
} as const;

export type BorderRadiusToken = keyof typeof BorderRadius;
export type BorderRadiusValue = (typeof BorderRadius)[BorderRadiusToken];

// ============================================================================
// 3. TYPOGRAPHY SCALE (Harmonic Type System on an 8pt / 4pt Baseline)
// ============================================================================

export const TypographyScale = {
  /** 10px / 12px LineHeight - Fine-print legal, timestamps, badge labels */
  captionSmall: {
    fontSize: 10,
    lineHeight: 12,
    letterSpacing: 0.2,
    fontWeight: '500' as const,
  },
  /** 12px / 16px LineHeight - Secondary captions, metadata, status indicators */
  caption: {
    fontSize: 12,
    lineHeight: 16,
    letterSpacing: 0.1,
    fontWeight: '400' as const,
  },
  /** 14px / 20px LineHeight - Secondary body text, list descriptions */
  bodySmall: {
    fontSize: 14,
    lineHeight: 20,
    letterSpacing: 0.1,
    fontWeight: '400' as const,
  },
  /** 16px / 24px LineHeight - Primary body copy, inputs, action buttons */
  body: {
    fontSize: 16,
    lineHeight: 24,
    letterSpacing: 0,
    fontWeight: '400' as const,
  },
  /** 16px / 24px LineHeight - Emphasized body text, table row headers */
  bodyBold: {
    fontSize: 16,
    lineHeight: 24,
    letterSpacing: 0,
    fontWeight: '600' as const,
  },
  /** 20px / 28px LineHeight - Section sub-headers, card headers */
  titleSmall: {
    fontSize: 20,
    lineHeight: 28,
    letterSpacing: -0.1,
    fontWeight: '600' as const,
  },
  /** 24px / 32px LineHeight - Main section titles, bottom sheet headers */
  title: {
    fontSize: 24,
    lineHeight: 32,
    letterSpacing: -0.2,
    fontWeight: '700' as const,
  },
  /** 32px / 40px LineHeight - Top-level screen headings, financial totals */
  headline: {
    fontSize: 32,
    lineHeight: 40,
    letterSpacing: -0.4,
    fontWeight: '700' as const,
  },
  /** 48px / 56px LineHeight - Major hero display, marketing numbers */
  display: {
    fontSize: 48,
    lineHeight: 56,
    letterSpacing: -0.8,
    fontWeight: '800' as const,
  },
} as const;

export type TypographyToken = keyof typeof TypographyScale;

// ============================================================================
// 4. TOUCH TARGETS & ACCESSIBILITY TOKENS (WCAG 2.5.5 & Material 3)
// ============================================================================

export const TouchTarget = {
  /** 48px - Minimum interactive touch target dimension */
  min: 48,
  /** 56px - Standard comfortable touch target for primary actions */
  comfortable: 56,
  /** 64px - Large bottom action bar target */
  large: 64,
} as const;

// ============================================================================
// 5. ICONOGRAPHY TOKENS (4, 8, 12, 16, 24, 32, 48 Scale)
// ============================================================================

export const IconSize = {
  /** 12px - Micro indicators, status badge icons */
  xs: 12,
  /** 16px - Small inline helper icons */
  sm: 16,
  /** 24px - Standard action icon size */
  md: 24,
  /** 32px - Prominent category & feature icons */
  lg: 32,
  /** 48px - Hero & empty state focal icons */
  xl: 48,
} as const;

export type IconSizeToken = keyof typeof IconSize;

// ============================================================================
// 6. ELEVATION & SHADOW TOKENS
// ============================================================================

export const Elevation = {
  level0: 'none',
  level1: '0px 1px 3px rgba(0, 0, 0, 0.12), 0px 1px 2px rgba(0, 0, 0, 0.24)',
  level2: '0px 3px 6px rgba(0, 0, 0, 0.15), 0px 2px 4px rgba(0, 0, 0, 0.12)',
  level3: '0px 10px 20px rgba(0, 0, 0, 0.19), 0px 6px 6px rgba(0, 0, 0, 0.23)',
  level4: '0px 14px 28px rgba(0, 0, 0, 0.25), 0px 10px 10px rgba(0, 0, 0, 0.22)',
  level5: '0px 19px 38px rgba(0, 0, 0, 0.30), 0px 15px 12px rgba(0, 0, 0, 0.22)',
} as const;

// ============================================================================
// 7. DESIGN SYSTEM SPECIFICATION OBJECT
// ============================================================================

export const DesignSystem = {
  gridBase: 8,
  microGrid: 4,
  spacing: Spacing,
  borderRadius: BorderRadius,
  typography: TypographyScale,
  touchTarget: TouchTarget,
  iconSize: IconSize,
  elevation: Elevation,
} as const;

export default DesignSystem;
