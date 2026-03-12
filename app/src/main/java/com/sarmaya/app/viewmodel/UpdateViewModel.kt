package com.sarmaya.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sarmaya.app.BuildConfig
import com.sarmaya.app.SarmayaApplication
import com.sarmaya.app.data.DataStoreManager
import com.sarmaya.app.network.ConnectivityChecker
import com.sarmaya.app.network.GitHubRelease
import com.sarmaya.app.network.api.GitHubApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpdateViewModel(
    private val gitHubApi: GitHubApi,
    private val dataStoreManager: DataStoreManager,
    private val connectivityChecker: ConnectivityChecker
) : ViewModel() {

    private val _latestRelease = MutableStateFlow<GitHubRelease?>(null)
    val latestRelease: StateFlow<GitHubRelease?> = _latestRelease.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /** True if an update is available AND the user hasn't dismissed this specific version */
    val showUpdateBanner: StateFlow<Boolean> = combine(
        _latestRelease,
        dataStoreManager.dismissedUpdateTag
    ) { release, dismissedTag ->
        release != null && release.tagName != dismissedTag
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        if (_isLoading.value) return
        if (!connectivityChecker.isOnline()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = gitHubApi.getLatestRelease()
                val tagName = response.tagName ?: return@launch

                // Compare versions: strip "v" prefix if present
                val remoteVersion = tagName.removePrefix("v").trim()
                val localVersion = BuildConfig.VERSION_NAME.trim()

                if (isNewerVersion(remoteVersion, localVersion)) {
                    // Find APK asset if any
                    val apkUrl = response.assets
                        ?.firstOrNull { it.name?.endsWith(".apk") == true }
                        ?.browserDownloadUrl

                    _latestRelease.value = GitHubRelease(
                        tagName = tagName,
                        name = response.name ?: tagName,
                        body = response.body ?: "",
                        htmlUrl = response.htmlUrl ?: "",
                        publishedAt = response.publishedAt ?: "",
                        apkDownloadUrl = apkUrl
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check for updates: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissUpdate() {
        val tag = _latestRelease.value?.tagName ?: return
        viewModelScope.launch {
            dataStoreManager.setDismissedUpdateTag(tag)
        }
    }

    /**
     * Simple semver comparison: returns true if remote > local.
     * Handles: "1.0.0" vs "1.1.0", "1.0" vs "1.0.1", etc.
     */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)

        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }

        return false // same version
    }

    companion object Factory : ViewModelProvider.Factory {
        private const val TAG = "UpdateVM"

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as SarmayaApplication
            return UpdateViewModel(
                application.container.gitHubApi,
                application.container.dataStoreManager,
                application.container.connectivityChecker
            ) as T
        }
    }
}
