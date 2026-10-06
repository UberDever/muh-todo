package dev.uberdever.muhtodo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.sp
import dev.uberdever.muhtodo.R

private val defaultTypography = Typography()
private val compactTypography = defaultTypography.let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
        headlineMedium = base.headlineMedium.copy(fontSize = 26.sp, lineHeight = 32.sp),
        headlineSmall = base.headlineSmall.copy(fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = base.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = base.titleMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        titleSmall = base.titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = base.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
        labelLarge = base.labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = base.labelMedium.copy(fontSize = 11.sp, lineHeight = 16.sp),
        labelSmall = base.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
    )
}

@Composable
fun TodoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = colorResource(R.color.todo_background),
            surface = colorResource(R.color.todo_surface),
            onBackground = colorResource(R.color.todo_text),
            onSurface = colorResource(R.color.todo_text),
            onSurfaceVariant = colorResource(R.color.todo_secondary_text),
            primary = colorResource(R.color.todo_accent),
            onPrimary = colorResource(R.color.todo_on_accent),
        ),
        typography = compactTypography,
        content = content,
    )
}
