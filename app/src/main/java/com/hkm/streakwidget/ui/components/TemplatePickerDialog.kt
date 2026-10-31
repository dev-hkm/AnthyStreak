package com.hkm.streakwidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkm.streakwidget.R
import com.hkm.streakwidget.domain.HabitTemplate
import com.hkm.streakwidget.domain.HabitTemplates
import com.hkm.streakwidget.model.GoalType
import com.hkm.streakwidget.ui.theme.AppIcons

/** A starting point should feel like a suggestion, never a prescription. */
@Composable
fun TemplatePickerDialog(
    onDismiss: () -> Unit,
    onPick: (HabitTemplate) -> Unit,
    onHaptic: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(start = 20.dp, top = 18.dp, end = 12.dp, bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.editor_templates_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            stringResource(R.string.editor_templates_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = { onHaptic(); onDismiss() }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            AppIcons.X,
                            contentDescription = stringResource(R.string.common_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HabitTemplates.all.forEach { template ->
                        TemplateRow(template = template) {
                            onHaptic()
                            onPick(template)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(template: HabitTemplate, onClick: () -> Unit) {
    val icon = templateIcon(template)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp).size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(template.nameRes),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(template.noteRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            AppIcons.ChevronRight,
            contentDescription = stringResource(R.string.template_use),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun templateIcon(template: HabitTemplate): ImageVector = when (template.id) {
    "water" -> AppIcons.Water
    "read" -> AppIcons.Book
    "move" -> AppIcons.Strength
    "meditate" -> AppIcons.Focus
    "sleep" -> AppIcons.Sleep
    else -> if (template.goalType == GoalType.CHECKLIST) AppIcons.ListChecks else AppIcons.Pencil
}
