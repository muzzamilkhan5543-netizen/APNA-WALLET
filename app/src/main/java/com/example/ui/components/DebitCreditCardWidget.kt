package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.UserEntity
import com.example.security.SecurityUtils
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalWalletPalette

@Composable
fun DebitCreditCardDialog(
    user: UserEntity,
    walletName: String,
    onPickCardPhoto: () -> Unit,
    onClearCardPhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalWalletPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = palette.pageWhite,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Debit & Credit Cards",
                        color = palette.headerRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close card details",
                            tint = palette.headerRed
                        )
                    }
                }

                DebitCreditCardWidget(
                    user = user,
                    walletName = walletName,
                    onPickCardPhoto = onPickCardPhoto,
                    onClearCardPhoto = onClearCardPhoto
                )
            }
        }
    }
}

@Composable
fun DebitCreditCardWidget(
    user: UserEntity,
    walletName: String,
    onPickCardPhoto: () -> Unit,
    onClearCardPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWalletPalette.current
    var isCreditSelected by remember { mutableStateOf(false) }
    var showSensitiveNumbers by remember { mutableStateOf(false) }

    val cardNumber = if (isCreditSelected) user.creditCardNumber else user.debitCardNumber
    val cardExpiry = if (isCreditSelected) user.creditCardExpiry else user.debitCardExpiry
    val cardCvc = if (isCreditSelected) user.creditCardCvc else user.debitCardCvc
    val cardTypeLabel = if (isCreditSelected) "PLATINUM CREDIT CARD" else "PHYSICAL & VIRTUAL DEBIT CARD"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Spacious Equal-Width Tab Switcher so Credit Card & Debit Card both have generous space
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.headerRed.copy(alpha = 0.09f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(false to "Debit Card", true to "Credit Card").forEach { (isCredit, label) ->
                val selected = isCreditSelected == isCredit
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) palette.headerRed else Color.Transparent)
                        .clickable { isCreditSelected = isCredit }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) palette.accentYellow else palette.headerRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Physical Card Canvas (Generous 220dp height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = if (isCreditSelected) {
                            listOf(Color(0xFF1F1F23), palette.headerRedDark, palette.headerRed)
                        } else {
                            listOf(palette.headerRed, palette.headerRedDark, Color(0xFF5A0614))
                        }
                    )
                )
                .border(1.5.dp, palette.accentYellow.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
        ) {
            if (!user.cardBackgroundUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(user.cardBackgroundUri),
                    contentDescription = "Custom card artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.height * 0.7f,
                        center = Offset(size.width * 0.88f, size.height * 0.2f)
                    )
                    drawCircle(
                        color = Color(0xFFFFD100).copy(alpha = 0.08f),
                        radius = size.height * 0.55f,
                        center = Offset(size.width * 0.12f, size.height * 0.9f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = walletName,
                            color = palette.accentYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = cardTypeLabel,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Contactless,
                            contentDescription = "Contactless NFC",
                            tint = palette.accentYellow,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showSensitiveNumbers = !showSensitiveNumbers },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("toggle_card_visibility_button")
                        ) {
                            Icon(
                                imageVector = if (showSensitiveNumbers) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showSensitiveNumbers) "Hide card details" else "Show card details",
                                tint = palette.accentYellow
                            )
                        }
                    }
                }

                // EMV Chip Graphic
                Canvas(modifier = Modifier.size(width = 46.dp, height = 34.dp)) {
                    drawRoundRect(
                        color = Color(0xFFFFD100),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawLine(
                        color = Color(0xFFB48600),
                        start = Offset(0f, size.height * 0.5f),
                        end = Offset(size.width, size.height * 0.5f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFFB48600),
                        start = Offset(size.width * 0.35f, 0f),
                        end = Offset(size.width * 0.35f, size.height),
                        strokeWidth = 2f
                    )
                    drawRect(
                        color = Color(0xFFB48600),
                        topLeft = Offset(size.width * 0.35f, size.height * 0.25f),
                        size = Size(size.width * 0.3f, size.height * 0.5f)
                    )
                }

                Text(
                    text = if (showSensitiveNumbers) {
                        SecurityUtils.formatCardGrouped(cardNumber)
                    } else {
                        SecurityUtils.formatCardMasked(cardNumber)
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = 1.4.sp,
                    color = Color.White,
                    modifier = Modifier.testTag("card_number_text")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "CARD HOLDER",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = user.fullName.uppercase(),
                            color = palette.accentYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "EXPIRES",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 9.sp
                            )
                            Text(
                                text = if (showSensitiveNumbers) cardExpiry else "••/••",
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "CVC",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 9.sp
                            )
                            Text(
                                text = if (showSensitiveNumbers) cardCvc else "•••",
                                fontFamily = JetBrainsMonoFontFamily,
                                color = palette.accentYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Card Photo Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPickCardPhoto,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.headerRed,
                    contentColor = palette.accentYellow
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Set card photo",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Card Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            if (!user.cardBackgroundUri.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onClearCardPhoto,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Remove card photo",
                        tint = palette.danger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
