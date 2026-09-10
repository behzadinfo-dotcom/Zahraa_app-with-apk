# 🚀 راهنمای پوش شاخه‌ی feature/prompt-03-wellness-v2

**پایه:** main @ 9541b40 · **این شاخه:** تمام دستاوردهای پرامپت-۰۳ (متن‌های نهایی ۴ بسته، کوئه‌های v2 تک‌فایل، سید سینک‌شده + BREATH_ANIM، ابزارهای میکس و بایگانی)

## روش ۱ — پوش توسط مالک (سریع‌ترین)
```bash
cd Zahraa_repo
git remote add origin https://github.com/behzadinfo-dotcom/Zahraa_app-with-apk.git   # اگر ریموت نیست
git push -u origin feature/prompt-03-wellness-v2
```

## روش ۲ — پوش توسط ایجنت (با توکن)
یک Fine-grained PAT با دسترسی Contents: Read and write روی همین مخزن بسازید و در چت بدهید؛ ایجنت پوش می‌کند و توکن را از پیکربندی پاک می‌کند.

> ⚠️ نکته‌ی محیط سندباکس: پیکربندی گیت (`.git/config`) بین نشست‌ها ذخیره نمی‌شود؛ بعد از ریست فقط کافی است ریموت را دوباره اضافه کنید — شاخه و کامیت‌ها محفوظ می‌مانند.

## پس از merge به main
ورک‌فلوی `deploy-prompt-03.yml` (۴ گام: migrate → cleanup → upload → seed) قابل اجراست.
