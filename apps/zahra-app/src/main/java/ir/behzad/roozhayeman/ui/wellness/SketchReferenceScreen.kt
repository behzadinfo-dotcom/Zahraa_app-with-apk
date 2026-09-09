package ir.behzad.roozhayeman.ui.wellness

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import androidx.compose.runtime.key
import ir.behzad.roozhayeman.LocalAppContainer
import kotlinx.coroutines.launch

private val SUBJECTS = listOf("چهره", "طبیعت", "حیوان", "اشیاء")

/**
 * «مرجع نقاشی» — ایجنت طراح تصاویر مرجع سیاه‌قلم (فایل توسعه ۰۲ بخش ۵).
 *
 * کاربر یک موضوع و یک سطح مهارت (۴ تا ۱۰) انتخاب می‌کند و تابع
 * generate-sketch-reference یک تصویر مرجعِ سیاه‌قلم می‌سازد؛ نتیجه در گالری
 * «مرجع‌های من» ذخیره می‌شود.
 */
@Composable
fun SketchReferenceScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf(SUBJECTS.first()) }
    var level by remember { mutableFloatStateOf(6f) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val gallery = remember { mutableListOf<String>() }
    var galleryVersion by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("مرجع نقاشی سیاه‌قلم", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("موضوع را انتخاب کن:", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SUBJECTS.forEach { s ->
                    FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text(s) })
                }
            }
            Text("سطح مهارت: ${level.toInt()} از ۱۰", style = MaterialTheme.typography.titleMedium)
            Slider(value = level, onValueChange = { level = it }, valueRange = 4f..10f, steps = 5)
            Text(
                "۴ = خطوط ساده · ۷ = سایه‌زنی میان‌رده · ۱۰ = جزئیات حرفه‌ای",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PrimaryButton(if (busy) "در حال ساخت مرجع…" else "ساخت تصویر مرجع") {
                if (busy) return@PrimaryButton
                busy = true; status = null
                scope.launch {
                    when (val r = container.serverActions.generateSketchReference(subject, level.toInt())) {
                        is AppResult.Ok -> { gallery.add(0, r.value.url); galleryVersion++; status = "مرجع تازه ساخته شد." }
                        is AppResult.Err -> status = r.error.userMessage
                    }
                    busy = false
                }
            }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            if (gallery.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("مرجع‌های من", style = MaterialTheme.typography.titleMedium)
                key(galleryVersion) {
                    gallery.forEach { url ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("تصویر مرجع سیاه‌قلم", style = MaterialTheme.typography.bodyMedium)
                                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
