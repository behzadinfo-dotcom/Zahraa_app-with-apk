package ir.behzad.roozhayeman.ui.content

/** دستور آشپزی ساده و قابل‌انجام برای نوجوان. */
data class Recipe(
    val id: String,
    val title: String,
    val minutes: Int,
    val servings: Int,
    val difficulty: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val tip: String,
)

/** یک درس کوتاه مهارتی یا درسی. */
data class Lesson(
    val id: String,
    val title: String,
    val subject: String,
    val grade: Int,
    val body: String,
)

/** سؤال چهارگزینه‌ای. */
data class QuizQuestion(
    val id: String,
    val lessonId: String,
    val question: String,
    val choices: List<String>,
    val answerIndex: Int,
)

/** گره‌ی نقشه‌ی راه آموزش آزاد (با پیش‌نیاز). */
data class LearningNode(
    val id: String,
    val title: String,
    val track: String,
    val orderIndex: Int,
    val prerequisiteId: String,
) {
    val isUnlocked: Boolean get() = prerequisiteId.isBlank()
}

/**
 * ماژول «پیش‌نیاز و جمع‌بندی» ابتدای هر درس (فایل توسعه ۰۶).
 * قبل از محتوای اصلی درس نمایش داده می‌شود تا نکات پایه‌ی سال‌های قبل یادآوری شود.
 */
data class LessonPrerequisite(
    val id: String,
    val lessonId: String,
    val type: String, // formula | grammar | vocabulary | concept
    val titleFa: String,
    val contentFa: String,
    val flashcardSetId: String,
    val orderIndex: Int,
) {
    /** برچسب فارسی نوع پیش‌نیاز برای نمایش. */
    val typeLabelFa: String
        get() = when (type) {
            "formula" -> "فرمول"
            "grammar" -> "گرامر"
            "vocabulary" -> "واژگان"
            "concept" -> "مفهوم پایه"
            else -> "یادآوری"
        }
}

/**
 * آزمون بازه‌ای یک کتاب/بازه‌ی درسی (فایل توسعه ۰۷).
 * سؤال‌ها با `questionIds` به بانک سؤال (`quizzes`) اشاره می‌کنند.
 */
data class Exam(
    val id: String,
    val bookCode: String,
    val rangeGroup: String,
    val titleFa: String,
    val questionIds: List<String>,
    val dueAtIso: String,
)

/** ایده‌ی روز برای سیاه‌قلم/اسکیس. */
data class ArtPrompt(
    val id: String,
    val title: String,
    val prompt: String,
    val moodTag: String,
)
