/**
 * generate-recipe — تولید دستور پخت با نام غذا (فایل پرامپت ۰۳).
 *
 * جریان:
 *   ۱) اپ اول کالکشن محلی/سرور `recipes` را می‌گردد (کلاینت این کار را می‌کند).
 *   ۲) اگر نبود، این تابع صدا زده می‌شود: با provider فعال (کالکشن ai_providers یا
 *      متغیر محیطی) یک دستور پخت ساختاریافته می‌سازد،
 *   ۳) در کالکشن `recipes` با `source="ai-generated"` ذخیره (کش) می‌کند تا دفعه‌ی بعد
 *      مستقیم از سرور بیاید،
 *   ۴) دستور را برمی‌گرداند.
 *
 * ورودی:  { "name": "قورمه‌سبزی", "servings": 4 }
 * خروجی:  { ok:true, cached:boolean, recipe:{id,title,servings,prepTimeMin,cookTimeMin,
 *           ingredients:[{name,amount}], steps:[{order,text}], tags:[], source} }
 *          { ok:false, error:"not_configured"|"empty_name"|"upstream_error" }
 *
 * چند-مدلی: `usedFor` شامل `generate-recipe` در کالکشن ai_providers؛ در نبودش
 * به AI_API_KEY/AI_MODEL برمی‌گردد. جزئیات در ai-provider.js.
 */
const { resolveProviders, logUsage } = require('./ai-provider');

let sdk = null;
function loadSdk() { if (!sdk) { try { sdk = require('node-appwrite'); } catch (e) { sdk = null; } } return sdk; }

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || process.env.APPWRITE_FUNCTION_DATABASE_ID || 'main_db';
const RECIPES_TABLE = 'recipes';
const USED_FOR = 'generate-recipe';

const SYSTEM_PROMPT = [
  'تو یک آشپز ایرانی و مربی آشپزی برای نوجوانان هستی.',
  'برای نام غذایی که کاربر می‌دهد، یک دستور پخت ساده، ایمن و قابل‌انجام در خانه بنویس.',
  'فقط و فقط یک شیء JSON معتبر برگردان (بدون توضیح اضافه، بدون Markdown) با این کلیدها:',
  '{ "title": string, "servings": number, "prepTimeMin": number, "cookTimeMin": number,',
  '  "ingredients": [{ "name": string, "amount": string }],',
  '  "steps": [{ "order": number, "text": string }],',
  '  "tags": [string] }',
  'مقادیر فارسی باشند. مراحل کوتاه و شماره‌دار. هیچ ماده یا ابزار خطرناک/غیرمجاز پیشنهاد نده.',
].join('\n');

function parseBody(req) {
  if (!req || !req.body) return {};
  if (typeof req.body === 'string') { try { return JSON.parse(req.body); } catch (e) { return {}; } }
  return req.body;
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
      Query: s.Query, ID: s.ID, Permission: s.Permission, Role: s.Role,
    };
  } catch (e) { return null; }
}

function slugId(name) {
  // شناسه‌ی پایدار برای کش: recipe-<hash از نام نرمال‌شده>
  const norm = String(name).trim().replace(/[يی]/g, 'ی').replace(/ك/g, 'ک').toLowerCase();
  let h = 0;
  for (let i = 0; i < norm.length; i += 1) { h = (h * 31 + norm.charCodeAt(i)) >>> 0; }
  return `recipe-ai-${h.toString(36)}`;
}

function extractJson(text) {
  if (!text) return null;
  // مدل ممکن است JSON را در ```json ... ``` بپیچد؛ اولین { تا آخرین } را می‌گیریم.
  const start = text.indexOf('{');
  const end = text.lastIndexOf('}');
  if (start < 0 || end <= start) return null;
  try { return JSON.parse(text.slice(start, end + 1)); } catch (e) { return null; }
}

function extractReply(payload) {
  if (!payload) return '';
  const openai = payload.choices && payload.choices[0];
  if (openai) {
    if (openai.message && typeof openai.message.content === 'string') return openai.message.content;
    if (typeof openai.text === 'string') return openai.text;
  }
  const gemini = payload.candidates && payload.candidates[0];
  if (gemini && gemini.content && Array.isArray(gemini.content.parts)) {
    return gemini.content.parts.map((p) => p.text || '').join('');
  }
  return '';
}

function normalizeRecipe(raw, name, servings) {
  const ingredients = Array.isArray(raw.ingredients) ? raw.ingredients.map((it) => (
    typeof it === 'string' ? { name: it, amount: '' } : { name: String(it.name || ''), amount: String(it.amount || '') }
  )).filter((it) => it.name) : [];
  const steps = Array.isArray(raw.steps) ? raw.steps.map((st, i) => (
    typeof st === 'string' ? { order: i + 1, text: st } : { order: Number(st.order) || i + 1, text: String(st.text || '') }
  )).filter((st) => st.text) : [];
  return {
    title: String(raw.title || name).trim(),
    servings: Number(raw.servings || servings || 2),
    prepTimeMin: Number(raw.prepTimeMin || 0),
    cookTimeMin: Number(raw.cookTimeMin || 0),
    ingredients,
    steps,
    tags: Array.isArray(raw.tags) ? raw.tags.map(String) : [],
    source: 'ai-generated',
  };
}

async function tryProvider(provider, name, servings) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 20000);
  try {
    const upstream = await fetch(provider.endpointUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${provider.apiKey}` },
      body: JSON.stringify({
        model: provider.modelId,
        messages: [
          { role: 'system', content: SYSTEM_PROMPT },
          { role: 'user', content: `نام غذا: ${name}\nتعداد نفرات: ${servings || 2}` },
        ],
        temperature: 0.5,
        max_tokens: 900,
        stream: false,
      }),
      signal: controller.signal,
    });
    clearTimeout(timer);
    if (!upstream.ok) return { error: 'upstream_error', status: upstream.status };
    const payload = await upstream.json();
    const json = extractJson(extractReply(payload));
    if (!json) return { error: 'bad_format' };
    return { recipe: normalizeRecipe(json, name, servings) };
  } catch (err) {
    clearTimeout(timer);
    return { error: 'network_error' };
  }
}

/** برای ذخیره در کالکشن recipes (سازگار با اسکیمای موجود). */
function toRowData(recipe) {
  return {
    title: recipe.title,
    ingredients: JSON.stringify(recipe.ingredients.map((it) => (it.amount ? `${it.name} (${it.amount})` : it.name))),
    steps: JSON.stringify(recipe.steps.map((st) => st.text)),
    minutes: recipe.prepTimeMin + recipe.cookTimeMin,
    servings: recipe.servings,
    difficulty: recipe.tags.includes('سخت') ? 'کمی سخت' : 'آسان',
    tip: recipe.tags.join('، '),
    source: recipe.source,
  };
}

module.exports = async function generateRecipe(req, res) {
  const requestId = Math.random().toString(36).slice(2, 10);
  const body = parseBody(req);
  const name = String(body.name || body.nameFa || '').trim().slice(0, 80);
  const servings = Number(body.servings || 2);
  if (!name) return res.json({ ok: false, error: 'empty_name', fallback: 'اسم غذا را بنویس.' });

  const tables = tablesService();
  const id = slugId(name);

  // ۱) کش: اگر قبلاً ساخته شده، همان را برگردان.
  if (tables) {
    try {
      const existing = await tables.get(DATABASE_ID, RECIPES_TABLE, id);
      const d = existing.data || existing;
      console.log('generate-recipe cache-hit', { requestId, id });
      return res.json({
        ok: true, cached: true,
        recipe: {
          id, title: d.title, servings: d.servings, prepTimeMin: 0, cookTimeMin: d.minutes,
          ingredients: JSON.parse(d.ingredients || '[]').map((x) => ({ name: x, amount: '' })),
          steps: JSON.parse(d.steps || '[]').map((t, i) => ({ order: i + 1, text: t })),
          tags: String(d.tip || '').split('،').map((x) => x.trim()).filter(Boolean),
          source: d.source || 'ai-generated',
        },
      });
    } catch (e) { /* نبود → می‌سازیم */ }
  }

  const providers = tables
    ? await resolveProviders(tables, DATABASE_ID, USED_FOR, tables.Query)
    : (require('./ai-provider').envProvider() ? [require('./ai-provider').envProvider()] : []);
  if (!providers.length) {
    return res.json({ ok: false, error: 'not_configured', fallback: 'تولید دستور پخت روی سرور تنظیم نشده.' });
  }

  let lastError = 'upstream_error';
  for (const provider of providers) {
    const result = await tryProvider(provider, name, servings);
    if (result.recipe) {
      const recipe = Object.assign({ id }, result.recipe);
      // ۳) کش برای دفعات بعد (خواندنی برای همه).
      if (tables) {
        try {
          const perms = tables.Permission && tables.Role ? [tables.Permission.read(tables.Role.any())] : [];
          await tables.create(DATABASE_ID, RECIPES_TABLE, id, toRowData(recipe), perms);
        } catch (e) { console.error('generate-recipe cache write failed', e && e.message ? e.message : e); }
        if (provider.id !== 'env') await logUsage(tables, DATABASE_ID, provider.id, USED_FOR, tables.ID);
      }
      console.log('generate-recipe ok', { requestId, provider: provider.providerName, steps: recipe.steps.length });
      return res.json({ ok: true, cached: false, recipe });
    }
    lastError = result.error || 'upstream_error';
  }
  return res.json({ ok: false, error: lastError, fallback: 'الان نتوانستم دستور پخت را بسازم؛ بعداً دوباره امتحان کن.' });
};
