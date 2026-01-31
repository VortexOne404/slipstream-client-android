package com.kmk.slipstream.vpn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kmk.slipstream.vpn.data.SlipstreamConfig

private fun validateResolver(resolver: String): String? {
    val r = resolver.trim()
    if (r.isEmpty()) return "Resolver is empty."
    if (!r.contains(":")) return "Resolver must be host:port"
    val parts = r.split(":", limit = 2)
    if (parts[0].isBlank()) return "Resolver host is empty."
    val port = parts[1].toIntOrNull() ?: return "Resolver port is not a number."
    if (port !in 1..65535) return "Resolver port must be 1..65535."
    return null
}

private fun validateConfig(cfg: SlipstreamConfig): String? {
    validateResolver(cfg.resolver)?.let { return it }
    if (cfg.name.trim().isEmpty()) return "Name is empty."
    if (cfg.domain.trim().isEmpty()) return "Domain is empty."
    if (cfg.socksAuthEnabled) {
        if (cfg.username.trim().isEmpty()) return "SOCKS username is empty."
        if (cfg.password.isEmpty()) return "SOCKS password is empty."
    }
    return null
}

@Composable
fun AddOrEditConfigDialog(
    modeTitle: String,
    initial: SlipstreamConfig?,
    onDismiss: () -> Unit,
    onSave: (SlipstreamConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "Manual") }
    var resolver by remember { mutableStateOf(initial?.resolver ?: "8.8.8.8:53") }
    var domain by remember { mutableStateOf(initial?.domain ?: "") }
    var authEnabled by remember { mutableStateOf(initial?.socksAuthEnabled ?: true) }
    var user by remember { mutableStateOf(initial?.username ?: "") }
    var pass by remember { mutableStateOf(initial?.password ?: "") }

    var showPass by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(modeTitle) },
        text = {
            Column {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(resolver, { resolver = it }, label = { Text("DNS Resolver (host:port)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(domain, { domain = it }, label = { Text("Domain") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(8.dp))
                Row {
                    Switch(checked = authEnabled, onCheckedChange = { authEnabled = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (authEnabled) "SOCKS Auth ON" else "SOCKS Auth OFF")
                }

                if (authEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        pass, { pass = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showPass = !showPass }) { Text(if (showPass) "Hide" else "Show") }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cfg = SlipstreamConfig(
                    id = initial?.id ?: "cfg_${System.currentTimeMillis()}",
                    name = name.trim(),
                    resolver = resolver.trim(),
                    domain = domain.trim(),
                    socksAuthEnabled = authEnabled,
                    username = user.trim(),
                    password = pass
                )
                validateConfig(cfg)?.let { error = it; return@TextButton }
                onSave(cfg)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
