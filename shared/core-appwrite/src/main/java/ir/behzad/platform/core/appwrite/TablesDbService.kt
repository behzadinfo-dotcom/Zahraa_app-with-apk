package ir.behzad.platform.core.appwrite

import io.appwrite.ID
import io.appwrite.models.Row
import io.appwrite.services.TablesDB
import ir.behzad.platform.core.common.AppError
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.common.PrivacyPolicy

/** یک سطر TablesDB به‌شکل ساده‌شده تا لایه‌ی UI به مدل‌های SDK وابسته نشود. */
data class TableRow(val id: String, val data: Map<String, Any?>) {
    fun string(key: String, default: String = ""): String = data[key] as? String ?: default
    fun long(key: String, default: Long = 0L): Long = when (val v = data[key]) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull() ?: default
        else -> default
    }

    fun boolean(key: String, default: Boolean = false): Boolean = data[key] as? Boolean ?: default
}

interface TablesDbService {
    val isConfigured: Boolean

    suspend fun create(
        table: String,
        data: Map<String, Any?>,
        permissions: List<String> = emptyList(),
        rowId: String = ID.unique(),
    ): AppResult<TableRow>

    suspend fun get(table: String, id: String): AppResult<TableRow>

    suspend fun list(table: String, queries: List<String> = emptyList()): AppResult<List<TableRow>>

    suspend fun update(table: String, id: String, data: Map<String, Any?>): AppResult<TableRow>

    /** ساخت یا به‌روزرسانی — پایه‌ی SyncEngine (همان rowId محلی حفظ می‌شود). */
    suspend fun upsert(
        table: String,
        id: String,
        data: Map<String, Any?>,
        permissions: List<String> = emptyList(),
    ): AppResult<TableRow>

    suspend fun delete(table: String, id: String): AppResult<Unit>
}

class AppwriteTablesDbService(
    private val provider: AppwriteClientProvider,
) : TablesDbService {

    override val isConfigured: Boolean get() = provider.isConfigured

    private val db get() = TablesDB(provider.client)

    /** تبدیل مدل Row<T> SDK به [TableRow] — داده‌ی سطر دیگر دور ریخته نمی‌شود. */
    private fun toTableRow(row: Row<*>): TableRow {
        val raw = row.data
        val map: Map<String, Any?> = if (raw is Map<*, *>) {
            raw.entries.associate { entry -> entry.key.toString() to entry.value }
        } else {
            emptyMap()
        }
        return TableRow(row.id, map)
    }

    override suspend fun create(
        table: String,
        data: Map<String, Any?>,
        permissions: List<String>,
        rowId: String,
    ): AppResult<TableRow> {
        if (!provider.isConfigured) {
            return AppResult.Err(AppError.Local("بک‌اند پیکربندی نشده؛ داده فقط روی همین دستگاه ذخیره شد."))
        }
        return runCatching {
            val row = db.createRow(
                databaseId = provider.databaseId,
                tableId = table,
                rowId = rowId,
                data = data,
                permissions = permissions,
            )
            AppResult.Ok(toTableRow(row))
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "ذخیره‌ی «$table» ناموفق بود.")) }
    }

    override suspend fun get(table: String, id: String): AppResult<TableRow> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Local("بک‌اند پیکربندی نشده."))
        return runCatching {
            val row = db.getRow(
                databaseId = provider.databaseId,
                tableId = table,
                rowId = id,
            )
            AppResult.Ok(toTableRow(row))
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it)) }
    }

    override suspend fun list(table: String, queries: List<String>): AppResult<List<TableRow>> {
        if (!provider.isConfigured) return AppResult.Ok(emptyList())
        return runCatching {
            val result = db.listRows(
                databaseId = provider.databaseId,
                tableId = table,
                queries = queries,
            )
            AppResult.Ok(result.rows.map { toTableRow(it) })
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it)) }
    }

    override suspend fun update(table: String, id: String, data: Map<String, Any?>): AppResult<TableRow> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Local("بک‌اند پیکربندی نشده."))
        return runCatching {
            val row = db.updateRow(
                databaseId = provider.databaseId,
                tableId = table,
                rowId = id,
                data = data,
            )
            AppResult.Ok(toTableRow(row))
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "به‌روزرسانی «$table» ناموفق بود.")) }
    }

    override suspend fun upsert(
        table: String,
        id: String,
        data: Map<String, Any?>,
        permissions: List<String>,
    ): AppResult<TableRow> {
        if (PrivacyPolicy.isNeverSynced(table)) {
            return AppResult.Err(AppError.Permission("این داده خصوصی است و هرگز به سرور فرستاده نمی‌شود."))
        }
        if (!provider.isConfigured) return AppResult.Err(AppError.Local("بک‌اند پیکربندی نشده."))
        return runCatching {
            // اول تلاش برای به‌روزرسانی؛ اگر سطر وجود نداشت، ساخت با همان rowId.
            // (عمداً از upsertRow استفاده نمی‌کنیم تا فقط به امضاهای قطعی SDK تکیه کنیم.)
            val updated = runCatching {
                db.updateRow(
                    databaseId = provider.databaseId,
                    tableId = table,
                    rowId = id,
                    data = data,
                )
            }.getOrNull()
            val row = updated ?: db.createRow(
                databaseId = provider.databaseId,
                tableId = table,
                rowId = id,
                data = data,
                permissions = permissions,
            )
            AppResult.Ok(toTableRow(row))
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "همگام‌سازی «$table» ناموفق بود.")) }
    }

    override suspend fun delete(table: String, id: String): AppResult<Unit> {
        if (!provider.isConfigured) return AppResult.Ok(Unit)
        return runCatching {
            db.deleteRow(
                databaseId = provider.databaseId,
                tableId = table,
                rowId = id,
            )
            AppResult.Ok(Unit)
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it)) }
    }
}
