# پرامپت ۰۲ — بسته‌ی push نهایی

این پوشه محتوای کامل پرامپت ۰۲ (ماژول سلامتی) را برای deploy در Appwrite جمع‌آوری کرده است.

## ساختار

```
artifacts/prompt-02-push/
├── wellness-references/         # ۴۶ تصویر مرجع
│   ├── yoga/        (۱۵ تصویر)
│   ├── exercise/    (۱۵ تصویر)
│   ├── breathing/   (۸ تصویر)
│   ├── learning/    (۵ تصویر)
│   └── _incoming/   (۳ تصویر مرجع اصلی)
└── wellness-audio/              # ۷۵ فایل صوتی TTS فارسی
    ├── cue-yoga-*.mp3    (۵۰ فایل — ۱۰ حرکت کامل × ۳ + ۱۰ end + ۱۰ start)
    ├── cue-ex-*.mp3      (۱۵ فایل)
    ├── cue-breath-*.mp3  (۸ فایل)
    └── cue-learn-*.mp3   (۵ فایل)
```

## بررسی کمی

- **تصاویر مرجع**: ۴۶ فایل (~۷.۷MB)
  - ۱۵ یوگا + ۱۵ ورزش + ۸ تنفس + ۵ یادگیری = **۴۳ تصویر اصلی** + ۳ مرجع
  - چهره‌ی ثابت: دختر ایرانی نوجوان، پوست صاف، بینی صاف طبیعی (no hooked/Roman nose)
  - پس‌زمینه‌ی ثابت: استودیوی یوگا با کتابخانه و گیاه

- **فایل‌های صوتی**: ۷۵ فایل (~۱۸MB)
  - صدای فارسی زنانه گرم و صمیمی
  - لحن همراهی با مخاطب «زهرا جان»
  - هر فایل ~۲۰-۲۵ ثانیه
  - ۱۰ یوگا با ۳ فایل کامل (start, mid, end) + ۵ یوگا با ۱ فایل طولانی + start/end اضافی

## مراحل deploy

### ۱. ساخت جداول و باکت
```bash
cd /home/user/Zahraa_app-with-apk
APPWRITE_PROJECT_ID=... APPWRITE_API_KEY=... node backend/seed/migrate-prompt-02.js
```

### ۲. آپلود فایل‌ها به باکت wellness-media
```bash
# ۴۶ تصویر + ۷۵ فایل صوتی
APPWRITE_PROJECT_ID=... APPWRITE_API_KEY=... node backend/seed/upload-wellness-assets.js
```

### ۳. بارگذاری ۴۳ حرکت به جدول wellness_moves
```bash
APPWRITE_PROJECT_ID=... APPWRITE_API_KEY=... node backend/seed/seed-wellness-moves.js
```

### ۴. URL عمومی
پس از آپلود، URL باکت عمومی:
```
https://fra.cloud.appwrite.io/v1/storage/buckets/wellness-media/files/{fileId}/view?project=6a9d59e3002751cc3ea8
```

که در `WellnessCatalog.kt` به‌صورت `IMG_VIEW` تنظیم شده است:
```kotlin
private const val IMG_VIEW = "/view?project=6a9d59e3002751cc3ea8"
```

## کدبیس مرتبط

- `apps/zahra-app/.../ui/wellness/` — ۸ فایل Compose + ۲ unit test
- `backend/appwrite.json` — schema با ۲۳ جدول، ۵ باکت، ۱۰ function
- `backend/functions/generate-sketch-reference/` — function تولید مرجع سیاه‌قلم
- `backend/seed/migrate-prompt-02.js` — migration script
- `backend/seed/upload-wellness-assets.js` — آپلود تصاویر + صوت
- `backend/seed/seed-wellness-moves.js` — بارگذاری ۴۳ حرکت
- `shared/core-common/.../TableIds.kt` — شناسه‌ی جداول

## یادداشت

- پوشه‌ی `artifacts/wellness-references/` (نه این پوشه) در `.gitignore` است
- این پوشه‌ی `prompt-02-push/` شامل همه چیز برای deploy است
- پس از آپلود به Appwrite، فایل‌های صوتی و تصاویر در app از URL عمومی قابل دسترسی می‌شوند
- فرمت audioCueId: برای یوگا `start|mid|end` (pipe-separated)؛ برای سایر دسته‌ها تک فایل
