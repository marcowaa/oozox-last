import React, { useEffect } from 'react';
import { SiteHeroConfig } from './types';

export interface MetadataProviderProps {
  heroConfig: SiteHeroConfig;
  canonicalUrl?: string;
  siteName?: string;
  ogImage?: string;
  keywords?: string[];
  children?: React.ReactNode;
}

/**
 * MetadataProvider dynamically injects and updates document SEO metadata,
 * OpenGraph tags, Twitter cards, canonical links, and title based on heroConfig.
 */
export const MetadataProvider: React.FC<MetadataProviderProps> = ({
  heroConfig,
  canonicalUrl = 'https://oozox.com',
  siteName = 'OOZOX',
  ogImage = 'https://oozox.com/assets/og-banner.png',
  keywords = [
    'فواتير الكترونية',
    'تطبيق محاسبة',
    'دفترة ديون',
    'حسابات الذهب',
    'تصدير اكسل',
    'كشف حساب عملاء',
    'Oozox Accounting',
    'Invoice App 2026'
  ],
  children
}) => {
  useEffect(() => {
    if (typeof document === 'undefined') return;

    // 1. Dynamic Page Title
    const titleText = `${siteName} — ${heroConfig.headline} | ${heroConfig.brandHighlight}`;
    document.title = titleText;

    // Helper to safely set or create a <meta> tag
    const setMetaTag = (attrName: 'name' | 'property', attrValue: string, content: string) => {
      let meta = document.querySelector(`meta[${attrName}="${attrValue}"]`) as HTMLMetaElement | null;
      if (!meta) {
        meta = document.createElement('meta');
        meta.setAttribute(attrName, attrValue);
        document.head.appendChild(meta);
      }
      meta.content = content;
    };

    // Helper to safely set or create a <link> tag (e.g. canonical)
    const setLinkTag = (rel: string, href: string) => {
      let link = document.querySelector(`link[rel="${rel}"]`) as HTMLLinkElement | null;
      if (!link) {
        link = document.createElement('link');
        link.setAttribute('rel', rel);
        document.head.appendChild(link);
      }
      link.href = href;
    };

    // 2. Standard Search Engine Meta
    setMetaTag('name', 'description', heroConfig.subheadline || 'تطبيق الفواتير والمحاسبة الذكية');
    setMetaTag('name', 'keywords', keywords.join(', '));
    setMetaTag('name', 'author', 'OOZOX Inc.');
    setMetaTag('name', 'robots', 'index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1');

    // 3. OpenGraph / Facebook / AI Crawlers Meta
    setMetaTag('property', 'og:site_name', siteName);
    setMetaTag('property', 'og:type', 'website');
    setMetaTag('property', 'og:title', `${heroConfig.headline} - ${heroConfig.brandHighlight}`);
    setMetaTag('property', 'og:description', heroConfig.subheadline);
    setMetaTag('property', 'og:url', canonicalUrl);
    setMetaTag('property', 'og:image', ogImage);
    setMetaTag('property', 'og:locale', 'ar_AR');

    // 4. Twitter / X Card Meta
    setMetaTag('name', 'twitter:card', 'summary_large_image');
    setMetaTag('name', 'twitter:title', `${heroConfig.headline} | ${siteName}`);
    setMetaTag('name', 'twitter:description', heroConfig.subheadline);
    setMetaTag('name', 'twitter:image', ogImage);

    // 5. Canonical Link Tag
    setLinkTag('canonical', canonicalUrl);

  }, [heroConfig, canonicalUrl, siteName, ogImage, keywords]);

  return <>{children}</>;
};

export default MetadataProvider;
