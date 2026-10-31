/* SPDX-License-Identifier: GPL-3.0-only */
package com.parallelc.micts.samsung

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.parallelc.micts.R
import com.parallelc.micts.ui.activity.MainActivity

object SamsungSetup {
    const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
    const val GOOGLE_SERVICE = "$GOOGLE_PACKAGE/com.google.android.voiceinteraction.GsaVoiceInteractionService"
    const val NAV_FLAGS = "navigationbar_splugin_flags"
    private val secureKeys = listOf("assistant", "voice_interaction_service")
    private fun prefs(c: Context) = c.getSharedPreferences("samsung_setup", Context.MODE_PRIVATE)
    fun isSamsung() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    fun authorized(c: Context) = c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    fun enabled(c: Context) = prefs(c).getBoolean("enabled", false)
    fun hasBackup(c: Context) = prefs(c).getBoolean("has_backup", false)
    fun backgroundAllowed(c: Context) = !c.getSystemService(ActivityManager::class.java).isBackgroundRestricted
    fun read(c: Context, key: String): String? = Settings.Secure.getString(c.contentResolver, key)
    fun triggerComponent(c: Context) = ComponentName(c, MainActivity::class.java).flattenToString()
    fun packagePresent(c: Context, name: String): Boolean = try {
        c.packageManager.getApplicationInfo(name, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) { false }
    fun configured(c: Context) = read(c, "assistant") == triggerComponent(c) &&
        read(c, "voice_interaction_service") == GOOGLE_SERVICE &&
        (Settings.Global.getInt(c.contentResolver, NAV_FLAGS, 0) and 8) != 0
    fun lastError(c: Context) = prefs(c).getString("last_error", "").orEmpty()
    fun lastBoot(c: Context) = prefs(c).getLong("last_boot", 0)
    fun clearRecoveredError(c: Context) {
        if (authorized(c) && configured(c)) prefs(c).edit().remove("last_error").apply()
    }

    private fun snapshot(editor: SharedPreferences.Editor, key: String, value: String?) {
        editor.putBoolean("${key}_null", value == null).putString(key, value.orEmpty())
    }
    private fun saved(c: Context, key: String): String? =
        if (prefs(c).getBoolean("${key}_null", true)) null else prefs(c).getString(key, "")
    private fun putSecure(c: Context, key: String, value: String?) {
        check(Settings.Secure.putString(c.contentResolver, key, value)) { c.getString(R.string.setup_write_failed) }
    }

    @Synchronized
    fun enable(c: Context): Result<Unit> = runCatching {
        check(isSamsung()) { c.getString(R.string.setup_samsung_only) }
        check(authorized(c)) { c.getString(R.string.setup_permission_missing) }
        check(packagePresent(c, GOOGLE_PACKAGE)) { c.getString(R.string.setup_google_missing) }
        check(Settings.Secure.getInt(c.contentResolver, "navigation_mode", 0) == 2) { c.getString(R.string.setup_gesture_navigation) }
        if (!enabled(c)) {
            val backup = prefs(c).edit().putBoolean("has_backup", true)
            secureKeys.forEach { snapshot(backup, "original_$it", read(c, it)) }
            snapshot(backup, "original_flags", Settings.Global.getString(c.contentResolver, NAV_FLAGS))
            check(backup.commit()) { c.getString(R.string.setup_write_failed) }
        }
        check(prefs(c).edit().putBoolean("enabled", true).commit()) { c.getString(R.string.setup_write_failed) }
        apply(c, "manual").getOrThrow()
    }

    @Synchronized
    fun apply(c: Context, source: String): Result<Unit> {
        if (!enabled(c)) return Result.success(Unit)
        return runCatching {
            check(isSamsung() && authorized(c)) { c.getString(R.string.setup_permission_missing) }
            check(packagePresent(c, GOOGLE_PACKAGE)) { c.getString(R.string.setup_google_missing) }
            putSecure(c, "voice_interaction_service", GOOGLE_SERVICE)
            putSecure(c, "assistant", triggerComponent(c))
            val flags = Settings.Global.getInt(c.contentResolver, NAV_FLAGS, 0)
            check(Settings.Global.putInt(c.contentResolver, NAV_FLAGS, flags or 8)) { c.getString(R.string.setup_write_failed) }
            check(configured(c)) { c.getString(R.string.setup_write_failed) }
        }.also { result ->
            prefs(c).edit().putString("last_source", source).putString("last_error", result.exceptionOrNull()?.message.orEmpty())
                .putLong("last_apply", System.currentTimeMillis()).apply()
        }
    }

    @Synchronized
    fun restore(c: Context): Result<Unit> = runCatching {
        check(authorized(c)) { c.getString(R.string.setup_permission_missing) }
        check(hasBackup(c)) { c.getString(R.string.setup_no_backup) }
        check(prefs(c).edit().putBoolean("enabled", false).commit()) { c.getString(R.string.setup_write_failed) }
        putSecure(c, "voice_interaction_service", saved(c, "original_voice_interaction_service"))
        putSecure(c, "assistant", saved(c, "original_assistant"))
        check(Settings.Global.putString(c.contentResolver, NAV_FLAGS, saved(c, "original_flags"))) { c.getString(R.string.setup_write_failed) }
        prefs(c).edit().putString("last_error", "").apply()
    }
    fun markBoot(c: Context) { prefs(c).edit().putLong("last_boot", System.currentTimeMillis()).apply() }
}
