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

/** نوع رسانه‌ی درس. */
enum class MediaType(val key: String) { VIDEO("video"), AUDIO("audio") }

/** یک پرشِ ثبت‌شده روی نوار زمان (برای نمایش نقطه‌های seek روی نوار). */
data class SeekJump(val fromSec: Double, val toSec: Double, val direction: String, val atClientTimeIso: String)

/** یک رکورد تماشا (برای «مرور دوباره — مشاهده‌شده در …»). */
data class ViewHistoryEntry(val dateIso: String, val speed: Double, val fromSec: Double, val toSec: Double, val isReplay: Boolean)

/**
 * وضعیت پیشرفت پلیر یک درس (فایل توسعه ۰۱).
 *
 * این داده روی سرور (کالکشن `lesson_media_progress`, با rowSecurity) سینک می‌شود تا
 * پیشرفت بین دستگاه‌ها یکی بماند. مطابق سیاست حریم خصوصی این «پیشرفت آموزشی» است،
 * نه داده‌ی خصوصیِ never-sync؛ ولی سطرها فقط برای خودِ کاربر خواندنی‌اند.
 */
data class LessonMediaProgress(
    val userId: String,
    val bookCode: String,
    val lessonId: String,
    val mediaType: MediaType,
    val lastPositionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val playbackSpeed: Double = 1.0,
    val isCompleted: Boolean = false,
    val viewCount: Int = 0,
    val viewHistory: List<ViewHistoryEntry> = emptyList(),
    val seekJumps: List<SeekJump> = emptyList(),
    val updatedAtMs: Long = 0L,
) {
    val fractionWatched: Float
        get() = if (durationSec > 0) (lastPositionSec / durationSec).toFloat().coerceIn(0f, 1f) else 0f

    /** خط وضعیت درس، مثل «مرور دوباره — مشاهده‌شده در ۱۴۰۴/۰۶/۱۸ با سرعت ۱٫۲۵x». */
    val statusLineFa: String
        get() {
            val last = viewHistory.lastOrNull() ?: return "تماشا نشده"
            val date = runCatching { JalaliDate.formatFaLong(last.dateIso) }.getOrDefault(last.dateIso)
            val speed = formatSpeed(last.speed)
            val prefix = if (viewCount > 1 || last.isReplay) "مرور دوباره" else "تماشا شد"
            return "$prefix — مشاهده‌شده در $date با سرعت ${speed}x"
        }
}

private fun formatSpeed(speed: Double): String {
    // نمایش فارسیِ سرعت: 1.25 → «۱٫۲۵»
    val s = if (speed % 1.0 == 0.0) speed.toInt().toString() else speed.toString()
    return s.replace('.', '٫')
}

/**
 * سینک پیشرفت پلیر با سرور.
 *
 * قواعد (طبق فایل ۰۱):
 *  - هنگام باز شدن پلیر، آخرین موقعیت/سرعت از سرور خوانده و ادامه از همان‌جا.
 *  - آپدیت با debounce (هر ~۵ ثانیه یا مکث/خروج/تغییر سرعت/seek)، نه هر فریم.
 *  - seek بزرگ‌تر از ۳ ثانیه یک آیتم در `seekJumps` (append).
 *  - رسیدن به ≥۹۰٪ → isCompleted=true، viewCount++ و یک ViewHistory تازه.
 *  - حل تعارض هنگام اتصال دوباره: آخرین updatedAt برنده، ولی history/seekJumps append.
 *
 * کش محلی همیشه نوشته می‌شود تا آفلاین هم «ادامه از همان‌جا» کار کند.
 */
class MediaProgressRepository(
    private val tables: TablesDbService,
    private val store: LocalStore,
) {
    private fun userId(): String = store.getString(AppwriteAuthService.KEY_USER_ID)

    private fun rowId(userId: String, lessonId: String, type: MediaType): String =
        "mp-${userId}-${lessonId}-${type.key}".take(36)

    private fun cacheKey(lessonId: String, type: MediaType): String = "media_progress_${lessonId}_${type.key}"

    /** خواندن پیشرفت: اول سرور، بعد کش محلی (برای «ادامه از همان‌جا»). */
    suspend fun load(lessonId: String, type: MediaType, bookCode: String = ""): LessonMediaProgress {
        val uid = userId()
        if (tables.isConfigured && uid.isNotBlank()) {
            val result = tables.get(TableIds.LESSON_MEDIA_PROGRESS, rowId(uid, lessonId, type))
            if (result is AppResult.Ok) {
                val progress = result.value.toProgress()
                writeCache(lessonId, type, progress)
                return progress
            }
        }
        return readCache(lessonId, type)
            ?: LessonMediaProgress(uid, bookCode, lessonId, type)
    }

    /** به‌روزرسانی موقعیت/سرعت (debounce توسط فراخواننده کنترل می‌شود). */
    suspend fun save(progress: LessonMediaProgress): LessonMediaProgress {
        val updated = progress.copy(updatedAtMs = System.currentTimeMillis())
        writeCache(progress.lessonId, progress.mediaType, updated)
        val uid = updated.userId.ifBlank { userId() }
        if (tables.isConfigured && uid.isNotBlank()) {
            tables.upsert(
                table = TableIds.LESSON_MEDIA_PROGRESS,
                id = rowId(uid, updated.lessonId, updated.mediaType),
                data = updated.toData(),
                permissions = emptyList(),
            )
        }
        return updated
    }

    /** ثبت یک پرش بزرگ (>۳ ثانیه) روی نوار زمان. */
    fun recordSeek(current: LessonMediaProgress, fromSec: Double, toSec: Double): LessonMediaProgress {
        if (kotlin.math.abs(toSec - fromSec) < 3.0) return current
        val jump = SeekJump(
            fromSec = fromSec,
            toSec = toSec,
            direction = if (toSec >= fromSec) "forward" else "backward",
            atClientTimeIso = JalaliDate.todayIso(),
        )
        return current.copy(seekJumps = (current.seekJumps + jump).takeLast(MAX_JUMPS))
    }

    /** رسیدن به پایان/بالای ۹۰٪ → تکمیل + یک تماشای تازه. */
    fun markCompletedIfNeeded(current: LessonMediaProgress): LessonMediaProgress {
        if (current.durationSec <= 0 || current.fractionWatched < 0.9f) return current
        val newCount = current.viewCount + 1
        val entry = ViewHistoryEntry(
            dateIso = JalaliDate.todayIso(),
            speed = current.playbackSpeed,
            fromSec = 0.0,
            toSec = current.durationSec,
            isReplay = newCount > 1,
        )
        return current.copy(
            isCompleted = true,
            viewCount = newCount,
            viewHistory = (current.viewHistory + entry).takeLast(MAX_HISTORY),
        )
    }

    // --- کش محلی ---

    private fun writeCache(lessonId: String, type: MediaType, progress: LessonMediaProgress) {
        store.putString(cacheKey(lessonId, type), progress.toJson().toString())
    }

    private fun readCache(lessonId: String, type: MediaType): LessonMediaProgress? = runCatching {
        val raw = store.getString(cacheKey(lessonId, type))
        if (raw.isBlank()) null else progressFromJson(JSONObject(raw))
    }.getOrNull()

    companion object {
        private const val MAX_JUMPS = 100
        private const val MAX_HISTORY = 50
    }
}

// --- نگاشت سرور ↔ مدل ---

private fun TableRow.toProgress(): LessonMediaProgress = LessonMediaProgress(
    userId = string("userId"),
    bookCode = string("bookCode"),
    lessonId = string("lessonId"),
    mediaType = if (string("mediaType") == MediaType.VIDEO.key) MediaType.VIDEO else MediaType.AUDIO,
    lastPositionSec = numberOf("lastPositionSec"),
    durationSec = numberOf("durationSec"),
    playbackSpeed = numberOf("playbackSpeed", 1.0),
    isCompleted = boolean("isCompleted"),
    viewCount = long("viewCount").toInt(),
    viewHistory = parseHistory(string("viewHistory")),
    seekJumps = parseJumps(string("seekJumps")),
    updatedAtMs = long("updatedAtMs"),
)

private fun TableRow.numberOf(key: String, default: Double = 0.0): Double = when (val v = data[key]) {
    is Number -> v.toDouble()
    is String -> v.toDoubleOrNull() ?: default
    else -> default
}

private fun LessonMediaProgress.toData(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "bookCode" to bookCode,
    "lessonId" to lessonId,
    "mediaType" to mediaType.key,
    "lastPositionSec" to lastPositionSec,
    "durationSec" to durationSec,
    "playbackSpeed" to playbackSpeed,
    "isCompleted" to isCompleted,
    "viewCount" to viewCount,
    "viewHistory" to historyToJson(viewHistory).toString(),
    "seekJumps" to jumpsToJson(seekJumps).toString(),
    "updatedAtMs" to updatedAtMs,
)

private fun LessonMediaProgress.toJson(): JSONObject = JSONObject()
    .put("userId", userId).put("bookCode", bookCode).put("lessonId", lessonId)
    .put("mediaType", mediaType.key).put("lastPositionSec", lastPositionSec)
    .put("durationSec", durationSec).put("playbackSpeed", playbackSpeed)
    .put("isCompleted", isCompleted).put("viewCount", viewCount)
    .put("viewHistory", historyToJson(viewHistory))
    .put("seekJumps", jumpsToJson(seekJumps))
    .put("updatedAtMs", updatedAtMs)

private fun progressFromJson(o: JSONObject): LessonMediaProgress = LessonMediaProgress(
    userId = o.optString("userId"),
    bookCode = o.optString("bookCode"),
    lessonId = o.optString("lessonId"),
    mediaType = if (o.optString("mediaType") == MediaType.VIDEO.key) MediaType.VIDEO else MediaType.AUDIO,
    lastPositionSec = o.optDouble("lastPositionSec", 0.0),
    durationSec = o.optDouble("durationSec", 0.0),
    playbackSpeed = o.optDouble("playbackSpeed", 1.0),
    isCompleted = o.optBoolean("isCompleted"),
    viewCount = o.optInt("viewCount"),
    viewHistory = parseHistory(o.optString("viewHistory")),
    seekJumps = parseJumps(o.optString("seekJumps")),
    updatedAtMs = o.optLong("updatedAtMs"),
)

private fun historyToJson(list: List<ViewHistoryEntry>): JSONArray = JSONArray().also { arr ->
    list.forEach {
        arr.put(
            JSONObject().put("date", it.dateIso).put("speed", it.speed)
                .put("fromSec", it.fromSec).put("toSec", it.toSec).put("isReplay", it.isReplay),
        )
    }
}

private fun parseHistory(raw: String): List<ViewHistoryEntry> = runCatching {
    val arr = JSONArray(raw)
    buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(ViewHistoryEntry(o.optString("date"), o.optDouble("speed", 1.0), o.optDouble("fromSec"), o.optDouble("toSec"), o.optBoolean("isReplay")))
        }
    }
}.getOrDefault(emptyList())

private fun jumpsToJson(list: List<SeekJump>): JSONArray = JSONArray().also { arr ->
    list.forEach {
        arr.put(
            JSONObject().put("atClientTime", it.atClientTimeIso).put("fromSec", it.fromSec)
                .put("toSec", it.toSec).put("direction", it.direction),
        )
    }
}

private fun parseJumps(raw: String): List<SeekJump> = runCatching {
    val arr = JSONArray(raw)
    buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(SeekJump(o.optDouble("fromSec"), o.optDouble("toSec"), o.optString("direction"), o.optString("atClientTime")))
        }
    }
}.getOrDefault(emptyList())
