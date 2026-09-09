package ir.behzad.roozhayeman.ui.wellness

/**
 * کاتالوگ داخلیِ حرکت‌ها (آفلاین) — بازتابِ backend/seed/content.json (فایل توسعه ۰۲).
 *
 * وقتی سرور در دسترس نباشد، همین فهرست نمایش داده می‌شود تا تایمر/شمارنده و راهنما
 * کامل آفلاین کار کند. تصاویر اینجا خالی‌اند؛ تصویر با چهره‌ی کاربر هنگام اجرا از
 * تابع generate-move-image ساخته می‌شود.
 */
object WellnessCatalog {
    val moves: List<WellnessMove> = listOf(
        WellnessMove("yoga-balasana", WellnessCategory.YOGA, "کودک (Balasana)", 3, 60, 0, "زانوها را خم کن و بنشین، پیشانی را روی زمین بگذار، دست‌ها جلو، سه نفس عمیق.", "cue-yoga-01", "", "", 1),
        WellnessMove("yoga-cat-cow", WellnessCategory.YOGA, "گربه-گاو (Cat-Cow)", 3, 60, 0, "چهار دست‌وپا، دم: کمر گود و سر بالا؛ بازدم: کمر گرد و چانه به سینه.", "cue-yoga-02", "", "", 2),
        WellnessMove("yoga-downdog", WellnessCategory.YOGA, "سگ رو به پایین (Downward Dog)", 4, 45, 0, "از حالت چهار دست‌وپا باسن را بالا ببر تا بدن مثلث شود، پاشنه‌ها به سمت زمین.", "cue-yoga-03", "", "", 3),
        WellnessMove("yoga-cobra", WellnessCategory.YOGA, "کبرا (Cobra)", 4, 40, 0, "روی شکم دراز بکش، کف دست‌ها کنار سینه، بالاتنه را آرام بالا بیاور.", "cue-yoga-04", "", "", 4),
        WellnessMove("yoga-warrior", WellnessCategory.YOGA, "جنگجو ۱ و ۲ (Warrior I/II)", 5, 60, 0, "یک پا جلو و خم، پای دیگر کشیده عقب، دست‌ها باز، نگاه به جلو.", "cue-yoga-05", "", "", 5),
        WellnessMove("yoga-tree", WellnessCategory.YOGA, "درخت (Tree Pose)", 5, 45, 0, "روی یک پا بایست، کف پای دیگر را روی ساق یا ران بگذار، دست‌ها بالا.", "cue-yoga-06", "", "", 6),
        WellnessMove("yoga-bridge", WellnessCategory.YOGA, "پل (Bridge Pose)", 4, 45, 0, "به پشت بخواب، زانوها خم، باسن را بالا ببر و چند نفس نگه دار.", "cue-yoga-07", "", "", 7),
        WellnessMove("yoga-seated-twist", WellnessCategory.YOGA, "پیچش نشسته‌ی ستون فقرات (Seated Spinal Twist)", 4, 45, 0, "نشسته، یک زانو خم، بالاتنه را به سمت همان زانو بچرخان.", "cue-yoga-08", "", "", 8),
        WellnessMove("yoga-wide-child", WellnessCategory.YOGA, "کودک گسترده (Wide-Legged Child)", 3, 60, 0, "مثل حالت کودک ولی زانوها باز، دست‌ها کشیده رو به جلو.", "cue-yoga-09", "", "", 9),
        WellnessMove("yoga-pigeon", WellnessCategory.YOGA, "کبوتر (Pigeon Pose)", 6, 50, 0, "یک ساق جلو روی زمین، پای دیگر کشیده عقب، بالاتنه آرام رو به جلو.", "cue-yoga-10", "", "", 10),
        WellnessMove("yoga-triangle", WellnessCategory.YOGA, "مثلث (Triangle Pose)", 5, 45, 0, "پاها باز، یک دست به سمت مچ پا، دست دیگر رو به آسمان، بدن یک صفحه.", "cue-yoga-11", "", "", 11),
        WellnessMove("yoga-savasana", WellnessCategory.YOGA, "شاواسانا/جسد (Savasana)", 2, 90, 0, "به پشت دراز بکش، بدن کاملاً شل، تنفس طبیعی، فقط رها شدن.", "cue-yoga-12", "", "", 12),
        WellnessMove("yoga-half-boat", WellnessCategory.YOGA, "نیم‌قایق (Half Boat)", 6, 30, 0, "نشسته، پاها را نیمه بالا بیاور، دست‌ها موازی زمین، تعادل را نگه دار.", "cue-yoga-13", "", "", 13),
        WellnessMove("yoga-ext-cat", WellnessCategory.YOGA, "گربه‌ی کشیده (Extended Cat Stretch)", 4, 40, 0, "از چهار دست‌وپا دست‌ها را جلو بکش و سینه را به زمین نزدیک کن.", "cue-yoga-14", "", "", 14),
        WellnessMove("yoga-forward-bend", WellnessCategory.YOGA, "خم‌شدن رو به جلو ایستاده (Standing Forward Bend)", 4, 40, 0, "ایستاده، از کمر آرام رو به جلو خم شو، زانوها کمی نرم.", "cue-yoga-15", "", "", 15),
        WellnessMove("ex-neck-4", WellnessCategory.EXERCISE, "کشش گردن ۴ جهت", 2, 60, 4, "سر را آرام به جلو، عقب، چپ و راست خم کن؛ هر جهت چند ثانیه نگه دار.", "cue-exercise-01", "", "", 1),
        WellnessMove("ex-shoulder", WellnessCategory.EXERCISE, "کشش شانه (Cross-body)", 2, 40, 2, "یک دست را جلوی سینه بکش و با دست دیگر آرام فشار بده.", "cue-exercise-02", "", "", 2),
        WellnessMove("ex-wrist-ankle", WellnessCategory.EXERCISE, "چرخش مچ دست و پا", 1, 40, 10, "مچ‌ها را در هر جهت آرام بچرخان.", "cue-exercise-03", "", "", 3),
        WellnessMove("ex-squat", WellnessCategory.EXERCISE, "اسکوات آرام (Bodyweight Squat)", 4, 0, 12, "پاها به عرض شانه، باسن را عقب و پایین ببر مثل نشستن روی صندلی.", "cue-exercise-04", "", "", 4),
        WellnessMove("ex-lunge", WellnessCategory.EXERCISE, "لانگز (Lunges)", 5, 0, 10, "یک قدم بلند جلو بگذار و زانوی عقب را به سمت زمین پایین ببر.", "cue-exercise-05", "", "", 5),
        WellnessMove("ex-plank", WellnessCategory.EXERCISE, "پلانک (Plank)", 5, 30, 0, "روی ساعد و پنجه، بدن یک خط راست، شکم را سفت نگه دار.", "cue-exercise-06", "", "", 6),
        WellnessMove("ex-crunch", WellnessCategory.EXERCISE, "کرانچ شکم ملایم", 4, 0, 12, "به پشت، زانوها خم، شانه‌ها را کمی از زمین بلند کن.", "cue-exercise-07", "", "", 7),
        WellnessMove("ex-hamstring", WellnessCategory.EXERCISE, "کشش همسترینگ نشسته", 3, 45, 0, "نشسته پاها کشیده، آرام رو به جلو خم شو و پنجه‌ها را بگیر.", "cue-exercise-08", "", "", 8),
        WellnessMove("ex-butterfly", WellnessCategory.EXERCISE, "حرکت پروانه (Butterfly Stretch)", 3, 45, 0, "نشسته، کف پاها به هم، زانوها را آرام پایین ببر.", "cue-exercise-09", "", "", 9),
        WellnessMove("ex-calf", WellnessCategory.EXERCISE, "بالا-پایین پاشنه (Calf Raises)", 3, 0, 15, "ایستاده روی پنجه بلند شو و آرام پایین بیا.", "cue-exercise-10", "", "", 10),
        WellnessMove("ex-trunk", WellnessCategory.EXERCISE, "چرخش تنه‌ی ایستاده (Standing Trunk Rotation)", 2, 40, 10, "ایستاده، دست‌ها جلو، بالاتنه را آرام چپ و راست بچرخان.", "cue-exercise-11", "", "", 11),
        WellnessMove("ex-wall-calf", WellnessCategory.EXERCISE, "کشش ساق پا رو به دیوار", 3, 40, 0, "دست‌ها روی دیوار، یک پا عقب و کشیده، پاشنه روی زمین.", "cue-exercise-12", "", "", 12),
        WellnessMove("ex-superman", WellnessCategory.EXERCISE, "حرکت سوپرمن برای کمر", 4, 30, 10, "روی شکم، دست‌ها و پاها را هم‌زمان کمی از زمین بلند کن.", "cue-exercise-13", "", "", 13),
        WellnessMove("ex-jumping-jack", WellnessCategory.EXERCISE, "جامپینگ جک ملایم", 4, 45, 0, "با پرش آرام دست‌ها و پاها را باز و بسته کن (گرم‌کردن).", "cue-exercise-14", "", "", 14),
        WellnessMove("ex-forearm", WellnessCategory.EXERCISE, "کشش مچ و ساعد", 2, 40, 0, "دست را جلو بکش و با دست دیگر انگشتان را آرام به عقب فشار بده.", "cue-exercise-15", "", "", 15),
        WellnessMove("br-diaphragm", WellnessCategory.BREATHING, "تنفس شکمی (Diaphragmatic)", 2, 120, 0, "دست روی شکم، از بینی نفس بکش تا شکم بالا بیاید، آرام بازدم کن.", "cue-breathing-01", "", "", 1),
        WellnessMove("br-478", WellnessCategory.BREATHING, "تنفس ۴-۷-۸ (Relaxing Breath)", 3, 120, 0, "۴ ثانیه دم، ۷ ثانیه نگه‌دار، ۸ ثانیه بازدم.", "cue-breathing-02", "", "", 2),
        WellnessMove("br-box", WellnessCategory.BREATHING, "تنفس جعبه‌ای (Box Breathing 4-4-4-4)", 3, 120, 0, "دم ۴، نگه ۴، بازدم ۴، نگه ۴؛ تکرار.", "cue-breathing-03", "", "", 3),
        WellnessMove("br-nadi", WellnessCategory.BREATHING, "تنفس بینی متناوب (نسخه‌ی ساده)", 4, 120, 0, "یک سوراخ بینی را ببند، از دیگری نفس بکش؛ جابه‌جا کن.", "cue-breathing-04", "", "", 4),
        WellnessMove("br-sigh", WellnessCategory.BREATHING, "تنفس آه‌کشیدن آرام‌بخش (Sighing Breath)", 2, 90, 0, "دم عمیق از بینی، بازدم بلند از دهان همراه با صدای آه.", "cue-breathing-05", "", "", 5),
        WellnessMove("br-lion", WellnessCategory.BREATHING, "تنفس شیر (Lion's Breath)", 3, 60, 0, "دم عمیق، بازدم قوی از دهان با بیرون‌آوردن زبان (رهاسازی تنش).", "cue-breathing-06", "", "", 6),
        WellnessMove("br-count-exam", WellnessCategory.BREATHING, "تنفس شمارشی قبل از امتحان", 2, 90, 0, "آرام تا ۵ دم بکش و تا ۵ بازدم کن تا تمرکز برگردد.", "cue-breathing-07", "", "", 7),
        WellnessMove("br-sleep", WellnessCategory.BREATHING, "تنفس آرام‌سازی قبل از خواب", 2, 120, 0, "بازدم‌ها را بلندتر از دم‌ها کن تا بدن آماده‌ی خواب شود.", "cue-breathing-08", "", "", 8),
        WellnessMove("lt-pomodoro", WellnessCategory.LEARNING, "تکنیک پومودورو (۲۵/۵)", 3, 1500, 0, "۲۵ دقیقه تمرکز روی یک کار، بعد ۵ دقیقه استراحت.", "cue-learning-01", "", "", 1),
        WellnessMove("lt-spaced", WellnessCategory.LEARNING, "تکرار فاصله‌دار (Spaced Repetition)", 4, 300, 0, "فلش‌کارت‌ها را با فاصله‌های بیشتر مرور کن، نه یک‌جا.", "cue-learning-02", "", "", 2),
        WellnessMove("lt-feynman", WellnessCategory.LEARNING, "روش فاینمن", 5, 300, 0, "درس را با زبان ساده برای یک نفر توضیح بده تا جای ابهام پیدا شود.", "cue-learning-03", "", "", 3),
        WellnessMove("lt-mindmap", WellnessCategory.LEARNING, "نقشه‌ی ذهنی (Mind Map)", 4, 300, 0, "موضوع اصلی وسط، شاخه‌ها را دور آن بکش تا فصل جمع‌بندی شود.", "cue-learning-04", "", "", 4),
        WellnessMove("lt-active-recall", WellnessCategory.LEARNING, "خودآزمایی (Active Recall)", 4, 300, 0, "قبل از خواندن دوباره، از حافظه جواب بده و بعد چک کن.", "cue-learning-05", "", "", 5),
    )

    fun byId(id: String): WellnessMove? = moves.firstOrNull { it.id == id }
}
