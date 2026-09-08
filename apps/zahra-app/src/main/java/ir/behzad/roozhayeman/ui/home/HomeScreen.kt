package ir.behzad.roozhayeman.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.platform.core.designsystem.SectionCard
import ir.behzad.roozhayeman.ui.navigation.Screen

@Composable
fun HomeScreen(nav: NavController) {
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { nav.navigate(Screen.Calm.route) }) { Text("💛") }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("سلام زهرا 🌸", style = MaterialTheme.typography.headlineMedium)
            Text("یه جای امن برای روزای تو", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            SectionCard("حالت امروز چطوره؟", "با یک ایموجی ثبتش کن — اختیاریه.") { nav.navigate(Screen.Mood.route) }
            SectionCard("روتین امروز", "بلوک‌های روزت را ببین یا روز سبک انتخاب کن.") { nav.navigate(Screen.Routine.route) }
            SectionCard("آب بنوش 💧", "لیوان‌های امروز را ثبت کن.") { nav.navigate(Screen.Water.route) }
            SectionCard("حرکت نرم", "یوگا، کشش یا تنفس ۳ دقیقه‌ای.") { nav.navigate(Screen.Exercise.route) }
            SectionCard("آموزش هوش مصنوعی", "از پایه تا پروژه؛ درس روزانه + آزمون + نمودار.") { nav.navigate(Screen.AiLearning.route) }
            SectionCard("حرف دل با بابا", "پیام، ویس، عکس یا تماس.") { nav.navigate(Screen.Heart.route) }
        }
    }
}
