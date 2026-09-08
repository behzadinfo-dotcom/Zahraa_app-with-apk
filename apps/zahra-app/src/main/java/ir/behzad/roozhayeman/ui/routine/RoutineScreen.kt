package ir.behzad.roozhayeman.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.behzad.platform.core.designsystem.AppTopBar
import ir.behzad.platform.core.designsystem.PrimaryButton
import ir.behzad.platform.core.designsystem.SectionCard
import ir.behzad.roozhayeman.LocalAppContainer

@Composable
fun RoutineScreen(onBack: () -> Unit) {
    val store = LocalAppContainer.current.store
    var light by remember { mutableStateOf(store.getBool("light_day")) }
    val blocks = listOf("بیدارشدن", "مدرسه", "درس", "ورزش", "وقت آزاد", "خواب")
    Column(Modifier.fillMaxSize()) {
        AppTopBar("روتین روزانه", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("امروز روز سبک‌تری می‌خوای؟ پیشنهاده، نه اجبار.")
            PrimaryButton(if (light) "روز سبک روشنه" else "روز سبک می‌خوام") {
                light = !light
                store.putBool("light_day", light)
            }
            blocks.forEach { SectionCard(it, if (light) "نسخه‌ی سبک‌تر" else "بلوک معمولی") { } }
        }
    }
}
