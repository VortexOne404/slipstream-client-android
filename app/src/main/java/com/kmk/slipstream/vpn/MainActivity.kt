package com.kmk.slipstream.vpn

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kmk.slipstream.vpn.data.SlipstreamConfig
import com.kmk.slipstream.vpn.ui.nav.DrawerPage
import com.kmk.slipstream.vpn.ui.screens.DnsListScreen
import com.kmk.slipstream.vpn.ui.screens.HomeScreen
import com.kmk.slipstream.vpn.ui.screens.InfoScreen
import com.kmk.slipstream.vpn.ui.screens.LogsScreen
import com.kmk.slipstream.vpn.ui.screens.SpeedScreen
import com.kmk.slipstream.vpn.ui.theme.SlipstreamVpnTheme
import kotlinx.coroutines.launch




class MainActivity : ComponentActivity() {

    private var slipstream: SlipstreamService? = null
    private var bound = false
    private var uiLogSink: ((String) -> Unit)? = null

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val b = service as? SlipstreamService.LocalBinder ?: return
            slipstream = b.getService()
            bound = true
            uiLogSink?.let { slipstream?.setLogListener(it) }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            slipstream = null
        }
    }

    private var pendingStart: (() -> Unit)? = null
    private var pendingStateSetter: ((VpnUiState) -> Unit)? = null

    private val vpnPrepare =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) pendingStart?.invoke()
            else pendingStateSetter?.invoke(VpnUiState.DISCONNECTED)
            pendingStart = null
            pendingStateSetter = null
        }

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindService(Intent(this, SlipstreamService::class.java), conn, Context.BIND_AUTO_CREATE)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            SlipstreamVpnTheme {

                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                fun alert(msg: String) {
                    scope.launch { snackbarHostState.showSnackbar(msg, withDismissAction = true) }
                }

                val vpnState = remember { mutableStateOf(VpnUiState.DISCONNECTED) }
                val statusReason = remember { mutableStateOf<String?>(null) }

                // Listen for service status broadcast
                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            if (intent?.action != SlipstreamVpnService.ACTION_STATUS) return
                            val s = intent.getStringExtra(SlipstreamVpnService.EXTRA_STATUS) ?: return
                            val r = intent.getStringExtra(SlipstreamVpnService.EXTRA_STATUS_REASON)
                            vpnState.value = runCatching { VpnUiState.valueOf(s) }.getOrDefault(VpnUiState.ERROR)
                            statusReason.value = r

                            // optional auto alerts (if you want)
                            when (vpnState.value) {
                                VpnUiState.CONNECTED -> alert("Connected")
                                VpnUiState.DISCONNECTED -> alert("Disconnected")
                                else -> {}
                            }
                        }
                    }
                    val f = IntentFilter(SlipstreamVpnService.ACTION_STATUS)
                    ContextCompat.registerReceiver(
                        /* context = */ this@MainActivity,
                        /* receiver = */ receiver,
                        /* filter = */ f,
                        /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    onDispose { runCatching { unregisterReceiver(receiver) } }
                }

                val logs = remember { mutableStateListOf<String>() }
                fun addLog(s: String) {
                    scope.launch {
                        if (logs.size > 1000) logs.removeRange(logs.size - 200, logs.size)
                        logs.add(0, s)
                    }
                }

                LaunchedEffect(Unit) {
                    uiLogSink = ::addLog
                    slipstream?.setLogListener(::addLog)
                }

                fun connectWithConfig(cfg: SlipstreamConfig, setState: (VpnUiState) -> Unit) {
                    val prep = android.net.VpnService.prepare(this@MainActivity)

                    val start = {
                        val prefs = getSharedPreferences("vpn", Context.MODE_PRIVATE)
                        val dnsList = prefs.getStringSet("dns_list", emptySet())?.toList() ?: emptyList()

                        val i = Intent(this@MainActivity, SlipstreamVpnService::class.java).apply {
                            action = SlipstreamVpnService.ACTION_CONNECT
                            putExtra(SlipstreamVpnService.EXTRA_DOMAIN, cfg.domain.trim())
                            putExtra(SlipstreamVpnService.EXTRA_SOCKS5_AUTH_ENABLED, cfg.socksAuthEnabled)
                            putExtra(SlipstreamVpnService.EXTRA_SOCKS5_AUTH_USERNAME, cfg.username.trim())
                            putExtra(SlipstreamVpnService.EXTRA_SOCKS5_AUTH_PASSWORD, cfg.password)
                            putExtra(SlipstreamVpnService.EXTRA_RESOLVER_LIST, dnsList.toTypedArray()) // ✅ آرایه DNS
                        }

                        setState(VpnUiState.CONNECTING)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
                        addLog("VPN start requested with DNS: $dnsList")
                    }

                    if (prep != null) {
                        pendingStart = start
                        pendingStateSetter = setState
                        vpnPrepare.launch(prep)
                    } else start()
                }


                fun disconnect(setState: (VpnUiState) -> Unit) {
                    setState(VpnUiState.DISCONNECTING)
                    val i = Intent(this@MainActivity, SlipstreamVpnService::class.java).apply {
                        action = SlipstreamVpnService.ACTION_DISCONNECT
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
                    slipstream?.stopSlipstream()
                    addLog("VPN stop requested.")
                }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                var page by remember { mutableStateOf(DrawerPage.HOME) }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Text("Slipstream", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

                                NavigationDrawerItem(
                                    label = { Text("Home") },
                                    selected = page == DrawerPage.HOME,
                                    onClick = { page = DrawerPage.HOME; scope.launch { drawerState.close() } },
                                    icon = { Icon(Icons.Default.Home, null) }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Speed") },
                                    selected = page == DrawerPage.SPEED,
                                    onClick = { page = DrawerPage.SPEED; scope.launch { drawerState.close() } },
                                    icon = { Icon(Icons.Default.Speed, null) }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Logs") },
                                    selected = page == DrawerPage.LOGS,
                                    onClick = { page = DrawerPage.LOGS; scope.launch { drawerState.close() } },
                                    icon = { Icon(Icons.Default.List, null) }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Dns") },
                                    selected = page == DrawerPage.DNS,
                                    onClick = { page = DrawerPage.DNS; scope.launch { drawerState.close() } },
                                    icon = { Icon(Icons.Default.Dns, null) }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Info") },
                                    selected = page == DrawerPage.INFO,
                                    onClick = { page = DrawerPage.INFO; scope.launch { drawerState.close() } },
                                    icon = { Icon(Icons.Default.Info, null) }
                                )

                                Spacer(Modifier.height(12.dp))
                                Divider()
                                Spacer(Modifier.height(12.dp))

                                DrawerLinkItem("GitHub", "Project repository", Icons.Default.Launch, "https://github.com/VortexOne404/slipstream-client-android", this@MainActivity)
                                DrawerLinkItem("Telegram", "@VortexOne", Icons.Default.Send, "https://t.me/VortexOne", this@MainActivity)
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        when (page) {
                            DrawerPage.HOME -> HomeScreen(
                                context = this@MainActivity,
                                onMenu = { scope.launch { drawerState.open() } },
                                vpnState = vpnState.value,
                                statusReason = statusReason.value,
                                onConnect = { cfg ->
                                    connectWithConfig(cfg) {
                                        vpnState.value = it
                                    }
                                },
                                onDisconnect = { disconnect { vpnState.value = it } },
                                addLog = ::addLog,
                                alertMessage = ::alert
                            )

                            DrawerPage.SPEED -> SpeedScreen(
                                onMenu = { scope.launch { drawerState.open() } },
                                running = vpnState.value == VpnUiState.CONNECTED
                            )

                            DrawerPage.LOGS -> LogsScreen(
                                onMenu = { scope.launch { drawerState.open() } },
                                vpnState = vpnState.value,
                                statusReason = statusReason.value,
                                logs = logs
                            )

                            DrawerPage.DNS -> DnsListScreen(
                                context = this@MainActivity,
                                onMenu = { scope.launch { drawerState.open() } },
                            )

                            DrawerPage.INFO -> InfoScreen(
                                onMenu = { scope.launch { drawerState.open() } },
                                githubUrl = "https://github.com/VortexOne404/slipstream-client-android",
                                telegramUrl = "https://t.me/VortexOne"
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        slipstream?.setLogListener(null)
        uiLogSink = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) runCatching { unbindService(conn) }
        bound = false
    }
}

@Composable
private fun DrawerLinkItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    url: String,
    context: Context
) {
    NavigationDrawerItem(
        label = {
            Column {
                Text(title)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        },
        selected = false,
        onClick = {
            val i = Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse(url) }
            runCatching { context.startActivity(i) }
        },
        icon = { Icon(icon, null) }
    )
}
