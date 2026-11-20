// ============================================================================
// محرك المحاسب الذكي v2 — تحليل ذكي + تنبيهات + ربط AI + أخبار
// ============================================================================

import { ChatAccount, HashtagAccount, HashtagEntry, NewsArticle, AdminConfig } from '@/types';
import { calculateHashtagTotals, formatCurrency } from './gold';

// ============================================================================
// أنواع البيانات
// ============================================================================

export interface AccountSummary {
  accountName: string;
  accountId: string;
  chatId: string;
  hashtags: {
    tag: string;
    hashtagId: string;
    totalDebit: number;
    totalCredit: number;
    remaining: number;
    entryCount: number;
    status: string;
  }[];
  totalDebit: number;
  totalCredit: number;
  totalRemaining: number;
  isSettled: boolean;
}

export interface ChatFinancialReport {
  chatId: string;
  chatName: string;
  accounts: AccountSummary[];
  grandTotalDebit: number;
  grandTotalCredit: number;
  grandTotalRemaining: number;
  totalTransactions: number;
  settledAccounts: number;
  activeAccounts: number;
}

export interface GlobalFinancialReport {
  totalChats: number;
  totalAccounts: number;
  totalHashtags: number;
  totalTransactions: number;
  grandTotalDebit: number;
  grandTotalCredit: number;
  grandTotalRemaining: number;
  owedToMe: { chatName: string; amount: number }[];
  iOwe: { chatName: string; amount: number }[];
  topAccounts: { chatName: string; accountName: string; remaining: number }[];
  weeklyActivity: { day: string; count: number; amount: number }[];
}

export interface SmartAlert {
  id: string;
  type: 'warning' | 'error' | 'info' | 'success' | 'critical';
  title: string;
  message: string;
  accountName?: string;
  chatName?: string;
  amount?: number;
  createdAt: number;
}

export interface AIAnalysisResult {
  insights: string[];
  alerts: SmartAlert[];
  recommendations: string[];
  healthScore: number; // 0-100
}

// ============================================================================
// تحليل الحساب
// ============================================================================

export function analyzeAccount(account: ChatAccount): AccountSummary {
  const hashtags = account.hashtags.map(h => {
    const totals = calculateHashtagTotals(h.entries);
    return {
      tag: h.tag,
      hashtagId: h.id,
      totalDebit: totals.totalDebit,
      totalCredit: totals.totalCredit,
      remaining: totals.remaining,
      entryCount: h.entries.length,
      status: h.status,
    };
  });

  const totalDebit = hashtags.reduce((s, h) => s + h.totalDebit, 0);
  const totalCredit = hashtags.reduce((s, h) => s + h.totalCredit, 0);

  return {
    accountName: account.name,
    accountId: account.id,
    chatId: account.chatId,
    hashtags,
    totalDebit,
    totalCredit,
    totalRemaining: totalDebit - totalCredit,
    isSettled: account.status === 'settled',
  };
}

// ============================================================================
// تقرير محادثة كامل
// ============================================================================

export function generateChatReport(
  chatId: string,
  chatName: string,
  accounts: ChatAccount[],
): ChatFinancialReport {
  const summaries = accounts.map(analyzeAccount);
  const grandTotalDebit = summaries.reduce((s, a) => s + a.totalDebit, 0);
  const grandTotalCredit = summaries.reduce((s, a) => s + a.totalCredit, 0);
  const totalTransactions = summaries.reduce((s, a) => s + a.hashtags.reduce((sh, h) => sh + h.entryCount, 0), 0);
  const settledAccounts = summaries.filter(a => a.isSettled).length;

  return {
    chatId, chatName, accounts: summaries,
    grandTotalDebit, grandTotalCredit,
    grandTotalRemaining: grandTotalDebit - grandTotalCredit,
    totalTransactions, settledAccounts,
    activeAccounts: summaries.length - settledAccounts,
  };
}

// ============================================================================
// تقرير شامل لكل المحادثات
// ============================================================================

export function generateGlobalReport(
  allAccounts: ChatAccount[],
  chats: { id: string; contactName: string }[],
): GlobalFinancialReport {
  const chatMap = new Map(chats.map(c => [c.id, c.contactName]));
  const reports = allAccounts.map(a => ({
    chatId: a.chatId,
    chatName: chatMap.get(a.chatId) ?? 'غير معروف',
    accounts: [analyzeAccount(a)],
  }));

  const totalAccounts = allAccounts.length;
  const totalHashtags = allAccounts.reduce((s, a) => s + a.hashtags.length, 0);
  const totalTransactions = allAccounts.reduce(
    (s, a) => s + a.hashtags.reduce((sh, h) => sh + h.entries.length, 0), 0,
  );
  const grandTotalDebit = allAccounts.reduce(
    (s, a) => s + a.hashtags.reduce((sh, h) => sh + calculateHashtagTotals(h.entries).totalDebit, 0), 0,
  );
  const grandTotalCredit = allAccounts.reduce(
    (s, a) => s + a.hashtags.reduce((sh, h) => sh + calculateHashtagTotals(h.entries).totalCredit, 0), 0,
  );

  const chatBalances = new Map<string, number>();
  for (const report of reports) {
    const balance = report.accounts.reduce((s, a) => s + a.totalRemaining, 0);
    const current = chatBalances.get(report.chatName) ?? 0;
    chatBalances.set(report.chatName, current + balance);
  }

  const owedToMe: { chatName: string; amount: number }[] = [];
  const iOwe: { chatName: string; amount: number }[] = [];
  for (const [name, amount] of chatBalances) {
    if (amount > 0) owedToMe.push({ chatName: name, amount });
    else if (amount < 0) iOwe.push({ chatName: name, amount: Math.abs(amount) });
  }
  owedToMe.sort((a, b) => b.amount - a.amount);
  iOwe.sort((a, b) => b.amount - a.amount);

  const topAccounts = allAccounts
    .map(a => {
      const summary = analyzeAccount(a);
      return {
        chatName: chatMap.get(a.chatId) ?? 'غير معروف',
        accountName: a.name,
        remaining: summary.totalRemaining,
      };
    })
    .sort((a, b) => Math.abs(b.remaining) - Math.abs(a.remaining))
    .slice(0, 5);

  const now = Date.now();
  const days: { day: string; count: number; amount: number }[] = [];
  for (let i = 6; i >= 0; i--) {
    const dayStart = now - i * 86400000;
    const dayEnd = dayStart + 86400000;
    const dayName = new Date(dayStart).toLocaleDateString('ar-EG', { weekday: 'short' });
    let count = 0;
    let amount = 0;
    for (const acc of allAccounts) {
      for (const h of acc.hashtags) {
        for (const e of h.entries) {
          if (e.createdAt >= dayStart && e.createdAt < dayEnd) {
            count++;
            amount += e.amount;
          }
        }
      }
    }
    days.push({ day: dayName, count, amount });
  }

  return {
    totalChats: reports.length, totalAccounts, totalHashtags, totalTransactions,
    grandTotalDebit, grandTotalCredit,
    grandTotalRemaining: grandTotalDebit - grandTotalCredit,
    owedToMe, iOwe, topAccounts, weeklyActivity: days,
  };
}

// ============================================================================
// محرك التنبيهات الذكي — يفحص كل الحسابات ويكشف المشاكل تلقائياً
// ============================================================================

export function generateSmartAlerts(
  chatAccounts: ChatAccount[],
  chatName: string,
): SmartAlert[] {
  const alerts: SmartAlert[] = [];
  const now = Date.now();

  for (const account of chatAccounts) {
    const summary = analyzeAccount(account);

    // 1. دين كبير ومستمر لفترة طويلة
    if (summary.totalRemaining > 50000 && !summary.isSettled) {
      const oldestEntry = account.hashtags
        .flatMap(h => h.entries)
        .sort((a, b) => a.createdAt - b.createdAt)[0];
      if (oldestEntry) {
        const daysOld = Math.floor((now - oldestEntry.createdAt) / 86400000);
        if (daysOld > 30) {
          alerts.push({
            id: `debt_old_${account.id}`,
            type: 'warning',
            title: '⚠️ دين مستمر لفترة طويلة',
            message: `حساب "${account.name}" لديه دين ${formatCurrency(summary.totalRemaining)} مستمر منذ ${daysOld} يوم. ننصح بمتابعة العميل للتسوية.`,
            accountName: account.name,
            chatName,
            amount: summary.totalRemaining,
            createdAt: now,
          });
        }
      }
    }

    // 2. دفع أكبر من الدين (overpayment)
    if (summary.totalRemaining < 0) {
      alerts.push({
        id: `overpay_${account.id}`,
        type: 'error',
        title: '🚨 دفعة زائدة عن الدين',
        message: `حساب "${account.name}" تم دفع ${formatCurrency(Math.abs(summary.totalRemaining))} أكثر من المطلوب. يرجى مراجعة المعاملات.`,
        accountName: account.name,
        chatName,
        amount: Math.abs(summary.totalRemaining),
        createdAt: now,
      });
    }

    // 3. معاملة بمبلغ غير منطقي (مبالغ فيه)
    for (const hashtag of account.hashtags) {
      for (const entry of hashtag.entries) {
        // مبلغ ضخم جداً (> مليون)
        if (entry.amount > 1000000) {
          alerts.push({
            id: `huge_${entry.id}`,
            type: 'critical',
            title: '🚨 مبلغ ضخم غير معتاد',
            message: `معاملة "${entry.description}" بمبلغ ${formatCurrency(entry.amount)} في حساب "${account.name}". هل هذا الرقم صحيح؟`,
            accountName: account.name,
            chatName,
            amount: entry.amount,
            createdAt: now,
          });
        }

        // مبلغ بصفر أو سالب
        if (entry.amount <= 0) {
          alerts.push({
            id: `invalid_${entry.id}`,
            type: 'error',
            title: '❌ مبلغ غير صالح',
            message: `معاملة "${entry.description}" تحتوي على مبلغ ${formatCurrency(entry.amount)}. يرجى تصحيح الرقم.`,
            accountName: account.name,
            chatName,
            amount: entry.amount,
            createdAt: now,
          });
        }

        // مبلغ مكرر (نفس المبلغ والتاريخ تقريباً)
        const similarEntries = hashtag.entries.filter(e =>
          e.amount === entry.amount &&
          Math.abs(e.createdAt - entry.createdAt) < 3600000 &&
          e.id !== entry.id
        );
        if (similarEntries.length > 0 && entry.direction === 'debit') {
          alerts.push({
            id: `dup_${entry.id}`,
            type: 'warning',
            title: '⚠️ معاملة مكررة محتملة',
            message: `معاملة "${entry.description}" بمبلغ ${formatCurrency(entry.amount)} قد تكون مكررة. تأكد من عدم إدخالها مرتين.`,
            accountName: account.name,
            chatName,
            amount: entry.amount,
            createdAt: now,
          });
        }
      }
    }

    // 4. حساب نشط بدون معاملات لفترة طويلة
    if (!summary.isSettled && summary.totalDebit > 0) {
      const lastEntry = account.hashtags
        .flatMap(h => h.entries)
        .sort((a, b) => b.createdAt - a.createdAt)[0];
      if (lastEntry) {
        const daysInactive = Math.floor((now - lastEntry.createdAt) / 86400000);
        if (daysInactive > 14) {
          alerts.push({
            id: `inactive_${account.id}`,
            type: 'info',
            title: '📭 حساب غير نشط',
            message: `حساب "${account.name}" لم تُسجل فيه معاملات منذ ${daysInactive} يوم. المتبقي: ${formatCurrency(summary.totalRemaining)}`,
            accountName: account.name,
            chatName,
            amount: summary.totalRemaining,
            createdAt: now,
          });
        }
      }
    }

    // 5. حساب بدون أي فواتير (مدين صفر)
    if (summary.totalDebit === 0 && summary.totalCredit === 0 && account.hashtags.length > 0) {
      alerts.push({
        id: `empty_${account.id}`,
        type: 'info',
        title: '📝 حساب فارغ',
        message: `حساب "${account.name}" لا يحتوي على أي معاملات بعد.`,
        accountName: account.name,
        chatName,
        createdAt: now,
      });
    }
  }

  return alerts;
}

// ============================================================================
// تحليل صحة الحسابات (Health Score)
// ============================================================================

export function calculateHealthScore(
  chatAccounts: ChatAccount[],
  chatName: string,
): AIAnalysisResult {
  const alerts = generateSmartAlerts(chatAccounts, chatName);
  const report = generateChatReport('', chatName, chatAccounts);
  const insights: string[] = [];
  const recommendations: string[] = [];

  // Insights
  if (report.grandTotalRemaining > 0) {
    insights.push(`💰 إجمالي الدين المستحق: ${formatCurrency(report.grandTotalRemaining)}`);
  }
  if (report.grandTotalRemaining < 0) {
    insights.push(`💚 لديك رصيد دائن: ${formatCurrency(Math.abs(report.grandTotalRemaining))}`);
  }
  if (report.settledAccounts === report.accounts.length && report.accounts.length > 0) {
    insights.push('✅ جميع الحسابات مسدّدة');
  }
  if (report.activeAccounts > 3) {
    insights.push(`📈 لديك ${report.activeAccounts} حسابات نشطة — تنظيم جيد`);
  }

  // Recommendations based on alerts
  const criticalCount = alerts.filter(a => a.type === 'critical').length;
  const errorCount = alerts.filter(a => a.type === 'error').length;
  const warningCount = alerts.filter(a => a.type === 'warning').length;

  if (criticalCount > 0) {
    recommendations.push(`🚨 لديك ${criticalCount} مشكلة حرجة تتطلب مراجعة فورية`);
  }
  if (errorCount > 0) {
    recommendations.push(`❌ ${errorCount} خطأ في البيانات — راجع المعاملات المعلّمة`);
  }
  if (warningCount > 0) {
    recommendations.push(`⚠️ ${warningCount} تنبيه يحتاج انتباهك`);
  }

  // Long-term debt recommendation
  const oldDebtAlerts = alerts.filter(a => a.type === 'warning' && a.title.includes('مستمر'));
  if (oldDebtAlerts.length > 0) {
    recommendations.push('📅 يوجد ديون مستمرة لأكثر من 30 يوم — ننصح بالتواصل مع العملاء للتسوية');
  }

  // Overpayment recommendation
  const overpayAlerts = alerts.filter(a => a.title.includes('زائدة'));
  if (overpayAlerts.length > 0) {
    recommendations.push('💰 يوجد دفعات زائدة — راجع المعاملات وأعد المبالغ الزائدة للعملاء');
  }

  // Calculate health score (0-100)
  let score = 100;
  score -= criticalCount * 25;
  score -= errorCount * 15;
  score -= warningCount * 8;
  score -= alerts.filter(a => a.type === 'info').length * 2;
  score = Math.max(0, Math.min(100, score));

  return {
    insights,
    alerts,
    recommendations,
    healthScore: score,
  };
}

// ============================================================================
// المساعد الذكي — يجيب على أسئلة المستخدم + يقدم تنبيهات استباقية
// ============================================================================

export function askAccountant(
  question: string,
  chatAccounts: ChatAccount[],
  chatName: string,
): string {
  const q = question.trim().toLowerCase();
  const report = generateChatReport('', chatName, chatAccounts);
  const alerts = generateSmartAlerts(chatAccounts, chatName);
  const analysis = calculateHealthScore(chatAccounts, chatName);

  // تنبيهات / مشاكل / أخطاء
  if (q.includes('تنبيه') || q.includes('مشكلة') || q.includes('خطأ') || q.includes('خطاء') || q.includes('مشاكل')) {
    if (alerts.length === 0) {
      return `✅ لا توجد مشاكل أو تنبيهات في "${chatName}". صحة حساباتك ممتازة!`;
    }
    return `🔔 لديك ${alerts.length} تنبيه في "${chatName}":\n\n${alerts.map(a => `${a.title}\n${a.message}`).join('\n\n')}`;
  }

  // صحة الحساب / health / تقييم
  if (q.includes('صحة') || q.includes('تقييم') || q.includes('health') || q.includes('حالة')) {
    const scoreEmoji = analysis.healthScore >= 80 ? '🟢' : analysis.healthScore >= 50 ? '🟡' : '🔴';
    let result = `${scoreEmoji} صحة الحسابات: ${analysis.healthScore}/100\n\n`;
    if (analysis.insights.length > 0) {
      result += `📊 تحليلات:\n${analysis.insights.map(i => `• ${i}`).join('\n')}\n\n`;
    }
    if (analysis.recommendations.length > 0) {
      result += `💡 توصيات:\n${analysis.recommendations.map(r => `• ${r}`).join('\n')}`;
    }
    return result;
  }

  // كم المتبقي / كم الدين / كم المستحق
  if (q.includes('متبقي') || q.includes('دين') || q.includes('مستحق') || q.includes('عليه') || q.includes('له')) {
    if (report.grandTotalRemaining > 0) {
      let result = `💰 إجمالي المبلغ المستحق عليك في "${chatName}": ${formatCurrency(report.grandTotalRemaining)}\n\nتفصيل:\n${report.accounts.map(a => `• ${a.accountName}: ${formatCurrency(a.totalRemaining)}`).join('\n')}`;
      // Add warnings if any
      const debtAlerts = alerts.filter(a => a.title.includes('مستمر') || a.title.includes('زائدة'));
      if (debtAlerts.length > 0) {
        result += `\n\n⚠️ تنبيهات:\n${debtAlerts.map(a => `• ${a.title}: ${a.message}`).join('\n')}`;
      }
      return result;
    } else if (report.grandTotalRemaining < 0) {
      return `💚 ليس عليك أي ديون في "${chatName}" — لديك رصيد دائن ${formatCurrency(Math.abs(report.grandTotalRemaining))}`;
    } else {
      return `✅ جميع الحسابات مسدّدة في "${chatName}"`;
    }
  }

  // كم عدد المعاملات / العمليات
  if (q.includes('عدد') || q.includes('كم معاملة') || q.includes('كم عملية')) {
    return `📊 إجمالي المعاملات في "${chatName}": ${report.totalTransactions} معاملة\n• حسابات نشطة: ${report.activeAccounts}\n• حسابات مسدّدة: ${report.settledAccounts}`;
  }

  // ما هي الحسابات / قائمة الحسابات
  if (q.includes('حسابات') || q.includes('قائمة') || q.includes('الحساب')) {
    if (report.accounts.length === 0) {
      return `📭 لا توجد حسابات في "${chatName}" بعد`;
    }
    return `📋 حسابات "${chatName}":\n\n${report.accounts.map((a, i) => `${i + 1}. ${a.accountName}\n   المتبقي: ${formatCurrency(a.totalRemaining)}\n   المعاملات: ${a.hashtags.reduce((s, h) => s + h.entryCount, 0)}\n   الحالة: ${a.isSettled ? '✅ مسدّد' : '🟡 نشط'}`).join('\n\n')}`;
  }

  // ملخص / تقرير / summary
  if (q.includes('ملخص') || q.includes('تقرير') || q.includes('summary')) {
    return `📊 ملخص مالي لمحادثة "${chatName}":\n\n💰 إجمالي المدين: ${formatCurrency(report.grandTotalDebit)}\n💚 إجمالي الدائن: ${formatCurrency(report.grandTotalCredit)}\n📋 المتبقي: ${formatCurrency(report.grandTotalRemaining)}\n📈 عدد المعاملات: ${report.totalTransactions}\n✅ مسدّد: ${report.settledAccounts}/${report.accounts.length}\n\n🔔 تنبيهات: ${alerts.length}`;
  }

  // مساعدة / help / كيف
  if (q.includes('مساعدة') || q.includes('help') || q.includes('كيف')) {
    return `🤖 أنا محاسبك الذكي. اسألني عن:\n\n• "كم المتبقي؟" — لمعرفة إجمالي الدين\n• "ما هي الحسابات؟" — قائمة كل الحسابات\n• "تنبيهات" — مشاكل وأخطاء في حساباتك\n• "صحة الحساب" — تقييم شامل ودرجة\n• "ملخص" — تقرير مالي كامل\n• "أكبر دين" — أكبر المبالغ المستحقة`;
  }

  // أكبر / أعلى
  if (q.includes('أكبر') || q.includes('اعلى') || q.includes('أعلى')) {
    const sorted = [...report.accounts].sort((a, b) => b.totalRemaining - a.totalRemaining);
    if (sorted.length === 0) return 'لا توجد حسابات';
    const top = sorted[0];
    return `📈 أكبر حساب في "${chatName}":\n\n${top.accountName}\nالمتبقي: ${formatCurrency(top.totalRemaining)}\nالمعاملات: ${top.hashtags.reduce((s, h) => s + h.entryCount, 0)}`;
  }

  // رد افتراضي — يحاول فهم السؤال وتقديم رد ذكي
  // فحص إذا كان المستخدم يكتب رقماً (ربما يريد التحقق منه)
  const numberMatch = question.match(/(\d[\d,]*\.?\d*)/);
  if (numberMatch) {
    const num = parseFloat(numberMatch[1].replace(/,/g, ''));
    if (!isNaN(num)) {
      if (num > 1000000) {
        return `⚠️ الرقم ${formatCurrency(num)} كبير جداً!\n\nهل أنت متأكد أن هذا المبلغ صحيح؟ الأرقام الكبيرة قد تشير إلى خطأ إدخال. تأكد من:\n• عدد الأصفار\n• العملة الصحيحة\n• نوع المعاملة`;
      }
      if (num <= 0) {
        return `❌ الرقم ${num} غير صالح!\n\nيجب أن يكون المبلغ رقماً موجباً أكبر من صفر.`;
      }
      return `✅ الرقم ${formatCurrency(num)} مقبول.\n\nهل تريد إضافته كمعاملة؟ استخدم زر "فاتورة" أو "دفع" في جدول الحساب.`;
    }
  }

  return `🤖 لم أفهم سؤالك تماماً. جرّب:\n• "كم المتبقي؟"\n• "تنبيهات"\n• "صحة الحساب"\n• "ملخص"\n• "مساعدة"\n\nأو اكتب رقماً لأتحقق من صحته لك.`;
}

// ============================================================================
// محرك الأخبار الذكي — يربط الأخبار بحسابات المستخدم
// ============================================================================

export function analyzeNewsForUser(
  news: NewsArticle[],
  chatAccounts: ChatAccount[],
  chatName: string,
): { relevantNews: { article: NewsArticle; reason: string }[]; alerts: string[] } {
  const relevantNews: { article: NewsArticle; reason: string }[] = [];
  const alerts: string[] = [];

  const report = generateChatReport('', chatName, chatAccounts);
  const hasGoldAccounts = chatAccounts.some(a =>
    a.hashtags.some(h => h.tag.toLowerCase().includes('ذهب') || h.tag.toLowerCase().includes('gold'))
  );
  const hasLargeDebt = report.grandTotalRemaining > 100000;
  const hasConstructionAccounts = chatAccounts.some(a =>
    a.hashtags.some(h =>
      h.tag.toLowerCase().includes('رمل') ||
      h.tag.toLowerCase().includes('أسمنت') ||
      h.tag.toLowerCase().includes('حديد') ||
      h.tag.toLowerCase().includes('بناء')
    )
  );

  for (const article of news) {
    const text = (article.title + ' ' + article.summary).toLowerCase();
    let reason = '';

    // Gold-related news
    if (hasGoldAccounts && (text.includes('ذهب') || text.includes('gold') || text.includes('أسعار'))) {
      reason = 'مرتبط بحسابات الذهب لديك';
    }
    // Construction materials news
    else if (hasConstructionAccounts && (text.includes('أسمنت') || text.includes('حديد') || text.includes('رمل') || text.includes('بناء'))) {
      reason = 'مرتبط بحسابات مواد البناء لديك';
    }
    // Economic news affecting debt
    else if (hasLargeDebt && (text.includes('تضخم') || text.includes('فائدة') || text.includes('اقتصاد') || text.includes('عملة'))) {
      reason = 'قد يؤثر على ديونك المستحقة';
    }
    // General market news
    else if (text.includes('سوق') || text.includes('بورصة') || text.includes('استثمار')) {
      reason = 'قد يهمك كرجل أعمال';
    }

    if (reason) {
      relevantNews.push({ article, reason });
    }
  }

  // Generate alerts from news
  if (relevantNews.length > 0) {
    alerts.push(`📰 لديك ${relevantNews.length} خبر ذو صلة بحساباتك`);
  }

  return { relevantNews, alerts };
}

// ============================================================================
// استدعاء AI API (من إعدادات الأدمن) — يحلل البيانات بالذكاء الاصطناعي
// ============================================================================

export async function callAIApi(
  config: AdminConfig,
  prompt: string,
  data: { accounts: ChatAccount[]; chatName: string },
): Promise<string | null> {
  if (!config.notificationApiUrl || !config.notificationApiKey) {
    return null;
  }

  try {
    const report = generateChatReport('', data.chatName, data.accounts);
    const alerts = generateSmartAlerts(data.accounts, data.chatName);
    const analysis = calculateHealthScore(data.accounts, data.chatName);

    const fullPrompt = `${prompt}\n\nبيانات الحسابات:\n${JSON.stringify({
      chatName: data.chatName,
      totalDebit: report.grandTotalDebit,
      totalCredit: report.grandTotalCredit,
      remaining: report.grandTotalRemaining,
      accounts: report.accounts.length,
      transactions: report.totalTransactions,
      alerts: alerts.length,
      healthScore: analysis.healthScore,
    }, null, 2)}`;

    const response = await fetch(config.notificationApiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${config.notificationApiKey}`,
      },
      body: JSON.stringify({ prompt: fullPrompt }),
    });

    if (response.ok) {
      const result = await response.json();
      return result.response ?? result.text ?? result.message ?? null;
    }
    return null;
  } catch {
    return null;
  }
}
