package com.saketkhundia.pocketserver.presentation.qr

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.presentation.components.EmptyState
import com.saketkhundia.pocketserver.presentation.components.MonoText
import com.saketkhundia.pocketserver.presentation.home.rememberSharedHomeViewModel
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dedicated connect screen: QR + URL + copy/share. Kept minimal.
 * QR encodes only the connection URL, never credentials.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    // Activity-shared: zero per-visit init cost, one network observer total.
    val vm = rememberSharedHomeViewModel()
    // Narrow collector: server state only. The full uiState also carries
    // stats/logs (per-request emissions during transfers) — irrelevant here.
    val serverState by vm.serverState.collectAsState()
    val clipboard = LocalClipboardManager.current

    val url = serverState.url
    val running = serverState.status == ServerStatus.RUNNING && url != null

    // QR encode is O(n²) pixel work — never on the main thread. Generates once
    // per URL on Dispatchers.Default, cached across recompositions.
    var qrBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        qrBmp = null
        val u = url ?: return@LaunchedEffect
        qrBmp = withContext(Dispatchers.Default) { QrCodeGenerator.generate(u, 640) }
    }
    // Subtle entrance: background fades, container scales 0.96 → 1.0.
    // Saveable: reopening the screen via back-stack restore must not replay it.
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!running) {
                EmptyState(
                    title = "Server is offline",
                    subtitle = "Start the server to get a connection code."
                )
            } else {
                Text("Connect to Pocket Server", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(PsSpacing.sm))
                Text(
                    "Scan this code from another device on the same Wi-Fi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(PsSpacing.xxl))
                AnimatedVisibility(
                    visible = entered,
                    enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                        scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.96f),
                    label = "qrEntrance"
                ) {
                    val bmp = qrBmp
                    if (bmp != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(20.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "QR code for $url",
                                modifier = Modifier.size(240.dp).clip(RoundedCornerShape(8.dp))
                            )
                        }
                    } else {
                        // Skeleton while encoding off-main-thread — no blank flash.
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
                Spacer(Modifier.height(PsSpacing.xl))
                MonoText(url!!)
                Spacer(Modifier.height(PsSpacing.xxl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(url)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Copy link")
                    }
                    FilledTonalButton(
                        onClick = { vm.shareUrl(ctx) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}
