package com.kmk.slipstream.vpn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun BottomBarPingAndToggle(
    running: Boolean,
    locked: Boolean,
    pinging: Boolean,
    pingMs: Long?,
    pingCode: Int?,
    onPingClick: () -> Unit,
    onToggle: () -> Unit
) {
    val pingText = when {
        !running -> "Ping: —"
        pinging -> "Ping: ..."
        else -> {
            val msTxt = pingMs?.toString() ?: "—"
            val codeTxt = pingCode?.toString() ?: "?"
            "Ping: ${msTxt}ms (HTTP $codeTxt)"
        }
    }

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onPingClick,
                enabled = running && !pinging
            ) { Text(pingText) }

            val icon = when {
                locked -> Icons.Default.HourglassTop
                running -> Icons.Default.Stop
                else -> Icons.Default.PlayArrow
            }

            FilledIconButton(
                onClick = onToggle,
                enabled = !locked,
                modifier = Modifier.size(56.dp).clip(CircleShape)
            ) {
                Icon(icon, contentDescription = "Toggle")
            }
        }
    }
}
