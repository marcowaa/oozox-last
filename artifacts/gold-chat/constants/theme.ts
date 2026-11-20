// constants/theme.ts
export const Theme = {
  spacing: {
    xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32
  },
  radius: {
    sm: 8, md: 12, lg: 16, xl: 20, xxl: 24, full: 999
  },
  shadow: {
    sm: {
      shadowColor: '#3D2B1F', shadowOffset: { width: 0, height: 1 },
      shadowOpacity: 0.08, shadowRadius: 2, elevation: 2
    },
    md: {
      shadowColor: '#3D2B1F', shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.12, shadowRadius: 6, elevation: 4
    },
    lg: {
      shadowColor: '#3D2B1F', shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.16, shadowRadius: 12, elevation: 8
    }
  },
  fontSize: {
    xs: 11, sm: 12, md: 14, base: 15, lg: 16, xl: 18, xxl: 22, xxxl: 28
  },
  fontWeight: {
    regular: '400' as const,
    medium: '500' as const,
    semibold: '600' as const,
    bold: '700' as const,
  }
};
