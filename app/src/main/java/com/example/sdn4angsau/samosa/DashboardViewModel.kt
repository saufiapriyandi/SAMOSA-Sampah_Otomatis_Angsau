package com.example.sdn4angsau.samosa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DashboardEmptyState {
    NONE,
    NO_ACTIVE_BINS,
    SEARCH_NO_RESULT
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val allBins: List<TempatSampah> = emptyList(),
    val visibleBins: List<TempatSampah> = emptyList(),
    val totalCount: Int = 0,
    val fullCount: Int = 0,
    val safeCount: Int = 0,
    val showWarning: Boolean = false,
    val warningBins: List<TempatSampah> = emptyList(),
    val lastUpdatedLabel: String = "",
    val emptyState: DashboardEmptyState = DashboardEmptyState.NONE,
    val isDataStale: Boolean = false,
    val staleMinutes: Int = 0,
    val dataVersion: Long = 0L
)

class DashboardViewModel(
    private val repository: TempatSampahRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(DashboardUiState())
    val uiState: LiveData<DashboardUiState> = _uiState

    private var sourceData: List<TempatSampah> = emptyList()
    private var currentQuery: String = ""
    private var lastUpdatedAtMillis: Long = 0L
    private val staleThresholdMillis = 10 * 60 * 1000L
    private val freshnessCheckIntervalMillis = 60 * 1000L

    init {
        loadData()
        startFreshnessTicker()
    }

    fun loadData() {
        _uiState.value = _uiState.value?.copy(
            isLoading = true,
            errorMessage = null
        ) ?: DashboardUiState(isLoading = true)

        viewModelScope.launch {
            runCatching {
                repository.getDaftarTempatSampah()
            }.onSuccess { bins ->
                sourceData = bins
                lastUpdatedAtMillis = System.currentTimeMillis()
                publishState()
            }.onFailure {
                _uiState.value = (_uiState.value ?: DashboardUiState()).copy(
                    isLoading = false,
                    errorMessage = it.message ?: "Gagal memuat data monitor."
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery == currentQuery) return

        currentQuery = normalizedQuery
        publishState()
    }

    private fun publishState(nowMillis: Long = System.currentTimeMillis()) {
        val filteredBins = if (currentQuery.isBlank()) {
            sourceData
        } else {
            sourceData.filter { it.lokasi.contains(currentQuery, ignoreCase = true) }
        }

        val fullBins = sourceData.filter { it.isFull }
        val isDataStale = lastUpdatedAtMillis != 0L && (nowMillis - lastUpdatedAtMillis) >= staleThresholdMillis
        val staleMinutes = if (isDataStale) {
            (((nowMillis - lastUpdatedAtMillis) / 60000L).toInt()).coerceAtLeast(1)
        } else {
            0
        }
        val emptyState = when {
            sourceData.isEmpty() -> DashboardEmptyState.NO_ACTIVE_BINS
            filteredBins.isEmpty() -> DashboardEmptyState.SEARCH_NO_RESULT
            else -> DashboardEmptyState.NONE
        }

        _uiState.value = DashboardUiState(
            isLoading = false,
            errorMessage = null,
            allBins = sourceData,
            visibleBins = filteredBins,
            totalCount = sourceData.size,
            fullCount = sourceData.count { it.isFull },
            safeCount = sourceData.count { it.status == TempatSampahStatus.AMAN },
            showWarning = fullBins.isNotEmpty(),
            warningBins = fullBins,
            lastUpdatedLabel = formatLastUpdated(lastUpdatedAtMillis),
            emptyState = emptyState,
            isDataStale = isDataStale,
            staleMinutes = staleMinutes,
            dataVersion = lastUpdatedAtMillis
        )
    }

    private fun startFreshnessTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(freshnessCheckIntervalMillis)
                if (lastUpdatedAtMillis != 0L) {
                    publishState()
                }
            }
        }
    }

    private fun formatLastUpdated(timestamp: Long): String {
        if (timestamp == 0L) return "--:--"

        val formatter = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        return formatter.format(Date(timestamp))
    }

    class Factory(
        private val repository: TempatSampahRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
