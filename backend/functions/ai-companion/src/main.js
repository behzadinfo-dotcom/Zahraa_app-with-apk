/**
 * ai-companion — لایه‌ی AI «همراه زهرا».
 *
 * چرا یک تابع سرور و نه صدازدن مستقیم از اپ؟
 *  ۱) **کلید API هرگز در APK نیست** (مهم‌ترین دلیل).
 *  ۲) پرامپت سیستمی و قواعد ایمنی سمت سرور است و با به‌روزرسانی تابع عوض می‌شود،
 *     بدون انتشار نسخه‌ی تازه‌ی اپ.
 *  ۳) «تصمیم بحران» این‌جا هم گرفته می‌شود: اگر پیام نشانه‌ی آسیب به خود داشت،
 *     **اصلاً به مدل فرستاده نمی‌شود** و پاسخ ایمن + شماره‌های کمک برمی‌گردد.
 *
 * چند-مدلی (فایل پرامپت ۰۴ بخش ۳): این تابع دیگر فقط به AI_API_KEY تکیه نمی‌کند.
 * اول providerهای فعالِ کالکشن `ai_providers` (که `usedFor` شامل `ai-companion` است)
 * را به‌ترتیب اولویت امتحان می‌کند؛ اگر همه fail شدند، به متغیرهای محیطی fallback
 * می‌کند. جزئیات در `ai-provider.js`.
 *
 * ورودی:
 *   { "message": "امروز خیلی خسته‌ام", "history": [{"role":"user"|"assistant","content":"..."}], "tone": "warm"|"formal" }
 * خروجی:
 *   { ok: true, reply: "...", model: "...", provider: "...", crisis: false }
 *   { ok: false, error: "not_configured"|"upstream_error", fallback: "..." }
 *
 * متغیرهای محیطی (fallback؛ در کنسول Appwrite › Functions › ai-companion › Variables):
 *   AI_API_KEY, AI_ENDPOINT, AI_MODELS, AI_MODEL, AI_MAX_TOKENS, AI_TEMPERATURE
 *   AI_CONFIG_SECRET — راز رمزگشایی کلیدهای کالکشن ai_providers (برای حالت چند-مدلی)
 *
 * حریم خصوصی: متن پیام **ذخیره نمی‌شود** و لاگ هم نمی‌شود (فقط طول پیام و وضعیت).
 * تاریخچه‌ی چت فقط روی دستگاه زهراست (`chat_history` در فهرست never-sync).
 */
const { resolveProviders, logUsage } = require('./ai-provider');

let sdk = null;
function loadSdk() { if (!sdk) { try { sdk = require('node-appwrite'); } catch (e) { sdk = null; } } return sdk; }

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || process.env.APPWRITE_FUNCTION_DATABASE_ID || 'main_db';
const USED_FOR = 'ai-companion';

const CRISIS_KEYWORDS = [
  'خودکشی', 'خودکشی کردن', 'می‌خوام بمیرم', 'میخوام بمیرم', 'کاش نبودم', 'کاش بمیرم',
  'آسیب به خود', 'خودزنی', 'بریدن دست', 'تمامش کنم', 'دیگه نمی‌تونم ادامه بدم',
];

const HELPLINES = [
  { name: 'صدای مشاور (بهزیستی)', number: '1480' },
  { name: 'اورژانس اجتماعی', number: '123' },
  { name: 'مشاوره‌ی نوجوان', number: '1570' },
];

const CRISIS_REPLY =
  'ممنون که این را گفتی — گفتنش شجاعت می‌خواهد. من یک برنامه‌ام و درمانگر نیستم، ' +
  'پس لطفاً همین حالا با یک آدم واقعی حرف بزن: بابا، مامان، مشاور مدرسه یا یکی از این شماره‌ها: ' +
  '۱۴۸۰ (صدای مشاور)، ۱۲۳ (اورژانس اجتماعی)، ۱۵۷۰ (مشاوره‌ی نوجوان). ' +
  'اگر الان در خطر فوری هستی با ۱۱۵ تماس بگیر. تو تنها نیستی.';

const SYSTEM_PROMPT_WARM = [
  'تو «همراه زهرا» هستی: یک همراه مهربان و کوتاه‌حرف برای یک نوجوان ایرانی.',
  'قواعد سخت:',
  '۱) همیشه فارسی ساده و صمیمی بنویس؛ حداکثر ۴ جمله.',
  '۲) هیچ‌وقت تظاهر نکن انسان یا درمانگر هستی؛ اگر پرسیدند بگو یک برنامه‌ای.',
  '۳) هیچ توصیه‌ی پزشکی، دارویی، حقوقی یا مالی نده؛ اگر لازم بود بگو «با یک بزرگ‌تر قابل اعتماد یا مشاور حرف بزن».',
  '۴) درباره‌ی آسیب به خود، خودکشی، خشونت یا مواد: راهنمایی عملی نده؛ فقط همدلی کن و ارجاع بده به بزرگ‌تر قابل اعتماد و شماره‌های ۱۴۸۰، ۱۲۳، ۱۵۷۰.',
  '۵) قضاوت نکن، نصیحت طولانی نکن، و در پایان یک سؤال کوچک و قابل‌جواب بپرس.',
  '۶) اگر نمی‌دانی، بگو نمی‌دانم؛ حدس نزن و منبع نساز.',
].join('\n');

const SYSTEM_PROMPT_FORMAL =
  SYSTEM_PROMPT_WARM.replace('فارسی ساده و صمیمی', 'فارسی ساده و کمی رسمی‌تر');

function parseBody(req) {
  if (!req || !req.body) return {};
  if (typeof req.body === 'string') {
    try { return JSON.parse(req.body); } catch (e) { return {}; }
  }
  return req.body;
}

function isCrisis(text) {
  const normalized = String(text || '').replace(/[يی]/g, 'ی').replace(/ك/g, 'ک').toLowerCase();
  return CRISIS_KEYWORDS.some((k) => normalized.includes(k));
}

function sanitizeHistory(history) {
  if (!Array.isArray(history)) return [];
  return history
    .filter((m) => m && (m.role === 'user' || m.role === 'assistant') && typeof m.content === 'string')
    .slice(-6)
    .map((m) => ({ role: m.role, content: m.content.slice(0, 2000) }));
}

/** پاسخ هم از فرمت OpenAI-سازگار و هم از فرمت Gemini خوانده می‌شود. */
function extractReply(payload) {
  if (!payload) return '';
  const openai = payload.choices && payload.choices[0];
  if (openai) {
    if (openai.message && typeof openai.message.content === 'string') return openai.message.content.trim();
    if (typeof openai.text === 'string') return openai.text.trim();
  }
  const gemini = payload.candidates && payload.candidates[0];
  if (gemini) {
    const parts = gemini.content && gemini.content.parts;
    if (Array.isArray(parts)) return parts.map((p) => p.text || '').join('').trim();
    if (typeof gemini.output === 'string') return gemini.output.trim();
  }
  if (typeof payload.reply === 'string') return payload.reply.trim();
  if (typeof payload.response === 'string') return payload.response.trim();
  return '';
}

function tablesService() {
  const s = loadSdk();
  if (!s) return null;
  try {
    const client = new s.Client()
      .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
      .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
      .setKey(process.env.APPWRITE_FUNCTION_API_KEY);
    const service = s.TablesDB ? new s.TablesDB(client) : new s.Databases(client);
    return {
      list: service.listRows ? (db, t, q) => service.listRows(db, t, q) : (db, t, q) => service.listDocuments(db, t, q),
      get: service.getRow ? (db, t, id) => service.getRow(db, t, id) : (db, t, id) => service.getDocument(db, t, id),
      create: service.createRow ? (db, t, id, d, p) => service.createRow(db, t, id, d, p) : (db, t, id, d, p) => service.createDocument(db, t, id, d, p),
      update: service.updateRow ? (db, t, id, d) => service.updateRow(db, t, id, d) : (db, t, id, d) => service.updateDocument(db, t, id, d),
      Query: s.Query,
      ID: s.ID,
    };
  } catch (e) {
    return null;
  }
}

/** یک provider را با upstream امتحان می‌کند. برمی‌گرداند {reply} یا null (تا fallback بعدی امتحان شود). */
async function tryProvider(provider, messages, temperature, maxTokens) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 12000);
  try {
    const upstream = await fetch(provider.endpointUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${provider.apiKey}` },
      body: JSON.stringify({ model: provider.modelId, messages, temperature, max_tokens: maxTokens, stream: false }),
      signal: controller.signal,
    });
    clearTimeout(timer);
    if (!upstream.ok) {
      console.error('ai-companion upstream status', { provider: provider.providerName, status: upstream.status });
      return { error: 'upstream_error', status: upstream.status };
    }
    const payload = await upstream.json();
    const reply = extractReply(payload);
    if (!reply) return { error: 'empty_reply' };
    return { reply };
  } catch (err) {
    clearTimeout(timer);
    console.error('ai-companion provider failed', { provider: provider.providerName, name: err && err.name ? err.name : 'error' });
    return { error: 'network_error' };
  }
}

module.exports = async function aiCompanion(req, res) {
  const requestId = Math.random().toString(36).slice(2, 10);
  const userId = (req && (req.userId || (req.headers && (req.headers['x-appwrite-user-id'] || req.headers['X-Appwrite-User-Id'])))) || '';
  const body = parseBody(req);
  const message = String(body.message || '').trim().slice(0, 2000);
  const tone = body.tone === 'formal' ? 'formal' : 'warm';

  // ۱) ایمنی اول: پیام بحران اصلاً به مدل نمی‌رود.
  if (isCrisis(message)) {
    console.log('ai-companion crisis', { requestId, userId, promptLength: message.length });
    return res.json({ ok: true, crisis: true, reply: CRISIS_REPLY, helplines: HELPLINES, model: 'safety-local' });
  }

  if (!message) {
    return res.json({ ok: false, error: 'empty_message', fallback: 'چیزی ننوشتی؛ هر وقت خواستی بنویس.' });
  }

  const messages = [
    { role: 'system', content: tone === 'formal' ? SYSTEM_PROMPT_FORMAL : SYSTEM_PROMPT_WARM },
    ...sanitizeHistory(body.history),
    { role: 'user', content: message },
  ];
  const temperature = Number(process.env.AI_TEMPERATURE || 0.6);
  const maxTokens = Number(process.env.AI_MAX_TOKENS || 400);

  const tables = tablesService();
  const providers = tables
    ? await resolveProviders(tables, DATABASE_ID, USED_FOR, tables.Query)
    : (require('./ai-provider').envProvider() ? [require('./ai-provider').envProvider()] : []);

  if (!providers.length) {
    // صادقانه: اپ این را می‌بیند و به قواعد محلی برمی‌گردد.
    console.log('ai-companion not_configured', { requestId, userId, promptLength: message.length });
    return res.json({
      ok: false,
      error: 'not_configured',
      fallback: 'لایه‌ی AI روی سرور تنظیم نشده (نه کالکشن ai_providers و نه AI_API_KEY). فعلاً با قواعد محلی جواب می‌دهم.',
    });
  }

  let lastError = 'upstream_error';
  for (const provider of providers) {
    const result = await tryProvider(provider, messages, temperature, maxTokens);
    if (result.reply) {
      // لاگ ساختاریافته (بدون متن): request id, userId, طول ورودی/خروجی، provider.
      console.log('ai-companion ok', {
        requestId, userId, provider: provider.providerName, model: provider.modelId,
        promptLength: message.length, responseLength: result.reply.length, responseStatus: 'ok',
      });
      if (tables && provider.id !== 'env') await logUsage(tables, DATABASE_ID, provider.id, USED_FOR, tables.ID);
      return res.json({ ok: true, crisis: false, reply: result.reply, model: provider.modelId, provider: provider.providerName });
    }
    lastError = result.error || 'upstream_error';
    console.log('ai-companion fallback', { requestId, userId, provider: provider.providerName, error: lastError });
  }

  console.error('ai-companion all_failed', { requestId, userId, error: lastError });
  return res.json({ ok: false, error: lastError, fallback: 'الان به هیچ مدلی نرسیدم. چند دقیقه دیگر دوباره امتحان کن.' });
};
