# artifacts/prompt-03-push — بسته‌ی جلسات هدایت‌شده (پرامپت ۰۳)

**۴۵ جلسه‌ی ۱۵ دقیقه‌ای چهاربخشی:** ۳۰ ذهن‌آگاهی + ۱۰ آرامش بین درس‌ها + ۵ خودهیپنوتیزم
همه با خطاب «زهرا جان / زهرای عزیز» — ریتم آرام، مکث استاندارد بین بخش‌ها.

## ساختار پوشه (مطابق قرارداد prompt-02-push)

```
prompt-03-push/
  guided-scripts/            ← متن کامل ۴ بخشی هر جلسه (نشانگر [مکث N ثانیه] فقط برای پلیر)
    mindfulness/  30 فایل → mind-01 ... mind-30
    calm/         10 فایل → calm-01 ... calm-10
    hypnosis/      5 فایل → hyp-01 ... hyp-05
  guided-audio/              ← صوت TTS فارسی، ۴ فایل به‌ازای هر جلسه
    {category}/{slug}/p1.mp3  (بخش استارت — ۲۱۰ ثانیه هدف)
    {category}/{slug}/p2.mp3  (بخش دوم — ۲۷۰ ثانیه هدف)
    {category}/{slug}/p3.mp3  (بخش سوم — ۲۷۰ ثانیه هدف)
    {category}/{slug}/p4.mp3  (بخش پایانی — ۱۵۰ ثانیه هدف)
  guided-references/         ← تصاویر تولیدی هم‌راستا با متن/صوت
    {category}/{slug}.jpg     (کاور هر جلسه)
    covers/cover-*.jpg        (کاور ۳ منو)
```

## وضعیت تولید

| آیتم | تعداد | وضعیت |
|------|-------|--------|
| اسکریپت‌ها | ۷ از ۴۵ | ✅ hyp-01..05 + mind-01 + calm-01 (پایلوت کامل) |
| صوت‌ها | ۸ از ۱۸۰ | ✅ hyp-01 (۴ بخش) + mind-01 (۴ بخش) |
| تصاویر | ۶ | ✅ ۳ کاور جلسه + ۳ کاور منو |

باقی‌مانده در نوبت تولید است (سقف تولید صوت در هر مرحله محدود است؛ ترتیب: calm-01 → mind-02..30 → calm-02..10).

## استقرار روی سرور (الگوی دقیق deploy-prompt-02.yml)

```bash
# اجرای دستی از GitHub → Actions → "Deploy Prompt 03 (guided sessions)" → Run workflow
# یا محلی:
export APPWRITE_PROJECT_ID=... APPWRITE_API_KEY=...
node backend/seed/migrate-prompt-03.js        # گام ۱: جداول guided_sessions + guided_progress
node backend/seed/cleanup-guided-sessions.js  # گام ۲: حذف سطر تکراری (الگوی prompt-02)
node backend/seed/upload-guided-assets.js     # گام ۳: آپلود صوت/تصویر → باکت wellness-media
node backend/seed/seed-guided-sessions.js     # گام ۴: seed ۴۵ جلسه (upsert بر اساس slug)
```

هر ۴ اسکریپت کپی توسعه‌یافته‌ی اسکریپت‌های prompt-02 هستند (همان envها، همان APIهای TablesDB v18، همان idempotency) و `--dry-run` هم دارند.
