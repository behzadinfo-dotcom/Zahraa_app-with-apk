package ir.behzad.roozhayeman.ui.content

import ir.behzad.platform.core.appwrite.AppwriteAuthService
import ir.behzad.platform.core.appwrite.TableRow
import ir.behzad.platform.core.appwrite.TablesDbService
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.common.JalaliDate
import ir.behzad.platform.core.common.LocalStore
import ir.behzad.platform.core.common.TableIds
import org.json.JSONArray
import org.json.JSONObject

/** پاسخ کاربر به یک سؤال آزمون. */
data class ExamAnswer(
    val questionId: String,
    val givenAnswer: String,
    val isCorrect: Boolean,
)

/**
 * نتیجه‌ی یک آزمون بازه‌ای (فایل توسعه ۰۷).
 *
 * روی سرور در کالکشن `exam_results` (rowSecurity، فقط مالِ خودِ کاربر) ذخیره می‌شود؛
 * این «پیشرفت آموزشی» است نه داده‌ی خصوصیِ never-sync. `weakTopics` مبنای صفحه‌ی
 * «نکات ضعف» و برنامه‌ی مرور هفتگی است.
 */
data class ExamResult(
    val examId: String,
    val userId: String,
    val bookCode: String,
    val answers: List<ExamAnswer>,
    val score: Double,
    val weakTopics: List<String>,
    val takenAtIso: String,
)

/**
 * ذخیره/خواندن نتیجه‌ی آزمون‌ها. کش محلی همیشه نوشته می‌شود تا صفحه‌ی نکات ضعف و
 * برنامه‌ی مرور آفلاین هم کار کنند.
 */
class ExamRepository(
    private val tables: TablesDbService,
    private val store: LocalStore,
) {
    private fun userId(): String = store.getString(AppwriteAuthService.KEY_USER_ID)

    private fun rowId(uid: String, examId: String): String = "er-${uid}-${examId}".take(36)

    /** ثبت نتیجه‌ی آزمون: کش محلی + (در صورت اتصال) سرور. */
    suspend fun save(result: ExamResult): ExamResult {
        val uid = result.userId.ifBlank { userId() }
        val filled = result.copy(userId = uid)
        store.putString(cacheKey(filled.examId), filled.toJson().toString())
        if (tables.isConfigured && uid.isNotBlank()) {
            tables.upsert(
                table = TableIds.EXAM_RESULTS,
                id = rowId(uid, filled.examId),
                data = filled.toData(),
                permissions = emptyList(),
            )
        }
        return filled
    }

    /** آخرین نتیجه‌ی یک آزمون (برای نمایش «قبلاً دادی»). */
    suspend fun latest(examId: String): ExamResult? {
        val uid = userId()
        if (tables.isConfigured && uid.isNotBlank()) {
            val r = tables.get(TableIds.EXAM_RESULTS, rowId(uid, examId))
            if (r is AppResult.Ok) {
                val res = r.value.toResult()
                store.putString(cacheKey(examId), res.toJson().toString())
                return res
            }
        }
        return runCatching {
            val raw = store.getString(cacheKey(examId))
            if (raw.isBlank()) null else resultFromJson(JSONObject(raw))
        }.getOrNull()
    }

    /** همه‌ی نقاط‌ضعف اخیر (از کش محلی) برای صفحه‌ی نکات ضعف و برنامه‌ی مرور. */
    fun recentWeakTopics(): List<String> =
        store.keysWithPrefix(CACHE_PREFIX).flatMap { key ->
            runCatching { resultFromJson(JSONObject(store.getString(key)))?.weakTopics ?: emptyList() }
                .getOrDefault(emptyList())
        }.distinct()

    private fun cacheKey(examId: String): String = "$CACHE_PREFIX$examId"

    companion object {
        private const val CACHE_PREFIX = "exam_result_"
    }
}

// --- نگاشت سرور ↔ مدل ---

private fun ExamResult.toData(): Map<String, Any?> = mapOf(
    "examId" to examId,
    "userId" to userId,
    "bookCode" to bookCode,
    "answers" to answersToJson(answers).toString(),
    "score" to score,
    "weakTopics" to JSONArray(weakTopics).toString(),
    "takenAtIso" to takenAtIso,
)

private fun ExamResult.toJson(): JSONObject = JSONObject()
    .put("examId", examId).put("userId", userId).put("bookCode", bookCode)
    .put("answers", answersToJson(answers)).put("score", score)
    .put("weakTopics", JSONArray(weakTopics)).put("takenAtIso", takenAtIso)

private fun answersToJson(answers: List<ExamAnswer>): JSONArray {
    val a = JSONArray()
    answers.forEach {
        a.put(JSONObject().put("questionId", it.questionId).put("givenAnswer", it.givenAnswer).put("isCorrect", it.isCorrect))
    }
    return a
}

private fun parseAnswers(raw: String): List<ExamAnswer> = runCatching {
    val a = JSONArray(raw)
    buildList {
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            add(ExamAnswer(o.optString("questionId"), o.optString("givenAnswer"), o.optBoolean("isCorrect")))
        }
    }
}.getOrDefault(emptyList())

private fun parseStrings(raw: String): List<String> = runCatching {
    val a = JSONArray(raw)
    buildList { for (i in 0 until a.length()) add(a.optString(i)) }
}.getOrDefault(emptyList())

private fun TableRow.toResult(): ExamResult = ExamResult(
    examId = string("examId"),
    userId = string("userId"),
    bookCode = string("bookCode"),
    answers = parseAnswers(string("answers")),
    score = when (val v = data["score"]) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: 0.0; else -> 0.0 },
    weakTopics = parseStrings(string("weakTopics")),
    takenAtIso = string("takenAtIso"),
)

private fun resultFromJson(o: JSONObject): ExamResult? = runCatching {
    ExamResult(
        examId = o.getString("examId"),
        userId = o.optString("userId"),
        bookCode = o.optString("bookCode"),
        answers = o.optJSONArray("answers")?.let { a ->
            buildList {
                for (i in 0 until a.length()) {
                    val x = a.optJSONObject(i) ?: continue
                    add(ExamAnswer(x.optString("questionId"), x.optString("givenAnswer"), x.optBoolean("isCorrect")))
                }
            }
        } ?: emptyList(),
        score = o.optDouble("score", 0.0),
        weakTopics = o.optJSONArray("weakTopics")?.let { a -> buildList { for (i in 0 until a.length()) add(a.optString(i)) } } ?: emptyList(),
        takenAtIso = o.optString("takenAtIso"),
    )
}.getOrNull()
