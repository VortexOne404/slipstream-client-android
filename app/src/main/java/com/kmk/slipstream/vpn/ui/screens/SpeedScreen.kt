package com.kmk.slipstream.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmk.slipstream.vpn.ui.components.TrafficSessionRowUid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedScreen(onMenu: () -> Unit, running: Boolean) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speed") },
                navigationIcon = { IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, null) } }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(12.dp)) {
            TrafficSessionRowUid(running = running)
        }
    }
}
