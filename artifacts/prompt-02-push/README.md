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
node backend/seed/migrate-prompt-02.js
```

### ۲. آپلود فایل‌ها به باکت wellness-media
```bash
# استفاده از Appwrite CLI
appwrite storage createFile \
  --bucketId wellness-media \
  --fileId "yoga/01-balasana-child-pose.jpg" \
  --file ./artifacts/prompt-02-push/wellness-references/yoga/01-balasana-child-pose.jpg

# یا با اسکریپت Node (نمونه در backend/seed/upload-assets.js)
```

### ۳. ساخت URL عمومی
پس از آپلود، URL باکت عمومی:
```
https://fra.cloud.appwrite.io/v1/storage/buckets/wellness-media/files/{fileId}/view?project=YOUR_PROJECT_ID
```

سپس در `WellnessCatalog.kt` ثابت `IMG_VIEW` را به‌روزرسانی کنید:
```kotlin
private const val IMG_VIEW = "/view?project=YOUR_PROJECT_ID"
```

## کدبیس مرتبط

- `apps/zahra-app/.../ui/wellness/` — ۸ فایل Compose
- `backend/appwrite.json` — schema با ۲۳ جدول، ۵ باکت، ۱۰ function
- `backend/functions/generate-sketch-reference/` — function تولید مرجع سیاه‌قلم
- `backend/seed/migrate-prompt-02.js` — migration script
- `shared/core-common/.../TableIds.kt` — شناسه‌ی جداول

## یادداشت

- پوشه‌ی `artifacts/wellness-references/` (نه این پوشه) در `.gitignore` است
- این پوشه‌ی `prompt-02-push/` شامل همه چیز برای deploy است
- پس از آپلود به Appwrite، فایل‌های صوتی و تصاویر در app از URL عمومی قابل دسترسی می‌شوند
