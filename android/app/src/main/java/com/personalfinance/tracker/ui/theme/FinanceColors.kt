package com.personalfinance.tracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

/** Financial meaning is independent of the accent used by the selected design. */
data class FinanceColors(val income: Color, val expense: Color, val goal: Color, val goalInk: Color, val latest: Color, val latestInk: Color, val chart: List<Color>)
fun financeColors(theme: AppTheme): FinanceColors = when (theme.id) {
 10 -> FinanceColors(Color(0xFF17603D),Color(0xFFB22D35),Color(0xFFFFD4B4),Color(0xFF412530),Color(0xFFF3DFEB),Color(0xFF412530),listOf(Color(0xFF703B59),Color(0xFF9D5227),Color(0xFFB2444F),Color(0xFF655494)))
 11 -> FinanceColors(Color(0xFF17603D),Color(0xFFB22D35),Color(0xFFD8E8B9),Color(0xFF1C3526),Color(0xFFE2EFDA),Color(0xFF1C3526),listOf(Color(0xFF315B42),Color(0xFF596B29),Color(0xFFA75A2E),Color(0xFF536D88)))
 12 -> FinanceColors(Color(0xFF17603D),Color(0xFFB22D35),Color(0xFFFFD5C8),Color(0xFF482827),Color(0xFFE3EDF8),Color(0xFF253346),listOf(Color(0xFFAE4036),Color(0xFF87550A),Color(0xFF2F668F),Color(0xFF6B5690)))
 6 -> FinanceColors(Color(0xFFA7EFB5),Color(0xFFFFADA6),Color(0xFFA7EFB5),Color(0xFF182D1D),Color(0xFF272727),Color(0xFFF3F4F5),listOf(Color(0xFF9AAEFF),Color(0xFFA7EFB5),Color(0xFFFFC078),Color(0xFFC6B4EE)))
 8 -> FinanceColors(Color(0xFF17603D),Color(0xFFAD272B),Color(0xFFEADF7E),Color(0xFF3C3512),Color(0xFFC8E8F5),Color(0xFF243E51),listOf(Color(0xFF6744CB),Color(0xFF806119),Color(0xFF276783),Color(0xFF98495F)))
 9 -> FinanceColors(Color(0xFF91E8BA),Color(0xFFFFADA6),Color(0xFF313131),Color(0xFFF3F4F5),Color(0xFF272727),Color(0xFFF3F4F5),listOf(Color(0xFF68DCCD),Color(0xFFFFC078),Color(0xFF9FAAB8),Color(0xFFC6B4EE)))
 else -> FinanceColors(Color(0xFF17603D),Color(0xFFB22D35),Color(0xFFFFFEFA),Color(0xFF253346),Color(0xFFFFFEFA),Color(0xFF253346),listOf(Color(0xFF445C77),Color(0xFF946536),Color(0xFF35746D),Color(0xFF79628D)))
}
val financeColors: FinanceColors @Composable get() = financeColors(LocalAppTheme.current)
