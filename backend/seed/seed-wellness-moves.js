/**
 * وارد کردن ۴۳ حرکت سلامتی به جدول wellness_moves در Appwrite.
 *
 * الگوریتم:
 *  - catalog داخلی Kotlin به JSON تبدیل می‌شود (در اینجا hardcoded).
 *  - اگر سطری با همان slug وجود داشته باشد، به‌روزرسانی می‌شود؛ در غیر این صورت ساخته می‌شود.
 *  - audioCueId به فرمت ۳-فایلی برای یوگا (start|mid|end).
 *
 * اجرا:  node backend/seed/seed-wellness-moves.js
 * خشک:  node backend/seed/seed-wellness-moves.js --dry-run
 */
const sdk = require('node-appwrite');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';
const TABLE_ID = 'wellness_moves';

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const tablesDb = new sdk.TablesDB(client);

const IMG_BASE = 'https://fra.cloud.appwrite.io/v1/storage/buckets/wellness-media/files/';
const IMG_VIEW = '/view?project=' + PROJECT_ID;
const img = (filename) => `${IMG_BASE}${filename}${IMG_VIEW}`;

// ۴۳ حرکت (مستقیم از WellnessCatalog.kt)
const moves = [
    // --- ۱۵ یوگا ---
    { slug: 'yoga-balasana', category: 'yoga', titleFa: 'کودک (Balasana)', level: 1, durationSec: 60, audioCueId: 'cue-yoga-balasana-start.mp3|cue-yoga-balasana-mid.mp3|cue-yoga-balasana-end.mp3', referenceImageUrl: img('yoga/01-balasana-child-pose.jpg'), orderIndex: 1, tags: '["آرامش‌بخش","کشش کمر"]' },
    { slug: 'yoga-cat-cow', category: 'yoga', titleFa: 'گربه-گاو', level: 2, durationSec: 60, audioCueId: 'cue-yoga-cat-cow-start.mp3|cue-yoga-cat-cow-mid.mp3|cue-yoga-cat-cow-end.mp3', referenceImageUrl: img('yoga/02-cat-cow.jpg'), orderIndex: 2, tags: '[]' },
    { slug: 'yoga-downward-dog', category: 'yoga', titleFa: 'سگ رو به پایین', level: 4, durationSec: 45, audioCueId: 'cue-yoga-downward-dog-start.mp3|cue-yoga-downward-dog-mid.mp3|cue-yoga-downward-dog-end.mp3', referenceImageUrl: img('yoga/03-downward-dog.jpg'), orderIndex: 3, tags: '["کشش همسترینگ","تقویت شانه"]' },
    { slug: 'yoga-cobra', category: 'yoga', titleFa: 'کبرا', level: 3, durationSec: 30, audioCueId: 'cue-yoga-cobra-start.mp3|cue-yoga-cobra-mid.mp3|cue-yoga-cobra-end.mp3', referenceImageUrl: img('yoga/04-cobra.jpg'), orderIndex: 4, tags: '["تقویت کمر"]' },
    { slug: 'yoga-warrior-1', category: 'yoga', titleFa: 'جنگجو ۱', level: 4, durationSec: 30, audioCueId: 'cue-yoga-warrior-1-start.mp3|cue-yoga-warrior-1-mid.mp3|cue-yoga-warrior-1-end.mp3', referenceImageUrl: img('yoga/05-warrior-1.jpg'), orderIndex: 5, tags: '[]' },
    { slug: 'yoga-warrior-2', category: 'yoga', titleFa: 'جنگجو ۲', level: 4, durationSec: 30, audioCueId: 'cue-yoga-warrior-2-start.mp3|cue-yoga-warrior-2-mid.mp3|cue-yoga-warrior-2-end.mp3', referenceImageUrl: img('yoga/06-warrior-2.jpg'), orderIndex: 6, tags: '[]' },
    { slug: 'yoga-tree', category: 'yoga', titleFa: 'درخت', level: 5, durationSec: 30, audioCueId: 'cue-yoga-tree-start.mp3|cue-yoga-tree-mid.mp3|cue-yoga-tree-end.mp3', referenceImageUrl: img('yoga/07-tree-pose.jpg'), orderIndex: 7, tags: '["تعادل"]' },
    { slug: 'yoga-bridge', category: 'yoga', titleFa: 'پل', level: 3, durationSec: 30, audioCueId: 'cue-yoga-bridge-start.mp3|cue-yoga-bridge-mid.mp3|cue-yoga-bridge-end.mp3', referenceImageUrl: img('yoga/08-bridge-pose.jpg'), orderIndex: 8, tags: '["تقویت باسن","کشش شکم"]' },
    { slug: 'yoga-spinal-twist', category: 'yoga', titleFa: 'پیچش ستون فقرات', level: 3, durationSec: 45, audioCueId: 'cue-yoga-spinal-twist-start.mp3|cue-yoga-spinal-twist-mid.mp3|cue-yoga-spinal-twist-end.mp3', referenceImageUrl: img('yoga/09-seated-spinal-twist.jpg'), orderIndex: 9, tags: '[]' },
    { slug: 'yoga-wide-child', category: 'yoga', titleFa: 'کودک گسترده', level: 2, durationSec: 60, audioCueId: 'cue-yoga-wide-child-start.mp3|cue-yoga-wide-child-mid.mp3|cue-yoga-wide-child-end.mp3', referenceImageUrl: img('yoga/10-wide-legged-child.jpg'), orderIndex: 10, tags: '[]' },
    { slug: 'yoga-pigeon', category: 'yoga', titleFa: 'کبوتر', level: 5, durationSec: 60, audioCueId: 'cue-yoga-pigeon-start.mp3|cue-yoga-pigeon-mid.mp3|cue-yoga-pigeon-end.mp3', referenceImageUrl: img('yoga/11-pigeon-pose.jpg'), orderIndex: 11, tags: '["بازکننده‌ی لگن"]' },
    { slug: 'yoga-triangle', category: 'yoga', titleFa: 'مثلث', level: 5, durationSec: 30, audioCueId: 'cue-yoga-triangle-start.mp3|cue-yoga-triangle-mid.mp3|cue-yoga-triangle-end.mp3', referenceImageUrl: img('yoga/12-triangle-pose.jpg'), orderIndex: 12, tags: '[]' },
    { slug: 'yoga-savasana', category: 'yoga', titleFa: 'شاواسانا (جسد)', level: 1, durationSec: 180, audioCueId: 'cue-yoga-savasana-start.mp3|cue-yoga-savasana-mid.mp3|cue-yoga-savasana-end.mp3', referenceImageUrl: img('yoga/13-savasana.jpg'), orderIndex: 13, tags: '["ریلکسیشن نهایی"]' },
    { slug: 'yoga-half-boat', category: 'yoga', titleFa: 'نیم‌قایق', level: 6, durationSec: 30, audioCueId: 'cue-yoga-half-boat-start.mp3|cue-yoga-half-boat-mid.mp3|cue-yoga-half-boat-end.mp3', referenceImageUrl: img('yoga/14-half-boat.jpg'), orderIndex: 14, tags: '["تقویت شکم"]' },
    { slug: 'yoga-standing-forward-bend', category: 'yoga', titleFa: 'خم‌شدن ایستاده', level: 2, durationSec: 45, audioCueId: 'cue-yoga-standing-forward-bend-start.mp3|cue-yoga-standing-forward-bend-mid.mp3|cue-yoga-standing-forward-bend-end.mp3', referenceImageUrl: img('yoga/15-standing-forward-bend.jpg'), orderIndex: 15, tags: '["کشش همسترینگ"]' },

    // --- ۱۵ ورزش ---
    { slug: 'ex-neck-4way', category: 'exercise', titleFa: 'کشش گردن ۴ جهت', level: 1, durationSec: 60, audioCueId: 'cue-ex-neck.mp3', referenceImageUrl: img('exercise/01-neck-stretch.jpg'), orderIndex: 1, tags: '[]' },
    { slug: 'ex-shoulder-cross', category: 'exercise', titleFa: 'کشش شانه ضربدری', level: 1, durationSec: 30, audioCueId: 'cue-ex-shoulder.mp3', referenceImageUrl: img('exercise/02-shoulder-stretch.jpg'), orderIndex: 2, tags: '[]' },
    { slug: 'ex-wrist-ankle', category: 'exercise', titleFa: 'چرخش مچ دست و پا', level: 1, durationSec: 60, audioCueId: 'cue-ex-wrist-ankle.mp3', referenceImageUrl: img('exercise/03-wrist-ankle-rotation.jpg'), orderIndex: 3, tags: '[]' },
    { slug: 'ex-squat', category: 'exercise', titleFa: 'اسکوات آرام', level: 4, durationSec: 60, reps: 10, audioCueId: 'cue-ex-squat.mp3', referenceImageUrl: img('exercise/04-bodyweight-squat.jpg'), orderIndex: 4, tags: '[]' },
    { slug: 'ex-lunge', category: 'exercise', titleFa: 'لانگز', level: 4, durationSec: 60, reps: 10, audioCueId: 'cue-ex-lunge.mp3', referenceImageUrl: img('exercise/05-lunges.jpg'), orderIndex: 5, tags: '[]' },
    { slug: 'ex-plank', category: 'exercise', titleFa: 'پلانک (روی آرنج)', level: 5, durationSec: 30, audioCueId: 'cue-ex-plank.mp3', referenceImageUrl: img('exercise/06-plank-beginner.jpg'), orderIndex: 6, tags: '[]' },
    { slug: 'ex-crunch', category: 'exercise', titleFa: 'کرانچ شکم ملایم', level: 4, durationSec: 60, reps: 10, audioCueId: 'cue-ex-crunch.mp3', referenceImageUrl: img('exercise/07-gentle-crunch.jpg'), orderIndex: 7, tags: '[]' },
    { slug: 'ex-hamstring', category: 'exercise', titleFa: 'کشش همسترینگ نشسته', level: 2, durationSec: 60, audioCueId: 'cue-ex-hamstring.mp3', referenceImageUrl: img('exercise/08-hamstring-stretch.jpg'), orderIndex: 8, tags: '[]' },
    { slug: 'ex-butterfly', category: 'exercise', titleFa: 'حرکت پروانه', level: 1, durationSec: 45, audioCueId: 'cue-ex-butterfly.mp3', referenceImageUrl: img('exercise/09-butterfly-stretch.jpg'), orderIndex: 9, tags: '[]' },
    { slug: 'ex-calf-raise', category: 'exercise', titleFa: 'بالا-پایین پاشنه', level: 3, durationSec: 45, reps: 15, audioCueId: 'cue-ex-calf.mp3', referenceImageUrl: img('exercise/10-calf-raises.jpg'), orderIndex: 10, tags: '[]' },
    { slug: 'ex-trunk-rotation', category: 'exercise', titleFa: 'چرخش تنه ایستاده', level: 2, durationSec: 45, audioCueId: 'cue-ex-trunk.mp3', referenceImageUrl: img('exercise/11-standing-trunk-rotation.jpg'), orderIndex: 11, tags: '[]' },
    { slug: 'ex-wall-calf', category: 'exercise', titleFa: 'کشش ساق پا به دیوار', level: 2, durationSec: 45, audioCueId: 'cue-ex-wall-calf.mp3', referenceImageUrl: img('exercise/12-wall-calf-stretch.jpg'), orderIndex: 12, tags: '[]' },
    { slug: 'ex-superman', category: 'exercise', titleFa: 'سوپرمن (تقویت کمر)', level: 5, durationSec: 45, reps: 8, audioCueId: 'cue-ex-superman.mp3', referenceImageUrl: img('exercise/13-superman.jpg'), orderIndex: 13, tags: '[]' },
    { slug: 'ex-jumping-jack', category: 'exercise', titleFa: 'جامپینگ جک ملایم', level: 4, durationSec: 60, reps: 15, audioCueId: 'cue-ex-jj.mp3', referenceImageUrl: img('exercise/14-jumping-jacks.jpg'), orderIndex: 14, tags: '["گرم‌کردن"]' },
    { slug: 'ex-wrist-forearm', category: 'exercise', titleFa: 'کشش مچ و ساعد', level: 1, durationSec: 45, audioCueId: 'cue-ex-wrist-forearm.mp3', referenceImageUrl: img('exercise/15-wrist-forearm-stretch.jpg'), orderIndex: 15, tags: '[]' },

    // --- ۸ تنفس ---
    { slug: 'breath-diaphragm', category: 'breathing', titleFa: 'تنفس شکمی', level: 1, durationSec: 120, audioCueId: 'cue-breath-diaphragm.mp3', referenceImageUrl: img('breathing/01-diaphragmatic.jpg'), orderIndex: 1, tags: '[]' },
    { slug: 'breath-4-7-8', category: 'breathing', titleFa: 'تنفس ۴-۷-۸', level: 2, durationSec: 120, audioCueId: 'cue-breath-478.mp3', referenceImageUrl: img('breathing/02-4-7-8-breath.jpg'), orderIndex: 2, tags: '["خواب"]' },
    { slug: 'breath-box', category: 'breathing', titleFa: 'تنفس جعبه‌ای', level: 2, durationSec: 180, audioCueId: 'cue-breath-box.mp3', referenceImageUrl: img('breathing/03-box-breathing.jpg'), orderIndex: 3, tags: '["تمرکز"]' },
    { slug: 'breath-nadi', category: 'breathing', titleFa: 'تنفس بینی متناوب', level: 4, durationSec: 180, audioCueId: 'cue-breath-nadi.mp3', referenceImageUrl: img('breathing/04-nadi-shodhana.jpg'), orderIndex: 4, tags: '[]' },
    { slug: 'breath-sigh', category: 'breathing', titleFa: 'آه‌کشیدن آرام‌بخش', level: 1, durationSec: 90, audioCueId: 'cue-breath-sigh.mp3', referenceImageUrl: img('breathing/05-sighing-breath.jpg'), orderIndex: 5, tags: '[]' },
    { slug: 'breath-lion', category: 'breathing', titleFa: 'تنفس شیر', level: 5, durationSec: 60, audioCueId: 'cue-breath-lion.mp3', referenceImageUrl: img('breathing/06-lions-breath.jpg'), orderIndex: 6, tags: '["رهاسازی تنش"]' },
    { slug: 'breath-counting', category: 'breathing', titleFa: 'تنفس شمارشی قبل امتحان', level: 2, durationSec: 120, audioCueId: 'cue-breath-counting.mp3', referenceImageUrl: img('breathing/07-counting-breath.jpg'), orderIndex: 7, tags: '["تمرکز","امتحان"]' },
    { slug: 'breath-bedtime', category: 'breathing', titleFa: 'آرام‌سازی قبل خواب', level: 1, durationSec: 300, audioCueId: 'cue-breath-bedtime.mp3', referenceImageUrl: img('breathing/08-bedtime-breath.jpg'), orderIndex: 8, tags: '["خواب"]' },

    // --- ۵ یادگیری ---
    { slug: 'learn-pomodoro', category: 'learning', titleFa: 'تکنیک پومودورو (۲۵/۵)', level: 1, durationSec: 1500, audioCueId: 'cue-learn-pomodoro.mp3', referenceImageUrl: img('learning/01-pomodoro.jpg'), orderIndex: 1, tags: '[]' },
    { slug: 'learn-spaced-repetition', category: 'learning', titleFa: 'تکنیک تکرار فاصله‌دار', level: 2, durationSec: 600, audioCueId: 'cue-learn-spaced.mp3', referenceImageUrl: img('learning/02-spaced-repetition.jpg'), orderIndex: 2, tags: '[]' },
    { slug: 'learn-feynman', category: 'learning', titleFa: 'روش فاینمن', level: 2, durationSec: 600, audioCueId: 'cue-learn-feynman.mp3', referenceImageUrl: img('learning/03-feynman.jpg'), orderIndex: 3, tags: '[]' },
    { slug: 'learn-mind-map', category: 'learning', titleFa: 'نقشه‌ی ذهنی', level: 2, durationSec: 900, audioCueId: 'cue-learn-mindmap.mp3', referenceImageUrl: img('learning/04-mind-map.jpg'), orderIndex: 4, tags: '[]' },
    { slug: 'learn-active-recall', category: 'learning', titleFa: 'فعال‌سازی حافظه', level: 2, durationSec: 600, audioCueId: 'cue-learn-recall.mp3', referenceImageUrl: img('learning/05-active-recall.jpg'), orderIndex: 5, tags: '[]' },
];

async function rowExists(slug) {
    try {
        const res = await tablesDb.listRows({
            databaseId: DATABASE_ID,
            tableId: TABLE_ID,
            queries: [`equal("slug", ["${slug}"])`],
        });
        return res.rows.length > 0 ? res.rows[0] : null;
    } catch (e) {
        return null;
    }
}

async function upsertMove(move) {
    const data = {
        slug: move.slug,
        category: move.category,
        titleFa: move.titleFa,
        level: move.level,
        durationSec: move.durationSec,
        reps: move.reps || 0,
        instructionsFa: move.instructionsFa || '',
        audioCueId: move.audioCueId,
        referenceImageUrl: move.referenceImageUrl,
        referenceImagePromptTemplate: move.referenceImagePromptTemplate || '',
        orderIndex: move.orderIndex,
        tags: move.tags || '[]',
    };
    const existing = await rowExists(move.slug);
    if (existing) {
        if (dryRun) return console.log(`  [dry] update ${move.slug}`);
        await tablesDb.updateRow({
            databaseId: DATABASE_ID,
            tableId: TABLE_ID,
            rowId: existing.$id,
            data,
        });
        console.log(`  ✅ ${move.slug} (updated)`);
    } else {
        if (dryRun) return console.log(`  [dry] create ${move.slug}`);
        // نیاز به permission برای ایجاد: user با role admin
        // امیدوارم API key این اجازه را داشته باشد
        await tablesDb.createRow({
            databaseId: DATABASE_ID,
            tableId: TABLE_ID,
            rowId: sdk.ID.unique(),
            data,
        });
        console.log(`  ✅ ${move.slug} (created)`);
    }
}

async function main() {
    console.log(`بارگذاری ${moves.length} حرکت به جدول ${TABLE_ID}`);
    let created = 0, updated = 0, failed = 0;
    for (const m of moves) {
        try {
            const before = await rowExists(m.slug);
            await upsertMove(m);
            if (before) updated++;
            else created++;
        } catch (e) {
            console.log(`  ❌ ${m.slug}: ${e.message || e}`);
            failed++;
        }
    }
    console.log(`\n${dryRun ? '🔍' : '✅'} خلاصه: ${created} ساخته، ${updated} به‌روز، ${failed} شکست`);
}

main().catch((e) => { console.error('❌', e); process.exit(1); });
