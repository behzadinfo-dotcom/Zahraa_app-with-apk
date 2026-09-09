package ir.behzad.platform.core.appwrite

import io.appwrite.Query
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.common.TableIds

/**
 * یک provider هوش مصنوعی (فایل توسعه ۰۴). کلید کامل هرگز در کلاینت نیست؛
 * فقط ۴ رقم آخر برای نمایش نگه داشته می‌شود.
 */
data class AiProvider(
    val id: String,
    val providerName: String,
    val modelId: String,
    val endpointUrl: String,
    val apiKeyLast4: String,
    val isActive: Boolean,
    val priority: Int,
    val usedFor: List<String>,
)

/**
 * مدیریت کالکشن `ai_providers` از سمت کلاینت (فقط متادیتا).
 *
 * نکته‌ی امنیتی: ستون `apiKeyEncrypted` از اینجا نوشته نمی‌شود؛ آن را ابزار سمت سرور
 * (با راز `AI_CONFIG_SECRET`) پر می‌کند تا کلید کامل هیچ‌وقت از دستگاه عبور نکند.
 * دسترسی نوشتن روی این کالکشن هم در سرور فقط برای نقش ادمین باز است.
 */
class AiProviderRepository(private val tables: TablesDbService) {

    suspend fun list(): AppResult<List<AiProvider>> {
        if (!tables.isConfigured) return AppResult.Ok(emptyList())
        return when (val r = tables.list(TableIds.AI_PROVIDERS, listOf(Query.limit(50)))) {
            is AppResult.Ok -> AppResult.Ok(r.value.map { it.toProvider() })
            is AppResult.Err -> r
        }
    }

    suspend fun add(provider: AiProvider): AppResult<TableRow> =
        tables.create(TableIds.AI_PROVIDERS, provider.toData(), permissions = emptyList())

    suspend fun setActive(id: String, active: Boolean): AppResult<TableRow> =
        tables.update(TableIds.AI_PROVIDERS, id, mapOf("isActive" to active))

    suspend fun setPriority(id: String, priority: Int): AppResult<TableRow> =
        tables.update(TableIds.AI_PROVIDERS, id, mapOf("priority" to priority))

    suspend fun delete(id: String): AppResult<Unit> = tables.delete(TableIds.AI_PROVIDERS, id)
}

private fun TableRow.toProvider(): AiProvider = AiProvider(
    id = id,
    providerName = string("providerName"),
    modelId = string("modelId"),
    endpointUrl = string("endpointUrl", "https://api.openai.com/v1/chat/completions"),
    apiKeyLast4 = string("apiKeyLast4"),
    isActive = boolean("isActive"),
    priority = long("priority", 100).toInt(),
    usedFor = (data["usedFor"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
)

private fun AiProvider.toData(): Map<String, Any?> = mapOf(
    "providerName" to providerName,
    "modelId" to modelId,
    "endpointUrl" to endpointUrl,
    "apiKeyLast4" to apiKeyLast4,
    "isActive" to isActive,
    "priority" to priority,
    "usedFor" to usedFor,
    "createdAtMs" to System.currentTimeMillis(),
)
