package com.kmk.slipstream.vpn.data

import android.content.Context

internal fun prefs(ctx: Context) = ctx.getSharedPreferences("vpn", Context.MODE_PRIVATE)

internal const val PREF_CONFIGS = "configs_v3"
internal const val PREF_SELECTED_CONFIG = "selected_cfg"
