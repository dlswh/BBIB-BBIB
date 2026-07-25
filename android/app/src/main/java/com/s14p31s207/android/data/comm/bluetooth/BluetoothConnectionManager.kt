package com.s14p31s207.android.data.comm.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.s14p31s207.android.data.comm.tcp.CommandDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothConnectionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val gson = com.google.gson.Gson()
    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow(BluetoothConnectionState())
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()
    private val _incomingMessages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val incomingMessages: SharedFlow<String> = _incomingMessages

    private var socket: BluetoothSocket? = null
    private var messageChannel: BluetoothMessageChannel? = null

    suspend fun connect(address: String, serviceUuid: UUID): Result<Unit> {
        val adapter = bluetoothAdapter ?: return Result.failure(
            IllegalStateException("This device does not support Bluetooth.")
        )

        if (!adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth is turned off."))
        }

        if (!BluetoothPermissionHelper.hasBluetoothPermissions(appContext)) {
            return Result.failure(SecurityException("Bluetooth permission is required."))
        }

        val device = runCatching { adapter.getRemoteDevice(address) }
            .getOrElse { return Result.failure(it) }

        return connect(device, serviceUuid)
    }

    suspend fun send(command: CommandDto): Result<Unit> {
        val json = gson.toJson(command)
        return sendMessage(json)
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice, serviceUuid: UUID): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!BluetoothPermissionHelper.hasBluetoothPermissions(appContext)) {
                return@withContext Result.failure(
                    SecurityException("Bluetooth permission is required.")
                )
            }

            disconnectInternal(clearMessages = false)
            _connectionState.update {
                it.copy(
                    status = BluetoothConnectionStatus.CONNECTING,
                    connectedDeviceAddress = safeDeviceAddress(device),
                    connectedDeviceName = safeDeviceName(device),
                    errorMessage = null
                )
            }

            return@withContext runCatching {
                safeCancelDiscovery()

                val newSocket = createSocket(device, serviceUuid)
                newSocket.connect()

                socket = newSocket
                messageChannel = BluetoothMessageChannel(
                    socket = newSocket,
                    onMessageReceived = { line ->
                        _incomingMessages.tryEmit(line)
                        _connectionState.update {
                            it.copy(
                                lastReceivedMessage = line,
                                errorMessage = null
                            )
                        }
                    },
                    onDisconnected = { message ->
                        handleDisconnected(message)
                    },
                    onSendFailure = { error ->
                        _connectionState.update {
                            it.copy(
                                errorMessage = error.message
                                    ?: "Could not send the Bluetooth message."
                            )
                        }
                    }
                )

                _connectionState.update {
                    it.copy(
                        status = BluetoothConnectionStatus.CONNECTED,
                        connectedDeviceAddress = safeDeviceAddress(device),
                        connectedDeviceName = safeDeviceName(device),
                        errorMessage = null
                    )
                }

                messageChannel?.startListening()
                Unit
            }.onFailure { error ->
                // CancellationException은 반드시 재전파 — 삼키면 코루틴 취소가 전파되지 않아
                // reconnectJob 취소 후에도 BT 연결 시도가 계속 진행되는 버그가 발생함
                if (error is CancellationException) throw error

                disconnectInternal(clearMessages = false)
                _connectionState.update {
                    it.copy(
                        status = BluetoothConnectionStatus.DISCONNECTED,
                        connectedDeviceAddress = null,
                        connectedDeviceName = null,
                        errorMessage = error.message ?: "Could not connect to the Bluetooth device."
                    )
                }
            }
        }

    suspend fun sendMessage(message: String): Result<Unit> = withContext(Dispatchers.IO) {
        val activeChannel = messageChannel ?: return@withContext Result.failure(
            IllegalStateException("Bluetooth socket is not connected.")
        )

        return@withContext activeChannel.send(message).map { payload ->
            _connectionState.update {
                it.copy(
                    lastSentMessage = payload.trimEnd('\n'),
                    errorMessage = null
                )
            }
            Unit
        }
    }

    suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            disconnectInternal(clearMessages = false)
            Unit
        }.onFailure { error ->
            _connectionState.update {
                it.copy(
                    errorMessage = error.message ?: "Could not disconnect the Bluetooth device."
                )
            }
        }
    }

    fun release() {
        closeResources()
        _connectionState.value = BluetoothConnectionState()
    }

    private fun handleDisconnected(message: String) {
        closeResources()
        _connectionState.update {
            it.copy(
                status = BluetoothConnectionStatus.DISCONNECTED,
                connectedDeviceAddress = null,
                connectedDeviceName = null,
                errorMessage = message
            )
        }
    }

    private fun disconnectInternal(clearMessages: Boolean) {
        Log.d("Bluetooth", "disconnectInternal: ")
        closeResources()
        _connectionState.update {
            it.copy(
                status = BluetoothConnectionStatus.DISCONNECTED,
                connectedDeviceAddress = null,
                connectedDeviceName = null,
                lastReceivedMessage = if (clearMessages) null else it.lastReceivedMessage,
                lastSentMessage = if (clearMessages) null else it.lastSentMessage,
                errorMessage = null
            )
        }
    }

    private fun closeResources() {
        val activeChannel = messageChannel
        val activeSocket = socket

        messageChannel = null
        socket = null

        if (activeChannel != null) {
            activeChannel.release()
        } else {
            runCatching { activeSocket?.close() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createSocket(device: BluetoothDevice, serviceUuid: UUID): BluetoothSocket {
        return device.createRfcommSocketToServiceRecord(serviceUuid)
    }

    @SuppressLint("MissingPermission")
    private fun safeCancelDiscovery() {
        val adapter = bluetoothAdapter ?: return
        val hasScanPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        if (hasScanPermission && adapter.isDiscovering) {
            runCatching { adapter.cancelDiscovery() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        return runCatching { device.name }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceAddress(device: BluetoothDevice): String? {
        return runCatching { device.address }.getOrNull()
    }
}