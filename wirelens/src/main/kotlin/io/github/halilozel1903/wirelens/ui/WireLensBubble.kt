package io.github.halilozel1903.wirelens.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.wirelens.core.StatusCategory
import io.github.halilozel1903.wirelens.core.WireLensStore
import kotlin.math.roundToInt

/**
 * A draggable round button with the live request count. It pops when a new request arrives
 * and shows a red badge while there are failed or 4xx / 5xx calls.
 */
@Composable
public fun WireLensBubble(store: WireLensStore, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val records by store.records.collectAsState()
    val errors = records.count {
        it.category == StatusCategory.ClientError || it.category == StatusCategory.ServerError || it.category == StatusCategory.Failed
    }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(records.size) {
        if (records.isNotEmpty()) {
            pop.animateTo(1.15f, tween(110))
            pop.animateTo(1f, tween(220))
        }
    }
    Box(
        modifier
            .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
            .pointerInput(Unit) { detectDragGestures { change, amount -> change.consume(); drag += amount } }
            .scale(pop.value)
            .semantics { contentDescription = "Open WireLens, ${records.size} requests" },
    ) {
        Box(
            Modifier
                .size(58.dp)
                .shadow(14.dp, CircleShape, ambientColor = Color(0x805B3DF5), spotColor = Color(0x805B3DF5))
                .clip(CircleShape)
                .background(WireLensColors.Light.header)
                .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(WireIcons.Traffic, null, Modifier.size(20.dp).offset(y = (-7).dp), tint = Color.White)
            Text(
                if (records.size > 999) "999+" else records.size.toString(),
                modifier = Modifier.offset(y = 10.dp),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (errors > 0) {
            Text(
                errors.toString(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .border(2.dp, Color.White, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
