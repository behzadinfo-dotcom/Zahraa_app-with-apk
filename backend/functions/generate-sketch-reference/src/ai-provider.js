/**
 * ai-provider — انتخاب ارائه‌دهنده‌ی هوش مصنوعی با fallback (فایل پرامپت ۰۴، بخش ۳).
 *
 * چرا این‌جا؟ همه‌ی توابع AI (ai-companion، generate-recipe، generate-move-image،
 * generate-sketch-reference، weekly-plan-engine) به‌جای کلید هاردکد باید:
 *   ۱) provider فعالِ مربوط به خودشان را از کالکشن `ai_providers` بخوانند،
 *   ۲) اگر provider فعال fail شد، به provider بعدیِ فهرست fallback کنند،
 *   ۳) اگر کالکشن خالی/تنظیم‌نشده بود، به متغیرهای محیطی قدیمی (AI_API_KEY/AI_MODELS)
 *      برگردند تا رفتار قبلی نشکند.
 *
 * امنیت کلید (سازگار با اصول پروژه):
 *   - کلید API هرگز در ریپو نیست و هرگز به کلاینت برنمی‌گردد.
 *   - در کالکشن `ai_providers`، ستون `apiKeyEncrypted` با AES-256-GCM و راز سرور
 *     (`AI_CONFIG_SECRET` در Environment Variables تابع) رمز شده است؛ فقط `apiKeyLast4`
 *     برای نمایش در تنظیمات خوانده می‌شود.
 *   - اگر `AI_CONFIG_SECRET` ست نشده باشد، ستون رمزشده نادیده گرفته می‌شود و فقط
 *     متغیرهای محیطی استفاده می‌شوند (باز هم امن، فقط بدون UI چند-مدلی).
 *
 * لاگ مصرف: هر فراخوانی موفق با `logUsage` یک شمارنده‌ی ماهانه به تفکیک provider را
 * در کالکشن `ai_usage` بالا می‌برد (بدون ذخیره‌ی متن).
 */
const crypto = require('node:crypto');

const PROVIDERS_TABLE = 'ai_providers';
const USAGE_TABLE = 'ai_usage';

/** رمزگشایی کلید API ذخیره‌شده. فرمت: base64(iv).base64(tag).base64(cipher) */
function decryptKey(encrypted, secret) {
  if (!encrypted || !secret) return '';
  try {
    const [ivB64, tagB64, dataB64] = String(encrypted).split('.');
    if (!ivB64 || !tagB64 || !dataB64) return '';
    const key = crypto.createHash('sha256').update(String(secret)).digest();
    const iv = Buffer.from(ivB64, 'base64');
    const tag = Buffer.from(tagB64, 'base64');
    const data = Buffer.from(dataB64, 'base64');
    const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
    decipher.setAuthTag(tag);
    return Buffer.concat([decipher.update(data), decipher.final()]).toString('utf8');
  } catch (e) {
    return '';
  }
}

/** رمزنگاری کلید (برای ابزار seed/تنظیمات؛ در خود تابع لازم نمی‌شود). */
function encryptKey(plain, secret) {
  const key = crypto.createHash('sha256').update(String(secret)).digest();
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
  const data = Buffer.concat([cipher.update(String(plain), 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return [iv.toString('base64'), tag.toString('base64'), data.toString('base64')].join('.');
}

/** provider از متغیرهای محیطی (رفتار قدیمی؛ همیشه در انتهای فهرست fallback است). */
function envProvider() {
  const apiKey = process.env.AI_API_KEY;
  const model = process.env.AI_MODEL
    || String(process.env.AI_MODELS || '').split(',').map((m) => m.trim()).filter(Boolean)[0]
    || '';
  if (!apiKey || !model) return null;
  return {
    id: 'env',
    providerName: 'env',
    apiKey,
    modelId: model,
    endpointUrl: String(process.env.AI_ENDPOINT || 'https://api.openai.com/v1/chat/completions').trim(),
    imageEndpointUrl: String(process.env.AI_IMAGE_ENDPOINT || '').trim(),
  };
}

/**
 * فهرست providerها برای یک قابلیت (`usedFor`)، به‌ترتیب اولویت:
 *   ۱) providerهای فعالِ کالکشن که این قابلیت را پشتیبانی می‌کنند (بر اساس `priority`).
 *   ۲) provider متغیر محیطی (در انتها، به‌عنوان تور ایمنی).
 *
 * @param tables سرویس list (همان الگوی بقیه‌ی توابع).
 * @param usedFor مثلاً "ai-companion" یا "generate-recipe".
 */
async function resolveProviders(tables, databaseId, usedFor, Query) {
  const secret = process.env.AI_CONFIG_SECRET || '';
  const list = [];
  try {
    const result = await tables.list(databaseId, PROVIDERS_TABLE, [Query.equal('isActive', true), Query.limit(50)]);
    const rows = (result && (result.rows || result.documents)) || [];
    rows
      .map((row) => Object.assign({ id: row.$id }, row.data || row))
      .filter((p) => {
        const uses = Array.isArray(p.usedFor) ? p.usedFor : String(p.usedFor || '').split(',').map((x) => x.trim());
        return uses.includes(usedFor);
      })
      .sort((a, b) => (Number(a.priority) || 0) - (Number(b.priority) || 0))
      .forEach((p) => {
        const apiKey = decryptKey(p.apiKeyEncrypted, secret);
        if (!apiKey || !p.modelId) return;
        list.push({
          id: p.id,
          providerName: String(p.providerName || p.id),
          apiKey,
          modelId: String(p.modelId),
          endpointUrl: String(p.endpointUrl || 'https://api.openai.com/v1/chat/completions'),
          imageEndpointUrl: String(p.imageEndpointUrl || ''),
        });
      });
  } catch (e) {
    // کالکشن نبود/دسترسی نبود → فقط به env تکیه می‌کنیم.
    console.error('ai-provider list unavailable', e && e.message ? e.message : e);
  }
  const env = envProvider();
  if (env) list.push(env);
  return list;
}

/** لاگ مصرف ماهانه به تفکیک provider (بدون متن). خطا در لاگ، کار اصلی را نمی‌شکند. */
async function logUsage(tables, databaseId, providerId, usedFor, ID) {
  try {
    const month = new Date().toISOString().slice(0, 7); // YYYY-MM
    const rowId = `${providerId}-${usedFor}-${month}`.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 36);
    let existing = null;
    try { existing = await tables.get(databaseId, USAGE_TABLE, rowId); } catch (e) { existing = null; }
    if (existing) {
      const prev = Number((existing.data || existing).calls || 0);
      await tables.update(databaseId, USAGE_TABLE, rowId, { calls: prev + 1, updatedAtMs: Date.now() });
    } else {
      await tables.create(databaseId, USAGE_TABLE, rowId, {
        providerId: String(providerId), usedFor: String(usedFor), month, calls: 1, updatedAtMs: Date.now(),
      }, []);
    }
  } catch (e) {
    console.error('ai-provider usage log skipped', e && e.message ? e.message : e);
  }
}

module.exports = { resolveProviders, logUsage, decryptKey, encryptKey, envProvider, PROVIDERS_TABLE, USAGE_TABLE };
