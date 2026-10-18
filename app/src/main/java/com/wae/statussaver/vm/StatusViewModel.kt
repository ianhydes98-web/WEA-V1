package com.wae.statussaver.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wae.statussaver.R
import com.wae.statussaver.data.StatusItem
import com.wae.statussaver.data.StatusRepository
import com.wae.statussaver.service.AutoSaveService
import com.wae.statussaver.util.Perms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StatusUiState(
    val loading: Boolean = true,
    val hasAccess: Boolean = false,
    val photos: List<StatusItem> = emptyList(),
    val videos: List<StatusItem> = emptyList(),
    val tab: Int = 0,
    val dirsFound: Int = 0,
    val autoSave: Boolean = false,
    val batteryOk: Boolean = true,
    val needsAllFiles: Boolean = false,
    val message: String? = null
)

class StatusViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = StatusRepository(app)
    private val _state = MutableStateFlow(StatusUiState())
    val state: StateFlow<StatusUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val hasAccess = Perms.hasMediaAccess(app)
            val items = withContext(Dispatchers.IO) {
                if (hasAccess) repo.list() else emptyList()
            }
            val dirs = withContext(Dispatchers.IO) { repo.dirsFound() }
            _state.update {
                it.copy(
                    loading = false,
                    hasAccess = hasAccess,
                    photos = items.filterNot { item -> item.isVideo },
                    videos = items.filter { item -> item.isVideo },
                    dirsFound = dirs
                )
            }
            refreshHealth()
        }
    }

    fun refreshHealth() {
        val app = getApplication<Application>()
        _state.update {
            it.copy(
                autoSave = AutoSaveService.isEnabled(app),
                batteryOk = Perms.ignoringBatteryOptimizations(app),
                needsAllFiles = Perms.needsAllFilesAccess()
            )
        }
    }

    fun selectTab(index: Int) = _state.update { it.copy(tab = index) }

    fun save(item: StatusItem) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.save(item) }
            val app = getApplication<Application>()
            _state.update {
                it.copy(
                    message = app.getString(
                        if (ok) R.string.saved_successfully else R.string.save_failed
                    )
                )
            }
            refresh()
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                repo.list().filterNot { it.saved }
            }
            val count = withContext(Dispatchers.IO) { repo.saveAll(items) }
            val app = getApplication<Application>()
            _state.update { it.copy(message = app.getString(R.string.saved_count, count)) }
            refresh()
        }
    }

    fun toggleAutoSave() {
        val app = getApplication<Application>()
        if (!Perms.hasNotifications(app)) {
            _state.update { it.copy(message = app.getString(R.string.need_notifications)) }
        }
        AutoSaveService.toggle(app)
        refreshHealth()
    }

    fun resetIndex() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.resetIndex() }
            val app = getApplication<Application>()
            _state.update { it.copy(message = app.getString(R.string.index_reset)) }
            refresh()
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
