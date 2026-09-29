package io.github.halilozel1903.wirelens.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.wirelens.WireLens
import io.github.halilozel1903.wirelens.core.SampleTraffic
import io.github.halilozel1903.wirelens.ui.WireLensTheme
import kotlinx.coroutines.launch

private class DemoCall(val method: String, val title: String, val detail: String, val run: suspend () -> Result<Int>)

private val calls = listOf(
    DemoCall("GET", "Products", "A JSON list") { ShopApi.products() },
    DemoCall("GET", "Product", "One item in detail") { ShopApi.product() },
    DemoCall("POST", "Sign in", "Tokens get hidden") { ShopApi.signIn() },
    DemoCall("POST", "Add to cart", "JSON body") { ShopApi.addToCart() },
    DemoCall("GET", "Not found", "A 404") { ShopApi.notFound() },
    DemoCall("GET", "Server error", "A 500") { ShopApi.serverError() },
    DemoCall("GET", "Slow call", "Two seconds") { ShopApi.slow() },
    DemoCall("GET", "Image", "A binary body") { ShopApi.image() },
)

@Composable
fun ShopScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var last by remember { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) { Header() }
        item(span = { GridItemSpan(2) }) {
            Text(
                "Send real requests",
                modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        items(calls.size) { index ->
            val call = calls[index]
            CallCard(call, Modifier.padding(start = if (index % 2 == 0) 16.dp else 0.dp, end = if (index % 2 == 1) 16.dp else 0.dp)) {
                scope.launch {
                    last = call.run().fold({ "${call.title}: HTTP $it" }, { "${call.title}: ${it.javaClass.simpleName}" })
                }
            }
        }
        item(span = { GridItemSpan(2) }) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                last?.let { Text("Last result  ·  $it", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                WideButton("Add sample traffic (offline)") {
                    SampleTraffic.records().reversed().forEach { WireLens.store.put(it.copy(id = WireLens.store.newId())) }
                }
                WideButton("Open WireLens") { WireLens.open(context) }
            }
        }
    }
}

@Composable
private fun Header() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF111827), Color(0xFF312E81), Color(0xFF5B3DF5)))),
    ) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text("NOVA SHOP", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text("WireLens demo", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Every call below goes through OkHttp with the WireLens interceptor. Shake the phone or tap the bubble to see them.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun CallCard(call: DemoCall, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = WireLensTheme.colors.method(call.method)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                call.method,
                Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.height(14.dp))
        Text(call.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(call.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun WideButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
