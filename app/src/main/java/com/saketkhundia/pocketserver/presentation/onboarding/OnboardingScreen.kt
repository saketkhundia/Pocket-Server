package com.saketkhundia.pocketserver.presentation.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.presentation.theme.PsRadius
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import kotlinx.coroutines.launch

/**
 * Short first-launch onboarding — three calm pages, generous spacing.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketServerApp
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    fun finish() {
        scope.launch { app.container.settingsRepository.update { it.copy(onboarded = true) } }
        onDone()
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val (icon, title, subtitle) = when (page) {
                        0 -> Triple(Icons.Filled.Dns, "Pocket Server", "Your phone.\nYour files.\nYour server.")
                        1 -> Triple(Icons.Filled.Computer, "Connect any device", "Laptop → Phone\nPhone → Tablet\nDesktop → Phone")
                        else -> Triple(Icons.Filled.RocketLaunch, "One tap", "Start the server and share your URL with any device on the same Wi-Fi.")
                    }
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(PsSpacing.xxl))
                    Text(title, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(PsSpacing.md))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Dots(current = pagerState.currentPage, total = 3)
            Spacer(Modifier.height(PsSpacing.xl))
            if (pagerState.currentPage < 2) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(PsRadius.lg)
                ) { Text("Next") }
                TextButton(onClick = { finish() }) { Text("Skip") }
            } else {
                Button(
                    onClick = { finish() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(PsRadius.lg)
                ) { Text("Get started") }
            }
        }
    }
}

@Composable
private fun Dots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            val selected = i == current
            // Active dot stretches with a short ease — no popping.
            val dotWidth by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "dot$i"
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}
