package com.kmk.slipstream.vpn.data

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object ConfigCodec {

    fun toUri(cfg: SlipstreamConfig): String {
        val obj = JSONObject().apply {
            put("id", cfg.id)
            put("name", cfg.name)
            put("resolver", cfg.resolver)
            put("domain", cfg.domain)
            put("socksAuthEnabled", cfg.socksAuthEnabled)
            put("username", cfg.username)
            put("password", cfg.password)
        }
        val bytes = obj.toString().toByteArray(StandardCharsets.UTF_8)
        val b64 = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
        return "slipstream://$b64"
    }

    fun fromUri(uri: String): SlipstreamConfig? {
        val s = uri.trim()
        if (!s.startsWith("slipstream://")) return null
        val b64 = s.removePrefix("slipstream://")

        return runCatching {
            val bytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
            val obj = JSONObject(String(bytes, StandardCharsets.UTF_8))

            SlipstreamConfig(
                id = obj.optString("id", "cfg_${System.currentTimeMillis()}"),
                name = obj.optString("name", "Imported"),
                resolver = obj.optString("resolver", "8.8.8.8:53"),
                domain = obj.getString("domain"),
                socksAuthEnabled = obj.optBoolean("socksAuthEnabled", true),
                username = obj.optString("username", ""),
                password = obj.optString("password", "")
            )
        }.getOrNull()
    }

    fun extractUriFromText(anyText: String): String? {
        val t = anyText.trim()
        val idx = t.indexOf("slipstream://")
        if (idx < 0) return null
        val sub = t.substring(idx)
        val end = sub.indexOfFirst { it.isWhitespace() }
        return if (end > 0) sub.substring(0, end) else sub
    }
}
