package ir.behzad.roozhayeman.ui.study

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.feature.playback.PlaybackController
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.content.LessonMediaProgress
import ir.behzad.roozhayeman.ui.content.MediaProgressRepository
import ir.behzad.roozhayeman.ui.content.MediaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

private fun fmt(sec: Double): String {
    val s = sec.toInt().coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * نوار وضعیت درس (فایل توسعه ۰۱): خط «مرور دوباره — مشاهده‌شده در … با سرعت …»
 * به‌همراه یک نوار پیشرفت کوچک که نقطه‌های seek-jump را روی نوار زمان نشان می‌دهد.
 */
@Composable
fun LessonMediaStatusBar(progress: LessonMediaProgress, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(progress.statusLineFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        val track = MaterialTheme.colorScheme.surfaceVariant
        val fill = MaterialTheme.colorScheme.primary
        val mark = MaterialTheme.colorScheme.error
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val h = size.height
            drawRect(color = track, size = size)
            if (progress.fractionWatched > 0f) {
                drawRect(color = fill, size = size.copy(width = size.width * progress.fractionWatched))
            }
            val dur = progress.durationSec
            if (dur > 0) {
                progress.seekJumps.forEach { jump ->
                    val x = ((jump.toSec / dur).toFloat().coerceIn(0f, 1f)) * size.width
                    drawCircle(color = mark, radius = h / 2f, center = androidx.compose.ui.geometry.Offset(x, h / 2f))
                }
            }
        }
    }
}

/**
 * پلیر ویدیویی/صوتی درس با حافظه و سینک سرور (فایل توسعه ۰۱).
 *
 * رفتار:
 *  - هنگام باز شدن، آخرین موقعیت و سرعت از سرور/کش خوانده و پخش از همان‌جا آماده می‌شود.
 *  - هر ۵ ثانیه یا هنگام مکث/خروج/تغییر سرعت/seek یک آپدیت debounce به سرور می‌رود.
 *  - seek بزرگ‌تر از ۳ ثانیه در seekJumps ثبت می‌شود.
 *  - رسیدن به ≥۹۰٪ → تکمیل، viewCount++ و یک ViewHistory تازه.
 *
 * ویدیو با ExoPlayer محلی داخل PlayerView پخش می‌شود؛ صوت با همان کنترلرِ
 * پس‌زمینه‌ی (`:feature-playback`) تا در حالت بسته‌بودن اپ هم ادامه یابد.
 */
@Composable
fun LessonMediaScreen(
    lessonId: String,
    mediaType: MediaType,
    mediaUri: String,
    title: String,
    bookCode: String = "",
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val repo = remember { MediaProgressRepository(container.tables, container.store) }
    val scope = rememberCoroutineScope()

    var progress by remember(lessonId, mediaType) { mutableStateOf<LessonMediaProgress?>(null) }
    LaunchedEffect(lessonId, mediaType) {
        progress = repo.load(lessonId, mediaType, bookCode)
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title, onBack)
        val current = progress
        if (current == null) {
            Column(Modifier.padding(16.dp)) { Text("در حال آماده‌سازی پلیر…") }
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LessonMediaStatusBar(current)

            if (mediaType == MediaType.VIDEO) {
                LessonVideoPlayer(
                    mediaUri = mediaUri,
                    startPositionSec = current.lastPositionSec,
                    startSpeed = current.playbackSpeed.toFloat(),
                    onTick = { posSec, durSec, speed ->
                        progress = current.copy(lastPositionSec = posSec, durationSec = durSec, playbackSpeed = speed.toDouble())
                    },
                    onSeek = { fromSec, toSec ->
                        progress = repo.recordSeek(progress ?: current, fromSec, toSec)
                    },
                    onPersist = { scope.launch { progress?.let { progress = repo.save(repo.markCompletedIfNeeded(it)) } } },
                )
            } else {
                LessonAudioPlayer(
                    mediaUri = mediaUri,
                    title = title,
                    startPositionSec = current.lastPositionSec,
                    startSpeed = current.playbackSpeed.toFloat(),
                    onTick = { posSec, durSec, speed ->
                        progress = (progress ?: current).copy(lastPositionSec = posSec, durationSec = durSec, playbackSpeed = speed.toDouble())
                    },
                    onSeek = { fromSec, toSec -> progress = repo.recordSeek(progress ?: current, fromSec, toSec) },
                    onPersist = { scope.launch { progress?.let { progress = repo.save(repo.markCompletedIfNeeded(it)) } } },
                )
            }

            if (current.seekJumps.isNotEmpty()) {
                Text(
                    "نقطه‌های قرمز روی نوار = جاهایی که پرش زدی (${current.seekJumps.size} پرش).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun LessonVideoPlayer(
    mediaUri: String,
    startPositionSec: Double,
    startSpeed: Float,
    onTick: (posSec: Double, durSec: Double, speed: Float) -> Unit,
    onSeek: (fromSec: Double, toSec: Double) -> Unit,
    onPersist: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            if (mediaUri.isNotBlank()) {
                setMediaItem(MediaItem.fromUri(mediaUri))
                prepare()
                seekTo((startPositionSec * 1000).toLong())
                setPlaybackSpeed(startSpeed.coerceIn(0.5f, 2f))
            }
        }
    }
    var speed by remember { mutableStateOf(startSpeed) }

    AndroidView(
        factory = { PlayerView(it).apply { this.player = player } },
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
    )

    SpeedRow(speed) { newSpeed -> speed = newSpeed; player.setPlaybackSpeed(newSpeed); onTick(player.currentPosition / 1000.0, player.duration.coerceAtLeast(0) / 1000.0, newSpeed); onPersist() }

    // حلقه‌ی تیک ۵ ثانیه‌ای + تشخیص پرش بزرگ.
    LaunchedEffect(player) {
        var lastPos = player.currentPosition / 1000.0
        while (true) {
            delay(1000)
            val pos = player.currentPosition / 1000.0
            val dur = player.duration.coerceAtLeast(0) / 1000.0
            if (kotlin.math.abs(pos - lastPos) > 3.0 && player.isPlaying.not()) onSeek(lastPos, pos)
            lastPos = pos
            onTick(pos, dur, player.playbackParameters.speed)
            if ((player.currentPosition / 1000) % 5 == 0L) onPersist()
        }
    }
    DisposableEffect(Unit) {
        onDispose { onPersist(); player.release() }
    }
}

@Composable
private fun LessonAudioPlayer(
    mediaUri: String,
    title: String,
    startPositionSec: Double,
    startSpeed: Float,
    onTick: (posSec: Double, durSec: Double, speed: Float) -> Unit,
    onSeek: (fromSec: Double, toSec: Double) -> Unit,
    onPersist: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { PlaybackController(context) }
    val state by controller.state.collectAsState()
    var speed by remember { mutableStateOf(startSpeed) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaUri) {
        if (controller.connect() && mediaUri.isNotBlank()) {
            controller.setMedia(mediaUri, title, (startPositionSec * 1000).toLong())
            controller.setSpeed(startSpeed)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val posSec = state.positionMs / 1000.0
            val durSec = state.durationMs / 1000.0
            Text("${fmt(posSec)} / ${fmt(durSec)}", style = MaterialTheme.typography.titleMedium)
            if (durSec > 0) {
                Slider(
                    value = posSec.toFloat(),
                    onValueChange = { v -> onSeek(posSec, v.toDouble()); controller.seekTo((v * 1000).toLong()) },
                    valueRange = 0f..durSec.toFloat(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { controller.seekTo((state.positionMs - 10_000).coerceAtLeast(0)) }) { Text("−۱۰ث") }
                TextButton(onClick = { if (state.playing) controller.pause() else controller.play(); onPersist() }) { Text(if (state.playing) "توقف" else "پخش") }
                TextButton(onClick = { controller.seekTo(state.positionMs + 10_000) }) { Text("+۱۰ث") }
            }
            SpeedRow(speed) { newSpeed -> speed = newSpeed; controller.setSpeed(newSpeed) }
        }
    }

    LaunchedEffect(state.positionMs) {
        onTick(state.positionMs / 1000.0, state.durationMs / 1000.0, state.speed)
        if ((state.positionMs / 1000) % 5 == 0L) onPersist()
    }
    DisposableEffect(Unit) { onDispose { onPersist(); controller.release() } }
}

@Composable
private fun SpeedRow(current: Float, onSelect: (Float) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SPEEDS.forEach { s ->
            FilterChip(
                selected = kotlin.math.abs(current - s) < 0.01f,
                onClick = { onSelect(s) },
                label = { Text("${if (s % 1f == 0f) s.toInt().toString() else s.toString()}x") },
            )
        }
    }
}
