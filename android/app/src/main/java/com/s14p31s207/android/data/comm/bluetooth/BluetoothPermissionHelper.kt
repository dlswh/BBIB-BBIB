package com.s14p31s207.android.data.comm.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object BluetoothPermissionHelper {
    val permissions: Array<String>
        get() = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

    fun hasBluetoothPermissions(context: Context): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
