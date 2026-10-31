/* SPDX-License-Identifier: GPL-3.0-only */
package com.parallelc.micts.ui.activity

import android.app.StatusBarManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.parallelc.micts.BuildConfig
import com.parallelc.micts.LaunchTileService
import com.parallelc.micts.R
import com.parallelc.micts.samsung.SamsungSetup
import com.parallelc.micts.ui.theme.MiCTSTheme
import java.text.DateFormat
import java.util.Date

class SettingsActivity : ComponentActivity() {
    private var refreshVersion by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        enableEdgeToEdge()
        setContent {
            MiCTSTheme {
                SetupScreen(refreshVersion, intent.getBooleanExtra("trigger_error", false)) { refreshVersion++ }
            }
        }
    }
    override fun onResume() { super.onResume(); refreshVersion++ }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupScreen(refreshVersion: Int, triggerError: Boolean, refresh: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val permission = remember(refreshVersion) { SamsungSetup.authorized(context) }
    val google = remember(refreshVersion) { SamsungSetup.packagePresent(context, SamsungSetup.GOOGLE_PACKAGE) }
    val background = remember(refreshVersion) { SamsungSetup.backgroundAllowed(context) }
    val configured = remember(refreshVersion) { SamsungSetup.configured(context) }
    val enabled = remember(refreshVersion) { SamsungSetup.enabled(context) }
    val hasBackup = remember(refreshVersion) { SamsungSetup.hasBackup(context) }
    val lastBoot = remember(refreshVersion) { SamsungSetup.lastBoot(context) }
    val lastError = remember(refreshVersion) { SamsungSetup.lastError(context) }
    val legacy = remember(refreshVersion) { SamsungSetup.packagePresent(context, "dev.oscopex.searchgesture") }
    var feedback by rememberSaveable { mutableStateOf("") }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val command = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    fun open(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure { feedback = resources.getString(R.string.open_failed) }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("SamsungCTS") }, actions = {
            TextButton(onClick = {
                open(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ValoHalo/SamsungCTS")))
            }) { Text(stringResource(R.string.source_code)) }
        })
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.headlineSmall)
            Text("${Build.MODEL} · Android ${Build.VERSION.RELEASE} · v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!SamsungSetup.isSamsung()) {
                Text(stringResource(R.string.setup_samsung_only), color = MaterialTheme.colorScheme.error)
            }
            ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(if (configured) R.string.status_configured else R.string.status_not_configured),
                        style = MaterialTheme.typography.titleLarge)
                    StatusRow(R.string.status_google, google)
                    StatusRow(R.string.status_permission, permission)
                    StatusRow(R.string.status_background, background)
                    StatusRow(R.string.status_boot, enabled)
                    Button(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        enabled = permission && google && SamsungSetup.isSamsung(), onClick = {
                            val result = SamsungSetup.enable(context)
                            feedback = result.exceptionOrNull()?.message ?: resources.getString(R.string.setup_enabled)
                            refresh()
                        }) { Text(stringResource(R.string.enable_gesture)) }
                    OutlinedButton(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = google,
                        onClick = { open(Intent(context, MainActivity::class.java)) }) {
                        Text(stringResource(R.string.search_now))
                    }
                }
            }
            if (triggerError) Text(stringResource(R.string.trigger_failed_help), color = MaterialTheme.colorScheme.error)
            if (feedback.isNotEmpty()) Text(feedback, style = MaterialTheme.typography.bodyMedium)
            if (lastError.isNotEmpty()) Text(lastError, color = MaterialTheme.colorScheme.error)
            if (!permission) {
                SetupCard(R.string.permission_title, R.string.permission_body) {
                    Text(command, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("ADB", command))
                        feedback = resources.getString(R.string.command_copied)
                    }) { Text(stringResource(R.string.copy_command)) }
                }
            }
            if (!background) {
                SetupCard(R.string.background_title, R.string.background_body) {
                    TextButton(onClick = { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}"))) }) { Text(stringResource(R.string.open_app_settings)) }
                }
            }
            if (legacy && !enabled) {
                SetupCard(R.string.migration_title, R.string.migration_body) {
                    TextButton(onClick = {
                        context.packageManager.getLaunchIntentForPackage("dev.oscopex.searchgesture")?.let { open(it) }
                    }) { Text(stringResource(R.string.open_previous_helper)) }
                }
            }
            SetupCard(R.string.usage_title, R.string.usage_body) {
                TextButton(onClick = { open(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }) {
                    Text(stringResource(R.string.assistant_settings))
                }
                TextButton(onClick = {
                    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
                        ComponentName(context, LaunchTileService::class.java), resources.getString(R.string.tile_label),
                        Icon.createWithResource(context, R.drawable.ic_tile_service), context.mainExecutor) { }
                }) { Text(stringResource(R.string.add_tile)) }
            }
            if (lastBoot > 0) Text(stringResource(R.string.last_boot,
                DateFormat.getDateTimeInstance().format(Date(lastBoot))),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = permission && hasBackup,
                onClick = { confirmRestore = true }) { Text(stringResource(R.string.restore_original)) }
            Text(stringResource(R.string.scope_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (confirmRestore) AlertDialog(onDismissRequest = { confirmRestore = false },
        title = { Text(stringResource(R.string.restore_original)) },
        text = { Text(stringResource(R.string.restore_body)) },
        confirmButton = { TextButton(onClick = {
            val result = SamsungSetup.restore(context)
            feedback = result.exceptionOrNull()?.message ?: resources.getString(R.string.restored)
            confirmRestore = false
            refresh()
        }) { Text(stringResource(R.string.restore_original)) } },
        dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text(stringResource(android.R.string.cancel)) } })
}

@Composable
private fun StatusRow(label: Int, ready: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(if (ready) R.string.ready else R.string.needs_setup),
            style = MaterialTheme.typography.labelLarge,
            color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SetupCard(title: Int, body: Int, actions: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}
