/**
 * weekly-plan-engine — تولید برنامه‌ی مرور روزانه بر اساس برنامه‌ی کلاسی و شیفت چرخشی
 * (فایل پرامپت ۰۷، بخش ۳).
 *
 * منطق (همه سمت سرور، deterministic و قابل‌تست):
 *   ۱) از `weekly_schedule` مشخص می‌شود امروز کدام هفته از چرخه است و «فردا» چه
 *      دروسی کلاس دارند.
 *   ۲) برای هر درسی که فردا کلاس دارد، یک تسک مرور فلش‌کارت + نکات کلیدی برای «امشب».
 *   ۳) برای دروسی که در ۷ روز اخیر آزمون بازه‌ای با `weakTopics` داشته‌اند، یک تسک
 *      مرور نقطه‌ضعف.
 *   ۴) اگر امروز کلاسی نیست (روز آزاد)، یک تسک «مرور آزاد» برای پرتکرارترین درسِ ضعیف.
 *   خروجی در `review_plans` نوشته می‌شود (یک سطر deterministic برای هر کاربر/روز).
 *
 * ورودی:  { "cycleWeekIndex": 0 (اختیاری), "date": "YYYY-MM-DD" (اختیاری) }
 * خروجی:  { ok:true, date, reviewTasks:[{bookCode,type,refId,isDone}] }
 *
 * این تابع به مدل AI نیاز ندارد؛ فقط داده‌ی برنامه و نتایج آزمون را می‌چیند.
 */
const sdk = safeRequire('node-appwrite');
function safeRequire(m) { try { return require(m); } catch (e) { return null; } }

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || process.env.APPWRITE_FUNCTION_DATABASE_ID || 'main_db';
const SCHEDULE = 'weekly_schedule';
const EXAM_RESULTS = 'exam_results';
const REVIEW_PLANS = 'review_plans';

// ترتیب روزهای هفته‌ی ایران (شنبه اول).
const DAYS = ['saturday', 'sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday'];
// نگاشت getUTCDay (۰=یکشنبه) به اندیس روز هفته‌ی ایرانی.
const JS_TO_IR = { 6: 0, 0: 1, 1: 2, 2: 3, 3: 4, 4: 5, 5: 6 };

function parseBody(req) {
  if (!req || !req.body) return {};
  if (typeof req.body === 'string') { try { return JSON.parse(req.body); } catch (e) { return {}; } }
  return req.body;
}
function userIdOf(req) {
  return (req && (req.userId || (req.headers && (req.headers['x-appwrite-user-id'] || req.headers['X-Appwrite-User-Id'])))) || '';
}
function dayIso(d = new Date()) { return new Date(d).toISOString().slice(0, 10); }

function tablesService() {
  if (!sdk) return null;
  try {
    const client = new sdk.Client()
      .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
      .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
      .setKey(process.env.APPWRITE_FUNCTION_API_KEY);
    const service = sdk.TablesDB ? new sdk.TablesDB(client) : new sdk.Databases(client);
    return {
      list: service.listRows ? (db, t, q) => service.listRows(db, t, q) : (db, t, q) => service.listDocuments(db, t, q),
      get: service.getRow ? (db, t, id) => service.getRow(db, t, id) : (db, t, id) => service.getDocument(db, t, id),
      create: service.createRow ? (db, t, id, d, p) => service.createRow(db, t, id, d, p) : (db, t, id, d, p) => service.createDocument(db, t, id, d, p),
      update: service.updateRow ? (db, t, id, d) => service.updateRow(db, t, id, d) : (db, t, id, d) => service.updateDocument(db, t, id, d),
    };
  } catch (e) { return null; }
}

function rows(result) {
  const items = (result && (result.rows || result.documents)) || [];
  return items.map((row) => Object.assign({ id: row.$id }, row.data || row));
}

function parseSlots(raw) {
  if (Array.isArray(raw)) return raw;
  if (typeof raw === 'string') { try { const p = JSON.parse(raw); return Array.isArray(p) ? p : []; } catch (e) { return []; } }
  return [];
}

/**
 * منطق خالصِ ساخت تسک‌ها — بدون شبکه، تا در تست مستقیم صدا زده شود.
 * @param schedule سطرهای weekly_schedule
 * @param examResults نتایج آزمون کاربر (۷ روز اخیر فیلتر می‌شود اینجا)
 * @param todayIso تاریخ امروز
 * @param cycleWeekIndex هفته‌ی چرخه (اگر داده نشود، از تعداد هفته‌ها حساب می‌شود)
 */
function buildTasks(schedule, examResults, todayIso, cycleWeekIndex) {
  const todayDate = new Date(`${todayIso}T00:00:00Z`);
  const todayIdx = JS_TO_IR[todayDate.getUTCDay()];
  const tomorrowIdx = (todayIdx + 1) % 7;
  const tomorrowName = DAYS[tomorrowIdx];
  const todayName = DAYS[todayIdx];

  // شیفت چرخشی: کدام هفته از چرخه؟
  const cycleCount = Math.max(1, new Set(schedule.map((s) => Number(s.cycleWeekIndex) || 0)).size);
  let weekIdx = Number.isFinite(cycleWeekIndex) ? Number(cycleWeekIndex) : null;
  if (weekIdx === null) {
    // هفته‌ی سال از مبدأ ثابت، به‌پیمانه‌ی تعداد هفته‌های چرخه.
    const epoch = new Date('2024-03-20T00:00:00Z').getTime(); // نوروز مرجع
    const weeksSince = Math.floor((todayDate.getTime() - epoch) / (7 * 86400000));
    weekIdx = ((weeksSince % cycleCount) + cycleCount) % cycleCount;
  }

  const forWeek = schedule.filter((s) => (Number(s.cycleWeekIndex) || 0) === weekIdx);
  const dayRow = (name) => forWeek.find((s) => String(s.dayOfWeek || '').toLowerCase() === name);

  const tomorrowSlots = parseSlots(dayRow(tomorrowName) && dayRow(tomorrowName).classSlots);
  const todaySlots = parseSlots(dayRow(todayName) && dayRow(todayName).classSlots);

  const tasks = [];
  const seenBooks = new Set();

  // ۲) برای دروس فردا: مرور فلش‌کارت + نکات کلیدی امشب.
  tomorrowSlots.forEach((slot) => {
    const book = String(slot.bookCode || '').trim();
    if (!book || seenBooks.has(`flash-${book}`)) return;
    seenBooks.add(`flash-${book}`);
    tasks.push({ bookCode: book, type: 'flashcard', refId: `${book}-flash`, isDone: false });
    tasks.push({ bookCode: book, type: 'prerequisite', refId: `${book}-keynotes`, isDone: false });
  });

  // ۳) نقاط ضعفِ ۷ روز اخیر.
  const weekAgo = new Date(todayDate.getTime() - 7 * 86400000).toISOString().slice(0, 10);
  const weakByBook = new Map();
  examResults
    .filter((r) => String(r.takenAtIso || r.dayIso || '') >= weekAgo)
    .forEach((r) => {
      const book = String(r.bookCode || '').trim();
      const weak = Array.isArray(r.weakTopics) ? r.weakTopics
        : (typeof r.weakTopics === 'string' && r.weakTopics ? JSON.parse(r.weakTopics || '[]') : []);
      if (!book || !weak.length) return;
      weakByBook.set(book, (weakByBook.get(book) || 0) + weak.length);
      const key = `weak-${book}`;
      if (!seenBooks.has(key)) {
        seenBooks.add(key);
        tasks.push({ bookCode: book, type: 'exam', refId: `${book}-weak`, isDone: false });
      }
    });

  // ۴) روز آزاد (امروز کلاسی نیست): مرور آزادِ پرتکرارترین درسِ ضعیف.
  if (todaySlots.length === 0 && weakByBook.size > 0) {
    const top = [...weakByBook.entries()].sort((a, b) => b[1] - a[1])[0][0];
    if (!seenBooks.has(`free-${top}`)) {
      tasks.push({ bookCode: top, type: 'flashcard', refId: `${top}-free-review`, isDone: false });
    }
  }

  return { weekIdx, tomorrowName, todayName, tasks };
}

module.exports = async function weeklyPlanEngine(req, res) {
  const userId = userIdOf(req);
  if (!userId) return res.json({ ok: false, error: 'no_user' }, 401);
  const body = parseBody(req);
  const iso = body.date ? dayIso(body.date) : dayIso();
  const cycleWeekIndex = body.cycleWeekIndex !== undefined ? Number(body.cycleWeekIndex) : NaN;

  const tables = tablesService();
  if (!tables) return res.json({ ok: false, error: 'not_configured' });

  try {
    const { Query } = sdk;
    const schedule = rows(await tables.list(DATABASE_ID, SCHEDULE, [Query.limit(200)]));
    let results = [];
    try {
      results = rows(await tables.list(DATABASE_ID, EXAM_RESULTS, [Query.equal('userId', userId), Query.limit(200)]));
    } catch (e) { results = []; }

    const built = buildTasks(schedule, results, iso, cycleWeekIndex);
    const planId = `plan-${userId}-${iso}`.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 36);
    const data = {
      userId, dayIso: iso, cycleWeekIndex: built.weekIdx,
      reviewTasks: JSON.stringify(built.tasks), updatedAtMs: Date.now(),
    };
    let updated = false;
    try { await tables.update(DATABASE_ID, REVIEW_PLANS, planId, data); updated = true; }
    catch (e) {
      const perms = sdk.Permission && sdk.Role ? [sdk.Permission.read(sdk.Role.user(userId))] : [];
      await tables.create(DATABASE_ID, REVIEW_PLANS, planId, data, perms);
    }

    return res.json({ ok: true, date: iso, updated, cycleWeekIndex: built.weekIdx, reviewTasks: built.tasks });
  } catch (err) {
    console.error('weekly-plan-engine failed', err && err.message ? err.message : err);
    return res.json({ ok: false, error: 'server_error' }, 500);
  }
};

// برای تست مستقیمِ منطق خالص.
module.exports.buildTasks = buildTasks;
