/**
 * آپلود assets پرامپت ۰۲ (تصاویر + صوت) به باکت wellness-media در Appwrite.
 *
 * ورودی: artifacts/prompt-02-push/wellness-references/ + wellness-audio/
 * ساختار فایل‌ها در باکت: {category}/{filename} (مثل yoga/01-balasana.jpg)
 *
 * اجرا:  node backend/seed/upload-wellness-assets.js
 * خشک:  node backend/seed/upload-wellness-assets.js --dry-run
 *
 * نکته: اگر فایل از قبل در باکت باشد، skip می‌شود (بر اساس filename یکتا).
 */
const sdk = require('node-appwrite');
const fs = require('fs');
const path = require('path');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const BUCKET_ID = 'wellness-media';
const SOURCE_DIR = path.resolve(__dirname, '../../artifacts/prompt-02-push');

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const storage = new sdk.Storage(client);

function listAllFiles(root) {
    const results = [];
    if (!fs.existsSync(root)) return results;
    for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
        const full = path.join(root, entry.name);
        if (entry.isDirectory()) {
            results.push(...listAllFiles(full));
        } else if (entry.name.endsWith('.jpg') || entry.name.endsWith('.png') || entry.name.endsWith('.mp3')) {
            // مسیر نسبی به SOURCE_DIR
            const rel = path.relative(SOURCE_DIR, full).replace(/\\/g, '/');
            results.push({ abs: full, rel });
        }
    }
    return results;
}

async function fileExistsInBucket(fileId) {
    try {
        await storage.getFile({ bucketId: BUCKET_ID, fileId });
        return true;
    } catch (e) {
        const msg = String(e.message || e);
        if (msg.includes('not found') || msg.includes('404')) return false;
        throw e;
    }
}

async function uploadFile(file) {
    const fileId = file.rel.replace(/[\\/]/g, '-').replace(/\.(jpg|png|mp3)$/, '');
    if (await fileExistsInBucket(fileId)) {
        console.log(`  · ${file.rel} (exists as ${fileId})`);
        return { skipped: true, fileId };
    }
    if (dryRun) {
        console.log(`  [dry] upload ${file.rel} as ${fileId}`);
        return { skipped: false, fileId };
    }
    // permissions: read=any (همانطور که در migration تنظیم شد)
    // file را به صورت stream آپلود می‌کنیم
    const buffer = fs.readFileSync(file.abs);
    await storage.createFile({
        bucketId: BUCKET_ID,
        fileId,
        file: buffer,
        // permissions: ['read("any")']  // از permissions باکت به ارث می‌برد
    });
    console.log(`  ✅ ${file.rel} → ${fileId}`);
    return { skipped: false, fileId };
}

async function main() {
    console.log(`آپلود assets پرامپت ۰۲ از ${SOURCE_DIR}`);
    console.log(`به باکت: ${BUCKET_ID}`);
    console.log(`پروژه: ${PROJECT_ID}`);

    const refDir = path.join(SOURCE_DIR, 'wellness-references');
    const audioDir = path.join(SOURCE_DIR, 'wellness-audio');

    const refFiles = listAllFiles(refDir);
    const audioFiles = listAllFiles(audioDir);
    const all = [...refFiles, ...audioFiles];

    console.log(`\n📁 ${refFiles.length} تصویر + ${audioFiles.length} فایل صوتی = ${all.length} فایل`);

    let uploaded = 0, skipped = 0, failed = 0;
    for (const file of all) {
        try {
            const res = await uploadFile(file);
            if (res.skipped) skipped++;
            else if (!dryRun) uploaded++;
        } catch (e) {
            console.log(`  ❌ ${file.rel}: ${e.message || e}`);
            failed++;
        }
    }

    console.log(`\n${dryRun ? '🔍' : '✅'} خلاصه: ${uploaded} آپلود، ${skipped} رد شد، ${failed} شکست`);
}

main().catch((e) => { console.error('❌', e); process.exit(1); });
