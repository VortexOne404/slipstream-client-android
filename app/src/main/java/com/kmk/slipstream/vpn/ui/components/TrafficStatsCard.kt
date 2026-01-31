package com.kmk.slipstream.vpn.ui.components

import android.net.TrafficStats
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun TrafficSessionRowUid(running: Boolean) {
    val uid = remember { android.os.Process.myUid() }

    var baseRx by remember { mutableStateOf(-1L) }
    var baseTx by remember { mutableStateOf(-1L) }
    var lastRx by remember { mutableStateOf(-1L) }
    var lastTx by remember { mutableStateOf(-1L) }

    var totalRx by remember { mutableStateOf(0L) }
    var totalTx by remember { mutableStateOf(0L) }
    var rxRate by remember { mutableStateOf(0L) }
    var txRate by remember { mutableStateOf(0L) }

    LaunchedEffect(running) {
        baseRx = -1; baseTx = -1; lastRx = -1; lastTx = -1
        totalRx = 0; totalTx = 0; rxRate = 0; txRate = 0
        if (!running) return@LaunchedEffect

        val startRx = TrafficStats.getUidRxBytes(uid)
        val startTx = TrafficStats.getUidTxBytes(uid)
        if (startRx < 0 || startTx < 0) return@LaunchedEffect

        baseRx = startRx; baseTx = startTx
        lastRx = startRx; lastTx = startTx

        while (running) {
            delay(1000)
            val rx = TrafficStats.getUidRxBytes(uid)
            val tx = TrafficStats.getUidTxBytes(uid)
            if (rx < 0 || tx < 0) break

            totalRx = max(0, rx - baseRx)
            totalTx = max(0, tx - baseTx)
            rxRate = max(0, rx - lastRx)
            txRate = max(0, tx - lastTx)
            lastRx = rx; lastTx = tx
        }
    }

    fun fmtBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun fmtRate(bps: Long): String {
        val kb = bps / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.2f MB/s", mb)
            kb >= 1 -> String.format("%.1f KB/s", kb)
            else -> "$bps B/s"
        }
    }

    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Downloaded", style = MaterialTheme.typography.labelMedium)
                    Text(fmtBytes(totalRx), style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("Uploaded", style = MaterialTheme.typography.labelMedium)
                    Text(fmtBytes(totalTx), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Down: ${fmtRate(rxRate)}")
                Text("Up: ${fmtRate(txRate)}")
            }
            if (!running) {
                Spacer(Modifier.height(6.dp))
                Text("VPN is disconnected", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
