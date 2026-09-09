package ir.behzad.roozhayeman.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.appwrite.AiProvider
import ir.behzad.platform.core.appwrite.AiProviderRepository
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import ir.behzad.roozhayeman.LocalAppContainer
import kotlinx.coroutines.launch

private val FEATURES = listOf(
    "ai-companion", "generate-recipe", "generate-move-image", "generate-sketch-reference",
)

/**
 * صفحه‌ی «مدل‌های هوش مصنوعی» (فایل توسعه ۰۴ بخش ۳) — فقط برای نقش ادمین.
 *
 * از داخل اپ می‌توان چند provider تعریف/فعال/غیرفعال کرد و بین آن‌ها سوییچ کرد.
 * نکته‌ی امنیتی: کلید API کامل هیچ‌وقت در کلاینت نمایش داده نمی‌شود (فقط ۴ رقم آخر)؛
 * خودِ کلید رمزنگاری‌شده روی سرور (با راز سرور) ذخیره می‌شود و توابع سرور آن را
 * می‌خوانند — پس اینجا فقط متادیتای provider مدیریت می‌شود.
 */
@Composable
fun AiProvidersScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val repo = remember { AiProviderRepository(container.tables) }
    val scope = rememberCoroutineScope()
    var providers by remember { mutableStateOf<List<AiProvider>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(reload) {
        when (val r = repo.list()) {
            is AppResult.Ok -> providers = r.value
            is AppResult.Err -> status = r.error.userMessage
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("مدل‌های هوش مصنوعی", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (container.role.label != "father" && container.role.label != "admin") {
                // پیام صادقانه؛ نوشتن روی این کالکشن سمت سرور هم فقط برای ادمین مجاز است.
                Text(
                    "این بخش برای مدیر است. اگر مجاز باشی، تغییرها روی سرور اعمال می‌شود؛ در غیر این صورت سرور اجازه نمی‌دهد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("providerها به‌ترتیب اولویت امتحان می‌شوند؛ اگر یکی fail شد، بعدی fallback است.", style = MaterialTheme.typography.bodySmall)

            providers.sortedBy { it.priority }.forEach { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${p.providerName} · ${p.modelId}", style = MaterialTheme.typography.titleMedium)
                            Switch(checked = p.isActive, onCheckedChange = { active ->
                                scope.launch {
                                    when (val r = repo.setActive(p.id, active)) {
                                        is AppResult.Ok -> reload++
                                        is AppResult.Err -> status = r.error.userMessage
                                    }
                                }
                            })
                        }
                        Text("اولویت ${p.priority} · کلید •••• ${p.apiKeyLast4}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("برای: ${p.usedFor.joinToString("، ")}", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = {
                            scope.launch {
                                when (val r = repo.delete(p.id)) {
                                    is AppResult.Ok -> reload++
                                    is AppResult.Err -> status = r.error.userMessage
                                }
                            }
                        }) { Text("حذف") }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            AddProviderForm(onAdd = { provider ->
                scope.launch {
                    when (val r = repo.add(provider)) {
                        is AppResult.Ok -> { reload++; status = "provider اضافه شد. کلید رمزنگاری‌شده باید روی سرور تکمیل شود." }
                        is AppResult.Err -> status = r.error.userMessage
                    }
                }
            })

            status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun AddProviderForm(onAdd: (AiProvider) -> Unit) {
    var name by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("https://api.openai.com/v1/chat/completions") }
    var last4 by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("100") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("افزودن provider تازه", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(name, { name = it }, label = { Text("نام (مثلاً OpenAI، Claude، Gemini)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(model, { model = it }, label = { Text("شناسه‌ی مدل (مثلاً gpt-4o-mini)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(endpoint, { endpoint = it }, label = { Text("آدرس endpoint") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(last4, { last4 = it.take(4) }, label = { Text("۴ رقم آخر کلید (فقط برای نمایش)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(priority, { priority = it.filter(Char::isDigit) }, label = { Text("اولویت (کوچک‌تر = زودتر)") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton("افزودن") {
                if (name.isBlank() || model.isBlank()) return@PrimaryButton
                onAdd(
                    AiProvider(
                        id = "",
                        providerName = name.trim(),
                        modelId = model.trim(),
                        endpointUrl = endpoint.trim(),
                        apiKeyLast4 = last4.trim(),
                        isActive = false,
                        priority = priority.toIntOrNull() ?: 100,
                        usedFor = FEATURES,
                    ),
                )
            }
            Text(
                "کلید کامل هیچ‌وقت اینجا ذخیره یا نمایش داده نمی‌شود؛ کلید رمزنگاری‌شده را ادمین روی سرور ست می‌کند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
