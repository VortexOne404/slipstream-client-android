package com.kmk.slipstream.vpn.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmk.slipstream.vpn.data.prefs
import kotlinx.coroutines.launch
import androidx.core.content.edit

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsListScreen(
    context: Context,
    onMenu: () -> Unit,
) {
    val sharedPrefs = prefs(context)
    val savedSet = sharedPrefs.getStringSet("dns_list", emptySet()) ?: emptySet()

    var dnsList by remember { mutableStateOf(savedSet.toList()) } // immutable list
    var newDns by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun showMsg(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg, withDismissAction = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DNS List") },
                navigationIcon = { IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(Modifier.padding(inner).padding(12.dp)) {

            // Input for new DNS
            OutlinedTextField(
                value = newDns,
                onValueChange = { newDns = it },
                label = { Text("New DNS (e.g., 8.8.8.8 or 1.1.1.1:53)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val dns = newDns.trim()
                    if (dns.isNotEmpty()) {
                        dnsList = dnsList + dns // create new list
                        sharedPrefs.edit { putStringSet("dns_list", dnsList.toSet()) }
                        newDns = ""
                        showMsg("DNS added")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add DNS")
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                itemsIndexed(dnsList) { index, dns ->
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dns)
                            IconButton(onClick = {
                                dnsList = dnsList.toMutableList().also { it.removeAt(index) }
                                sharedPrefs.edit { putStringSet("dns_list", dnsList.toSet()) }
                                showMsg("DNS removed")
                            }) {
                                Icon(Icons.Filled.Delete, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

