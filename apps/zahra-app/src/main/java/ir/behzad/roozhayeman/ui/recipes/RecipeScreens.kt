package ir.behzad.roozhayeman.ui.recipes

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.common.AppResult
import ir.behzad.platform.core.common.JalaliDate
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import ir.behzad.platform.core.designsystem.SectionCard
import ir.behzad.roozhayeman.LocalAppContainer
import ir.behzad.roozhayeman.ui.content.Recipe
import kotlinx.coroutines.launch

private const val COOKED_KEY = "recipe_cooked_"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(onBack: () -> Unit, onDetail: (String) -> Unit) {
    val container = LocalAppContainer.current
    var query by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf<String?>(null) }
    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    LaunchedEffect(Unit) { recipes = container.catalog.recipes() }

    val results = remember(recipes, query, difficulty) {
        recipes.filter { r ->
            (difficulty == null || r.difficulty == difficulty) &&
                (query.isBlank() || r.title.contains(query.trim(), ignoreCase = true) ||
                    r.ingredients.any { it.contains(query.trim(), ignoreCase = true) })
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آشپزی", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("چی بپزم؟ اسم غذا یا ماده‌ی اولیه") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("آسان", "متوسط", "کمی سخت").forEach { level ->
                    FilterChip(
                        selected = difficulty == level,
                        onClick = { difficulty = if (difficulty == level) null else level },
                        label = { Text(level) },
                    )
                }
            }

            if (results.isEmpty()) {
                Text(
                    if (recipes.isEmpty()) "فهرست در حال آماده‌سازی است…" else "با این جست‌وجو چیزی پیدا نشد.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                // فایل توسعه ۰۳: اگر دستور پخت محلی نبود، از سرور (تابع generate-recipe) بخواه.
                if (query.isNotBlank() && recipes.isNotEmpty()) {
                    val scope = rememberCoroutineScope()
                    var busy by remember(query) { mutableStateOf(false) }
                    var note by remember(query) { mutableStateOf<String?>(null) }
                    PrimaryButton(if (busy) "در حال ساختن دستور «${query.trim()}»…" else "دستور «${query.trim()}» را بساز") {
                        if (busy) return@PrimaryButton
                        busy = true; note = null
                        scope.launch {
                            when (val r = container.serverActions.generateRecipe(query.trim())) {
                                is AppResult.Ok -> {
                                    // کش سرور پر شد؛ فهرست را دوباره می‌خوانیم تا دستور تازه بیاید.
                                    recipes = container.catalog.recipes()
                                    note = if (r.value.cached) "از قبل ذخیره بود." else "ساخته و ذخیره شد."
                                }
                                is AppResult.Err -> note = r.error.userMessage
                            }
                            busy = false
                        }
                    }
                    note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            results.forEach { r ->
                val cooked = container.store.getString(COOKED_KEY + r.id).isNotEmpty()
                SectionCard(
                    title = r.title,
                    body = "${r.difficulty} · ${r.minutes} دقیقه · ${r.servings} نفره" +
                        if (cooked) " · پختیش ✅" else "",
                    onClick = { onDetail(r.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(recipeId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var recipe by remember { mutableStateOf<Recipe?>(null) }

    LaunchedEffect(recipeId) {
        recipe = container.catalog.recipes().firstOrNull { it.id == recipeId }
    }

    val current = recipe
    Column(Modifier.fillMaxSize()) {
        AppTopBar(current?.title ?: "دستور", onBack)
        if (current == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("این دستور پیدا نشد. اگر آنلاین نیستی، بعداً دوباره امتحان کن.")
                PrimaryButton("بازگشت", onBack)
            }
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text("${current.minutes} دقیقه") })
                AssistChip(onClick = { }, label = { Text("${current.servings} نفره") })
                AssistChip(onClick = { }, label = { Text(current.difficulty) })
            }

            Text("مواد لازم", style = MaterialTheme.typography.titleMedium)
            current.ingredients.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }

            Spacer(Modifier.height(4.dp))
            Text("طرز تهیه", style = MaterialTheme.typography.titleMedium)
            current.steps.forEachIndexed { i, step ->
                Text("${i + 1}) $step", style = MaterialTheme.typography.bodyMedium)
            }

            if (current.tip.isNotBlank()) {
                SectionCard("نکته‌ی زهرا", current.tip) { }
            }

            val cookedLabel = container.store.getString(COOKED_KEY + current.id)
                .let { if (it.isEmpty()) null else JalaliDate.formatFaLong(it) }
            PrimaryButton(if (cookedLabel == null) "پختمش ✅" else "پختمش ✅ ($cookedLabel)") {
                container.store.putString(COOKED_KEY + current.id, JalaliDate.todayIso())
                onBack()
            }
        }
    }
}
