/**
 * وارد کردن ۴۵ جلسه‌ی هدایت‌شده به جدول guided_sessions در Appwrite.
 *
 * ۳۰ ذهن‌آگاهی + ۱۰ آرامش بین درس‌ها + ۵ خودهیپنوتیزم — هرکدام ۴ بخش، جمعاً ۱۵ دقیقه.
 * الگو: seed-wellness-moves.js (همان قراردادها — upsert بر اساس slug، idempotent).
 *
 * اجرا:  node backend/seed/seed-guided-sessions.js
 * خشک:  node backend/seed/seed-guided-sessions.js --dry-run
 */
const sdk = require('node-appwrite');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const TABLE_ID = 'guided_sessions';

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const tablesDb = new sdk.TablesDB(client);
const { Query } = sdk;

const BUCKET_ID = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b';
const IMG_BASE = `${ENDPOINT}/storage/buckets/${BUCKET_ID}/files/`;
const IMG_VIEW = '/view?project=' + PROJECT_ID;
const img = (fileId) => `${IMG_BASE}${fileId}${IMG_VIEW}`;

// timingMap استاندارد ۴ بخشی — ۹۰۰ ثانیه = ۱۵ دقیقه
// استراتژی «۴ فایل = دقیقاً ۱۵:۰۰»: مدت هر فایل صوتی دقیقاً برابر targetSec است
// (گفتار TTS تا سقف + کشش ۰.۹۵x + پد سکوت — اسکریپت: pad-guided-audio.sh).
// مکث بین بخش‌ها داخل همان فایل (انتهای p1 تا p3) تعبیه شده است؛ فایل‌ها پشت‌سرهم
// بدون درز، کل جلسه‌ی ۱۵ دقیقه‌ای را می‌سازند و نوار ۴ سگمنتی پلیر مو‌به‌موی فایل‌هاست.
const TIMING = JSON.stringify({
    totalSec: 900,
    fileStrategy: 'pad-guided-audio.sh — 4 files back-to-back = 900s exactly',
    parts: [
        { order: 1, key: 'start', titleFa: 'آغاز و امن‌سازی', targetSec: 270, file: 'p1.mp3', speechRate: 0.95, silenceTailSec: 'متغیر — مکث آغاز' },
        { order: 2, key: 'deep', titleFa: 'عمیق‌شدن و تصویرسازی', targetSec: 240, file: 'p2.mp3', speechRate: 0.95, silenceTailSec: 'متغیر — مکث گذار' },
        { order: 3, key: 'core', titleFa: 'تمرکز و تلقین', targetSec: 240, file: 'p3.mp3', speechRate: 0.95, silenceTailSec: 'متغیر — مکث تثبیت' },
        { order: 4, key: 'end', titleFa: 'بازگشت و تقویت', targetSec: 150, file: 'p4.mp3', speechRate: 0.95, silenceTailSec: 0 },
    ],
    addressFa: 'زهرا جان',
});

// ---------- ۳۰ ذهن‌آگاهی (چشم بسته) ----------
const mindfulness = [
    ['mind-01-breath-arrival', 'آشنایی با نفس', 'اولین قرار آرام با تنفس', '["شروع","نفس"]'],
    ['mind-02-body-scan', 'اسکن بدن کامل', 'از سر تا پا با توجه آرام', '["بدن‌اسکن"]'],
    ['mind-03-breath-counting', 'نفس‌شماری', 'شمارش آرام دم و بازدم', '["تمرکز"]'],
    ['mind-04-sounds', 'صداهای پیرامون', 'شنیدن بدون قضاوت', '["حضور"]'],
    ['mind-05-body-anchor', 'لنگرگاه بدن', 'لنگر انداختن توجه در بدن', '["تمرکز"]'],
    ['mind-06-thoughts-clouds', 'افکار مثل ابرها', 'دیدن عبور افکار در آسمان ذهن', '["افکار"]'],
    ['mind-07-leaf-on-stream', 'برگ روی رودخانه', 'گذاشتن هر فکر روی یک برگ', '["رهاسازی"]'],
    ['mind-08-mountain', 'کوه آرام', 'ایستادگی آرام مثل کوه', '["ثبات"]'],
    ['mind-09-lake', 'دریاچه‌ی آینه', 'سکوت و شفافیت ذهن', '["سکون"]'],
    ['mind-10-five-senses', 'پنج حس', 'بیدار کردن پنج حس با چشم بسته', '["حضور"]'],
    ['mind-11-self-kindness', 'مهربانی با خود', 'حرف مهربان با خودت', '["شفقت"]'],
    ['mind-12-loving-kindness', 'مهربانی گسترده', 'از خودت تا دنیا', '["شفقت"]'],
    ['mind-13-gratitude', 'سه چیز خوب', 'قدردانی از لحظه‌های امروز', '["شکرگزاری"]'],
    ['mind-14-present-anchor', 'لنگر در اکنون', 'بازگشت به همین لحظه', '["حضور"]'],
    ['mind-15-gap-between-breaths', 'مکث میان دو نفس', 'فضای سکوت کوچک در تنفس', '["دقت"]'],
    ['mind-16-feelings-in-body', 'جای احساس در بدن', 'پیدا کردن احساس در بدن', '["احساسات"]'],
    ['mind-17-naming-feelings', 'نام‌گذاری احساس‌ها', 'بگو اسمم چیست و بگذار بروی', '["احساسات"]'],
    ['mind-18-hard-day', 'آرامش در روز سخت', 'وقتی روز سنگین بود', '["شفقت"]'],
    ['mind-19-focal-point', 'نقطه‌ی تمرکز', 'توجه یک‌نقطه‌ای آرام', '["تمرکز"]'],
    ['mind-20-mental-walk', 'قدم‌های ذهنی', 'پیاده‌روی آهسته در خیال', '["تصویرسازی"]'],
    ['mind-21-sky-mind', 'آسمان و هوا', 'ذهن مثل آسمان؛ هوا عوض می‌شود', '["نگرش"]'],
    ['mind-22-non-judgment', 'بدون قضاوت', 'دیدن بدون برچسب زدن', '["نگرش"]'],
    ['mind-23-aware-relaxation', 'تن آگاهانه', 'آرام‌سازی با چشم بازِ ذهن', '["آرامش"]'],
    ['mind-24-countdown-calm', 'شمارش معکوس آرام', 'از ۱۰ تا ۱ به سوی سکون', '["آرامش"]'],
    ['mind-25-good-moments', 'ذخیره‌ی لحظه‌های خوب', 'جای خوشی‌ها در قلب', '["شادی"]'],
    ['mind-26-silent-awareness', 'سکوت آگاهانه', 'نشستن در سکوت با حضور', '["سکون"]'],
    ['mind-27-roots-tree', 'ریشه‌های من', 'ریشه داشتن مثل درخت', '["ثبات"]'],
    ['mind-28-night-release', 'رهاکردن روز (شب)', 'زمین گذاشتن روز قبل از خواب', '["خواب"]'],
    ['mind-29-morning-awake', 'بیدارباش آگاهانه', 'شروع آگاهانه‌ی روز', '["صبح"]'],
    ['mind-30-kind-breath', 'نفس مهربان', 'هر نفس، یک لبخند کوچک', '["شفقت"]'],
];

// ---------- ۱۰ آرامش بین درس‌ها (رفع خستگی ذهنی، چشم بسته) ----------
const calm = [
    ['calm-01-golden-reset', 'ریست طلایی بین دو درس', '۵ دقیقه‌ی طلایی که ذهن را تازه می‌کند', '["بین درس","ریست"]'],
    ['calm-02-478-guided', 'نفس ۴-۷-۸ هدایت‌شده', 'همراه می‌شمارم برایت؛ تو فقط نفس بکش', '["نفس","آرامش"]'],
    ['calm-03-shoulders-neck', 'شانه و گردن رها', 'خستگی نشستن را از شانه‌ها بگیر', '["بدن"]'],
    ['calm-04-rested-eyes', 'چشم‌های خسته', 'استراحت عمیق برای چشم و ذهن', '["چشم","استراحت"]'],
    ['calm-05-rainy-forest', 'جنگل بارانی', 'صدای باران در جنگل، بازسازی توجه', '["تصویرسازی"]'],
    ['calm-06-waves-beach', 'ساحل و موج', 'هر موج، یک فکر خسته را می‌برد', '["تصویرسازی"]'],
    ['calm-07-mountain-breeze', 'نسیم کوهستان', 'هوای تازه‌ی بلندی‌ها در ذهن', '["انرژی"]'],
    ['calm-08-three-breath-stop', 'توقف سه‌نفسه', 'تکنیک STOP برای وسط روز', '["تکنیک"]'],
    ['calm-09-glow-breath', 'نفس درخشان', 'انرژی‌گیری دوباره با تنفس', '["انرژی"]'],
    ['calm-10-next-lesson-bridge', 'پل به درس بعدی', 'ذهن را آماده‌ی درس بعد کن', '["بین درس"]'],
];

// ---------- ۱۵ خودهیپنوتیزم (مناسب ۱۴ سال، تلقین‌های مثبت اثبات‌شده) ----------
const hypnosis = [
    ['hyp-01-exam-calm', 'آرامش در امتحان', 'ذهنِ آرام در جلسه‌ی امتحان', '["امتحان","اضطراب"]'],
    ['hyp-02-deep-focus', 'تمرکز عمیق مطالعه', 'غرق‌شدن آسان در درس', '["تمرکز","مطالعه"]'],
    ['hyp-03-sleep-well', 'خواب شبانه‌ی آرام', 'خواب راحت و عمیق شبانه', '["خواب"]'],
    ['hyp-04-confidence', 'اعتماد به نفس', 'صدای درونی مهربان و قوی', '["اعتمادبه‌نفس"]'],
    ['hyp-05-no-compare', 'رهایی از مقایسه', 'مسیر خودت، نه شبکه‌ی اجتماعی', '["مقایسه","شبکه‌ی اجتماعی"]'],
    ['hyp-06-period-pain-relief', 'کنترل درد پریود', 'همراهی آرامش‌بخش با روزهای قاعدگی', '["پریود","آرامش"]'],
    ['hyp-07-energy-boost', 'بالا بردن انرژی', 'شارژ انرژی و رفع بی‌حالی', '["انرژی","شادابی"]'],
    ['hyp-08-boredom-relief', 'غلبه بر بی‌تابی', 'رفع حوصله‌سررفتگی با ماجرای واقعی', '["بی‌تابی","ماجرا"]'],
    ['hyp-09-silence-solitude', 'اهمیت سکوت و تنهایی', 'دوست شدن با خلوتِ خود', '["سکوت","تنهایی"]'],
    ['hyp-10-lesson-motivation', 'انگیزه به درس', 'درس؛ آجری برای ساختن آینده', '["انگیزه","درس"]'],
    ['hyp-11-study-motivation', 'انگیزه به مطالعه', 'شروع آسان و لذت پیشرفت', '["انگیزه","مطالعه"]'],
    ['hyp-12-communication-skills', 'اصول ارتباطات', 'حرف زدن مطمئن، شنیدن فعال', '["ارتباط","مهارت"]'],
    ['hyp-13-friendship-skills', 'اصول و لزوم دوستیابی', 'دوست پیدا کردن و دوست خوب ماندن', '["دوستی","مهارت"]'],
    ['hyp-14-emotion-logic-balance', 'تعادل احساس و منطق', 'جلسه‌ی مشترک قلب و مغز', '["تصمیم","تعادل"]'],
    ['hyp-15-anger-control', 'کنترل خشم', 'آتش در دست، نه در حال سوختن', '["خشم","کنترل"]'],
];


function toRows() {
    const rows = [];
    const push = (arr, prefix) => arr.forEach(([slug, title, subtitle, tags], i) => {
        rows.push({
            slug,
            category: catOf[prefix],
            titleFa: title,
            subtitleFa: subtitle,
            eyesClosed: true,
            totalSec: 900,
            audioCueId: `g-${slug.split('-').slice(0, 2).join('')}:start|deep|core|end`,
            referenceImageUrl: img(`g-${slug}`),
            timingMap: TIMING,
            addressFa: 'زهرا جان',
            orderIndex: i + 1,
            tags,
        });
    });
    push(mindfulness, 'mind');
    push(calm, 'calm');
    return rows;
}

async function upsertRow(row) {
    // جستجوی سطر موجود با slug یکتا
    let existing = null;
    try {
        const res = await tablesDb.listRows({
            databaseId: DATABASE_ID,
            tableId: TABLE_ID,
            queries: [Query.equal('slug', row.slug), Query.limit(1)],
        });
        existing = res.rows?.[0] || null;
    } catch (e) {
        console.log(`  ⚠️ list ${row.slug}: ${e.message || e}`);
    }
    if (dryRun) return 'dry';
    try {
        if (existing) {
            await tablesDb.updateRow({ databaseId: DATABASE_ID, tableId: TABLE_ID, rowId: existing.$id, data: row });
            return 'updated';
        } else {
            await tablesDb.createRow({ databaseId: DATABASE_ID, tableId: TABLE_ID, rowId: row.slug, data: row });
            return 'created';
        }
    } catch (e) {
        const msg = String(e.message || e);
        if (msg.includes('already exists') || msg.includes('409')) {
            try {
                await tablesDb.updateRow({ databaseId: DATABASE_ID, tableId: TABLE_ID, rowId: row.slug, data: row });
                return 'updated';
            } catch (e2) {
                console.log(`  ❌ ${row.slug}: ${e2.message || e2}`);
                return 'failed';
            }
        }
        console.log(`  ❌ ${row.slug}: ${msg}`);
        return 'failed';
    }
}

async function main() {
    const rows = toRows();
    const count = (c) => rows.filter((r) => r.category === c).length;
    if (rows.length !== 45) {
        console.error('❌ تعداد باید دقیقاً ۴۵ باشد (۳۰+۱۰+۵).');
        process.exit(1);
    }

    let created = 0, updated = 0, failed = 0;
    for (const row of rows) {
        const res = await upsertRow(row);
        if (res === 'created') created++;
        else if (res === 'updated') updated++;
        else if (res === 'failed') failed++;
        else process.stdout.write('.'); // dry
    }
    console.log(`\n${dryRun ? '🔍' : '✅'} خلاصه: ${created} ساخته شد، ${updated} به‌روز شد، ${failed} شکست`);
    if (failed > 0) process.exit(2);
}

main().catch((e) => { console.error('❌', e); process.exit(1); });
