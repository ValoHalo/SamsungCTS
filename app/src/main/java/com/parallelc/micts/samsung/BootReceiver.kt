/* SPDX-License-Identifier: GPL-3.0-only */
package com.parallelc.micts.samsung

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED) || !SamsungSetup.enabled(context)) return
        val pending = goAsync()
        val app = context.applicationContext
        Thread({
            try {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) SamsungSetup.markBoot(app)
                SamsungSetup.apply(app, "boot")
                Thread.sleep(3000)
                SamsungSetup.apply(app, "boot_recheck")
            } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            finally { pending.finish() }
        }, "samsungcts-boot").start()
    }
}
