package com.s14p31s207.android.feature.logview.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s14p31s207.android.data.comm.bluetooth.BluetoothConnectionManager
import com.s14p31s207.android.data.db.IpcDeviceDao
import com.s14p31s207.android.domain.repository.ConnectRepository
import com.s14p31s207.android.domain.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val connectRepository: ConnectRepository,
    private val btManager: BluetoothConnectionManager,  // 추가
    private val ipcDeviceDao: IpcDeviceDao// 추가
) : ViewModel() {

    private fun getTodayPeriod(): Pair<String, String> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return Pair("${today}T00:00:00", "${today}T23:59:59")
    }

    private val _searchParams = MutableStateFlow(
        Triple(getTodayPeriod().first, getTodayPeriod().second, "")
    )

    // 현재 연결된 ipcId 관찰
    private val currentIpcId: StateFlow<String?> = combine(
        connectRepository.currentIpcId,
        btManager.connectionState
    ) { wifiIpcId, btState ->
        wifiIpcId ?: btState.connectedDeviceAddress?.let { mac ->
            ipcDeviceDao.getByMacAddress(mac)?.name
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val logs = combine(currentIpcId, _searchParams) { ipcId, params ->
        Triple(ipcId, params.first, params.second) to params.third
    }.flatMapLatest { (triple, query) ->
        val (ipcId, start, end) = triple
        if (ipcId == null) return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())
        if (query.isBlank()) {
            logRepository.getLogsByPeriod(ipcId, start, end)
        } else {
            logRepository.searchLogs(ipcId, start, end, query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun performSearch(start: String?, end: String?, query: String) {
        val s = start ?: getTodayPeriod().first
        val e = end ?: getTodayPeriod().second
        _searchParams.value = Triple(s, e, query)
    }
}