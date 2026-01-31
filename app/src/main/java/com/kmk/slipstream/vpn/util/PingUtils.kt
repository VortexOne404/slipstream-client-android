package com.kmk.slipstream.vpn.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object PingUtils {

    suspend fun ping204(url: String = "https://www.google.com/generate_204"): Pair<Long?, Int?> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val start = System.nanoTime()
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                }
                conn.connect()
                val code = conn.responseCode
                runCatching { conn.inputStream?.close() }
                conn.disconnect()

                val ms = (System.nanoTime() - start) / 1_000_000
                Pair(ms, code)
            }.getOrElse { Pair(null, null) }
        }
    }
}
