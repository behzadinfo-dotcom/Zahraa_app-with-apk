package ir.behzad.roozhayeman.ui.content

import ir.behzad.platform.core.common.JalaliDate
import ir.behzad.platform.core.common.LocalStore
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * «خوانده‌شدن» یک درس — ساده و قابل اتکا: کلید `lesson_read_<id>` با تاریخ شمسی/میلادی ISO.
 *
 * چرا روی دستگاه؟ چون پیشرفت آموزشی داده‌ی خصوصی زهراست و فقط خلاصه‌ی هفتگی
 * (با opt-in) از آن بیرون می‌رود. نمودارهای پیشرفت هم از همین کلیدها ساخته می‌شوند.
 */
const val LESSON_READ_PREFIX = "lesson_read_"

fun markLessonRead(store: LocalStore, lessonId: String) {
    if (lessonId.isBlank()) return
    store.putString(LESSON_READ_PREFIX + lessonId, JalaliDate.todayIso())
}

/** شناسه‌ی همه‌ی درس‌هایی که خوانده شده‌اند. */
fun readLessonIds(store: LocalStore): Set<String> =
    store.keysWithPrefix(LESSON_READ_PREFIX).map { it.removePrefix(LESSON_READ_PREFIX) }.toSet()

/** تاریخ خواندن یک درس (برای «امروز خواندی» و استریک). */
fun lessonReadDate(store: LocalStore, lessonId: String): String =
    store.getString(LESSON_READ_PREFIX + lessonId)

/**
 * وضعیت «دیده‌شدن» کارت پیش‌نیاز هر درس (فایل توسعه ۰۶).
 * برای گزارش پیشرفت والدین/معلم ذخیره می‌شود؛ مثل بقیه‌ی پیشرفت، فقط روی دستگاه.
 */
const val PREREQ_SEEN_PREFIX = "lesson_prereq_seen_"

fun markPrerequisiteSeen(store: LocalStore, lessonId: String) {
    if (lessonId.isBlank()) return
    store.putString(PREREQ_SEEN_PREFIX + lessonId, JalaliDate.todayIso())
}

fun prerequisiteSeenDate(store: LocalStore, lessonId: String): String =
    store.getString(PREREQ_SEEN_PREFIX + lessonId)

/** چند روز پشت‌سرهم درس خوانده شده (استریک مطالعه؛ مثل استریک نقاشی). */
fun lessonStreak(store: LocalStore, today: LocalDate = LocalDate.now()): Int {
    val days = store.keysWithPrefix(LESSON_READ_PREFIX)
        .mapNotNull { key -> runCatching { LocalDate.parse(store.getString(key)) }.getOrNull() }
        .filter { !it.isAfter(today) }
        .distinct()
        .sortedDescending()
    if (days.isEmpty()) return 0
    if (ChronoUnit.DAYS.between(days.first(), today) > 1) return 0
    var streak = 1
    for (i in 1 until days.size) {
        if (ChronoUnit.DAYS.between(days[i], days[i - 1]) == 1L) streak++ else break
    }
    return streak
}
