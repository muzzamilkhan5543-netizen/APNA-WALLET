package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.LocalWalletPalette

object WalletIcons {
    val homeIconOptions = listOf(
        "wallet" to "Wallet",
        "shield" to "Shield",
        "bank" to "Bank",
        "vault" to "Vault",
        "flash" to "Flash",
        "star" to "Star"
    )

    fun resolve(key: String): ImageVector = when (key) {
        "wallet" -> Icons.Filled.AccountBalanceWallet
        "shield" -> Icons.Filled.Shield
        "bank" -> Icons.Filled.AccountBalance
        "vault" -> Icons.Filled.Savings
        "flash" -> Icons.Filled.Bolt
        "star" -> Icons.Filled.Star
        "send" -> Icons.AutoMirrored.Filled.Send
        "phone" -> Icons.Filled.PhoneAndroid
        "receipt" -> Icons.Filled.ReceiptLong
        "add_money" -> Icons.Filled.AddCard
        else -> Icons.Filled.Payments
    }
}

@Composable
fun UserAvatar(
    fullName: String,
    imageUri: String?,
    size: Dp = 48.dp,
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalWalletPalette.current
    val initials = fullName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "AW" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.accentYellow)
            .then(
                if (borderColor != null) Modifier.border(2.dp, borderColor, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(imageUri),
                contentDescription = "$fullName profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                color = palette.headerRed,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp
            )
        }
    }
}
