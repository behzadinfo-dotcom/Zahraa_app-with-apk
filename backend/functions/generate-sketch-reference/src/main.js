/**
 * generate-sketch-reference — «ایجنت طراح تصاویر مرجع سیاه‌قلم» (فایل پرامپت ۰۲، بخش ۵).
 *
 * کاربر یک موضوع (چهره/طبیعت/حیوان/اشیاء) و یک سطح مهارت (۴ تا ۱۰ از ۱۰) انتخاب می‌کند
 * و این تابع یک تصویر مرجعِ سیاه‌قلم (فقط سیاه‌وسفید، مناسب کپی با مداد) تولید می‌کند
 * و در گالری «مرجع‌های من» (`sketch_references`) ذخیره می‌کند.
 *
 * ورودی:  { "subject":"چهره", "level":7 }
 * خروجی:  { ok:true, imageUrl:"...", refId:"...", level:7, subject:"..." }
 *          { ok:false, error:"not_configured"|"bad_level"|"upstream_error" }
 */
const { resolveProviders, logUsage } = require('./ai-provider');

let sdk = null;
function loadSdk() { if (!sdk) { try { sdk = require('node-appwrite'); } catch (e) { sdk = null; } } return sdk; }

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || process.env.APPWRITE_FUNCTION_DATABASE_ID || 'main_db';
const SKETCH_TABLE = 'sketch_references';
const USED_FOR = 'generate-sketch-reference';

const SUBJECTS = ['چهره', 'طبیعت', 'حیوان', 'اشیاء'];

function parseBody(req) {
  if (!req || !req.body) return {};
  if (typeof req.body === 'string') { try { return JSON.parse(req.body); } catch (e) { return {}; } }
  return req.body;
}
function userIdOf(req) {
  return (req && (req.userId || (req.headers && (req.headers['x-appwrite-user-id'] || req.headers['X-Appwrite-User-Id'])))) || '';
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
      create: service.createRow ? (db, t, id, d, p) => service.createRow(db, t, id, d, p) : (db, t, id, d, p) => service.createDocument(db, t, id, d, p),
      list: service.listRows ? (db, t, q) => service.listRows(db, t, q) : (db, t, q) => service.listDocuments(db, t, q),
      Query: s.Query, ID: s.ID, Permission: s.Permission, Role: s.Role,
    };
  } catch (e) { return null; }
}

/** توضیح سطح مهارت طبق فایل ۰۲. */
function levelDescription(level) {
  if (level <= 4) return 'خطوط ساده و سایه‌زنی پایه';
  if (level <= 5) return 'خطوط تمیز با سایه‌زنی ملایم';
  if (level <= 7) return 'سایه‌زنی میان‌رده و تناسبات دقیق‌تر';
  if (level <= 8) return 'جزئیات بیشتر، نور و سایه‌ی متوسط تا پیشرفته';
  return 'جزئیات حرفه‌ای، نور و سایه‌ی پیچیده، بافت';
}

function buildPrompt(subject, level) {
  return [
    `یک تصویر مرجع برای تمرین نقاشی سیاه‌قلم (پنسیل)، موضوع: ${subject}، سطح مهارت: ${level}/۱۰`,
    `(سطح ${level} = ${levelDescription(level)})،`,
    'تصویر باید فقط طرح خطی/کدر سیاه و سفید باشد (نه رنگی)، مناسب کپی‌کردن با مداد روی کاغذ.',
  ].join(' ');
}

async function callImageProvider(provider, prompt) {
  const endpoint = provider.imageEndpointUrl || process.env.AI_IMAGE_ENDPOINT || '';
  if (!endpoint) return { error: 'no_image_endpoint' };
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 25000);
  try {
    const upstream = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${provider.apiKey}` },
      body: JSON.stringify({ model: provider.modelId, prompt, n: 1, size: '768x768' }),
      signal: controller.signal,
    });
    clearTimeout(timer);
    if (!upstream.ok) return { error: 'upstream_error', status: upstream.status };
    const payload = await upstream.json();
    const url = payload && payload.data && payload.data[0] && (payload.data[0].url || payload.data[0].b64_json);
    if (!url) return { error: 'empty_image' };
    return { imageUrl: String(url) };
  } catch (err) {
    clearTimeout(timer);
    return { error: 'network_error' };
  }
}

module.exports = async function generateSketchReference(req, res) {
  const requestId = Math.random().toString(36).slice(2, 10);
  const userId = userIdOf(req);
  if (!userId) return res.json({ ok: false, error: 'no_user' }, 401);
  const body = parseBody(req);
  const subject = SUBJECTS.includes(String(body.subject)) ? String(body.subject) : (String(body.subject || 'چهره').trim() || 'چهره');
  const level = Math.round(Number(body.level));
  if (!Number.isFinite(level) || level < 4 || level > 10) {
    return res.json({ ok: false, error: 'bad_level', fallback: 'سطح باید بین ۴ تا ۱۰ باشد.' });
  }

  const tables = tablesService();
  const providers = tables
    ? await resolveProviders(tables, DATABASE_ID, USED_FOR, tables.Query)
    : (require('./ai-provider').envProvider() ? [require('./ai-provider').envProvider()] : []);
  const imageProviders = providers.filter((p) => p.imageEndpointUrl || process.env.AI_IMAGE_ENDPOINT);
  if (!imageProviders.length) {
    return res.json({ ok: false, error: 'not_configured', fallback: 'سرویس تولید تصویر مرجع تنظیم نشده.' });
  }

  const prompt = buildPrompt(subject, level);
  let lastError = 'upstream_error';
  for (const provider of imageProviders) {
    const result = await callImageProvider(provider, prompt);
    if (result.imageUrl) {
      let refId = `sk-${Date.now().toString(36)}`;
      if (tables) {
        refId = (tables.ID && tables.ID.unique && tables.ID.unique()) || refId;
        const data = { userId, subject, level, imageUrl: result.imageUrl, prompt, createdAtMs: Date.now() };
        try {
          const perms = tables.Permission && tables.Role ? [tables.Permission.read(tables.Role.user(userId))] : [];
          await tables.create(DATABASE_ID, SKETCH_TABLE, refId, data, perms);
        } catch (e) { console.error('generate-sketch-reference write failed', e && e.message ? e.message : e); }
        if (provider.id !== 'env') await logUsage(tables, DATABASE_ID, provider.id, USED_FOR, tables.ID);
      }
      console.log('generate-sketch-reference ok', { requestId, provider: provider.providerName, subject, level });
      return res.json({ ok: true, imageUrl: result.imageUrl, refId, level, subject });
    }
    lastError = result.error || 'upstream_error';
  }
  return res.json({ ok: false, error: lastError, fallback: 'تولید مرجع الان ممکن نشد؛ بعداً دوباره امتحان کن.' });
};
