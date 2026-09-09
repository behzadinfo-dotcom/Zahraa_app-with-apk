/**
 * generate-move-image — تولید تصویر آموزشیِ یک حرکت با «شباهت چهره‌ی کاربر»
 * (فایل پرامپت ۰۲، بخش ۲).
 *
 * جریان:
 *   ۱) کاربر یک سلفی در پروفایل آپلود کرده (سطل `avatars`)؛ شناسه‌ی فایل به تابع می‌آید.
 *   ۲) این تابع پرامپت پایه‌ی حرکت (`referenceImagePromptTemplate`) + تصویر مرجع را
 *      به سرویس تولید تصویر (image-gen سازگار با OpenAI images) می‌فرستد.
 *   ۳) خروجی را در سطل `move-images` ذخیره و URL را در کالکشن `move_images` می‌نویسد.
 *   ۴) نتیجه cache می‌شود؛ فقط با تغییر عکس پروفایل یا `regenerate=true` دوباره ساخته می‌شود.
 *
 * ورودی:  { "moveId":"yoga-01", "prompt":"...", "avatarFileId":"...", "regenerate":false }
 * خروجی:  { ok:true, cached:boolean, imageUrl:"...", moveId:"..." }
 *          { ok:false, error:"not_configured"|"no_avatar"|"upstream_error" }
 *
 * حریم خصوصی: عکس چهره فقط برای همین منظور استفاده می‌شود؛ متن پرامپت لاگ می‌شود
 * ولی هیچ داده‌ی خصوصی دیگری ذخیره نمی‌شود. تولید فقط با درخواست صریح کاربر است.
 *
 * نکته: چون خروجی به کلید image-gen و شبکه نیاز دارد و در تست آفلاین قابل فراخوانی
 * واقعی نیست، منطق کش/اعتبارسنجی/انتخاب provider به‌صورت قابل‌تست جدا شده و مسیر
 * شبکه پشت `not_configured` می‌ماند تا در نبود کلید، اپ به تصویر پیش‌فرض برگردد.
 */
const { resolveProviders, logUsage } = require('./ai-provider');

let sdk = null;
function loadSdk() { if (!sdk) { try { sdk = require('node-appwrite'); } catch (e) { sdk = null; } } return sdk; }

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || process.env.APPWRITE_FUNCTION_DATABASE_ID || 'main_db';
const MOVE_IMAGES_TABLE = 'move_images';
const USED_FOR = 'generate-move-image';

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
      get: service.getRow ? (db, t, id) => service.getRow(db, t, id) : (db, t, id) => service.getDocument(db, t, id),
      create: service.createRow ? (db, t, id, d, p) => service.createRow(db, t, id, d, p) : (db, t, id, d, p) => service.createDocument(db, t, id, d, p),
      update: service.updateRow ? (db, t, id, d) => service.updateRow(db, t, id, d) : (db, t, id, d) => service.updateDocument(db, t, id, d),
      list: service.listRows ? (db, t, q) => service.listRows(db, t, q) : (db, t, q) => service.listDocuments(db, t, q),
      Query: s.Query, ID: s.ID, Permission: s.Permission, Role: s.Role,
    };
  } catch (e) { return null; }
}

function rowId(userId, moveId) {
  return `mv-${String(userId)}-${String(moveId)}`.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 36);
}

/** ساخت پرامپت نهایی از قالب حرکت (فایل ۰۲). */
function buildPrompt(moveTitle, template) {
  const base = template && template.trim()
    ? template
    : 'یک تصویر آموزشی ساده و واضح از حالت بدنی [نام حرکت]، با چهره‌ای شبیه به تصویر پیوست‌شده '
      + '(فقط شباهت چهره، نه کپی دقیق)، در پوشش ورزشی محجبه و مناسب، پس‌زمینه‌ی ساده و روشن، '
      + 'زاویه‌ی دید کامل بدن، سبک illustration آموزشی، بدون متن روی تصویر.';
  return base.replace('[نام حرکت]', moveTitle || '');
}

async function callImageProvider(provider, prompt) {
  // سرویس‌های سازگار با OpenAI images: POST {model, prompt} → {data:[{url}]}
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

module.exports = async function generateMoveImage(req, res) {
  const requestId = Math.random().toString(36).slice(2, 10);
  const userId = userIdOf(req);
  if (!userId) return res.json({ ok: false, error: 'no_user' }, 401);
  const body = parseBody(req);
  const moveId = String(body.moveId || '').trim();
  const moveTitle = String(body.moveTitle || body.titleFa || '').trim();
  const avatarFileId = String(body.avatarFileId || '').trim();
  const regenerate = body.regenerate === true;
  if (!moveId) return res.json({ ok: false, error: 'empty_move' });
  if (!avatarFileId) return res.json({ ok: false, error: 'no_avatar', fallback: 'اول یک عکس پروفایل اضافه کن تا تصویر با چهره‌ی خودت ساخته شود.' });

  const tables = tablesService();
  const id = rowId(userId, moveId);

  // کش: اگر با همان آواتار قبلاً ساخته شده و regenerate نخواسته، همان را بده.
  if (tables && !regenerate) {
    try {
      const existing = await tables.get(DATABASE_ID, MOVE_IMAGES_TABLE, id);
      const d = existing.data || existing;
      if (d.avatarFileId === avatarFileId && d.imageUrl) {
        return res.json({ ok: true, cached: true, imageUrl: d.imageUrl, moveId });
      }
    } catch (e) { /* نبود → می‌سازیم */ }
  }

  const providers = tables
    ? await resolveProviders(tables, DATABASE_ID, USED_FOR, tables.Query)
    : (require('./ai-provider').envProvider() ? [require('./ai-provider').envProvider()] : []);
  const imageProviders = providers.filter((p) => p.imageEndpointUrl || process.env.AI_IMAGE_ENDPOINT);
  if (!imageProviders.length) {
    return res.json({ ok: false, error: 'not_configured', fallback: 'سرویس تولید تصویر تنظیم نشده؛ فعلاً تصویر پیش‌فرض حرکت نشان داده می‌شود.' });
  }

  const prompt = buildPrompt(moveTitle || moveId, body.prompt);
  let lastError = 'upstream_error';
  for (const provider of imageProviders) {
    const result = await callImageProvider(provider, prompt);
    if (result.imageUrl) {
      if (tables) {
        const data = { userId, moveId, avatarFileId, imageUrl: result.imageUrl, updatedAtMs: Date.now() };
        try {
          const perms = tables.Permission && tables.Role ? [tables.Permission.read(tables.Role.user(userId))] : [];
          try { await tables.update(DATABASE_ID, MOVE_IMAGES_TABLE, id, data); }
          catch (e) { await tables.create(DATABASE_ID, MOVE_IMAGES_TABLE, id, data, perms); }
        } catch (e) { console.error('generate-move-image cache write failed', e && e.message ? e.message : e); }
        if (provider.id !== 'env') await logUsage(tables, DATABASE_ID, provider.id, USED_FOR, tables.ID);
      }
      console.log('generate-move-image ok', { requestId, provider: provider.providerName, moveId });
      return res.json({ ok: true, cached: false, imageUrl: result.imageUrl, moveId });
    }
    lastError = result.error || 'upstream_error';
  }
  return res.json({ ok: false, error: lastError, fallback: 'تولید تصویر حرکت الان ممکن نشد؛ تصویر پیش‌فرض نشان داده می‌شود.' });
};
