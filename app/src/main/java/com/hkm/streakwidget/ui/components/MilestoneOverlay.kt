package com.hkm.streakwidget.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hkm.streakwidget.R
import com.hkm.streakwidget.ui.MilestoneEvent
import com.hkm.streakwidget.ui.theme.AppIcons

/** A quiet celebration for the few streak lengths worth marking. */
@Composable
fun MilestoneOverlay(
    event: MilestoneEvent,
    reduceMotion: Boolean,
    onDismiss: () -> Unit,
    onHaptic: () -> Unit
) {
    Dialog(
        onDismissRequest = { onHaptic(); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val scale = remember(event.habitId, event.days) {
            Animatable(if (reduceMotion) 1f else 0.86f)
        }
        LaunchedEffect(event.habitId, event.days, reduceMotion) {
            if (reduceMotion) {
                scale.snapTo(1f)
            } else {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
                )
            }
        }
        Surface(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = scale.value.coerceIn(0f, 1f)
                },
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = AppIcons.Trophy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(18.dp).size(34.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.milestone_eyebrow),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.milestone_title, event.days),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.milestone_body, event.habitName, event.days),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { onHaptic(); onDismiss() },
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(stringResource(R.string.milestone_continue), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
