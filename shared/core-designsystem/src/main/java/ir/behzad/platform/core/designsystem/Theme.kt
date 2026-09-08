package ir.behzad.platform.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class BrandTheme { DollStage, CalmFather }

private val DollLight = lightColorScheme(
    primary = DollPink, onPrimary = DollCreamOn,
    primaryContainer = DollPinkContainer, onPrimaryContainer = DollOnPinkContainer,
    secondary = DollPeach, onSecondary = DollOnPeach,
    secondaryContainer = DollPeachContainer, onSecondaryContainer = DollOnPeachContainer,
    tertiary = DollLavender, onTertiary = DollOnLavender,
    tertiaryContainer = DollLavenderContainer, onTertiaryContainer = DollOnLavenderContainer,
    background = DollCream, onBackground = DollCreamOn,
    surface = DollSurface, onSurface = DollCreamOn,
    surfaceVariant = DollSurfaceVariant, onSurfaceVariant = DollOnSurfaceVariant,
    outline = DollOutline, error = DollError,
)
private val DollDark = darkColorScheme(
    primary = DollPinkNight, onPrimary = DollOnPinkNight,
    primaryContainer = DollPinkNightContainer, onPrimaryContainer = DollOnPinkNightContainer,
    secondary = DollPeachNight, onSecondary = DollOnPeachNight,
    secondaryContainer = DollPeachNightContainer, onSecondaryContainer = DollOnPeachNightContainer,
    tertiary = DollLavenderNight, onTertiary = DollOnLavenderNight,
    tertiaryContainer = DollLavenderNightContainer, onTertiaryContainer = DollOnLavenderNightContainer,
    background = DollNightBackground, onBackground = DollNightOnBackground,
    surface = DollNightSurface, onSurface = DollNightOnBackground,
    surfaceVariant = DollNightSurfaceVariant, onSurfaceVariant = DollNightOnSurfaceVariant,
    outline = DollNightOutline, error = DollNightError,
)
private val CalmLight = lightColorScheme(
    primary = CalmPrimary, onPrimary = CalmOnPrimary,
    primaryContainer = CalmPrimaryContainer, onPrimaryContainer = CalmOnPrimaryContainer,
    secondary = CalmSecondary, onSecondary = CalmOnSecondary,
    secondaryContainer = CalmSecondaryContainer, onSecondaryContainer = CalmOnSecondaryContainer,
    tertiary = CalmTertiary, onTertiary = CalmOnTertiary,
    tertiaryContainer = CalmTertiaryContainer, onTertiaryContainer = CalmOnTertiaryContainer,
    background = CalmBackground, onBackground = CalmOnBackground,
    surface = CalmSurface, onSurface = CalmOnBackground,
    surfaceVariant = CalmSurfaceVariant, onSurfaceVariant = CalmOnSurfaceVariant,
    outline = CalmOutline, error = CalmError,
)
private val CalmDark = darkColorScheme(
    primary = CalmPrimaryNight, onPrimary = CalmOnPrimaryNight,
    background = CalmBackgroundNight, onBackground = CalmOnBackgroundNight,
    surface = CalmSurfaceNight, onSurface = CalmOnBackgroundNight,
    surfaceVariant = CalmSurfaceVariantNight, onSurfaceVariant = CalmOnSurfaceVariantNight,
    outline = CalmOutlineNight, error = CalmErrorNight,
)

@Composable
fun PlatformTheme(
    brand: BrandTheme,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = when (brand) {
        BrandTheme.DollStage -> if (darkTheme) DollDark else DollLight
        BrandTheme.CalmFather -> if (darkTheme) CalmDark else CalmLight
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, typography = PlatformTypography, shapes = PlatformShapes, content = content)
    }
}
