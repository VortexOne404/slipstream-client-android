package com.kmk.slipstream.vpn.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    onMenu: () -> Unit,
    githubUrl: String,
    telegramUrl: String
) {
    val ctx = LocalContext.current
    fun open(url: String) {
        val i = Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse(url) }
        runCatching { ctx.startActivity(i) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Info") },
                navigationIcon = { IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, null) } }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(12.dp)) {
            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Slipstream Client", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("• Configs: slipstream://{base64}", style = MaterialTheme.typography.bodySmall)
                    Text("• Ping is manual (tap Ping button)", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = { open(githubUrl) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Launch, null)
                Spacer(Modifier.width(8.dp))
                Text("Open GitHub")
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = { open(telegramUrl) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(8.dp))
                Text("Open Telegram")
            }
        }
    }
}
