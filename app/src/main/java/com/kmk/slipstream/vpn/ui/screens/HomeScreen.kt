package com.kmk.slipstream.vpn.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmk.slipstream.vpn.VpnUiState
import com.kmk.slipstream.vpn.data.ConfigCodec
import com.kmk.slipstream.vpn.data.ConfigStorage
import com.kmk.slipstream.vpn.data.SlipstreamConfig
import com.kmk.slipstream.vpn.ui.components.AddOrEditConfigDialog
import com.kmk.slipstream.vpn.ui.components.BottomBarPingAndToggle
import com.kmk.slipstream.vpn.ui.components.ConfigRow
import com.kmk.slipstream.vpn.util.ClipboardUtils
import com.kmk.slipstream.vpn.util.PingUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    context: Context,
    onMenu: () -> Unit,
    vpnState: VpnUiState,
    statusReason: String?,
    onConnect: (SlipstreamConfig) -> Unit,
    onDisconnect: () -> Unit,
    addLog: (String) -> Unit,
    alertMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var configs by remember { mutableStateOf(ConfigStorage.load(context)) }
    var selectedId by remember { mutableStateOf(ConfigStorage.getSelectedId(context)) }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SlipstreamConfig?>(null) }

    var pinging by remember { mutableStateOf(false) }
    var pingMs by remember { mutableStateOf<Long?>(null) }
    var pingCode by remember { mutableStateOf<Int?>(null) }

    val running = vpnState == VpnUiState.CONNECTED
    val locked = vpnState == VpnUiState.CONNECTING || vpnState == VpnUiState.DISCONNECTING

    fun setSelected(id: String) {
        selectedId = id
        ConfigStorage.setSelectedId(context, id)
    }

    fun saveAll(list: List<SlipstreamConfig>) {
        configs = list.toMutableList()
        ConfigStorage.save(context, configs)
    }

    fun importFromClipboard() {
        val raw = ClipboardUtils.readText(context)
        if (raw == null) {
            addLog("ERROR: Clipboard empty")
            alertMessage("Clipboard is empty")
            return
        }
        val uri = ConfigCodec.extractUriFromText(raw)
        if (uri == null) {
            addLog("ERROR: No slipstream:// found")
            alertMessage("No slipstream:// found")
            return
        }
        val parsed = ConfigCodec.fromUri(uri)
        if (parsed == null) {
            addLog("ERROR: Invalid config")
            alertMessage("Invalid config")
            return
        }

        // ✅ Always add as NEW (never replace old ones)
        val cfg = parsed.copy(
            id = "cfg_${System.currentTimeMillis()}",
            name = parsed.name.ifBlank { "Imported" }
        )

        val list = configs.toMutableList()
        list.add(0, cfg)
        saveAll(list)
        setSelected(cfg.id)

        addLog("Imported: ${cfg.name}")
        alertMessage("Config imported: ${cfg.name}")
    }

    fun upsert(cfg: SlipstreamConfig) {
        val list = configs.toMutableList()
        val idx = list.indexOfFirst { it.id == cfg.id }
        if (idx >= 0) list[idx] = cfg else list.add(0, cfg)
        saveAll(list)
        setSelected(cfg.id)
        alertMessage("Config saved: ${cfg.name}")
    }

    fun delete(cfg: SlipstreamConfig) {
        val list = configs.toMutableList().apply { removeAll { it.id == cfg.id } }
        saveAll(list)
        if (selectedId == cfg.id) {
            val next = list.firstOrNull()?.id
            selectedId = next
            ConfigStorage.setSelectedId(context, next)
        }
        alertMessage("Deleted: ${cfg.name}")
    }

    fun copy(cfg: SlipstreamConfig) {
        val uri = ConfigCodec.toUri(cfg)
        ClipboardUtils.copyText(context, "slipstream", uri)
        addLog("Copied: ${cfg.name}")
        alertMessage("Config copied: ${cfg.name}")
    }

    fun share(cfg: SlipstreamConfig) {
        val uri = ConfigCodec.toUri(cfg)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "Share config")) }
        addLog("Share opened: ${cfg.name}")
        alertMessage("Share: ${cfg.name}")
    }

    val selectedCfg = configs.firstOrNull { it.id == selectedId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                navigationIcon = { IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, null) } },
                actions = {
                    IconButton(onClick = { importFromClipboard() }, enabled = !locked) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                    IconButton(onClick = { showAdd = true }, enabled = !locked) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        },
        bottomBar = {
            BottomBarPingAndToggle(
                running = running,
                locked = locked,
                pinging = pinging,
                pingMs = pingMs,
                pingCode = pingCode,
                onPingClick = {
                    if (!running || pinging) return@BottomBarPingAndToggle
                    scope.launch {
                        pinging = true
                        val (ms, code) = PingUtils.ping204()
                        pingMs = ms
                        pingCode = code
                        pinging = false
                        alertMessage("Ping: ${ms ?: "-"}ms (HTTP ${code ?: "?"})")
                    }
                },
                onToggle = {
                    if (locked) return@BottomBarPingAndToggle
                    if (running) {
                        alertMessage("Disconnecting...")
                        onDisconnect()
                    } else {
                        val cfg = selectedCfg
                        if (cfg == null) {
                            alertMessage("No config selected")
                            return@BottomBarPingAndToggle
                        }
                        alertMessage("Connecting...")
                        onConnect(cfg)
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(12.dp)) {

            statusReason?.takeIf { it.isNotBlank() }?.let {
                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Text(it, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
            }

            if (configs.isEmpty()) {
                Text("No configs. Paste or Add.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(configs) { cfg ->
                        ConfigRow(
                            cfg = cfg,
                            selected = cfg.id == selectedId,
                            enabled = !locked,
                            onSelect = { setSelected(cfg.id) },
                            onCopy = { copy(cfg) },
                            onShare = { share(cfg) },
                            onEdit = { editing = cfg },
                            onDelete = { delete(cfg) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddOrEditConfigDialog(
            modeTitle = "Add Config",
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { upsert(it); showAdd = false }
        )
    }

    if (editing != null) {
        val init = editing!!
        AddOrEditConfigDialog(
            modeTitle = "Edit Config",
            initial = init,
            onDismiss = { editing = null },
            onSave = { upsert(it); editing = null }
        )
    }
}
