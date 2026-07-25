package com.s14p31s207.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.s14p31s207.android.app.navigation.AppNavigation
import com.s14p31s207.android.data.comm.bluetooth.BluetoothPermissionHelper
import com.s14p31s207.android.perf.ResponsePerfRunner
import com.s14p31s207.android.ui.theme.AndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var responsePerfRunner: ResponsePerfRunner

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BluetoothPermissionHelper.hasBluetoothPermissions(this)) {
            bluetoothPermissionLauncher.launch(BluetoothPermissionHelper.permissions)
        }

        setContent {
            AndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }

        lifecycleScope.launch {
            responsePerfRunner.runIfRequested(intent)
        }
    }
}
