package com.s14p31s207.android.data.comm.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.s14p31s207.android.util.PairingPerfTrace
import com.s14p31s207.android.util.PerfLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothScanner @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val discoveredDeviceMap = linkedMapOf<String, BluetoothScanDevice>()
    private val _scanState = MutableStateFlow(BluetoothScanState())
    val scanState: StateFlow<BluetoothScanState> = _scanState.asStateFlow()

    private var isDiscoveryReceiverRegistered = false
    private var isBondReceiverRegistered = false
    private var isExternalBondReceiverRegistered = false
    private var pairingTargetAddress: String? = null
    private var pairingPerfTrace: PairingPerfTrace? = null

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _scanState.update { it.copy(isScanning = true, errorMessage = null) }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    updateFoundDevice(intent)
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _scanState.update { it.copy(isScanning = false) }
                    unregisterDiscoveryReceiverIfNeeded()
                }
            }
        }
    }

    private val bondReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    handleBondStateChanged(intent)
                }
            }
        }
    }

    /**
     * 앱 외부(시스템 설정 등)에서 발생하는 bond 변경을 상시 감지.
     * 페어링 삭제 시 discoveredDeviceMap에서 제거하여 UI에 즉시 반영한다.
     */
    private val externalBondReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return

            val device = intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java
            ) ?: return
            val address = runCatching { device.address }.getOrNull() ?: return
            val newState = BluetoothBondState.fromRaw(
                intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
            )

            when (newState) {
                BluetoothBondState.BONDED -> {
                    // 외부에서 페어링된 경우 맵에 추가
                    if (!discoveredDeviceMap.containsKey(address)) {
                        val name = runCatching { device.name }.getOrNull()
                        val deviceType = runCatching { BluetoothDeviceType.fromRaw(device.type) }
                            .getOrDefault(BluetoothDeviceType.UNKNOWN)
                        discoveredDeviceMap[address] = BluetoothScanDevice(
                            name = name,
                            address = address,
                            deviceType = deviceType,
                            bondState = BluetoothBondState.BONDED
                        )
                    } else {
                        updateKnownDeviceBondState(address, BluetoothBondState.BONDED)
                    }
                    _scanState.update { it.copy(devices = sortedDevices()) }
                }

                BluetoothBondState.NONE -> {
                    // 페어링 삭제 → 맵에서 제거해 UI에 즉시 반영
                    discoveredDeviceMap.remove(address)
                    _scanState.update { it.copy(devices = sortedDevices()) }
                }

                BluetoothBondState.BONDING -> Unit // 진행 중은 무시
            }
        }
    }

    fun startScan(): BluetoothScanStartResult {
        if (!BluetoothPermissionHelper.hasBluetoothPermissions(appContext)) {
            _scanState.update {
                it.copy(errorMessage = "Bluetooth permission is required.")
            }
            return BluetoothScanStartResult.MISSING_PERMISSION
        }

        val adapter = bluetoothAdapter ?: run {
            _scanState.update {
                it.copy(errorMessage = "This device does not support Bluetooth.")
            }
            return BluetoothScanStartResult.NOT_SUPPORTED
        }

        if (!adapter.isEnabled) {
            _scanState.update {
                it.copy(errorMessage = "Bluetooth is turned off.")
            }
            return BluetoothScanStartResult.BLUETOOTH_DISABLED
        }

        registerDiscoveryReceiverIfNeeded()

        // bonded 기기는 유지하고, 스캔으로만 발견된 기기만 제거
        discoveredDeviceMap.entries.removeIf { it.value.bondState != BluetoothBondState.BONDED }
        _scanState.update {
            it.copy(
                isScanning = true,
                devices = sortedDevices(),
                errorMessage = null
            )
        }

        val hasScanPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        if (hasScanPermission && adapter.isDiscovering) {
            safeCancelDiscovery(adapter)
        }

        val started = safeStartDiscovery(adapter)
        if (!started) {
            unregisterDiscoveryReceiverIfNeeded()
            _scanState.update {
                it.copy(
                    isScanning = false,
                    errorMessage = "Could not start Bluetooth discovery."
                )
            }
            return BluetoothScanStartResult.START_FAILED
        }

        return BluetoothScanStartResult.STARTED
    }

    fun stopScan() {
        val adapter = bluetoothAdapter
        val hasScanPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        if (adapter != null && hasScanPermission && adapter.isDiscovering) {
            safeCancelDiscovery(adapter)
        }
        _scanState.update { it.copy(isScanning = false) }
        unregisterDiscoveryReceiverIfNeeded()
    }

    fun pairDevice(address: String, perfTrace: PairingPerfTrace? = null): Boolean {
        pairingPerfTrace = perfTrace
        if (perfTrace != null) {
            PerfLogger.d(
                "[PAIR][START] attemptId=${perfTrace.attemptId} target=$address ts=${perfTrace.startedAt}"
            )
        }

        val adapter = bluetoothAdapter ?: run {
            publishPairingFailure(address, "This device does not support Bluetooth.")
            return false
        }

        if (!adapter.isEnabled) {
            publishPairingFailure(address, "Bluetooth is turned off.")
            return false
        }

        val hasConnectPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasConnectPermission) {
            publishPairingFailure(address, "Bluetooth permission is required.")
            return false
        }

        if (discoveredDeviceMap[address] == null) {
            publishPairingFailure(
                address,
                "Start scan first, then pair only from discovered devices."
            )
            return false
        }

        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: run {
            publishPairingFailure(address, "Could not find the selected device.")
            return false
        }

        val hasScanPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
        if (hasScanPermission && adapter.isDiscovering) {
            safeCancelDiscovery(adapter)
        }

        val currentBondState = runCatching { BluetoothBondState.fromRaw(device.bondState) }
            .getOrDefault(BluetoothBondState.NONE)

        updateKnownDeviceBondState(
            address = address,
            bondState = currentBondState
        )

        registerBondReceiverIfNeeded()
        pairingTargetAddress = address

        _scanState.update {
            it.copy(
                isPairing = true,
                pairingResult = null,
                errorMessage = null
            )
        }

        if (currentBondState == BluetoothBondState.BONDED) {
            logPairingSuccess(address)
            completePairing(
                address = address,
                isSuccess = true,
                message = "페어링 성공하였습니다."
            )
            return true
        }

        if (currentBondState == BluetoothBondState.BONDING) {
            updateKnownDeviceBondState(
                address = address,
                bondState = BluetoothBondState.BONDING
            )
            return true
        }

        val started = safeCreateBond(device)
        if (!started) {
            completePairing(
                address = address,
                isSuccess = false,
                message = "페어링에 실패하였습니다."
            )
            return false
        }

        updateKnownDeviceBondState(
            address = address,
            bondState = BluetoothBondState.BONDING
        )
        return true
    }

    fun clearPairingResult() {
        _scanState.update { it.copy(pairingResult = null) }
    }

    /**
     * 이미 페어링된(BONDED) 기기를 discoveredDeviceMap에 미리 채운다.
     * ViewModel init{}에서 호출하면 화면 진입 시 즉시 목록이 표시된다.
     *
     * pairDevice()는 discoveredDeviceMap에 없는 주소를 거부하므로,
     * bonded 기기도 여기서 등록해야 목록 탭 후 재페어링이 가능하다.
     */
    @SuppressLint("MissingPermission")
    fun loadBondedDevices() {
        val adapter = bluetoothAdapter ?: return
        if (!BluetoothPermissionHelper.hasBluetoothPermissions(appContext)) return

        val bonded = runCatching { adapter.bondedDevices }.getOrNull() ?: return

        bonded.forEach { device ->
            val address = runCatching { device.address }.getOrNull() ?: return@forEach
            // 스캔으로 이미 발견된 기기는 덮어쓰지 않는다
            if (discoveredDeviceMap.containsKey(address)) return@forEach

            val name = runCatching { device.name }.getOrNull()
            val deviceType = runCatching { BluetoothDeviceType.fromRaw(device.type) }
                .getOrDefault(BluetoothDeviceType.UNKNOWN)

            discoveredDeviceMap[address] = BluetoothScanDevice(
                name = name,
                address = address,
                deviceType = deviceType,
                bondState = BluetoothBondState.BONDED
            )
        }

        _scanState.update { it.copy(devices = sortedDevices()) }

        // 이후 외부 bond 변경(삭제 포함)을 상시 감지
        registerExternalBondReceiverIfNeeded()
    }

    fun release() {
        stopScan()
        pairingTargetAddress = null
        unregisterBondReceiverIfNeeded()
        unregisterExternalBondReceiverIfNeeded()
        discoveredDeviceMap.clear()
        _scanState.value = BluetoothScanState()
    }

    private fun registerDiscoveryReceiverIfNeeded() {
        if (isDiscoveryReceiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        appContext.registerReceiver(
            discoveryReceiver,
            filter,
            Context.RECEIVER_EXPORTED
        )
        isDiscoveryReceiverRegistered = true
    }

    private fun unregisterDiscoveryReceiverIfNeeded() {
        if (!isDiscoveryReceiverRegistered) return

        runCatching { appContext.unregisterReceiver(discoveryReceiver) }
        isDiscoveryReceiverRegistered = false
    }

    private fun registerBondReceiverIfNeeded() {
        if (isBondReceiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        appContext.registerReceiver(
            bondReceiver,
            filter,
            Context.RECEIVER_EXPORTED
        )
        isBondReceiverRegistered = true
    }

    private fun unregisterBondReceiverIfNeeded() {
        if (!isBondReceiverRegistered) return

        runCatching { appContext.unregisterReceiver(bondReceiver) }
        isBondReceiverRegistered = false
    }

    private fun registerExternalBondReceiverIfNeeded() {
        if (isExternalBondReceiverRegistered) return

        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        appContext.registerReceiver(externalBondReceiver, filter, Context.RECEIVER_EXPORTED)
        isExternalBondReceiverRegistered = true
    }

    private fun unregisterExternalBondReceiverIfNeeded() {
        if (!isExternalBondReceiverRegistered) return

        runCatching { appContext.unregisterReceiver(externalBondReceiver) }
        isExternalBondReceiverRegistered = false
    }

    @SuppressLint("MissingPermission")
    private fun updateFoundDevice(intent: Intent) {
        val device = intent.getParcelableExtra(
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        ) ?: return

        val address = runCatching { device.address }.getOrNull() ?: return
        val name = runCatching { device.name }.getOrNull()
        val deviceType = runCatching { BluetoothDeviceType.fromRaw(device.type) }
            .getOrDefault(BluetoothDeviceType.UNKNOWN)
        val bondState = runCatching { BluetoothBondState.fromRaw(device.bondState) }
            .getOrDefault(BluetoothBondState.NONE)

        if (deviceType == BluetoothDeviceType.LE) {
            return
        }

        discoveredDeviceMap[address] = BluetoothScanDevice(
            name = name,
            address = address,
            deviceType = deviceType,
            bondState = bondState
        )

        _scanState.update { it.copy(devices = sortedDevices()) }
    }

    @SuppressLint("MissingPermission")
    private fun handleBondStateChanged(intent: Intent) {
        val device = intent.getParcelableExtra(
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        ) ?: return
        val address = runCatching { device.address }.getOrNull() ?: return

        val currentStateRaw = intent.getIntExtra(
            BluetoothDevice.EXTRA_BOND_STATE,
            BluetoothDevice.ERROR
        )
        val previousStateRaw = intent.getIntExtra(
            BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
            BluetoothDevice.ERROR
        )
        val currentState = BluetoothBondState.fromRaw(currentStateRaw)

        updateKnownDeviceBondState(
            address = address,
            bondState = currentState
        )

        val targetAddress = pairingTargetAddress ?: return
        if (address != targetAddress) return

        when (currentState) {
            BluetoothBondState.BONDING -> {
                _scanState.update {
                    it.copy(
                        isPairing = true,
                        pairingResult = null,
                        errorMessage = null
                    )
                }
            }

            BluetoothBondState.BONDED -> {
                logPairingSuccess(targetAddress)
                completePairing(
                    address = targetAddress,
                    isSuccess = true,
                    message = "페어링 성공하였습니다."
                )
            }

            BluetoothBondState.NONE -> {
                if (previousStateRaw == BluetoothDevice.BOND_BONDING) {
                    logPairingFailure(targetAddress, "pairing_failed")
                    completePairing(
                        address = targetAddress,
                        isSuccess = false,
                        message = "페어링에 실패하였습니다."
                    )
                }
            }
        }
    }

    private fun updateKnownDeviceBondState(
        address: String,
        bondState: BluetoothBondState
    ) {
        val existing = discoveredDeviceMap[address] ?: return

        discoveredDeviceMap[address] = existing.copy(bondState = bondState)
        _scanState.update { it.copy(devices = sortedDevices()) }
    }

    private fun publishPairingFailure(
        address: String,
        message: String
    ) {
        logPairingFailure(address, message)
        pairingTargetAddress = null
        unregisterBondReceiverIfNeeded()
        _scanState.update {
            it.copy(
                isPairing = false,
                errorMessage = message,
                pairingResult = BluetoothPairingResult(
                    address = address,
                    isSuccess = false,
                    message = message
                )
            )
        }
    }

    private fun completePairing(
        address: String,
        isSuccess: Boolean,
        message: String
    ) {
        pairingTargetAddress = null
        if (!isSuccess) {
            pairingPerfTrace = null
        }
        unregisterBondReceiverIfNeeded()
        _scanState.update {
            it.copy(
                isPairing = false,
                errorMessage = if (isSuccess) null else message,
                pairingResult = BluetoothPairingResult(
                    address = address,
                    isSuccess = isSuccess,
                    message = message
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeStartDiscovery(adapter: BluetoothAdapter): Boolean {
        return runCatching { adapter.startDiscovery() }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    private fun safeCancelDiscovery(adapter: BluetoothAdapter) {
        runCatching { adapter.cancelDiscovery() }
    }

    @SuppressLint("MissingPermission")
    private fun safeCreateBond(device: BluetoothDevice): Boolean {
        return runCatching { device.createBond() }.getOrDefault(false)
    }

    private fun logPairingSuccess(address: String) {
        val perfTrace = pairingPerfTrace ?: return
        val successTs = System.currentTimeMillis()
        val elapsedMs = successTs - perfTrace.startedAt
        PerfLogger.d(
            "[PAIR][SUCCESS] attemptId=${perfTrace.attemptId} " +
                    "target=$address ts=$successTs elapsedMs=$elapsedMs"
        )
        pairingPerfTrace = null
    }

    private fun logPairingFailure(address: String, error: String) {
        val perfTrace = pairingPerfTrace ?: return
        val failTs = System.currentTimeMillis()
        PerfLogger.d(
            "[PAIR][FAIL] attemptId=${perfTrace.attemptId} " +
                    "target=$address ts=$failTs error=$error"
        )
        pairingPerfTrace = null
    }

    private fun sortedDevices(): List<BluetoothScanDevice> {
        return discoveredDeviceMap.values
            .sortedWith(
                // 1순위: BONDED 기기를 상단에
                compareByDescending<BluetoothScanDevice> { it.bondState == BluetoothBondState.BONDED }
                    // 2순위: 이름 없는 기기를 뒤로
                    .thenBy { it.name.isNullOrBlank() }
            )
    }
}