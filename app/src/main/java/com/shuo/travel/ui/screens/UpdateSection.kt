package com.shuo.travel.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shuo.travel.BuildConfig
import com.shuo.travel.R
import com.shuo.travel.update.*
import java.io.File

@Composable fun UpdateSection(controller: UpdateController) {
    val state by controller.state.collectAsStateWithLifecycle()
    val automatic by controller.autoCheck.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val openInstaller: (File) -> Unit = { file ->
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        installer.launch(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) controller.install(openInstaller)
    }
    Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.testTag("updates")) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.update_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(stringResource(R.string.update_automatic), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(automatic, controller::automatic, Modifier.testTag("update-auto"))
            }
            Text(stringResource(R.string.update_privacy), style = MaterialTheme.typography.bodySmall)
            val status = when (state.phase) {
                UpdatePhase.IDLE -> R.string.update_idle
                UpdatePhase.CHECKING -> R.string.update_checking
                UpdatePhase.CURRENT -> R.string.update_current
                UpdatePhase.AVAILABLE -> R.string.update_available
                UpdatePhase.DOWNLOADING -> R.string.update_downloading
                UpdatePhase.VERIFYING -> R.string.update_verifying
                UpdatePhase.READY -> R.string.update_ready
                UpdatePhase.ERROR -> when (state.failure) {
                    UpdateFailure.UNAVAILABLE -> R.string.update_unavailable
                    UpdateFailure.INVALID -> R.string.update_invalid
                    UpdateFailure.INSTALL -> R.string.update_install_error
                    else -> R.string.update_network_error
                }
            }
            Text(stringResource(status, state.release?.version?.toString().orEmpty()), Modifier.testTag("update-status"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.phase == UpdatePhase.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (state.phase == UpdatePhase.DOWNLOADING) {
                LinearProgressIndicator(progress = { state.progress }, Modifier.fillMaxWidth())
                Text(stringResource(R.string.update_progress, (state.progress * 100).toInt()), style = MaterialTheme.typography.bodySmall)
            }
            if (state.phase in listOf(UpdatePhase.CHECKING, UpdatePhase.VERIFYING)) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.phase == UpdatePhase.READY || state.failure == UpdateFailure.INSTALL)
                Text(stringResource(R.string.update_install_hint), style = MaterialTheme.typography.bodySmall)
            when (state.phase) {
                UpdatePhase.CHECKING, UpdatePhase.DOWNLOADING, UpdatePhase.VERIFYING ->
                    TextButton(controller::cancel) { Text(stringResource(R.string.cancel)) }
                UpdatePhase.READY -> Button({
                    try {
                        if (context.packageManager.canRequestPackageInstalls()) controller.install(openInstaller)
                        else permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                    } catch (_: Exception) { controller.installFailed() }
                }, Modifier.testTag("update-install")) { Text(stringResource(R.string.update_install)) }
                else -> {
                    if (state.release != null) Button(controller::download, Modifier.testTag("update-download")) {
                        Text(stringResource(R.string.update_download, state.release!!.version.toString()))
                    }
                    OutlinedButton({ controller.check() }, Modifier.testTag("update-check")) {
                        Text(stringResource(R.string.update_check))
                    }
                }
            }
        }
    }
}
