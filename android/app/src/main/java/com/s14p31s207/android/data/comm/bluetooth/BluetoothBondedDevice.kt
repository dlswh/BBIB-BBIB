package com.s14p31s207.android.data.comm.bluetooth

data class BluetoothBondedDevice(
    val name: String?,
    val address: String
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Unknown device"
}
