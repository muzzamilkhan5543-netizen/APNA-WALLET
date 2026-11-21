package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.local.UserRole
import com.example.ui.components.ReceiptBottomSheet
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.TransferMoneyScreen
import com.example.ui.theme.ApnaWalletTheme
import com.example.ui.viewmodel.WalletViewModel
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class TransferRoute(
    val methodKey: String,
    val methodTitle: String,
    val prefilledPhone: String? = null
)

@Serializable
data class QrScannerRoute(val initialTab: Int = 0)

@Serializable
object AdminRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApnaWalletRoot()
        }
    }
}

@Composable
fun ApnaWalletRoot(viewModel: WalletViewModel = viewModel()) {
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val rememberedUser by viewModel.rememberedUser.collectAsStateWithLifecycle()
    val authStatus by viewModel.authStatusState.collectAsStateWithLifecycle()
    val isAuthBusy by viewModel.isAuthBusy.collectAsStateWithLifecycle()
    val releasedMethods by viewModel.releasedMethods.collectAsStateWithLifecycle()
    val allMethods by viewModel.allMethods.collectAsStateWithLifecycle()
    val activeBanners by viewModel.activeAdBanners.collectAsStateWithLifecycle()
    val allBanners by viewModel.allAdBanners.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val pendingUsers by viewModel.pendingUsers.collectAsStateWithLifecycle()
    val lastReceiptTx by viewModel.lastReceiptTx.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    ApnaWalletTheme(appConfig = appConfig) {
        Box(modifier = Modifier.fillMaxSize()) {
            val activeUser = currentUser
            if (activeUser == null) {
                AuthScreen(
                    appConfig = appConfig,
                    rememberedUser = rememberedUser,
                    authStatus = authStatus,
                    isBusy = isAuthBusy,
                    onLogin = { phone, mpin -> viewModel.login(phone, mpin) },
                    onRegister = { name, phone, cnic, mpin, confirm, dpUri ->
                        viewModel.register(name, phone, cnic, mpin, confirm, dpUri)
                    },
                    onSwitchAccount = { viewModel.switchRememberedAccount() },
                    onClearStatus = { viewModel.clearAuthStatus() }
                )
            } else {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            user = activeUser,
                            appConfig = appConfig,
                            releasedMethods = releasedMethods,
                            activeBanners = activeBanners,
                            transactions = userTransactions,
                            onOpenMethod = { method ->
                                navController.navigate(
                                    TransferRoute(
                                        methodKey = method.methodKey,
                                        methodTitle = method.title,
                                        prefilledPhone = null
                                    )
                                )
                            },
                            onOpenQrScanner = { tab ->
                                if (appConfig.isQrScannerEnabled) {
                                    navController.navigate(QrScannerRoute(initialTab = tab))
                                }
                            },
                            onOpenAdminPanel = {
                                if (activeUser.role == UserRole.ADMIN) {
                                    navController.navigate(AdminRoute)
                                }
                            },
                            onUpdateDp = { uri -> viewModel.updateProfileDp(uri) },
                            onUpdateCardPhoto = { uri -> viewModel.updateCardBackground(uri) },
                            onSelectTransaction = { tx -> viewModel.showReceipt(tx) },
                            onLogout = { viewModel.logout() }
                        )
                    }

                    composable<TransferRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<TransferRoute>()
                        TransferMoneyScreen(
                            sender = activeUser,
                            methodKey = route.methodKey,
                            methodTitle = route.methodTitle,
                            prefilledReceiverPhone = route.prefilledPhone,
                            onLookupRegisteredUser = { phone -> viewModel.lookupRegisteredRecipient(phone) },
                            onExecuteTransfer = { receiverInput, amount, mpin, methodKey, onResult ->
                                viewModel.executeTransfer(
                                    receiverInput = receiverInput,
                                    amountPkr = amount,
                                    mpin = mpin,
                                    methodKey = methodKey,
                                    onResult = onResult
                                )
                            },
                            onBack = { navController.popBackStack() },
                            onCompleted = {
                                navController.popBackStack(HomeRoute, inclusive = false)
                            }
                        )
                    }

                    composable<QrScannerRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<QrScannerRoute>()
                        QrScannerScreen(
                            currentUser = activeUser,
                            walletName = appConfig.walletName,
                            initialTab = route.initialTab,
                            onLookupRegisteredUser = { phone -> viewModel.lookupRegisteredRecipient(phone) },
                            onQrRecipientResolved = { phone ->
                                navController.navigate(
                                    TransferRoute(
                                        methodKey = "transfer_money",
                                        methodTitle = "QR Pay",
                                        prefilledPhone = phone
                                    )
                                ) {
                                    popUpTo(HomeRoute) { inclusive = false }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AdminRoute> {
                        if (activeUser.role == UserRole.ADMIN) {
                            AdminPanelScreen(
                                adminUser = activeUser,
                                appConfig = appConfig,
                                allUsers = allUsers,
                                pendingUsers = pendingUsers,
                                allMethods = allMethods,
                                allBanners = allBanners,
                                onSetUserStatus = { phone, status -> viewModel.adminSetUserStatus(phone, status) },
                                onSendBonus = { phone, amt, note -> viewModel.adminSendBonus(phone, amt, note) },
                                onUpdateConfig = { cfg -> viewModel.adminUpdateConfig(cfg) },
                                onToggleMethod = { key, rel -> viewModel.adminToggleMethod(key, rel) },
                                onAddCustomMethod = { title, sub, cat, icon, rel ->
                                    viewModel.adminAddCustomMethod(title, sub, cat, icon, rel)
                                },
                                onDeleteCustomMethod = { key -> viewModel.adminDeleteCustomMethod(key) },
                                onUseMethodAsAdmin = { method ->
                                    navController.navigate(
                                        TransferRoute(
                                            methodKey = method.methodKey,
                                            methodTitle = method.title,
                                            prefilledPhone = null
                                        )
                                    )
                                },
                                onPublishAdBanner = { title, uri, isVid, url, durLabel, durMs ->
                                    viewModel.adminPublishAdBanner(title, uri, isVid, url, durLabel, durMs)
                                },
                                onDeleteAdBanner = { id -> viewModel.adminDeleteAdBanner(id) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }

            // Half-height Animated Slide-Up Receipt Modal
            ReceiptBottomSheet(
                transaction = lastReceiptTx,
                walletName = appConfig.walletName,
                onDismiss = { viewModel.dismissReceipt() },
                onSavedMessage = { msg -> viewModel.notifyMessage(msg) }
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}
