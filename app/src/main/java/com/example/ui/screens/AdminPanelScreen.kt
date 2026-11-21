package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountStatus
import com.example.data.local.AdBannerEntity
import com.example.data.local.AppConfigEntity
import com.example.data.local.PaymentMethodEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.ui.components.UserAvatar
import com.example.ui.components.WalletIcons
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalWalletPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DurationOption(val label: String, val millis: Long)

private val adDurationOptions = listOf(
    DurationOption("2 Minutes", 2L * 60_000L),
    DurationOption("2 Hours", 2L * 3_600_000L),
    DurationOption("24 Hours", 24L * 3_600_000L),
    DurationOption("2 Days", 2L * 86_400_000L),
    DurationOption("2 Years", 730L * 86_400_000L)
)

private data class ThemePreset(
    val name: String,
    val primaryHex: Long,
    val accentHex: Long,
    val pageHex: Long
)

private val themePresets = listOf(
    ThemePreset("Classic Red & Yellow", 0xFFC8102EL, 0xFFFFD100L, 0xFFFFFFFFL),
    ThemePreset("Deep Crimson Gold", 0xFF9E0B22L, 0xFFFFE047L, 0xFFFFFFFFL),
    ThemePreset("Emerald & Gold", 0xFF047857L, 0xFFFFD100L, 0xFFFFFFFFL),
    ThemePreset("Royal Indigo Gold", 0xFF1E3A8AL, 0xFFFFD100L, 0xFFFFFFFFL),
    ThemePreset("Warm Cream Red", 0xFFC8102EL, 0xFFFFD100L, 0xFFFFFBEB)
)

@Composable
fun AdminPanelScreen(
    adminUser: UserEntity,
    appConfig: AppConfigEntity,
    allUsers: List<UserEntity>,
    pendingUsers: List<UserEntity>,
    allMethods: List<PaymentMethodEntity>,
    allBanners: List<AdBannerEntity>,
    onSetUserStatus: (phone: String, status: AccountStatus) -> Unit,
    onSendBonus: (phone: String, amount: Double, note: String) -> Unit,
    onUpdateConfig: (AppConfigEntity) -> Unit,
    onToggleMethod: (key: String, released: Boolean) -> Unit,
    onAddCustomMethod: (title: String, subtitle: String, category: String, iconKey: String, releaseNow: Boolean) -> Unit,
    onDeleteCustomMethod: (key: String) -> Unit,
    onUseMethodAsAdmin: (PaymentMethodEntity) -> Unit,
    onPublishAdBanner: (title: String, mediaUri: Uri, isVideo: Boolean, url: String, durationLabel: String, durationMillis: Long) -> Unit,
    onDeleteAdBanner: (id: Long) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalWalletPalette.current
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var bonusTargetUser by remember { mutableStateOf<UserEntity?>(null) }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageWhite)
            .navigationBarsPadding()
    ) {
        // Admin Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.headerRed)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Wallet",
                            tint = palette.accentYellow
                        )
                    }
                    Column {
                        Text(
                            text = "Admin Control Center",
                            color = palette.accentYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${adminUser.fullName} • Unlimited Vault",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 4 Admin Tabs
            val tabs = listOf(
                Triple(0, "Users", Icons.Filled.People),
                Triple(1, "Services", Icons.Filled.Tune),
                Triple(2, "Branding", Icons.Filled.Palette),
                Triple(3, "Ads", Icons.Filled.Campaign)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.18f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (idx, label, icon) ->
                    val active = selectedTab == idx
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) palette.accentYellow else Color.Transparent)
                            .testTag("admin_tab_${label.lowercase()}")
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (active) palette.headerRed else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            color = if (active) palette.headerRed else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> AdminUsersTab(
                pendingUsers = pendingUsers,
                allUsers = allUsers.filter { it.role != UserRole.ADMIN },
                onApprove = { onSetUserStatus(it, AccountStatus.APPROVED) },
                onReject = { onSetUserStatus(it, AccountStatus.REJECTED) },
                onFreezeToggle = { user ->
                    val next = if (user.status == AccountStatus.FROZEN) AccountStatus.APPROVED else AccountStatus.FROZEN
                    onSetUserStatus(user.phoneNumber, next)
                },
                onSuspendToggle = { user ->
                    val next = if (user.status == AccountStatus.SUSPENDED) AccountStatus.APPROVED else AccountStatus.SUSPENDED
                    onSetUserStatus(user.phoneNumber, next)
                },
                onOpenBonusDialog = { bonusTargetUser = it }
            )
            1 -> AdminServicesTab(
                appConfig = appConfig,
                allMethods = allMethods,
                onUpdateConfig = onUpdateConfig,
                onToggleMethod = onToggleMethod,
                onAddCustomMethod = onAddCustomMethod,
                onDeleteCustomMethod = onDeleteCustomMethod,
                onUseMethodAsAdmin = onUseMethodAsAdmin
            )
            2 -> AdminBrandingTab(
                appConfig = appConfig,
                onUpdateConfig = onUpdateConfig
            )
            3 -> AdminAdsTab(
                allBanners = allBanners,
                onPublishBanner = { title, uri, url, dur ->
                    val mime = context.contentResolver.getType(uri).orEmpty()
                    val isVideo = mime.startsWith("video/")
                    onPublishAdBanner(title, uri, isVideo, url, dur.label, dur.millis)
                },
                onDeleteBanner = onDeleteAdBanner
            )
        }
    }

    bonusTargetUser?.let { target ->
        BonusDialog(
            user = target,
            onDismiss = { bonusTargetUser = null },
            onSend = { amt, note ->
                onSendBonus(target.phoneNumber, amt, note)
                bonusTargetUser = null
            }
        )
    }
}

@Composable
private fun AdminUsersTab(
    pendingUsers: List<UserEntity>,
    allUsers: List<UserEntity>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onFreezeToggle: (UserEntity) -> Unit,
    onSuspendToggle: (UserEntity) -> Unit,
    onOpenBonusDialog: (UserEntity) -> Unit
) {
    val palette = LocalWalletPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Pending Approvals (${pendingUsers.size})",
                color = palette.headerRed,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (pendingUsers.isEmpty()) {
            item {
                Surface(
                    color = palette.headerRed.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No pending user registrations right now.",
                        color = palette.mutedText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(pendingUsers, key = { "pending_${it.phoneNumber}" }) { user ->
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UserAvatar(user.fullName, user.profileImageUri, size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = palette.inkDark)
                                Text("Mobile: ${user.phoneNumber}", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = palette.mutedText)
                                Text("CNIC: ${user.cnic}", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = palette.mutedText)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onApprove(user.phoneNumber) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.success,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("approve_user_${user.phoneNumber}")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onReject(user.phoneNumber) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reject_user_${user.phoneNumber}")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = palette.danger, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject", color = palette.danger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Registered Users (${allUsers.size})",
                color = palette.headerRed,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (allUsers.isEmpty()) {
            item {
                Text("No registered users yet.", color = palette.mutedText, fontSize = 12.sp)
            }
        } else {
            items(allUsers, key = { it.phoneNumber }) { user ->
                Surface(
                    color = palette.pageWhite,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                UserAvatar(user.fullName, user.profileImageUri, size = 42.dp)
                                Column {
                                    Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.inkDark)
                                    Text(user.phoneNumber, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = palette.mutedText)
                                    Text("Bal: Rs. ${"%,.2f".format(user.balancePkr)}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = palette.headerRed)
                                }
                            }
                            Surface(
                                color = when (user.status) {
                                    AccountStatus.APPROVED -> palette.success.copy(alpha = 0.14f)
                                    AccountStatus.PENDING -> Color(0xFFFEF3C7)
                                    else -> palette.danger.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = user.status.name,
                                    color = when (user.status) {
                                        AccountStatus.APPROVED -> palette.success
                                        AccountStatus.PENDING -> Color(0xFFB45309)
                                        else -> palette.danger
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onOpenBonusDialog(user) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.headerRed,
                                    contentColor = palette.accentYellow
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CardGiftcard, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bonus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onFreezeToggle(user) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (user.status == AccountStatus.FROZEN) "Unfreeze" else "Freeze",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.headerRed
                                )
                            }
                            OutlinedButton(
                                onClick = { onSuspendToggle(user) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (user.status == AccountStatus.SUSPENDED) "Activate" else "Suspend",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.danger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminServicesTab(
    appConfig: AppConfigEntity,
    allMethods: List<PaymentMethodEntity>,
    onUpdateConfig: (AppConfigEntity) -> Unit,
    onToggleMethod: (key: String, released: Boolean) -> Unit,
    onAddCustomMethod: (title: String, subtitle: String, category: String, iconKey: String, releaseNow: Boolean) -> Unit,
    onDeleteCustomMethod: (key: String) -> Unit,
    onUseMethodAsAdmin: (PaymentMethodEntity) -> Unit
) {
    val palette = LocalWalletPalette.current
    var newTitle by remember { mutableStateOf("") }
    var newSubtitle by remember { mutableStateOf("") }
    var releaseNewNow by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "App Feature Controls",
                color = palette.headerRed,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        item {
            Surface(
                color = palette.pageWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminToggleRow(
                        title = "Global Payments Active",
                        subtitle = "Turn OFF to pause all user payments & transfers",
                        checked = appConfig.globalPaymentsEnabled,
                        testTag = "toggle_global_payments",
                        onCheckedChange = { onUpdateConfig(appConfig.copy(globalPaymentsEnabled = it)) }
                    )
                    HorizontalDivider(color = palette.cardBorder)
                    AdminToggleRow(
                        title = "QR Code & Scanner Visible",
                        subtitle = "Show or hide QR scanner & My QR across the app",
                        checked = appConfig.isQrScannerEnabled,
                        testTag = "toggle_qr_scanner",
                        onCheckedChange = { onUpdateConfig(appConfig.copy(isQrScannerEnabled = it)) }
                    )
                    HorizontalDivider(color = palette.cardBorder)
                    AdminToggleRow(
                        title = "Physical Debit & Credit Card",
                        subtitle = "Show or hide physical/virtual cards for users",
                        checked = appConfig.isPhysicalCardEnabled,
                        testTag = "toggle_physical_card",
                        onCheckedChange = { onUpdateConfig(appConfig.copy(isPhysicalCardEnabled = it)) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Payment Methods (Release to Users)",
                color = palette.headerRed,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Hidden methods stay Admin-only until you toggle them ON.",
                color = palette.mutedText,
                fontSize = 11.sp
            )
        }

        items(allMethods, key = { it.methodKey }) { method ->
            Surface(
                color = palette.pageWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.headerRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = WalletIcons.resolve(method.iconKey),
                                contentDescription = null,
                                tint = palette.accentYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(method.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.inkDark)
                            Text(
                                text = if (method.isReleased) "Released to Users" else "Admin Only (Hidden)",
                                color = if (method.isReleased) palette.success else palette.mutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onUseMethodAsAdmin(method) }) {
                            Text("Use", color = palette.headerRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (!method.isBuiltIn) {
                            IconButton(onClick = { onDeleteCustomMethod(method.methodKey) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = palette.danger)
                            }
                        }
                        Switch(
                            checked = method.isReleased,
                            onCheckedChange = { onToggleMethod(method.methodKey, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.accentYellow,
                                checkedTrackColor = palette.headerRed
                            ),
                            modifier = Modifier.testTag("switch_method_${method.methodKey}")
                        )
                    }
                }
            }
        }

        // Add Custom Payment Method
        item {
            Surface(
                color = palette.headerRed.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.headerRed.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Add New Payment Method",
                        color = palette.headerRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Method Name (e.g., NayaPay, Raast)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSubtitle,
                        onValueChange = { newSubtitle = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Release immediately to users", fontSize = 12.sp, color = palette.inkDark)
                        Switch(
                            checked = releaseNewNow,
                            onCheckedChange = { releaseNewNow = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.accentYellow,
                                checkedTrackColor = palette.headerRed
                            )
                        )
                    }
                    Button(
                        onClick = {
                            onAddCustomMethod(newTitle, newSubtitle, "WALLET", "wallet", releaseNewNow)
                            newTitle = ""
                            newSubtitle = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.headerRed,
                            contentColor = palette.accentYellow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Payment Method", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBrandingTab(
    appConfig: AppConfigEntity,
    onUpdateConfig: (AppConfigEntity) -> Unit
) {
    val palette = LocalWalletPalette.current
    var walletNameInput by remember(appConfig.walletName) { mutableStateOf(appConfig.walletName) }
    var tileSizeFloat by remember(appConfig.serviceTileSizeDp) {
        mutableFloatStateOf(appConfig.serviceTileSizeDp.toFloat())
    }

    val curPrimary = Color(appConfig.primaryColorHex)
    var redR by remember(appConfig.primaryColorHex) { mutableFloatStateOf(curPrimary.red) }
    var redG by remember(appConfig.primaryColorHex) { mutableFloatStateOf(curPrimary.green) }
    var redB by remember(appConfig.primaryColorHex) { mutableFloatStateOf(curPrimary.blue) }

    val curAccent = Color(appConfig.accentTextColorHex)
    var accR by remember(appConfig.accentTextColorHex) { mutableFloatStateOf(curAccent.red) }
    var accG by remember(appConfig.accentTextColorHex) { mutableFloatStateOf(curAccent.green) }
    var accB by remember(appConfig.accentTextColorHex) { mutableFloatStateOf(curAccent.blue) }

    val curInput = Color(appConfig.inputTextColorHex)
    var inpR by remember(appConfig.inputTextColorHex) { mutableFloatStateOf(curInput.red) }
    var inpG by remember(appConfig.inputTextColorHex) { mutableFloatStateOf(curInput.green) }
    var inpB by remember(appConfig.inputTextColorHex) { mutableFloatStateOf(curInput.blue) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = palette.pageWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Transfer & Service Icon Size", color = palette.headerRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Adjust Transfer Money & Service box size (${tileSizeFloat.toInt()} dp)",
                        fontSize = 12.sp,
                        color = palette.mutedText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(56 to "Small", 68 to "Medium", 84 to "Large").forEach { (dpVal, label) ->
                            val active = tileSizeFloat.toInt() in (dpVal - 4)..(dpVal + 4)
                            OutlinedButton(
                                onClick = {
                                    tileSizeFloat = dpVal.toFloat()
                                    onUpdateConfig(appConfig.copy(serviceTileSizeDp = dpVal))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) palette.headerRed else palette.mutedText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Slider(
                        value = tileSizeFloat,
                        onValueChange = { tileSizeFloat = it },
                        onValueChangeFinished = {
                            onUpdateConfig(appConfig.copy(serviceTileSizeDp = tileSizeFloat.toInt().coerceIn(50, 96)))
                        },
                        valueRange = 50f..96f,
                        colors = SliderDefaults.colors(thumbColor = palette.headerRed, activeTrackColor = palette.headerRed)
                    )
                }
            }
        }

        item {
            Surface(
                color = palette.pageWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Wallet Name & Home Icon", color = palette.headerRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(
                        value = walletNameInput,
                        onValueChange = { walletNameInput = it },
                        label = { Text("App Header Wallet Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (walletNameInput.isNotBlank()) {
                                onUpdateConfig(appConfig.copy(walletName = walletNameInput.trim()))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.headerRed,
                            contentColor = palette.accentYellow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Wallet Name", fontWeight = FontWeight.Bold)
                    }

                    Text("Select Home Icon", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = palette.inkDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WalletIcons.homeIconOptions.forEach { (key, label) ->
                            val selected = appConfig.homeIconKey == key
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) palette.headerRed else palette.headerRed.copy(alpha = 0.08f))
                                    .clickable { onUpdateConfig(appConfig.copy(homeIconKey = key)) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = WalletIcons.resolve(key),
                                    contentDescription = label,
                                    tint = if (selected) palette.accentYellow else palette.headerRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    color = if (selected) palette.accentYellow else palette.headerRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = palette.pageWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme Color Presets", color = palette.headerRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    themePresets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    onUpdateConfig(
                                        appConfig.copy(
                                            primaryColorHex = preset.primaryHex,
                                            accentTextColorHex = preset.accentHex,
                                            pageBackgroundHex = preset.pageHex
                                        )
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(preset.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = palette.inkDark)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(22.dp).clip(CircleShape).background(Color(preset.primaryHex)))
                                Box(Modifier.size(22.dp).clip(CircleShape).background(Color(preset.accentHex)).border(1.dp, palette.cardBorder, CircleShape))
                                Box(Modifier.size(22.dp).clip(CircleShape).background(Color(preset.pageHex)).border(1.dp, palette.cardBorder, CircleShape))
                            }
                        }
                    }

                    HorizontalDivider(color = palette.cardBorder)
                    Text("Transfer Number & Amount Text Color", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.headerRed)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0xFF000000L to "Black",
                            0xFFC8102EL to "Red",
                            0xFF1E3A8AL to "Navy",
                            0xFF047857L to "Green"
                        ).forEach { (hexVal, label) ->
                            OutlinedButton(
                                onClick = { onUpdateConfig(appConfig.copy(inputTextColorHex = hexVal)) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, color = Color(hexVal), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    val previewInput = Color(inpR, inpG, inpB, 1f)
                    Text("Input Text Preview: 03001234567 • Rs. 5,000", color = previewInput, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Slider(value = inpR, onValueChange = { inpR = it }, colors = SliderDefaults.colors(thumbColor = palette.inkDark, activeTrackColor = palette.inkDark))
                    Slider(value = inpG, onValueChange = { inpG = it }, colors = SliderDefaults.colors(thumbColor = palette.inkDark, activeTrackColor = palette.inkDark))
                    Slider(value = inpB, onValueChange = { inpB = it }, colors = SliderDefaults.colors(thumbColor = palette.inkDark, activeTrackColor = palette.inkDark))

                    HorizontalDivider(color = palette.cardBorder)
                    Text("Custom Primary & Accent Colors", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = palette.headerRed)
                    val previewPrimary = Color(redR, redG, redB, 1f)
                    val previewAccent = Color(accR, accG, accB, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(previewPrimary)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LIVE PREVIEW • ${appConfig.walletName}",
                            color = previewAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text("Header / Button Background (R / G / B)", fontSize = 11.sp, color = palette.mutedText)
                    Slider(value = redR, onValueChange = { redR = it }, colors = SliderDefaults.colors(thumbColor = palette.headerRed, activeTrackColor = palette.headerRed))
                    Slider(value = redG, onValueChange = { redG = it }, colors = SliderDefaults.colors(thumbColor = palette.headerRed, activeTrackColor = palette.headerRed))
                    Slider(value = redB, onValueChange = { redB = it }, colors = SliderDefaults.colors(thumbColor = palette.headerRed, activeTrackColor = palette.headerRed))

                    Text("Text / Accent Color (R / G / B)", fontSize = 11.sp, color = palette.mutedText)
                    Slider(value = accR, onValueChange = { accR = it }, colors = SliderDefaults.colors(thumbColor = palette.accentYellow, activeTrackColor = palette.headerRed))
                    Slider(value = accG, onValueChange = { accG = it }, colors = SliderDefaults.colors(thumbColor = palette.accentYellow, activeTrackColor = palette.headerRed))
                    Slider(value = accB, onValueChange = { accB = it }, colors = SliderDefaults.colors(thumbColor = palette.accentYellow, activeTrackColor = palette.headerRed))

                    Button(
                        onClick = {
                            onUpdateConfig(
                                appConfig.copy(
                                    primaryColorHex = previewPrimary.toArgb().toLong() and 0xFFFFFFFFL,
                                    accentTextColorHex = previewAccent.toArgb().toLong() and 0xFFFFFFFFL,
                                    inputTextColorHex = previewInput.toArgb().toLong() and 0xFFFFFFFFL
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.headerRed,
                            contentColor = palette.accentYellow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Custom Colors", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAdsTab(
    allBanners: List<AdBannerEntity>,
    onPublishBanner: (title: String, mediaUri: Uri, targetUrl: String, duration: DurationOption) -> Unit,
    onDeleteBanner: (Long) -> Unit
) {
    val palette = LocalWalletPalette.current
    var adTitle by remember { mutableStateOf("") }
    var adUrl by remember { mutableStateOf("https://") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDuration by remember { mutableStateOf(adDurationOptions[2]) } // 24 Hours default

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedMediaUri = uri
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = palette.pageWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Publish Ad Banner (Image / Video)", color = palette.headerRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(
                        value = adTitle,
                        onValueChange = { adTitle = it },
                        label = { Text("Banner Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = adUrl,
                        onValueChange = { adUrl = it },
                        label = { Text("Product Website URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = palette.headerRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedMediaUri == null) "Pick Image or Video from Gallery" else "Media Selected ✓",
                            color = palette.headerRed,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("Ad Duration", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = palette.inkDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        adDurationOptions.forEach { option ->
                            val active = selectedDuration == option
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (active) palette.headerRed else palette.headerRed.copy(alpha = 0.08f))
                                    .clickable { selectedDuration = option }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = option.label,
                                    color = if (active) palette.accentYellow else palette.headerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val uri = selectedMediaUri ?: return@Button
                            onPublishBanner(adTitle, uri, adUrl, selectedDuration)
                            adTitle = ""
                            adUrl = "https://"
                            selectedMediaUri = null
                        },
                        enabled = selectedMediaUri != null && adTitle.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.headerRed,
                            contentColor = palette.accentYellow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Publish Banner", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Published Banners (${allBanners.size})", color = palette.headerRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(allBanners, key = { it.id }) { banner ->
            val expired = banner.expiresAt <= System.currentTimeMillis()
            val expStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(banner.expiresAt))
            Surface(
                color = palette.pageWhite,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(banner.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.inkDark)
                        Text(banner.targetUrl, fontSize = 11.sp, color = palette.mutedText)
                        Text(
                            text = if (expired) "Expired ($expStr)" else "${banner.durationLabel} • Ends $expStr",
                            color = if (expired) palette.danger else palette.success,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = { onDeleteBanner(banner.id) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete banner", tint = palette.danger)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalWalletPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.inkDark)
            Text(subtitle, fontSize = 11.sp, color = palette.mutedText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.accentYellow,
                checkedTrackColor = palette.headerRed
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun BonusDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSend: (Double, String) -> Unit
) {
    val palette = LocalWalletPalette.current
    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("Admin Bonus") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Send Bonus to ${user.fullName}", fontWeight = FontWeight.Bold, color = palette.headerRed)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mobile: ${user.phoneNumber}", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp)
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Bonus Amount (PKR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteStr,
                    onValueChange = { noteStr = it },
                    label = { Text("Note") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) onSend(amt, noteStr)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.headerRed,
                    contentColor = palette.accentYellow
                )
            ) {
                Text("Send Bonus", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.mutedText)
            }
        }
    )
}
