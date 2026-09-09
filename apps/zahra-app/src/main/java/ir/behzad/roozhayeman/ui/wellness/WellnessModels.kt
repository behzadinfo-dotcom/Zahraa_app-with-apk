package ir.behzad.roozhayeman.ui.wellness

import io.appwrite.Query
import ir.behzad.platform.core.appwrite.TableRow
import ir.behzad.platform.core.appwrite.TablesDbService
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.common.LocalStore
import ir.behzad.platform.core.common.TableIds
import org.json.JSONArray
import org.json.JSONObject

/** دسته‌ی حرکت‌ها (فایل توسعه ۰۲). */
enum class WellnessCategory(val key: String, val displayName: String, val emoji: String) {
    YOGA("yoga", "یوگا", "🧘"),
    EXERCISE("exercise", "ورزش و کشش", "🤸"),
    BREATHING("breathing", "تنفس", "🌬️"),
    LEARNING("learning", "تکنیک یادگیری", "🧠");

    companion object {
        fun of(raw: String): WellnessCategory =
            entries.firstOrNull { it.key == raw.trim() || it.displayName == raw.trim() } ?: EXERCISE
    }
}

/** یک حرکت ورزش/یوگا/تنفس/تکنیک یادگیری. */
data class WellnessMove(
    val id: String,
    val category: WellnessCategory,
    val titleFa: String,
    val level: Int,
    val durationSec: Int,
    val reps: Int,
    val instructionsFa: String,
    val audioCueId: String,
    val referenceImagePromptTemplate: String,
    val defaultImageUrl: String,
    val orderIndex: Int,
)

/**
 * کاتالوگ حرکت‌های تندرستی.
 *
 * اولویت: سرور (`wellness_moves`) → کش محلی → کاتالوگ داخلی (آفلاین).
 * الگوی این کلاس عمداً شبیه [ir.behzad.roozhayeman.ui.content.CatalogRepository] است.
 */
class WellnessRepository(
    private val tables: TablesDbService,
    private val store: LocalStore,
) {
    suspend fun moves(): List<WellnessMove> {
        if (tables.isConfigured) {
            val result = tables.list(TableIds.WELLNESS_MOVES, listOf(Query.limit(200)))
            if (result is AppResult.Ok) {
                val items = result.value.mapNotNull { it.toMove() }
                if (items.isNotEmpty()) {
                    writeCache(items)
                    return items.sortedWith(compareBy({ it.category.ordinal }, { it.orderIndex }))
                }
            }
        }
        return readCache().ifEmpty { WellnessCatalog.moves }
            .sortedWith(compareBy({ it.category.ordinal }, { it.orderIndex }))
    }

    suspend fun move(id: String): WellnessMove? = moves().firstOrNull { it.id == id }

    suspend fun byCategory(category: WellnessCategory): List<WellnessMove> = moves().filter { it.category == category }

    private fun writeCache(items: List<WellnessMove>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        store.putString(CACHE_KEY, arr.toString())
    }

    private fun readCache(): List<WellnessMove> = runCatching {
        val arr = JSONArray(store.getString(CACHE_KEY, "[]"))
        buildList { for (i in 0 until arr.length()) moveFromJson(arr.getJSONObject(i))?.let { add(it) } }
    }.getOrDefault(emptyList())

    companion object {
        private const val CACHE_KEY = "wellness_moves_cache"
    }
}

private fun TableRow.toMove(): WellnessMove? {
    val title = string("titleFa")
    if (title.isBlank()) return null
    return WellnessMove(
        id = id,
        category = WellnessCategory.of(string("category")),
        titleFa = title,
        level = long("level", 1).toInt(),
        durationSec = long("durationSec").toInt(),
        reps = long("reps").toInt(),
        instructionsFa = string("instructionsFa"),
        audioCueId = string("audioCueId"),
        referenceImagePromptTemplate = string("referenceImagePromptTemplate"),
        defaultImageUrl = string("defaultImageUrl"),
        orderIndex = long("orderIndex").toInt(),
    )
}

private fun WellnessMove.toJson(): JSONObject = JSONObject()
    .put("id", id).put("category", category.key).put("titleFa", titleFa)
    .put("level", level).put("durationSec", durationSec).put("reps", reps)
    .put("instructionsFa", instructionsFa).put("audioCueId", audioCueId)
    .put("referenceImagePromptTemplate", referenceImagePromptTemplate)
    .put("defaultImageUrl", defaultImageUrl).put("orderIndex", orderIndex)

private fun moveFromJson(o: JSONObject): WellnessMove? = runCatching {
    WellnessMove(
        id = o.getString("id"),
        category = WellnessCategory.of(o.optString("category")),
        titleFa = o.getString("titleFa"),
        level = o.optInt("level", 1),
        durationSec = o.optInt("durationSec"),
        reps = o.optInt("reps"),
        instructionsFa = o.optString("instructionsFa"),
        audioCueId = o.optString("audioCueId"),
        referenceImagePromptTemplate = o.optString("referenceImagePromptTemplate"),
        defaultImageUrl = o.optString("defaultImageUrl"),
        orderIndex = o.optInt("orderIndex"),
    )
}.getOrNull()
