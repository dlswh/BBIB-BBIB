package com.s14p31s207.android.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.s14p31s207.android.feature.auth.session.AuthMode
import com.s14p31s207.android.feature.auth.session.SessionState
import com.s14p31s207.android.feature.auth.ui.LoginRoute
import com.s14p31s207.android.feature.auth.vm.LoginViewModel
import com.s14p31s207.android.feature.common.ui.OfflineBanner
import com.s14p31s207.android.feature.connection.ui.DeviceRegistrationRoute
import com.s14p31s207.android.feature.control.ui.BlockingErrorDialog
import com.s14p31s207.android.feature.control.ui.ControlRoute
import com.s14p31s207.android.feature.control.vm.ControlViewModel
import com.s14p31s207.android.feature.logview.ui.LogRoute
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext


@Composable
fun AppNavigation(
    appErrorViewModel: AppErrorViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val dialogError by appErrorViewModel.dialogError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val loginViewModel: LoginViewModel = hiltViewModel(
        viewModelStoreOwner = context as ComponentActivity
    )
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    fun navigate(nav: NavMenu) {
        val route = when (nav) {
            NavMenu.CONNECTION -> "connection"
            NavMenu.CONTROL -> "control"
            NavMenu.STATE -> "state"
            NavMenu.LOG -> "log"
            NavMenu.WIFI_SETTING -> "wifi_setting"
        }

        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            if (
                SessionState.authMode == AuthMode.OFFLINE &&
                currentRoute != "login"
            ) {
                OfflineBanner()
            }

            NavHost(
                navController = navController,
                startDestination = "login", // ✅ login으로 변경
                modifier = Modifier.weight(1f)
            ) {
                composable("login") {
                    LoginRoute(
                        onLoginSuccess = {
                            SessionState.authMode = AuthMode.ONLINE

                            navController.navigate("connection") {
                                popUpTo("login") { inclusive = true }
                            }
                        },

                        onOfflineMode = {
                            SessionState.authMode = AuthMode.OFFLINE

                            navController.navigate("connection") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                composable("connection") {
                    DeviceRegistrationRoute(
                        onNavSelected = { navigate(it) },
                        onLogout = {
                            loginViewModel.logout {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onSaved = {}
                    )
                }
                composable("control") { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(navController.graph.id)
                    }
                    val sharedViewModel: ControlViewModel = hiltViewModel(parentEntry)
                    // loginViewModel 별도 선언 제거 ← 상단 Activity 스코프 것 그대로 사용

                    ControlRoute(
                        viewModel = sharedViewModel,
                        onNavSelected = { navigate(it) },
                        onLogout = {
                            loginViewModel.logout { // ← 상단에서 선언한 Activity 스코프 VM
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable("log") {
                    LogRoute(
                        onNavSelected = { navigate(it) },
                        onLogout = {
                            loginViewModel.logout {  // ✅ API 호출 추가
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable("state") {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("State")
                    }
                }
            }
        }
        dialogError?.let { error ->
            BlockingErrorDialog(
                error = error,
                onDismiss = appErrorViewModel::dismissDialog
            )
        }
    }
}