package com.shuo.travel.update

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import com.shuo.travel.BuildConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class UpdatePhase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, VERIFYING, READY, ERROR }
data class UpdateState(val phase: UpdatePhase = UpdatePhase.IDLE, val release: GitHubRelease? = null,
    val progress: Float = 0f, val file: File? = null, val failure: UpdateFailure? = null)

class UpdateController(context: Context, private val scope: CoroutineScope, private val client: UpdateClient,
    private val clock: () -> Long = System::currentTimeMillis,
    installedVersion: String = BuildConfig.VERSION_NAME) {
    private val store = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val cached = runCatching {
        Json.decodeFromString<GitHubRelease>(store.getString("available", null) ?: "").takeIf {
            it.version > (ReleaseVersion.parse(installedVersion) ?: it.version) &&
                GitHubRelease.allowedUrl(it.url) && it.size in 1..GitHubRelease.MAX_APK_BYTES
        }
    }.getOrNull()
    private val mutable = MutableStateFlow(if (cached == null) UpdateState() else UpdateState(UpdatePhase.AVAILABLE, cached))
    val state = mutable.asStateFlow()
    private val automatic = MutableStateFlow(store.getBoolean("automatic", true))
    val autoCheck = automatic.asStateFlow()
    private var operation: Job? = null
    fun automatic(enabled: Boolean) {
        automatic.value = enabled
        store.edit().putBoolean("automatic", enabled).apply()
    }
    fun check(manual: Boolean = true) {
        if (operation?.isActive == true) return
        if (!manual) {
            if (!automatic.value || mutable.value.phase !in listOf(UpdatePhase.IDLE, UpdatePhase.CURRENT, UpdatePhase.ERROR, UpdatePhase.AVAILABLE)) return
            val age = clock() - store.getLong("last-attempt", 0L)
            if (age in 0 until 24 * 60 * 60 * 1000L) return
        }
        operation = scope.launch {
            val previous = mutable.value
            mutable.value = UpdateState(UpdatePhase.CHECKING)
            try {
                store.edit().putLong("last-attempt", clock()).apply()
                val release = client.latest()
                store.edit().apply {
                    if (release == null) remove("available") else putString("available", Json.encodeToString(release))
                }.apply()
                mutable.value = UpdateState(if (release == null) UpdatePhase.CURRENT else UpdatePhase.AVAILABLE, release)
            } catch (error: Exception) {
                if (error is CancellationException) { mutable.value = previous; throw error }
                mutable.value = if (manual) UpdateState(UpdatePhase.ERROR, previous.release, failure = failure(error)) else previous
            }
        }
    }
    fun download() {
        if (operation?.isActive == true) return
        val release = mutable.value.release ?: return
        operation = scope.launch {
            mutable.value = UpdateState(UpdatePhase.DOWNLOADING, release)
            try {
                val file = client.download(release) { progress ->
                    mutable.value = UpdateState(if (progress >= 1f) UpdatePhase.VERIFYING else UpdatePhase.DOWNLOADING, release, progress)
                }
                mutable.value = UpdateState(UpdatePhase.READY, release, 1f, file)
            } catch (error: Exception) {
                if (error is CancellationException) { mutable.value = UpdateState(UpdatePhase.AVAILABLE, release); throw error }
                mutable.value = UpdateState(UpdatePhase.ERROR, release, failure = failure(error))
            }
        }
    }
    fun install(openInstaller: (File) -> Unit) {
        if (operation?.isActive == true) return
        val ready = mutable.value
        val file = ready.file ?: return
        val release = ready.release ?: return
        operation = scope.launch {
            mutable.value = ready.copy(phase = UpdatePhase.VERIFYING)
            try {
                client.verify(file, release)
                openInstaller(file)
                mutable.value = ready
            } catch (error: Exception) {
                if (error is CancellationException) { mutable.value = ready; throw error }
                mutable.value = ready.copy(phase = UpdatePhase.ERROR, failure = if (error is UpdateException) error.failure else UpdateFailure.INSTALL)
            }
        }
    }
    fun cancel() { operation?.cancel() }
    fun installFailed() { mutable.value = mutable.value.copy(phase = UpdatePhase.ERROR, failure = UpdateFailure.INSTALL) }
    private fun failure(error: Exception) = (error as? UpdateException)?.failure ?: UpdateFailure.NETWORK
}
