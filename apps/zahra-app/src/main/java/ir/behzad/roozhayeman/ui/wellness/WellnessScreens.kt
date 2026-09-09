package ir.behzad.roozhayeman.ui.wellness

import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import androidx.compose.foundation.verticalScroll
import ir.behzad.roozhayeman.LocalAppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ماژول ورزش/یوگا/تنفس/تکنیک یادگیری (فایل توسعه ۰۲).
 *
 * فهرست کامل از سرور/کش/کاتالوگ داخلی می‌آید (۱۵ یوگا + ۱۵ ورزش + ۸ تنفس + ۵ تکنیک).
 * هر حرکت یک تایمر بصری با شمارنده‌ی صوتی دارد؛ در صورت وجود عکس پروفایل، امکان
 * تولید تصویر با چهره‌ی کاربر (تابع generate-move-image) هست.
 */
@Composable
fun WellnessScreen(onMoveClick: (String) -> Unit, onSketch: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val repo = remember { WellnessRepository(container.tables, container.store) }
    var selected by remember { mutableStateOf<WellnessCategory?>(null) }
    var moves by remember { mutableStateOf<List<WellnessMove>>(WellnessCatalog.moves) }
    LaunchedEffect(Unit) { moves = repo.moves() }

    val filtered = moves.filter { selected == null || it.category == selected }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("ورزش، یوگا و تنفس", onBack)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "اگر درد داشتی متوقف کن. هیچ ادعای درمانی نیست.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth().clickable { onSketch() }) {
                Column(Modifier.padding(14.dp)) {
                    Text("✏️ مرجع نقاشی سیاه‌قلم", style = MaterialTheme.typography.titleMedium)
                    Text("سطح ۴ تا ۱۰ — تصویر مرجع بساز و تمرین کن.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selected == null, onClick = { selected = null }, label = { Text("همه") })
                WellnessCategory.entries.forEach { cat ->
                    FilterChip(selected = selected == cat, onClick = { selected = cat }, label = { Text("${cat.emoji} ${cat.displayName}") })
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { move ->
                    Card(Modifier.fillMaxWidth().clickable { onMoveClick(move.id) }) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${move.category.emoji}  ${move.titleFa}", style = MaterialTheme.typography.titleMedium)
                                Text("سطح ${move.level}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(move.instructionsFa, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/**
 * صفحه‌ی یک حرکت با تایمر بصری + شمارنده‌ی صوتی (فایل توسعه ۰۲ بخش ۴).
 *
 * برای تمرین‌های زمان‌دار (تنفس/یوگا): شمارش معکوس با بوقِ کوتاه هر ثانیه‌ی پایانی و
 * زنگ پایان. برای تمرین‌های تکراری (reps): شمارش تکرارها با بوق کوتاه در هر تکرار.
 */
@Composable
fun WellnessDetailScreen(moveId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val repo = remember { WellnessRepository(container.tables, container.store) }
    var move by remember(moveId) { mutableStateOf(WellnessCatalog.byId(moveId)) }
    LaunchedEffect(moveId) { repo.move(moveId)?.let { move = it } }

    val current = move
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
    DisposableEffect(Unit) { onDispose { runCatching { tone.release() } } }

    if (current == null) {
        Column(Modifier.fillMaxSize()) { AppTopBar("حرکت", onBack); Text("این حرکت پیدا نشد.", Modifier.padding(16.dp)) }
        return
    }

    var running by remember { mutableStateOf(false) }
    var secondsLeft by remember(moveId) { mutableIntStateOf(current.durationSec.coerceAtLeast(1)) }
    var repsDone by remember(moveId) { mutableIntStateOf(0) }
    val isRepBased = current.reps > 0

    LaunchedEffect(running) {
        if (running && !isRepBased) {
            while (running && secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
                if (secondsLeft in 1..3) runCatching { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120) }
            }
            if (running && secondsLeft <= 0) {
                runCatching { tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 300) } // زنگ پایان
                running = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(current.titleFa, onBack)
        Column(Modifier.fillMaxSize().verticalScrollState().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${current.category.emoji} ${current.category.displayName} · سطح ${current.level}", color = MaterialTheme.colorScheme.primary)
            Text(current.instructionsFa, style = MaterialTheme.typography.bodyMedium)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRepBased) {
                        Text("تکرار: $repsDone از ${current.reps}", style = MaterialTheme.typography.headlineSmall)
                        LinearProgressIndicator(progress = { (repsDone.toFloat() / current.reps).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        PrimaryButton(if (repsDone >= current.reps) "دوباره" else "یک تکرار انجام شد") {
                            if (repsDone >= current.reps) { repsDone = 0 } else {
                                repsDone += 1
                                runCatching { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120) }
                                if (repsDone >= current.reps) runCatching { tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 300) }
                            }
                        }
                    } else {
                        Text("%d:%02d".format(secondsLeft / 60, secondsLeft % 60), style = MaterialTheme.typography.headlineMedium)
                        val total = current.durationSec.coerceAtLeast(1)
                        LinearProgressIndicator(progress = { ((total - secondsLeft).toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { running = !running }) { Text(if (running) "توقف" else "شروع") }
                            TextButton(onClick = { running = false; secondsLeft = total }) { Text("از نو") }
                        }
                    }
                }
            }

            MoveImageCard(move = current)
        }
    }
}

/** کارت تولید تصویرِ حرکت با چهره‌ی کاربر (فایل توسعه ۰۲ بخش ۲). */
@Composable
private fun MoveImageCard(move: WellnessMove) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val avatarFileId = container.store.getString("profile_avatar_file_id")
    var status by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf(move.defaultImageUrl.ifBlank { null }) }
    var busy by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("تصویر حرکت با چهره‌ی خودت", style = MaterialTheme.typography.titleMedium)
            if (imageUrl != null) {
                Text("آدرس تصویر آماده است:", style = MaterialTheme.typography.bodySmall)
                Text(imageUrl!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            when {
                avatarFileId.isBlank() ->
                    Text("برای ساخت تصویر با چهره‌ی خودت، اول در پروفایل یک عکس اضافه کن.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> PrimaryButton(if (busy) "در حال ساخت…" else "ساخت/به‌روزرسانی تصویر") {
                    if (busy) return@PrimaryButton
                    busy = true; status = null
                    scope.launch {
                        when (val r = container.serverActions.generateMoveImage(move.id, move.titleFa, avatarFileId, regenerate = imageUrl != null)) {
                            is AppResult.Ok -> { imageUrl = r.value.url; status = if (r.value.cached) "از کش آمد." else "ساخته شد." }
                            is AppResult.Err -> status = r.error.userMessage
                        }
                        busy = false
                    }
                }
            }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** helper کوچک تا فراموش‌نکردن اسکرول عمودی در صفحه‌ی جزئیات. */
@Composable
private fun Modifier.verticalScrollState(): Modifier = this.verticalScroll(rememberScrollState())
