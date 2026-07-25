package com.s14p31s207.android.data.comm.bluetooth

data class BluetoothConnectionState(
    val status: BluetoothConnectionStatus = BluetoothConnectionStatus.DISCONNECTED,
    val connectedDeviceAddress: String? = null,
    val connectedDeviceName: String? = null,
    val lastReceivedMessage: String? = null,
    val lastSentMessage: String? = null,
    val errorMessage: String? = null
)

enum class BluetoothConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
