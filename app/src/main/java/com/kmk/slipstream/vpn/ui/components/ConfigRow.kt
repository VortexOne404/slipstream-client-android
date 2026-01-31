package com.kmk.slipstream.vpn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmk.slipstream.vpn.data.SlipstreamConfig

private fun maskDomain(d: String): String {
    val s = d.trim()
    if (s.length <= 6) return s
    return "${s.take(3)}***${s.takeLast(3)}"
}

@Composable
fun ConfigRow(
    cfg: SlipstreamConfig,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = if (selected) 3.dp else 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect, enabled = enabled)
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(cfg.name, style = MaterialTheme.typography.titleMedium)
                Text(maskDomain(cfg.domain), style = MaterialTheme.typography.bodySmall)
            }

            Box {
                IconButton(onClick = { if (enabled) menu = true }, enabled = enabled) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Copy") }, onClick = { menu = false; onCopy() })
                    DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; onShare() })
                }
            }

            IconButton(onClick = onEdit, enabled = enabled) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
