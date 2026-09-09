package ir.behzad.roozhayeman.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Study : Screen("study")
    data object Chat : Screen("chat")
    data object Heart : Screen("heart")
    data object More : Screen("more")
    data object Cycle : Screen("cycle")
    data object Mood : Screen("mood")
    data object Mindfulness : Screen("mindfulness")
    data object ScreenTime : Screen("screentime")
    data object Focus : Screen("focus")
    data object Calm : Screen("calm")
    data object Journal : Screen("journal")
    data object Breath : Screen("breath")
    data object Routine : Screen("routine")
    data object SafeSpace : Screen("safespace")
    data object Album : Screen("album")
    data object Writing : Screen("writing")
    data object Helplines : Screen("helplines")
    data object Library : Screen("library")
    data object Audiobook : Screen("audiobook")
    data object School : Screen("school")
    /** آزمون. `lessonId` اختیاری است تا از صفحه‌ی درس فقط سؤال‌های همان درس بیاید. */
    data object Quiz : Screen("quiz?lessonId={lessonId}") {
        fun of(lessonId: String? = null) =
            if (lessonId.isNullOrBlank()) "quiz" else "quiz?lessonId=${Uri.encode(lessonId)}"
    }
    data object QuizReview : Screen("quizreview")
    data object Pdf : Screen("pdf")
    data object Charts : Screen("charts")
    data object Art : Screen("art")
    data object Gallery : Screen("gallery")
    data object Learning : Screen("learning")
    data object Lesson : Screen("lesson/{id}") {
        fun of(id: String) = "lesson/${Uri.encode(id)}"
    }
    data object Placement : Screen("placement")

    /**
     * پلیر رسانه‌ی درس (فایل توسعه ۰۱): ویدیو یا صوت با حافظه و سینک.
     * پارامترها: شناسه‌ی درس، نوع (video/audio)، آدرس فایل و عنوان.
     */
    data object LessonMedia : Screen("lessonmedia?lessonId={lessonId}&type={type}&uri={uri}&title={title}&book={book}") {
        fun of(lessonId: String, type: String, uri: String, title: String, book: String = "") =
            "lessonmedia?lessonId=${Uri.encode(lessonId)}&type=${Uri.encode(type)}&uri=${Uri.encode(uri)}&title=${Uri.encode(title)}&book=${Uri.encode(book)}"
    }

    /** ماژول ورزش/یوگا/تنفس کامل + مرجع نقاشی (فایل توسعه ۰۲). */
    data object Wellness : Screen("wellness")
    data object WellnessDetail : Screen("wellness/{id}") { fun of(id: String) = "wellness/${Uri.encode(id)}" }
    data object SketchReference : Screen("sketchref")

    /** آزمون بازه‌ای و نکات ضعف و برنامه‌ی مرور هفتگی (فایل توسعه ۰۷). */
    data object ExamCenter : Screen("examcenter")
    data object ExamRun : Screen("exam/{id}") { fun of(id: String) = "exam/${Uri.encode(id)}" }
    data object WeakTopics : Screen("weaktopics/{id}") { fun of(id: String) = "weaktopics/${Uri.encode(id)}" }
    data object WeeklyPlan : Screen("weeklyplan")

    /** تنظیمات چند-مدلی هوش مصنوعی (فایل توسعه ۰۴). */
    data object AiProviders : Screen("aiproviders")
    /** نقشه‌ی راه. `track` اختیاری است تا ماژول هوش مصنوعی فقط گره‌های خودش را ببیند. */
    data object Roadmap : Screen("roadmap?track={track}") {
        fun of(track: String? = null) = if (track.isNullOrBlank()) "roadmap" else "roadmap?track=${Uri.encode(track)}"
    }
    data object AiLearning : Screen("ailearning")
    data object AiAssessment : Screen("aiassessment")
    data object Recipes : Screen("recipes")
    data object RecipeDetail : Screen("recipedetail/{id}") {
        fun of(id: String) = "recipedetail/${Uri.encode(id)}"
    }
    data object Exercise : Screen("exercise")
    data object ExerciseDetail : Screen("exercise/{id}") { fun of(id: String) = "exercise/${Uri.encode(id)}" }
    data object Water : Screen("water")
    data object Pairing : Screen("pairing")
    data object Call : Screen("call")
    data object Settings : Screen("settings")
    data object Privacy : Screen("privacy")
    data object ChatSettings : Screen("chatsettings")
    data object Badges : Screen("badges")
    data object Lock : Screen("lock")
    data object Reminders : Screen("reminders")
    data object Sync : Screen("sync")
}

data class Tab(val route: String, val icon: ImageVector, val label: String)
val Tabs = listOf(
    Tab(Screen.Home.route, Icons.Filled.Home, "خانه"),
    Tab(Screen.Study.route, Icons.Filled.MenuBook, "درس"),
    Tab(Screen.Chat.route, Icons.Filled.SmartToy, "همراه"),
    Tab(Screen.Heart.route, Icons.Filled.Favorite, "بابا"),
    Tab(Screen.More.route, Icons.Filled.MoreHoriz, "بیشتر"),
)
val TopRoutes = Tabs.map { it.route }.toSet()
