package com.s14p31s207.android.data.comm.bluetooth

import android.bluetooth.BluetoothDevice

data class BluetoothScanDevice(
    val name: String?,
    val address: String,
    val deviceType: BluetoothDeviceType,
    val bondState: BluetoothBondState
)

enum class BluetoothDeviceType {
    CLASSIC,
    LE,
    DUAL,
    UNKNOWN;

    companion object {
        fun fromRaw(rawType: Int): BluetoothDeviceType {
            return when (rawType) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> CLASSIC
                BluetoothDevice.DEVICE_TYPE_LE -> LE
                BluetoothDevice.DEVICE_TYPE_DUAL -> DUAL
                else -> UNKNOWN
            }
        }
    }
}

enum class BluetoothBondState {
    NONE,
    BONDING,
    BONDED;

    companion object {
        fun fromRaw(rawState: Int): BluetoothBondState {
            return when (rawState) {
                BluetoothDevice.BOND_BONDING -> BONDING
                BluetoothDevice.BOND_BONDED -> BONDED
                else -> NONE
            }
        }
    }
}
