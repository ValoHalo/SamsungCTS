/* SPDX-License-Identifier: GPL-3.0-only */
package com.parallelc.micts.permission

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.R
import com.parallelc.micts.samsung.SamsungSetup
import rikka.shizuku.Shizuku

class ShizukuGrant(private val activity: Activity, private val changed: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false
    private var connection: ServiceConnection? = null
    var busy = false
        private set
    var message = ""
        private set
    private val args = Shizuku.UserServiceArgs(ComponentName(activity, SettingsGrantService::class.java))
        .daemon(false).processNameSuffix("settings_grant").tag("settings_grant").version(BuildConfig.VERSION_CODE)
    private val received = Shizuku.OnBinderReceivedListener { activity.runOnUiThread { if (!closed) changed() } }
    private val dead = Shizuku.OnBinderDeadListener {
        activity.runOnUiThread {
            if (!closed) {
                if (busy) finish(activity.getString(R.string.shizuku_not_running)) else changed()
            }
        }
    }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQUEST && !closed && busy) {
            if (result == PackageManager.PERMISSION_GRANTED) bind()
            else finish(activity.getString(R.string.shizuku_denied))
        }
    }
    private val timeout = Runnable { if (busy) finish(activity.getString(R.string.shizuku_timeout)) }

    init {
        Shizuku.addBinderReceivedListenerSticky(received)
        Shizuku.addBinderDeadListener(dead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
    }

    fun running() = Shizuku.pingBinder()

    fun request() {
        if (busy || closed) return
        if (SamsungSetup.authorized(activity)) { changed(); return }
        try {
            check(running()) { activity.getString(R.string.shizuku_not_running) }
            check(!Shizuku.isPreV11() && Shizuku.getVersion() >= 13) { activity.getString(R.string.shizuku_update) }
            check(Shizuku.getUid() == 2000) { activity.getString(R.string.shizuku_adb_mode) }
            busy = true
            message = ""
            changed()
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) bind()
            else if (Shizuku.shouldShowRequestPermissionRationale()) finish(activity.getString(R.string.shizuku_denied))
            else Shizuku.requestPermission(REQUEST)
        } catch (e: RuntimeException) { finish(e.message ?: activity.getString(R.string.shizuku_failed)) }
    }

    private fun bind() {
        if (closed) return
        val userId = android.os.Process.myUid() / 100000
        val bound = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (closed || !busy || connection !== this) return
                val currentConnection = this
                Thread({
                    val result = runCatching { ISettingsGrant.Stub.asInterface(binder).grantSettings(userId) }
                    handler.post {
                        if (!closed && busy && connection === currentConnection) {
                            val error = result.exceptionOrNull()?.message ?: result.getOrNull().orEmpty()
                            if (error.isEmpty() && SamsungSetup.authorized(activity)) {
                                SamsungSetup.clearRecoveredError(activity)
                                finish(activity.getString(R.string.shizuku_granted))
                            } else finish(error.ifBlank { activity.getString(R.string.shizuku_failed) })
                        }
                    }
                }, "samsungcts-grant").start()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                if (!closed && busy && connection === this) finish(activity.getString(R.string.shizuku_failed))
            }
        }
        connection = bound
        handler.postDelayed(timeout, 12000)
        try { Shizuku.bindUserService(args, bound) }
        catch (e: RuntimeException) { finish(e.message ?: activity.getString(R.string.shizuku_failed)) }
    }

    private fun release() {
        handler.removeCallbacks(timeout)
        connection?.let { runCatching { Shizuku.unbindUserService(args, it, true) } }
        connection = null
    }
    private fun finish(text: String) {
        busy = false
        message = text
        release()
        if (!closed) changed()
    }
    fun close() {
        closed = true
        busy = false
        release()
        Shizuku.removeBinderReceivedListener(received)
        Shizuku.removeBinderDeadListener(dead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
    }
    companion object { private const val REQUEST = 7123 }
}
