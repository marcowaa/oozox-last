import React, { useEffect } from 'react';
import { SiteHeroConfig } from '../types';

export interface MetadataProviderProps {
  heroConfig: SiteHeroConfig;
  canonicalUrl?: string;
  siteName?: string;
  ogImage?: string;
  keywords?: string[];
  lang?: 'ar' | 'en';
  children?: React.ReactNode;
}

/**
 * MetadataProvider component
 * Dynamically injects SEO meta tags, OpenGraph data, Twitter cards, 
 * JSON-LD structured data, and canonical URLs based on heroConfig.
 * Optimized for indexing by AI agents (Gemini, ChatGPT, Claude) and search crawlers.
 */
export const MetadataProvider: React.FC<MetadataProviderProps> = ({
  heroConfig,
  canonicalUrl = 'https://oozox.com',
  siteName = 'OOZOX',
  ogImage = 'https://oozox.com/assets/og-banner.png',
  keywords = [
    'OOZOX',
    'فواتير الكترونية',
    'تطبيق محاسبة',
    'دفترة ديون',
    'حسابات الذهب',
    'تصدير اكسل',
    'كشف حساب عملاء',
    'حاسبة المصنعية',
    'برنامج فواتير للمحلات',
    'Smart Invoicing',
    'Gold Accounting App'
  ],
  lang = 'ar',
  children
}) => {
  useEffect(() => {
    if (typeof document === 'undefined') return;

    // 1. Dynamic Page Title
    const titleText = `${siteName} — ${heroConfig.headline || 'نظام الفواتير والمحاسبة الذكي'} | ${heroConfig.brandHighlight || 'تداول الذهب والديون'}`;
    document.title = titleText;

    // Helper to safely set or update a <meta> tag
    const setMetaTag = (attrName: 'name' | 'property', attrValue: string, content: string) => {
      let meta = document.querySelector(`meta[${attrName}="${attrValue}"]`) as HTMLMetaElement | null;
      if (!meta) {
        meta = document.createElement('meta');
        meta.setAttribute(attrName, attrValue);
        document.head.appendChild(meta);
      }
      meta.content = content;
    };

    // Helper to safely set or update a <link> tag
    const setLinkTag = (rel: string, href: string) => {
      let link = document.querySelector(`link[rel="${rel}"]`) as HTMLLinkElement | null;
      if (!link) {
        link = document.createElement('link');
        link.setAttribute('rel', rel);
        document.head.appendChild(link);
      }
      link.href = href;
    };

    // 2. Primary Meta & SEO Engine Tags
    setMetaTag('name', 'description', heroConfig.subheadline || 'منظومة أوزوكس الذكية لإصدار الفواتير، متابعة الديون، وحسابات الذهب مع تصدير إكسل');
    setMetaTag('name', 'keywords', keywords.join(', '));
    setMetaTag('name', 'author', 'OOZOX Technologies');
    setMetaTag('name', 'robots', 'index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1');
    setMetaTag('name', 'theme-color', '#020617');
    setMetaTag('name', 'application-name', siteName);

    // 3. OpenGraph / Facebook / AI Retrieval Agents Meta
    setMetaTag('property', 'og:site_name', siteName);
    setMetaTag('property', 'og:type', 'website');
    setMetaTag('property', 'og:title', `${heroConfig.headline} — ${heroConfig.brandHighlight}`);
    setMetaTag('property', 'og:description', heroConfig.subheadline || 'إدارة الفواتير، الديون، ومحاسبة الذهب الذكية');
    setMetaTag('property', 'og:url', canonicalUrl);
    setMetaTag('property', 'og:image', ogImage);
    setMetaTag('property', 'og:image:alt', `${siteName} Dashboard Preview`);
    setMetaTag('property', 'og:locale', lang === 'ar' ? 'ar_AR' : 'en_US');

    // 4. Twitter / X Cards Meta
    setMetaTag('name', 'twitter:card', 'summary_large_image');
    setMetaTag('name', 'twitter:title', `${heroConfig.headline} | ${siteName}`);
    setMetaTag('name', 'twitter:description', heroConfig.subheadline);
    setMetaTag('name', 'twitter:image', ogImage);
    setMetaTag('name', 'twitter:creator', '@oozox_app');

    // 5. Canonical Link
    setLinkTag('canonical', canonicalUrl);

    // 6. AI Agent Indexing Context (JSON-LD WebSite & SoftwareApplication Schema)
    const schemaId = 'oozox-software-schema';
    let scriptTag = document.getElementById(schemaId) as HTMLScriptElement | null;
    if (!scriptTag) {
      scriptTag = document.createElement('script');
      scriptTag.id = schemaId;
      scriptTag.type = 'application/ld+json';
      document.head.appendChild(scriptTag);
    }

    const structuredData = {
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'SoftwareApplication',
          'name': siteName,
          'applicationCategory': 'FinanceApplication',
          'operatingSystem': 'Android, iOS, Web',
          'offers': {
            '@type': 'Offer',
            'price': '0',
            'priceCurrency': 'USD',
            'availability': 'https://schema.org/InStock',
            'description': '60-Day Full Access Free Trial'
          },
          'description': heroConfig.subheadline,
          'featureList': [
            'Instant PDF Invoicing with WhatsApp Sharing',
            'Interactive In-Chat Hashtag Accounting',
            'Dynamic SUMIFS Excel Export & 90-Day Cash Forecast',
            'Live Gold Rates & Craftsmanship Calculator',
            'Zero-Trace Hardware Encrypted Vaults',
            '100% Offline-First Room Database'
          ]
        },
        {
          '@type': 'WebSite',
          'name': siteName,
          'url': canonicalUrl,
          'description': heroConfig.subheadline,
          'inLanguage': ['ar', 'en']
        }
      ]
    };

    scriptTag.text = JSON.stringify(structuredData);

  }, [heroConfig, canonicalUrl, siteName, ogImage, keywords, lang]);

  return <>{children}</>;
};

export default MetadataProvider;
