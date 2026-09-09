package ir.behzad.roozhayeman.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.platform.core.common.JalaliDate
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import ir.behzad.platform.core.designsystem.SectionCard
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.content.Exam
import ir.behzad.roozhayeman.ui.content.ExamAnswer
import ir.behzad.roozhayeman.ui.content.ExamRepository
import ir.behzad.roozhayeman.ui.content.ExamResult
import ir.behzad.roozhayeman.ui.content.QuizQuestion
import ir.behzad.roozhayeman.ui.navigation.Screen
import kotlinx.coroutines.launch

/**
 * مرکز آزمون بازه‌ای (فایل توسعه ۰۷ بخش ۱): فهرست آزمون‌های در دسترس + دکمه‌ی برنامه‌ی مرور هفتگی.
 */
@Composable
fun ExamCenterScreen(nav: NavController) {
    val container = LocalAppContainer.current
    var exams by remember { mutableStateOf<List<Exam>>(emptyList()) }

    LaunchedEffect(Unit) { exams = container.catalog.exams() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آزمون بازه‌ای") { nav.popBackStack() }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "در پایان هر بازه‌ی درسی یک آزمون کوتاه هست؛ بعد از آزمون فقط «نکات ضعف» را برایت جمع می‌کنم، نه کل درس را.",
                style = MaterialTheme.typography.bodySmall,
            )
            SectionCard("برنامه‌ی مرور هفتگی", "بر اساس برنامه‌ی کلاسی و شیفت چرخشی.") {
                nav.navigate(Screen.WeeklyPlan.route)
            }
            Spacer(Modifier.height(4.dp))
            if (exams.isEmpty()) {
                Text("آزمون‌ها در حال آماده‌سازی‌اند…", style = MaterialTheme.typography.bodySmall)
            }
            exams.forEach { exam ->
                SectionCard(exam.titleFa, "کتاب ${exam.bookCode} · ${exam.questionIds.size} سؤال") {
                    nav.navigate(Screen.ExamRun.of(exam.id))
                }
            }
        }
    }
}

/**
 * اجرای یک آزمون بازه‌ای با اصلاح فوری چندگزینه‌ای و صفحه‌ی نکات ضعف (فایل توسعه ۰۷ بخش ۲).
 */
@Composable
fun ExamRunScreen(examId: String, onBack: () -> Unit, onReview: (String) -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val repo = remember { ExamRepository(container.tables, container.store) }

    var exam by remember { mutableStateOf<Exam?>(null) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    val answers = remember { mutableListOf<ExamAnswer>() }
    var finished by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ExamResult?>(null) }

    LaunchedEffect(examId) {
        val e = container.catalog.exam(examId)
        exam = e
        questions = if (e != null) container.catalog.questionsForExam(e) else emptyList()
    }

    val current = exam
    Column(Modifier.fillMaxSize()) {
        AppTopBar(current?.titleFa ?: "آزمون", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                current == null || questions.isEmpty() -> Text("آزمون در حال آماده‌سازی است…")

                finished -> {
                    val r = result
                    val correct = answers.count { it.isCorrect }
                    Text("تمام شد: $correct از ${questions.size} درست", style = MaterialTheme.typography.titleMedium)
                    if (r != null && r.weakTopics.isNotEmpty()) {
                        Text(
                            "نکات ضعف: چند موضوع را غلط زدی. بیا فقط همان‌ها را مرور کنیم — نه کل درس دوباره.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        PrimaryButton("دیدن نکات ضعف") { onReview(examId) }
                    } else {
                        Text("همه را درست زدی — عالی! چیزی برای مرور ویژه نمانده.", style = MaterialTheme.typography.bodyMedium)
                    }
                    PrimaryButton("بازگشت", onBack)
                }

                else -> {
                    val q = questions[index.coerceAtMost(questions.lastIndex)]
                    LinearProgressIndicator(progress = { (index + 1f) / questions.size })
                    Spacer(Modifier.height(6.dp))
                    Text("سؤال ${index + 1} از ${questions.size}", style = MaterialTheme.typography.bodySmall)
                    Text(q.question, style = MaterialTheme.typography.titleMedium)
                    q.choices.forEachIndexed { i, choice ->
                        val chosen = picked
                        val label = when {
                            chosen == null -> choice
                            i == q.answerIndex -> "$choice ✅"
                            i == chosen -> "$choice ❌"
                            else -> choice
                        }
                        PrimaryButton(label) {
                            if (chosen == null) {
                                picked = i
                                answers.add(ExamAnswer(q.id, q.choices.getOrElse(i) { "" }, i == q.answerIndex))
                            }
                        }
                    }
                    if (picked != null) {
                        Text(
                            if (picked == q.answerIndex) "درست بود ✅" else "اشکالی ندارد — این موضوع می‌رود توی نکات ضعف.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        PrimaryButton(if (index < questions.lastIndex) "سؤال بعدی" else "پایان آزمون") {
                            picked = null
                            if (index < questions.lastIndex) {
                                index++
                            } else {
                                val correct = answers.count { it.isCorrect }
                                val score = if (questions.isEmpty()) 0.0 else correct.toDouble() / questions.size
                                // نکات ضعف = درسِ سؤال‌هایی که غلط پاسخ داده شده‌اند.
                                val wrongQ = questions.filter { qq -> answers.any { it.questionId == qq.id && !it.isCorrect } }
                                val weak = wrongQ.map { it.lessonId }.filter { it.isNotBlank() }.distinct()
                                val res = ExamResult(
                                    examId = examId,
                                    userId = "",
                                    bookCode = current.bookCode,
                                    answers = answers.toList(),
                                    score = score,
                                    weakTopics = weak,
                                    takenAtIso = JalaliDate.todayIso(),
                                )
                                scope.launch { result = repo.save(res) }
                                result = res
                                finished = true
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * صفحه‌ی «نکات ضعف» (فایل توسعه ۰۷ بخش ۲): فقط weakTopics با لینک مستقیم به همان درس/نکته.
 */
@Composable
fun WeakTopicsScreen(examId: String, nav: NavController) {
    val container = LocalAppContainer.current
    val repo = remember { ExamRepository(container.tables, container.store) }
    var result by remember { mutableStateOf<ExamResult?>(null) }
    var titles by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(examId) {
        result = repo.latest(examId)
        titles = container.catalog.lessons().associate { it.id to it.title }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("نکات ضعف") { nav.popBackStack() }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val weak = result?.weakTopics ?: emptyList()
            if (weak.isEmpty()) {
                Text("نکته‌ی ضعفی ثبت نشده. اگر تازه آزمون دادی، دوباره امتحان کن.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("فقط همین‌ها را مرور کن — مستقیم برو سراغ درس مرتبط:", style = MaterialTheme.typography.bodyMedium)
                weak.forEach { lessonId ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(titles[lessonId] ?: lessonId, style = MaterialTheme.typography.titleSmall)
                            PrimaryButton("مرور این درس") { nav.navigate(Screen.Lesson.of(lessonId)) }
                        }
                    }
                }
            }
        }
    }
}
