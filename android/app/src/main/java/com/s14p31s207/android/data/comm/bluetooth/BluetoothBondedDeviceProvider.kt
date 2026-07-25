package com.s14p31s207.android.data.comm.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothBondedDeviceProvider @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    fun getBondedDevices(): Result<List<BluetoothBondedDevice>> {
        val adapter = bluetoothAdapter ?: return Result.failure(
            IllegalStateException("This device does not support Bluetooth.")
        )

        if (!adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth is turned off."))
        }

        val hasConnectPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasConnectPermission) {
            return Result.failure(SecurityException("Bluetooth permission is required."))
        }

        return runCatching {
            fetchBondedDevices(adapter)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchBondedDevices(adapter: BluetoothAdapter): List<BluetoothBondedDevice> {
        return adapter.bondedDevices
            .orEmpty()
            .asSequence()
            .filter { it.bondState == BluetoothDevice.BOND_BONDED }
            .map { device ->
                BluetoothBondedDevice(
                    name = runCatching { device.name }.getOrNull(),
                    address = runCatching { device.address }.getOrDefault("")
                )
            }
            .filter { it.address.isNotBlank() }
            .sortedWith(
                compareBy<BluetoothBondedDevice> { it.name.isNullOrBlank() }
                    .thenBy { it.name?.lowercase(Locale.ROOT) ?: "" }
                    .thenBy { it.address }
            )
            .toList()
    }
}
