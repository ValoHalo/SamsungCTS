/* Based on MiCTS by parallelcc; Samsung adaptation by ValoHalo.
 * SPDX-License-Identifier: GPL-3.0-only */
package com.parallelc.micts.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.parallelc.micts.samsung.SamsungSetup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("PrivateApi")
fun triggerCircleToSearch(): Boolean = runCatching {
    val arguments = Bundle().apply {
        putLong("invocation_time_ms", SystemClock.elapsedRealtime())
        putInt("omni.entry_point", 1)
    }
    val binder = Class.forName("android.os.ServiceManager")
        .getMethod("getService", String::class.java).invoke(null, "voiceinteraction") as? IBinder
        ?: return@runCatching false
    // AOSP Android 13–17 and the tested Samsung framework use transaction 3.
    // Android 14 added attributionTag. Keep this protocol scoped and fail closed.
    if (Build.VERSION.SDK_INT !in 33..37) return@runCatching false
    val data = Parcel.obtain()
    val reply = Parcel.obtain()
    try {
        data.writeInterfaceToken("com.android.internal.app.IVoiceInteractionManagerService")
        data.writeStrongBinder(null)
        data.writeTypedObject(arguments, 0)
        data.writeInt(7)
        if (Build.VERSION.SDK_INT >= 34) data.writeString("SamsungCTS")
        if (!binder.transact(3, data, reply, 0)) return@runCatching false
        reply.readException()
        reply.readBoolean()
    } finally {
        reply.recycle()
        data.recycle()
    }
}.onFailure { Log.e("SamsungCTS", "Circle to Search invocation failed", it) }.getOrDefault(false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false
        lifecycleScope.launch {
            if (SamsungSetup.enabled(this@MainActivity) && !SamsungSetup.configured(this@MainActivity)) {
                SamsungSetup.apply(this@MainActivity, "launch")
                delay(250)
            }
            delay(if (intent.getBooleanExtra("from_tile", false)) 400 else 80)
            if (!triggerCircleToSearch()) {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java).putExtra("trigger_error", true))
            }
            finish()
        }
    }
}
