package ir.behzad.roozhayeman.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.platform.core.designsystem.SectionCard
import ir.behzad.roozhayeman.ui.navigation.Screen

@Composable
fun MoreScreen(nav: NavController) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("بیشتر", style = MaterialTheme.typography.titleLarge)
        SectionCard("آموزش هوش مصنوعی", "مسیر پیش‌نیازدار، یادآور روزانه و ارزیابی.") { nav.navigate(Screen.AiLearning.route) }
        SectionCard("چرخه و ذهن‌آگاهی", "تقویم و تمرین تنفس.") { nav.navigate(Screen.Cycle.route) }
        SectionCard("زمان صفحه", "سقف خودانتخابی و حالت تمرکز.") { nav.navigate(Screen.ScreenTime.route) }
        SectionCard("فضای امن من", "نوشتن، آلبوم با بابا، شماره‌های کمک.") { nav.navigate(Screen.SafeSpace.route) }
        SectionCard("نقاشی سیاه‌قلم", "ایده‌ی امروز و گالری.") { nav.navigate(Screen.Art.route) }
        SectionCard("آشپزی", "دستور پخت درخواستی.") { nav.navigate(Screen.Recipes.route) }
        SectionCard("ورزش و یوگا", "کاتالوگ حرکت‌ها.") { nav.navigate(Screen.Exercise.route) }
        SectionCard("ورزش، یوگا و تنفس (کامل)", "۴۳ حرکت با شمارنده‌ی صوتی و مرجع نقاشی.") { nav.navigate(Screen.Wellness.route) }
        SectionCard("آب", "یادآور نوشیدن.") { nav.navigate(Screen.Water.route) }
        SectionCard("امتیاز و بج", "فقط جنبه‌ی مثبت.") { nav.navigate(Screen.Badges.route) }
        SectionCard("پیوند با بابا", "کد ۶ رقمی.") { nav.navigate(Screen.Pairing.route) }
        SectionCard("تنظیمات", "حریم، قفل، تم.") { nav.navigate(Screen.Settings.route) }
    }
}
