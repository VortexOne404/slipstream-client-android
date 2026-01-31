package com.kmk.slipstream.vpn.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardUtils {

    fun readText(ctx: Context): String? {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount <= 0) return null
        val item = clip.getItemAt(0)
        return item.coerceToText(ctx)?.toString()?.takeIf { it.isNotBlank() }
    }

    fun copyText(ctx: Context, label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
