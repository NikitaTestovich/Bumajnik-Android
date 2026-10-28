package com.personalfinance.tracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.personalfinance.tracker.R
import kotlinx.coroutines.delay

internal fun HomeBlock.label() = when (this) {
    HomeBlock.SUMMARY -> R.string.home_balance
    HomeBlock.QUICK_ACTIONS -> R.string.home_quick_actions
    HomeBlock.GOAL -> R.string.home_goal
    HomeBlock.LATEST -> R.string.home_latest
    HomeBlock.EXPENSES -> R.string.home_expense_breakdown
}

@Composable
fun HomeCustomizationSheet(settings: List<HomeBlockSetting>, onSave: (List<HomeBlockSetting>) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(settings) }
    var starting by remember { mutableStateOf(settings) }
    var dragging by remember { mutableStateOf<HomeBlock?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    val bounds = remember { mutableStateMapOf<HomeBlock, Rect>() }
    val latestSave by rememberUpdatedState(onSave)
    val listState = rememberLazyListState()
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(settings) { if (dragging == null) draft = settings }
    LaunchedEffect(dragging, dragY) {
        if (dragging == null) return@LaunchedEffect
        while (true) {
            val distance = when {
                dragY < viewport.top + 70 -> -18f
                dragY > viewport.bottom - 70 -> 18f
                else -> 0f
            }
            if (distance == 0f) break
            listState.scrollBy(distance)
            val target = bounds.entries.filter { it.value.overlaps(viewport) }.minByOrNull { kotlin.math.abs(it.value.center.y - dragY) }?.key
            if (target != null) draft = moveHomeBlock(draft, dragging!!, draft.indexOfFirst { it.id == target })
            delay(16)
        }
    }
    Dialog(onDismissRequest = { if (dragging == null) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.home_customize), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.home_customize_hint), style = MaterialTheme.typography.bodySmall)
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).testTag("home_settings_list").onGloballyPositioned { viewport = it.boundsInRoot() }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(draft, key = { _, item -> item.id.key }) { index, item ->
                        DisposableEffect(item.id) { onDispose { bounds.remove(item.id) } }
                        val label = stringResource(item.id.label())
                        val dragLabel = stringResource(R.string.home_drag, label)
                        val upLabel = "${stringResource(R.string.home_move_up)}: $label"
                        val downLabel = "${stringResource(R.string.home_move_down)}: $label"
                        Card(border = BorderStroke(if (dragging == item.id) 2.dp else 1.dp, if (dragging == item.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(if (dragging == item.id) 8.dp else 0.dp),
                            modifier = Modifier.fillMaxWidth().testTag("home_setting_${item.id.key}").onGloballyPositioned { bounds[item.id] = it.boundsInRoot() }) {
                            Column(Modifier.padding(10.dp)) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text("≡", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.size(48.dp).semantics { contentDescription = dragLabel }.testTag("home_drag_${item.id.key}")
                                        .pointerInput(item.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { starting = draft; dragging = item.id; dragY = bounds[item.id]?.center?.y ?: 0f },
                                                onDragCancel = { draft = starting; dragging = null },
                                                onDragEnd = { dragging = null; latestSave(draft) },
                                                onDrag = { change, delta ->
                                                    change.consume(); dragY += delta.y
                                                    val target = bounds.entries.filter { it.value.overlaps(viewport) }.minByOrNull { kotlin.math.abs(it.value.center.y - dragY) }?.key
                                                    if (target != null) draft = moveHomeBlock(draft, item.id, draft.indexOfFirst { it.id == target })
                                                })
                                        })
                                    Text(label, Modifier.weight(1f))
                                    Switch(checked = item.visible, enabled = dragging == null, onCheckedChange = { visible -> onSave(draft.map { if (it.id == item.id) it.copy(visible = visible) else it }) }, modifier = Modifier.testTag("home_visible_${item.id.key}").semantics { contentDescription = label })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(enabled = index > 0 && dragging == null, onClick = { onSave(moveHomeBlock(draft, item.id, index - 1)) }, modifier = Modifier.testTag("home_up_${item.id.key}").semantics { contentDescription = upLabel }) { Text(stringResource(R.string.home_move_up)) }
                                    TextButton(enabled = index < draft.lastIndex && dragging == null, onClick = { onSave(moveHomeBlock(draft, item.id, index + 1)) }, modifier = Modifier.testTag("home_down_${item.id.key}").semantics { contentDescription = downLabel }) { Text(stringResource(R.string.home_move_down)) }
                                    if (dragging == item.id) Text("${index + 1}/${draft.size}", Modifier.padding(8.dp), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { confirmReset = true }, enabled = dragging == null) { Text(stringResource(R.string.home_reset)) }
                    TextButton(onClick = onDismiss, enabled = dragging == null) { Text(stringResource(R.string.home_done)) }
                }
            }
        }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text(stringResource(R.string.home_reset)) }, text = { Text(stringResource(R.string.home_reset_confirm)) }, confirmButton = { TextButton(onClick = { onReset(); confirmReset = false }) { Text(stringResource(R.string.home_reset)) } }, dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.home_done)) } })
}

