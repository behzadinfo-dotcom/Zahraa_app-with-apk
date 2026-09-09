package ir.behzad.roozhayeman.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.appwrite.ReviewTask
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.navigation.Screen
import kotlinx.coroutines.launch

/**
 * برنامه‌ی مرور هفتگی (فایل توسعه ۰۷ بخش ۳).
 *
 * منطق تولید در تابع سرور `weekly-plan-engine` است (برنامه‌ی کلاسی + شیفت چرخشی).
 * اینجا فقط برنامه را می‌گیریم و تسک‌ها را با لینک مستقیم به منبع نشان می‌دهیم.
 */
@Composable
fun WeeklyPlanScreen(nav: NavController) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<ReviewTask>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    fun refresh() {
        if (busy) return
        busy = true; note = null
        scope.launch {
            when (val r = container.serverActions.weeklyPlan()) {
                is AppResult.Ok -> { tasks = r.value; if (r.value.isEmpty()) note = "امروز تسک مروری نداری — روز آزاد!" }
                is AppResult.Err -> note = r.error.userMessage
            }
            busy = false; loaded = true
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("برنامه‌ی مرور هفتگی") { nav.popBackStack() }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "برنامه بر اساس روزهای کلاسِ هر درس و شیفت چرخشی چیده می‌شود: برای درسی که فردا کلاس داری، امشب یک مرور کوتاه؛ و برای نقاط‌ضعف آزمون‌های اخیر یک مرور ویژه.",
                style = MaterialTheme.typography.bodySmall,
            )
            PrimaryButton(if (busy) "در حال ساختن برنامه…" else "به‌روزرسانی برنامه‌ی امروز") { refresh() }
            note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }

            if (!loaded && !busy) {
                Text("برای دیدن برنامه‌ی امروز، دکمه‌ی بالا را بزن.", style = MaterialTheme.typography.bodySmall)
            }

            tasks.forEach { task ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${taskTypeLabel(task.type)} · ${task.bookCode}", style = MaterialTheme.typography.titleSmall)
                        if (task.isDone) {
                            Text("انجام‌شده ✅", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val (label, dest) = taskDestination(task)
                        if (dest != null) PrimaryButton(label) { nav.navigate(dest) }
                    }
                }
            }
        }
    }
}

private fun taskTypeLabel(type: String): String = when (type) {
    "flashcard" -> "مرور فلش‌کارت"
    "video" -> "تماشای ویدیو"
    "exam" -> "مرور نقطه‌ضعف / آزمون"
    "prerequisite" -> "مرور پیش‌نیاز"
    else -> "مرور"
}

/** لینک مستقیم هر تسک به منبع مربوط (نه صفحه‌ی عمومی). */
private fun taskDestination(task: ReviewTask): Pair<String, String?> = when (task.type) {
    "flashcard" -> "شروع مرور" to (if (task.refId.isNotBlank()) Screen.Lesson.of(task.refId) else Screen.Quiz.of())
    "prerequisite" -> "مرور پیش‌نیاز" to (if (task.refId.isNotBlank()) Screen.Lesson.of(task.refId) else null)
    "exam" -> "برو به آزمون" to (if (task.refId.isNotBlank()) Screen.ExamRun.of(task.refId) else Screen.ExamCenter.route)
    "video" -> "درس" to (if (task.refId.isNotBlank()) Screen.Lesson.of(task.refId) else null)
    else -> "باز کن" to (if (task.refId.isNotBlank()) Screen.Lesson.of(task.refId) else null)
}
