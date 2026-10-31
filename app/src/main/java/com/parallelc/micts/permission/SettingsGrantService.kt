/* SPDX-License-Identifier: GPL-3.0-only */
package com.parallelc.micts.permission

import androidx.annotation.Keep
import com.parallelc.micts.BuildConfig
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** A short-lived Shizuku user service with one fixed, self-scoped operation. */
@Keep
class SettingsGrantService : ISettingsGrant.Stub() {
    override fun destroy() { exitProcess(0) }

    override fun grantSettings(userId: Int): String {
        if (android.os.Process.myUid() != 2000 || userId !in 0..9999) return "ADB service required"
        return try {
            val process = ProcessBuilder("/system/bin/pm", "grant", "--user", userId.toString(),
                BuildConfig.APPLICATION_ID, "android.permission.WRITE_SECURE_SETTINGS")
                .redirectErrorStream(true).start()
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                "Permission grant timed out"
            } else {
                val output = process.inputStream.bufferedReader().use { it.readText() }.trim().take(1200)
                if (process.exitValue() == 0) "" else output.ifBlank { "Permission grant failed" }
            }
        } catch (e: Exception) { e.message ?: "Permission grant failed" }
    }
}
