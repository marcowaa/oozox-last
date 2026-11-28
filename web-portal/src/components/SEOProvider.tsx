import React, { useEffect, useMemo } from 'react';
import { SiteHeroConfig } from '../types';

export interface FAQItem {
  q: string;
  a: string;
}

export interface SEOProviderProps {
  heroConfig: SiteHeroConfig;
  canonicalUrl?: string;
  siteName?: string;
  ogImage?: string;
  keywords?: string[];
  faqs?: FAQItem[];
  lang?: 'ar' | 'en';
  children?: React.ReactNode;
}

/**
 * Default High-Ranking ASO & Invoice Management FAQs identified in the 2026 ASO Report
 */
export const defaultOozoxFaqs: FAQItem[] = [
  {
    q: 'كيف أستفيد من فترة التجربة المجانية الشاملة لمدة 60 يوماً في أوزوكس؟',
    a: 'بمجرد تنزيل تطبيق OOZOX والتسجيل برقم هاتفك، يتم تفعيل باقة التجربة المجانية الكاملة تلقائياً لمدة شهرين كاملين (60 يوماً) مع فتح كافة الميزات المتقدمة دون الحاجة لإدخال أي بطاقة بنكية أو التزامات مسبقة.'
  },
  {
    q: 'كيف يعمل نظام إصدار فواتير PDF الإلكترونية ومشاركتها عبر واتساب؟',
    a: 'يتيح لك OOZOX إنشاء فواتير بيع وشراء ضريبية وتجارية معتمدة تدعم رمز الاستجابة السريعة (QR Code)، حساب ضريبة القيمة المضافة (VAT)، الخصومات، والأرقام التسلسلية، ثم إرسالها بصيغة PDF مباشرة إلى محادثة العميل عبر WhatsApp بنقرة واحدة.'
  },
  {
    q: 'كيف يحول نظام الهاشتاج المحاسبي الذكي (#) نصوص المحادثة إلى قيود مالية؟',
    a: 'بمجرد كتابة رمز الشباك متبوعاً باسم الصنف مثل #ذهب أو #حديد أو #بضاعة داخل المحادثة، يقوم المحرك المحاسبي في أوزوكس بتحويل النص تلقائياً إلى جدول قيود محاسبي تفاعلي لحساب إجمالي المبيعات، الدائن والمدين، والمتبقي بدقة متناهية.'
  },
  {
    q: 'هل يدعم تطبيق OOZOX تصدير كشوف الحسابات إلى Excel بصيغ SUMIFS ومصفوفات ديناميكية؟',
    a: 'نعم، يولد محرك تصدير إكسل في OOZOX ملفات جداول بيانات متطورة مدمجة بصيغ SUMIFS التلقائية، مع لوحة مؤشرات السيولة النقدية لـ 90 يوماً وتنسيق شرطي ينبهك للتركزات الائتمانية والديون المتأخرة.'
  },
  {
    q: 'ما هي الحلول المتخصصة التي يقدمها OOZOX لتجار وصاغة الذهب والمجوهرات؟',
    a: 'يقدم OOZOX متابعة لحظية لأسعار الذهب بكافة العيارات (24، 21، 18، والأونصة)، حاسبة دقيقة للدمغة والمصنعية والتقاص، وميزة قراءة شاشة ميزان الذهب الحساس آلياً عبر كاميرا الذكاء الاصطناعي (AI Scale OCR).'
  },
  {
    q: 'كيف تحمي الخزائن المشفرة بنظام التخفي (Zero-Trace Stealth) سرية المعاملات المالية؟',
    a: 'تتيح الميزة إنشاء خزائن وحسابات فرعية مستقلة مقفلة برمز PIN مخصص؛ وعند إغلاق الخزنة، تُخفى تماماً ولا تترك أي إشعارات أو سجلات مرئية على شاشة الهاتف، مع تشفير محلي كامل للبيانات بتقنية AES-256-GCM العسكرية.'
  },
  {
    q: 'هل يعمل نظام OOZOX في الميدان والمحلات عند انقطاع الإنترنت (Offline-First)؟',
    a: 'نعم بنسبة 100%! تم بناء التطبيق بهندسة Offline-First وقاعدة بيانات Room المحلية المشفرة، مما يسمح لك بإصدار الفواتير، تسجيل المدفوعات، وإدارة حركة المخزن دون إنترنت، مع مزامنة سحابية ذكية وفورية فور استعادة الاتصال.'
  },
  {
    q: 'كيف يساعد OOZOX في إدارة ديون العملاء وإرسال تذكيرات السداد الآلية؟',
    a: 'يوفر التطبيق دفتر أستاذ تفاعلي لكل عميل ومورد يرصد كل معاملة وتاريخ استحقاقها بدقة، مع إمكانية توليد وإرسال مطالبات وتذكيرات دفع دورية مهذبة وموثقة بكشف حساب رسمي عبر واتساب ورسائل SMS.'
  },
  {
    q: 'هل يتوافق OOZOX مع متطلبات الفوترة الإلكترونية وضريبة القيمة المضافة (VAT) في مصر والخليج؟',
    a: 'نعم، صُممت فواتير OOZOX لتتوافق مع معايير الفوترة الإلكترونية الحديثة، بما في ذلك حساب الضرائب المتعددة، تفقيط المبالغ، وتضمين رموز QR التشفيرية المعتمدة للتحقق من صحة الفاتورة.'
  }
];

/**
 * SEOProvider component
 * Generates and injects structured JSON-LD schemas (Organization, Product/SoftwareApplication, FAQPage)
 * and metadata for search engines and AI agents (Gemini, ChatGPT, Claude, Perplexity).
 */
export const SEOProvider: React.FC<SEOProviderProps> = ({
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
  faqs = defaultOozoxFaqs,
  lang = 'ar',
  children
}) => {
  // Generate structured JSON-LD schemas
  const structuredData = useMemo(() => {
    const activeFaqs = faqs && faqs.length > 0 ? faqs : defaultOozoxFaqs;

    const schemas: any[] = [
      // 1. Organization Schema
      {
        '@type': 'Organization',
        '@id': `${canonicalUrl}/#organization`,
        'name': siteName,
        'legalName': 'OOZOX Financial Systems LLC',
        'url': canonicalUrl,
        'logo': `${canonicalUrl}/assets/logo.png`,
        'sameAs': [
          'https://twitter.com/oozox_app',
          'https://facebook.com/oozox.app',
          'https://linkedin.com/company/oozox'
        ],
        'contactPoint': {
          '@type': 'ContactPoint',
          'telephone': '+20-100-000-0000',
          'contactType': 'customer support',
          'availableLanguage': ['Arabic', 'English'],
          'areaServed': ['EG', 'SA', 'AE', 'KW', 'QA', 'BH', 'OM']
        },
        'description': heroConfig.subheadline || 'المنظومة المحاسبية الذكية لإدارة الفواتير، الديون، وتداول الذهب.'
      },

      // 2. Product & SoftwareApplication Schema
      {
        '@type': 'SoftwareApplication',
        '@id': `${canonicalUrl}/#software`,
        'name': siteName,
        'applicationCategory': 'FinanceApplication',
        'operatingSystem': 'Android, iOS, Web',
        'softwareVersion': '3.0.0',
        'aggregateRating': {
          '@type': 'AggregateRating',
          'ratingValue': '4.9',
          'reviewCount': '1420'
        },
        'offers': {
          '@type': 'Offer',
          'price': '0',
          'priceCurrency': 'USD',
          'priceValidUntil': '2027-12-31',
          'availability': 'https://schema.org/InStock',
          'description': '60-Day Full Access Free Trial'
        },
        'description': heroConfig.subheadline,
        'featureList': [
          'Instant PDF Invoicing with WhatsApp Sharing',
          'Interactive In-Chat Hashtag Accounting (#item)',
          'Dynamic SUMIFS Excel Export & 90-Day Cash Flow Forecast',
          'Live Multi-Karat Gold Rates & Craftsmanship Calculator',
          'Zero-Trace Military-Grade AES-256 Encrypted Vaults',
          '100% Offline-First Room Database Architecture'
        ],
        'publisher': {
          '@id': `${canonicalUrl}/#organization`
        }
      },

      // 3. FAQPage Schema
      {
        '@type': 'FAQPage',
        '@id': `${canonicalUrl}/#faq`,
        'mainEntity': activeFaqs.map(faq => ({
          '@type': 'Question',
          'name': faq.q,
          'acceptedAnswer': {
            '@type': 'Answer',
            'text': faq.a
          }
        }))
      }
    ];

    return {
      '@context': 'https://schema.org',
      '@graph': schemas
    };
  }, [heroConfig, canonicalUrl, siteName, faqs]);

  // Inject meta tags, canonical link, and JSON-LD script into DOM head
  useEffect(() => {
    if (typeof document === 'undefined') return;

    // Document Title
    const titleText = `${siteName} — ${heroConfig.headline || 'نظام الفواتير والمحاسبة الذكي'} | ${heroConfig.brandHighlight || 'تداول الذهب والديون'}`;
    document.title = titleText;

    // Helper: Meta Tag
    const setMetaTag = (attrName: 'name' | 'property', attrValue: string, content: string) => {
      let meta = document.querySelector(`meta[${attrName}="${attrValue}"]`) as HTMLMetaElement | null;
      if (!meta) {
        meta = document.createElement('meta');
        meta.setAttribute(attrName, attrValue);
        document.head.appendChild(meta);
      }
      meta.content = content;
    };

    // Helper: Link Tag
    const setLinkTag = (rel: string, href: string) => {
      let link = document.querySelector(`link[rel="${rel}"]`) as HTMLLinkElement | null;
      if (!link) {
        link = document.createElement('link');
        link.setAttribute('rel', rel);
        document.head.appendChild(link);
      }
      link.href = href;
    };

    // SEO Meta
    setMetaTag('name', 'description', heroConfig.subheadline || 'منظومة أوزوكس الذكية لإصدار الفواتير ومتابعة الديون وحسابات الذهب');
    setMetaTag('name', 'keywords', keywords.join(', '));
    setMetaTag('name', 'author', 'OOZOX Technologies');
    setMetaTag('name', 'robots', 'index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1');
    setMetaTag('name', 'theme-color', '#020617');

    // OpenGraph Meta
    setMetaTag('property', 'og:site_name', siteName);
    setMetaTag('property', 'og:type', 'website');
    setMetaTag('property', 'og:title', `${heroConfig.headline} — ${heroConfig.brandHighlight}`);
    setMetaTag('property', 'og:description', heroConfig.subheadline);
    setMetaTag('property', 'og:url', canonicalUrl);
    setMetaTag('property', 'og:image', ogImage);
    setMetaTag('property', 'og:locale', lang === 'ar' ? 'ar_AR' : 'en_US');

    // Twitter Card Meta
    setMetaTag('name', 'twitter:card', 'summary_large_image');
    setMetaTag('name', 'twitter:title', `${heroConfig.headline} | ${siteName}`);
    setMetaTag('name', 'twitter:description', heroConfig.subheadline);
    setMetaTag('name', 'twitter:image', ogImage);

    // Canonical
    setLinkTag('canonical', canonicalUrl);

    // Inject JSON-LD Script Tag in Head
    const scriptId = 'oozox-seo-jsonld-schema';
    let script = document.getElementById(scriptId) as HTMLScriptElement | null;
    if (!script) {
      script = document.createElement('script');
      script.id = scriptId;
      script.type = 'application/ld+json';
      document.head.appendChild(script);
    }
    script.text = JSON.stringify(structuredData);

  }, [heroConfig, canonicalUrl, siteName, ogImage, keywords, lang, structuredData]);

  return (
    <>
      {/* Inline Schema for immediate Server, Browser, and AI Crawler discovery */}
      <script
        id="oozox-ssr-jsonld-schema"
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(structuredData)
        }}
      />
      {children}
    </>
  );
};

export default SEOProvider;
