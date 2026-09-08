package ir.behzad.roozhayeman

import android.app.Application
import ir.behzad.platform.core.notifications.NotificationChannels
import ir.behzad.platform.core.notifications.Reminder
import ir.behzad.roozhayeman.di.AppContainer
import ir.behzad.roozhayeman.ui.ailearning.AI_LESSON_REMINDER_ID

class RoozhayeManApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensure(this)
        seedDefaultReminders()
    }

    /**
     * سه یادآور پیش‌فرض ملایم. فقط یک‌بار (اولین اجرا) ساخته می‌شوند و کاربر
     * می‌تواند خاموششان کند؛ یادآور اجباری، ابزار مراقبتی نیست بلکه آزار است.
     */
    private fun seedDefaultReminders() {
        val scheduler = container.reminders
        val existing = scheduler.all()
        if (existing.isEmpty()) {
            listOf(
                Reminder("water-morning", "یک لیوان آب", "صبح‌ها با یک لیوان آب شروع کن 🙂", 9, 30),
                Reminder("study-review", "مرور درس امروز", "ده دقیقه مرور، فردا خیلی راحت‌تر می‌شود.", 18, 0),
                Reminder("calm-evening", "آرام‌سازی شبانه", "چند نفس عمیق و یک کشش کوتاه پیش از خواب.", 21, 30),
            ).forEach { scheduler.upsert(it) }
        }
        // یادآور روزانه‌ی ماژول هوش مصنوعی — با شناسه‌ی ثابت، پس فقط یک‌بار ساخته می‌شود
        // و از داخل خود ماژول قابل خاموش‌کردن است (ساعات سکوت هم رعایت می‌شود).
        if (scheduler.find(AI_LESSON_REMINDER_ID) == null) {
            scheduler.upsert(
                Reminder(
                    id = AI_LESSON_REMINDER_ID,
                    title = "درس امروز هوش مصنوعی",
                    body = "ده دقیقه یادگیری AI: یک درس کوتاه + یک آزمون کوچولو.",
                    hour = 17,
                    minute = 0,
                ),
            )
        }
    }
}
