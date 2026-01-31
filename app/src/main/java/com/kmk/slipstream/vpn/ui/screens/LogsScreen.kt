package com.kmk.slipstream.vpn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmk.slipstream.vpn.VpnUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onMenu: () -> Unit,
    vpnState: VpnUiState,
    statusReason: String?,
    logs: List<String>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                navigationIcon = { IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, null) } }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(12.dp)) {

            Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text("State: $vpnState", style = MaterialTheme.typography.titleSmall)
                    statusReason?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                LazyColumn(Modifier.fillMaxSize().padding(10.dp)) {
                    items(logs.take(400)) { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
