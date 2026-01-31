package com.kmk.slipstream.vpn.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ConfigStorage {

    fun load(ctx: Context): MutableList<SlipstreamConfig> {
        val raw = prefs(ctx).getString(PREF_CONFIGS, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { idx ->
                val o = arr.getJSONObject(idx)
                SlipstreamConfig(
                    id = o.optString("id", "cfg_${idx}_${System.currentTimeMillis()}"),
                    name = o.optString("name", "Config ${idx + 1}"),
                    resolver = o.optString("resolver", "8.8.8.8:53"),
                    domain = o.optString("domain", ""),
                    socksAuthEnabled = o.optBoolean("socksAuthEnabled", true),
                    username = o.optString("username", ""),
                    password = o.optString("password", "")
                )
            }
        }.getOrElse { mutableListOf() }
    }

    fun save(ctx: Context, list: List<SlipstreamConfig>) {
        val arr = JSONArray()
        list.forEach { cfg ->
            arr.put(JSONObject().apply {
                put("id", cfg.id)
                put("name", cfg.name)
                put("resolver", cfg.resolver)
                put("domain", cfg.domain)
                put("socksAuthEnabled", cfg.socksAuthEnabled)
                put("username", cfg.username)
                put("password", cfg.password)
            })
        }
        prefs(ctx).edit().putString(PREF_CONFIGS, arr.toString()).apply()
    }

    fun getSelectedId(ctx: Context): String? =
        prefs(ctx).getString(PREF_SELECTED_CONFIG, null)

    fun setSelectedId(ctx: Context, id: String?) {
        prefs(ctx).edit().putString(PREF_SELECTED_CONFIG, id).apply()
    }
}
