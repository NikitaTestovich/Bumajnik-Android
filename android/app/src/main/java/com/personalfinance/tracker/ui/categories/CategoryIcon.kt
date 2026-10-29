package com.personalfinance.tracker.ui.categories

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.personalfinance.tracker.domain.model.CategoryIcons

// Bundled, dependency-free vector paths. Persist only the stable registry key.
private val paths = listOf(
    "M3,3 L5,3 L8,15 L19,15 L22,6 L6,6 L7,8 L19,8 L17,13 L10,13 L7,3 Z M9,17 L12,17 L12,20 L9,20 Z M17,17 L20,17 L20,20 L17,20 Z",
    "M2,11 L12,2 L22,11 L20,13 L19,12 L19,22 L14,22 L14,15 L10,15 L10,22 L5,22 L5,12 L4,13 Z",
    "M5,3 L19,3 L22,14 L22,20 L19,20 L19,18 L5,18 L5,20 L2,20 L2,14 Z M6,5 L4,12 L20,12 L18,5 Z M5,14 L8,14 L8,16 L5,16 Z M16,14 L19,14 L19,16 L16,16 Z",
    "M7,1 L17,1 L19,3 L19,21 L17,23 L7,23 L5,21 L5,3 Z M7,4 L7,19 L17,19 L17,4 Z M10,20 L14,20 L14,22 L10,22 Z",
    "M9,2 L15,2 L15,9 L22,9 L22,15 L15,15 L15,22 L9,22 L9,15 L2,15 L2,9 L9,9 Z",
    "M1,8 L12,2 L23,8 L12,14 Z M5,12 L12,16 L19,12 L19,18 L12,22 L5,18 Z M21,10 L23,10 L23,19 L21,19 Z",
    "M8,2 L10,5 L14,5 L16,2 L23,7 L19,12 L17,10 L17,22 L7,22 L7,10 L5,12 L1,7 Z",
    "M3,2 L5,2 L5,8 L7,8 L7,2 L9,2 L9,10 L7,12 L7,22 L5,22 L5,12 L3,10 Z M16,2 L20,2 L20,22 L18,22 L18,13 L14,13 L14,8 Z",
    "M5,3 L19,3 L19,21 L5,21 Z M7,5 L7,7 L9,7 L9,5 Z M15,5 L15,7 L17,7 L17,5 Z M7,10 L7,17 L17,17 L17,10 Z M7,19 L9,19 L9,17 L7,17 Z M15,19 L17,19 L17,17 L15,17 Z",
    "M4,4 L20,4 L22,9 L22,21 L2,21 L2,9 Z M5,6 L4,9 L20,9 L19,6 Z M7,12 L17,12 L17,15 L7,15 Z",
    "M8,2 L16,2 L18,4 L18,7 L22,7 L22,22 L2,22 L2,7 L6,7 L6,4 Z M8,4 L8,7 L16,7 L16,4 Z M5,10 L5,19 L7,19 L7,10 Z M17,10 L17,19 L19,19 L19,10 Z",
    "M2,8 L22,8 L22,12 L2,12 Z M4,13 L11,13 L11,22 L4,22 Z M13,13 L20,13 L20,22 L13,22 Z M12,7 C2,8 2,0 7,1 C10,1 12,7 12,7 C12,7 14,1 17,1 C22,0 22,8 12,7 Z",
    "M4,3 L8,3 L9,8 L5,10 L2,7 Z M16,3 L20,3 L22,7 L19,10 L15,8 Z M10,1 L14,1 L15,6 L12,8 L9,6 Z M12,10 C16,10 22,19 18,22 C15,24 12,20 9,22 C3,24 2,17 7,13 Z",
    "M4,7 L7,7 L7,17 L4,17 Z M1,9 L3,9 L3,15 L1,15 Z M17,7 L20,7 L20,17 L17,17 Z M21,9 L23,9 L23,15 L21,15 Z M8,11 L16,11 L16,13 L8,13 Z",
    "M10,2 L14,2 L15,7 L20,9 L20,19 L17,22 L7,22 L4,19 L4,9 L9,7 Z M8,11 L8,18 L16,18 L16,11 Z",
    "M9,1 L15,1 L17,4 L15,7 L9,7 L7,4 Z M7,9 L17,9 L22,15 L20,17 L17,14 L17,22 L13,22 L12,17 L11,22 L7,22 L7,14 L4,17 L2,15 Z",
    "M12,1 L22,5 L21,15 L18,20 L12,24 L6,20 L3,15 L2,5 Z M11,7 L11,14 L7,11 L5,13 L11,19 L19,9 L17,7 L11,15 Z",
    "M5,1 L19,1 L19,23 L16,21 L13,23 L10,21 L7,23 L5,21 Z M8,5 L16,5 L16,7 L8,7 Z M8,10 L16,10 L16,12 L8,12 Z M8,15 L14,15 L14,17 L8,17 Z",
    "M15,2 L21,2 L18,6 L20,9 L23,6 L23,12 L18,15 L8,23 L3,18 L11,9 L10,5 Z M5,17 L4,19 L6,21 L8,19 Z",
    "M3,3 L9,3 L9,9 L3,9 Z M15,3 L21,3 L21,9 L15,9 Z M3,15 L9,15 L9,21 L3,21 Z M15,15 L21,15 L21,21 L15,21 Z",
)

@Composable
fun CategoryIcon(key: String, modifier: Modifier = Modifier) {
    val resolved = CategoryIcons.resolve(key)
    val vector = remember(resolved) {
        ImageVector.Builder("category_$resolved", 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(paths[CategoryIcons.keys.indexOf(resolved)]).toNodes(), pathFillType = PathFillType.EvenOdd, fill = SolidColor(Color.Black))
            .build()
    }
    Icon(vector, contentDescription = null, modifier = modifier.size(24.dp))
}
