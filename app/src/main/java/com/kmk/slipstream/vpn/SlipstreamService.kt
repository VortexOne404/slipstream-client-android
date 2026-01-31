package com.kmk.slipstream.vpn

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class SlipstreamService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var proc: Process? = null
    private var procJob: Job? = null

    private var logListener: ((String) -> Unit)? = null

    private val mainHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    inner class LocalBinder : Binder() {
        fun getService(): SlipstreamService = this@SlipstreamService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun startSlipstream(resolver: String, domain: String, tcpListenPort: Int) {
        if (proc != null) {
            log("Slipstream already running.")
            return
        }

        try {
            val exe = getSlipstreamExe()

            val args = listOf(
                exe.absolutePath,
                "--resolver", resolver,
                "--domain", domain,
                "--tcp-listen-port", tcpListenPort.toString()
            )

            log("Starting slipstream: ${args.joinToString(" ")}")

            val p = ProcessBuilder(args)
                .redirectErrorStream(true) // ✅ merge stdout+stderr
                .start()

            proc = p

            procJob?.cancel()
            procJob = scope.launch {
                // ✅ read everything
                streamLines(p.inputStream) { log("[SS] $it") }

                val code = try { p.waitFor() } catch (_: Throwable) { -1 }
                log("Slipstream exited with code=$code")

                proc = null
                procJob = null
            }

        } catch (t: Throwable) {
            log("Slipstream start failed: $t")
            proc = null
            procJob = null
        }
    }

    fun stopSlipstream() {
        log("Stopping slipstream...")

        val p = proc
        proc = null

        procJob?.cancel()
        procJob = null

        if (p != null) {
            try { p.destroy() } catch (_: Throwable) {}
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    if (!p.waitFor(200, TimeUnit.MILLISECONDS)) {
                        try { p.destroyForcibly() } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopSlipstream()
        scope.cancel()
    }

    private val logBuffer = ArrayDeque<String>(300)

    private fun log(msg: String) {
        val l = logListener ?: return
        mainHandler.post { l.invoke(msg) }
    }

    fun setLogListener(listener: ((String) -> Unit)?) {
        logListener = listener
        if (listener != null) {
            val copy: List<String> = synchronized(logBuffer) { logBuffer.toList() }
            copy.forEach { listener(it) }
        }
    }



    private suspend fun streamLines(input: java.io.InputStream, onLine: (String) -> Unit) {
        try {
            BufferedReader(InputStreamReader(input)).use { reader ->
                while (coroutineContext.isActive) {
                    val line = try {
                        reader.readLine()
                    } catch (_: InterruptedIOException) {
                        break // normal when stopping
                    } catch (_: IOException) {
                        break
                    }
                    if (line == null) break
                    onLine(line)
                }
            }
        } catch (_: Throwable) {
            // never crash app
        }
    }

    private fun getSlipstreamExe(): File {
        val libDir = applicationInfo.nativeLibraryDir
        val exe = File(libDir, "libslipstream.so")

        if (!exe.exists()) {
            throw IllegalStateException(
                "Slipstream binary not found at ${exe.absolutePath}"
            )
        }

        // Just in case (usually already executable)
        exe.setExecutable(true, false)

        log("Using slipstream binary: ${exe.absolutePath}")

        return exe
    }
}
