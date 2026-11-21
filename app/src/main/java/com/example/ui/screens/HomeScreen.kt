package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountStatus
import com.example.data.local.AdBannerEntity
import com.example.data.local.AppConfigEntity
import com.example.data.local.PaymentMethodEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.ui.components.AdBannerCarousel
import com.example.ui.components.DebitCreditCardDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.WalletIcons
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalWalletPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HomeFooterTab { HOME, HISTORY, PROFILE }

private sealed class CompactActionItem {
    data class Method(val method: PaymentMethodEntity) : CompactActionItem()
    data object DebitCard : CompactActionItem()
}

@Composable
fun HomeScreen(
    user: UserEntity,
    appConfig: AppConfigEntity,
    releasedMethods: List<PaymentMethodEntity>,
    activeBanners: List<AdBannerEntity>,
    transactions: List<TransactionEntity>,
    onOpenMethod: (PaymentMethodEntity) -> Unit,
    onOpenQrScanner: (initialTab: Int) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onUpdateDp: (Uri) -> Unit,
    onUpdateCardPhoto: (Uri?) -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onLogout: () -> Unit
) {
    val palette = LocalWalletPalette.current
    var hideBalance by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(HomeFooterTab.HOME) }
    var showCardDialog by remember { mutableStateOf(false) }

    val dpPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUpdateDp(uri)
    }

    val cardPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUpdateCardPhoto(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Red Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = palette.headerRed,
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Brand Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.accentYellow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = WalletIcons.resolve(appConfig.homeIconKey),
                                    contentDescription = appConfig.walletName,
                                    tint = palette.headerRed,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = appConfig.walletName,
                                color = palette.accentYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 0.7.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (user.role == UserRole.ADMIN) {
                                Surface(
                                    color = palette.accentYellow,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .testTag("open_admin_panel_button")
                                        .clickable { onOpenAdminPanel() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AdminPanelSettings,
                                            contentDescription = "Admin Panel",
                                            tint = palette.headerRed,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Admin",
                                            color = palette.headerRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Logout",
                                    tint = palette.accentYellow
                                )
                            }
                        }
                    }

                    // Physical Debit-Card-Sized Balance Hero Card (only on HOME tab to keep History/Profile spacious)
                    if (activeTab == HomeFooterTab.HOME) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 192.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.24f),
                                            Color.Black.copy(alpha = 0.36f)
                                        )
                                    )
                                )
                                .border(1.5.dp, palette.accentYellow.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                                .padding(18.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 156.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top User Row: Full Name + Phone + DP + Eye Toggle (Full horizontal space for Name!)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        UserAvatar(
                                            fullName = user.fullName,
                                            imageUri = user.profileImageUri,
                                            size = 52.dp,
                                            borderColor = palette.accentYellow,
                                            modifier = Modifier
                                                .testTag("change_dp_avatar")
                                                .clickable {
                                                    dpPicker.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.fullName,
                                                color = palette.accentYellow,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = user.phoneNumber,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = Color.White.copy(alpha = 0.94f),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { hideBalance = !hideBalance },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (hideBalance) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = "Toggle balance visibility",
                                            tint = palette.accentYellow
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Bottom Balance Section (Large & Spacious)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "AVAILABLE BALANCE",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = when {
                                                hideBalance -> "Rs. ••••••"
                                                user.isUnlimitedBalance -> "∞ Unlimited"
                                                else -> "Rs. ${"%,.2f".format(user.balancePkr)}"
                                            },
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = palette.accentYellow,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 29.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("wallet_balance_text")
                                        )
                                    }

                                    Surface(
                                        color = palette.accentYellow.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.border(
                                            1.dp,
                                            palette.accentYellow.copy(alpha = 0.4f),
                                            RoundedCornerShape(50)
                                        )
                                    ) {
                                        Text(
                                            text = if (user.status == AccountStatus.FROZEN) "FROZEN" else "PKR ACCOUNT",
                                            color = palette.accentYellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = when (activeTab) {
                                HomeFooterTab.HISTORY -> "Transaction History"
                                HomeFooterTab.PROFILE -> "My Profile & Account"
                                HomeFooterTab.HOME -> ""
                            },
                            color = palette.accentYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Tab Content Body (Pure White)
            when (activeTab) {
                HomeFooterTab.HOME -> {
                    val tileSizeDp = appConfig.serviceTileSizeDp.coerceIn(50, 96).dp
                    val compactActions = remember(releasedMethods, appConfig.isPhysicalCardEnabled) {
                        buildList {
                            // Ensure Transfer Money comes first, followed by Debit Card right beside it, then all other Admin-released services!
                            val transfer = releasedMethods.find { it.methodKey == "transfer_money" }
                            if (transfer != null) add(CompactActionItem.Method(transfer))
                            if (appConfig.isPhysicalCardEnabled) add(CompactActionItem.DebitCard)
                            releasedMethods.filter { it.methodKey != "transfer_money" }.forEach {
                                add(CompactActionItem.Method(it))
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 14.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (user.status == AccountStatus.FROZEN) {
                            item {
                                StatusAlertCard(
                                    text = "Your account is Frozen by Admin. Transfers are paused.",
                                    color = palette.danger,
                                    bg = Color(0xFFFEF2F2)
                                )
                            }
                        }

                        if (!appConfig.globalPaymentsEnabled) {
                            item {
                                StatusAlertCard(
                                    text = "Payments are temporarily paused by Admin maintenance.",
                                    color = Color(0xFFB45309),
                                    bg = Color(0xFFFFFBEB)
                                )
                            }
                        }

                        // Compact 4-Column Quick Actions Grid (Small Icons, Never Elongated!)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Quick Actions",
                                    color = palette.headerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                if (compactActions.isEmpty()) {
                                    Text(
                                        text = "No services are currently enabled by Admin.",
                                        color = palette.mutedText,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        compactActions.chunked(4).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowItems.forEach { item ->
                                                    when (item) {
                                                        is CompactActionItem.DebitCard -> {
                                                            CompactServiceTile(
                                                                title = "Debit Card",
                                                                icon = Icons.Filled.CreditCard,
                                                                boxSize = tileSizeDp,
                                                                testTag = "home_debit_card_tile",
                                                                onClick = { showCardDialog = true },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                        is CompactActionItem.Method -> {
                                                            val icon = if (item.method.methodKey == "transfer_money") {
                                                                Icons.Filled.NorthEast
                                                            } else {
                                                                WalletIcons.resolve(item.method.iconKey)
                                                            }
                                                            CompactServiceTile(
                                                                title = item.method.title,
                                                                icon = icon,
                                                                boxSize = tileSizeDp,
                                                                testTag = "method_card_${item.method.methodKey}",
                                                                onClick = { onOpenMethod(item.method) },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                                repeat(4 - rowItems.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (activeBanners.isNotEmpty()) {
                            item {
                                AdBannerCarousel(banners = activeBanners)
                            }
                        }

                        // Compact Recent Activity Preview (Max 3 items; full list in History tab!)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Activity",
                                    color = palette.headerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (transactions.isNotEmpty()) {
                                    TextButton(onClick = { activeTab = HomeFooterTab.HISTORY }) {
                                        Text(
                                            text = "See All",
                                            color = palette.headerRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (transactions.isEmpty()) {
                            item {
                                Surface(
                                    color = palette.headerRed.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                ) {
                                    Text(
                                        text = "No transactions yet. Tap Transfer Money to send funds.",
                                        color = palette.mutedText,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(transactions.take(3), key = { it.id }) { tx ->
                                TransactionRowCard(
                                    tx = tx,
                                    isIncoming = tx.receiverPhone == user.phoneNumber,
                                    onClick = { onSelectTransaction(tx) }
                                )
                            }
                        }
                    }
                }

                HomeFooterTab.HISTORY -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (transactions.isEmpty()) {
                            item {
                                Surface(
                                    color = palette.headerRed.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("No Transactions Found", color = palette.headerRed, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Your sent and received payments will appear here.", color = palette.mutedText, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            items(transactions, key = { "hist_${it.id}" }) { tx ->
                                TransactionRowCard(
                                    tx = tx,
                                    isIncoming = tx.receiverPhone == user.phoneNumber,
                                    onClick = { onSelectTransaction(tx) }
                                )
                            }
                        }
                    }
                }

                HomeFooterTab.PROFILE -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Surface(
                                color = palette.pageWhite,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UserAvatar(
                                        fullName = user.fullName,
                                        imageUri = user.profileImageUri,
                                        size = 76.dp,
                                        borderColor = palette.headerRed,
                                        modifier = Modifier.clickable {
                                            dpPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )
                                    Text(
                                        text = user.fullName,
                                        color = palette.headerRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Tap photo to update DP",
                                        color = palette.mutedText,
                                        fontSize = 11.sp
                                    )
                                    HorizontalDivider(color = palette.cardBorder)
                                    ProfileInfoRow("Mobile Number", user.phoneNumber)
                                    ProfileInfoRow("CNIC", user.cnic)
                                    ProfileInfoRow("Account Status", user.status.name)
                                    ProfileInfoRow("Role", user.role.name)
                                }
                            }
                        }

                        if (appConfig.isPhysicalCardEnabled) {
                            item {
                                Button(
                                    onClick = { showCardDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.headerRed,
                                        contentColor = palette.accentYellow
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Debit & Credit Cards", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (user.role == UserRole.ADMIN) {
                            item {
                                Button(
                                    onClick = onOpenAdminPanel,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accentYellow,
                                        contentColor = palette.headerRed
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Admin Control Center", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = onLogout,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = palette.danger, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lock & Sign Out", color = palette.danger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // JazzCash-Style 5-Option Compact Bottom Navigation Bar:
        // [Home] [History] [Scan QR (center)] [My QR] [Profile]
        Surface(
            color = palette.pageWhite,
            shadowElevation = 14.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.8.dp, color = palette.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FooterNavItem(
                        label = "Home",
                        icon = Icons.Filled.Home,
                        selected = activeTab == HomeFooterTab.HOME,
                        testTag = "footer_home_tab",
                        onClick = { activeTab = HomeFooterTab.HOME },
                        modifier = Modifier.weight(1f)
                    )
                    FooterNavItem(
                        label = "History",
                        icon = Icons.Filled.ReceiptLong,
                        selected = activeTab == HomeFooterTab.HISTORY,
                        testTag = "footer_history_tab",
                        onClick = { activeTab = HomeFooterTab.HISTORY },
                        modifier = Modifier.weight(1f)
                    )

                    if (appConfig.isQrScannerEnabled) {
                        // Center Scan QR Button (JazzCash position)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("footer_scan_qr_tab")
                                .clickable { onOpenQrScanner(0) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(y = (-6).dp)
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(palette.headerRed)
                                    .border(2.dp, palette.accentYellow, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = palette.accentYellow,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "Scan QR",
                                color = palette.headerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        FooterNavItem(
                            label = "My QR",
                            icon = Icons.Filled.QrCode2,
                            selected = false,
                            testTag = "footer_my_qr_tab",
                            onClick = { onOpenQrScanner(1) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FooterNavItem(
                        label = "Profile",
                        icon = Icons.Filled.Person,
                        selected = activeTab == HomeFooterTab.PROFILE,
                        testTag = "footer_profile_tab",
                        onClick = { activeTab = HomeFooterTab.PROFILE },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showCardDialog) {
        DebitCreditCardDialog(
            user = user,
            walletName = appConfig.walletName,
            onPickCardPhoto = {
                cardPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClearCardPhoto = { onUpdateCardPhoto(null) },
            onDismiss = { showCardDialog = false }
        )
    }
}

@Composable
private fun CompactServiceTile(
    title: String,
    icon: ImageVector,
    boxSize: Dp,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWalletPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .testTag(testTag)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(boxSize)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.headerRed)
                .border(1.dp, palette.accentYellow.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = palette.accentYellow,
                modifier = Modifier.size((boxSize.value * 0.44f).dp)
            )
        }
        Text(
            text = title,
            color = palette.inkDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FooterNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWalletPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .testTag(testTag)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) palette.headerRed else palette.mutedText,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            color = if (selected) palette.headerRed else palette.mutedText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusAlertCard(text: String, color: Color, bg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Block, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(text = text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    val palette = LocalWalletPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = palette.mutedText, fontSize = 13.sp)
        Text(value, color = palette.inkDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun TransactionRowCard(
    tx: TransactionEntity,
    isIncoming: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalWalletPalette.current
    val dateStr = remember(tx.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    }

    Surface(
        color = palette.pageWhite,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isIncoming) palette.success.copy(alpha = 0.12f)
                            else palette.headerRed.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncoming) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = if (isIncoming) palette.success else palette.headerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isIncoming) "From ${tx.senderName}" else "To ${tx.receiverName}",
                        color = palette.inkDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tx.methodTitle} • $dateStr",
                        color = palette.mutedText,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncoming) "+" else "-"}Rs. ${"%,.0f".format(tx.amountPkr)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    color = if (isIncoming) palette.success else palette.headerRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = tx.transactionRef,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = palette.mutedText,
                    fontSize = 10.sp
                )
            }
        }
    }
}
