/* Shizuku single-process binder protocol, based on RikkaW's MIT-licensed API.
 * See assets/SHIZUKU-LICENSE.txt. No other backend is initialized. */
package com.parallelc.micts.permission

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import moe.shizuku.api.BinderContainer
import rikka.shizuku.Shizuku

class ShizukuBridgeProvider : ContentProvider() {
    override fun onCreate() = true
    @Suppress("DEPRECATION")
    @SuppressLint("RestrictedApi") // Implements the official provider-to-API binder handoff.
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "sendBinder" || extras == null) return null
        extras.classLoader = BinderContainer::class.java.classLoader
        val container = extras.getParcelable<BinderContainer>("moe.shizuku.privileged.api.intent.extra.BINDER")
        if (container?.binder != null && !Shizuku.pingBinder()) {
            Shizuku.onBinderReceived(container.binder, requireNotNull(context).packageName)
        }
        return Bundle()
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
}
