# راهنمای صداهای رسمی آژور (Microsoft) و گوگل برای تولید ۱۸۰ فایل صوتی جلسات

> خط تولید (upload-guided-assets.js) هر mp3‌ای را از هر منبعی قبول می‌کند.
> برای کیفیت استودیویی و صدای طبیعی فارسی، این صداهای رسمی پیشنهاد می‌شوند:

## 🔵 Microsoft Azure — صداهای فارسی `fa-IR`

| صدا | جنسیت | ویژگی | مناسب برای |
|-----|--------|--------|------------|
| **`fa-IR-DilaraNeural`** | زن | گرم، طبیعی، محبوب‌ترین صدای فارسی آژور | ذهن‌آگاهی و آرامش (پیشنهاد اول) |
| **`fa-IR-FaridNeural`** | مرد | آرام، بم، قابل‌اعتماد | جایگزین / تنوع |

### تنظیمات پیشنهادی برای جلسات مدیتیشن (SSML)
```xml
<speak version="1.0" xml:lang="fa-IR">
  <voice name="fa-IR-DilaraNeural">
    <prosody rate="-20%" pitch="-5%">
      زهرا جان، سلام به تو، زهرای عزیز...
      <break time="2000ms"/>
      چشم‌هایت را با آرامش ببند...
    </prosody>
  </voice>
</speak>
```
- `rate="-20%"` تا `-30%` → ریتم آرام مدیتیشن (معادل speed 0.92 در timingMap)
- `<break time="..."/>` → مکث‌های بین بخش‌ها (به‌جای نشانگر [مکث] در متن)
- مستند: learn.microsoft.com/azure/ai-services/speech-service/language-support

## 🟢 Google Cloud Text-to-Speech — صداهای فارسی

| صدا | جنسیت | ویژگی | مناسب برای |
|-----|--------|--------|------------|
| **`fa-IR-Wavenet-A`** | زن | شفاف، نرم | ذهن‌آگاهی |
| **`fa-IR-Wavenet-B`** | زن | کمی بم‌تر و گرم‌تر | خودهیپنوتیزم / خواب |

### تنظیم (JSON API)
```json
{
  "input": { "ssml": "<speak>...زهرا جان...<break time=\"2s\"/></speak>" },
  "voice": { "languageCode": "fa-IR", "name": "fa-IR-Wavenet-A" },
  "audioConfig": {
    "audioEncoding": "MP3",
    "speakingRate": 0.85,
    "pitch": -2.0,
    "effectsProfileId": ["headphone-class-device"]
  }
}
```
- `speakingRate: 0.85` → آرام و مدیتیشن‌وار
- `effectsProfileId: headphone-class-device` → بهینه برای هدفون (تمرین با چشم بسته)

## 📋 چک‌لیست خط تولید صوت (برای هر ۴۵ جلسه × ۴ بخش = ۱۸۰ فایل)
1. متن md هر جلسه → حذف نشانگرهای `[مکث N ثانیه]` → تبدیل به SSML با `<break>` متناسب.
2. تولید با صدای انتخابی (آژور یا گوگل) → خروجی `p1..p4.mp3` در
   `artifacts/prompt-03-push/guided-audio/{category}/{slug}/`.
3. **پد به مدت دقیق (استراتژی ۴ فایل = ۱۵:۰۰):**
   `./backend/seed/pad-guided-audio.sh <session-dir>`
   - هر فراخوانی TTS حداکثر ~۱۵۰۰ کاراکتر ≈ ۲.۵-۳ دقیقه گفتار است؛
   - اسکریپت گفتار را ۰.۹۵x آرام می‌کند و با سکوتِ طبیعی به اهداف دقیق می‌رساند:
     **p1=۲۷۰s + p2=۲۴۰s + p3=۲۴۰s + p4=۱۵۰s = دقیقاً ۹۰۰s (۱۵:۰۰)**؛
   - مکث بین بخش‌ها داخل انتهای همان فایل است → فایل‌ها پشت‌سرهم بدون درز کل جلسه را می‌سازند؛
   - نوار ۴ سگمنتی پلیر و timingMap (در seed) با همین اعداد همگام‌اند.
4. آپلود با `node backend/seed/upload-guided-assets.js` (idempotent — فایل موجود را skip می‌کند).

## ✅ وضعیت فعلی
- دو نمونه‌ی همین محیط (`نمونه-صدا-A.mp3` و `نمونه-صدا-B.mp3`) برای انتخاب طعم صدا ساخته شده.
- پس از انتخاب A یا B، تولید پایلوت ۸ فایل (hyp-01 + mind-01) با همان طعم انجام می‌شود.
- برای تولید نهایی ۱۸۰ فایلی، اتصال به Azure Speech یا Google TTS در CI (workflow جدا: `tts-prompt-03.yml`) پیشنهاد می‌شود — کلید API در GitHub Secrets.
