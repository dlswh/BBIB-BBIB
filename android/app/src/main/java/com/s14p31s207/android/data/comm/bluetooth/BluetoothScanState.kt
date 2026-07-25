package com.s14p31s207.android.data.comm.bluetooth

data class BluetoothScanState(
    val isScanning: Boolean = false,
    val devices: List<BluetoothScanDevice> = emptyList(),
    val errorMessage: String? = null,
    val isPairing: Boolean = false,
    val pairingResult: BluetoothPairingResult? = null
)

data class BluetoothPairingResult(
    val address: String,
    val isSuccess: Boolean,
    val message: String
)
