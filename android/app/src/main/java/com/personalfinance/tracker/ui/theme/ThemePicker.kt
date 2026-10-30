package com.personalfinance.tracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.R

@Composable
fun ThemePicker(selected: AppTheme, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.themes_title), style = MaterialTheme.typography.titleLarge)
        AppThemeRegistry.available.forEach { theme ->
            val colors = themeColorScheme(theme)
            Surface(color = colors.surface, contentColor = colors.onSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
                modifier = Modifier.fillMaxWidth().testTag("theme_${theme.id}")
                    .selectable(theme.id == selected.id, role = Role.RadioButton, onClick = { onSelect(theme.id) })) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = theme.id == selected.id, onClick = null,
                        modifier = Modifier.testTag("theme_radio_${theme.id}"),
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.primary,
                            unselectedColor = colors.onSurfaceVariant,
                            disabledSelectedColor = colors.primary,
                            disabledUnselectedColor = colors.onSurfaceVariant,
                        ))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(theme.nameRes), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(colors.primary, colors.secondary, colors.background).forEach { color ->
                                Box(Modifier.size(width = 30.dp, height = 12.dp).background(color))
                            }
                        }
                    }
                }
            }
        }
    }
}
