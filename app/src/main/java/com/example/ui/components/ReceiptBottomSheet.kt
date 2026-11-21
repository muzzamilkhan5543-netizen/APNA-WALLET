package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalWalletPalette
import com.example.util.QrAndMediaUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptBottomSheet(
    transaction: TransactionEntity?,
    walletName: String,
    onDismiss: () -> Unit,
    onSavedMessage: (String) -> Unit
) {
    val palette = LocalWalletPalette.current
    val context = LocalContext.current
    var showTick by remember { mutableStateOf(false) }

    LaunchedEffect(transaction?.transactionRef) {
        if (transaction != null) {
            showTick = false
            delay(120)
            showTick = true
        }
    }

    AnimatedVisibility(
        visible = transaction != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = transaction != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                transaction?.let { tx ->
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = palette.pageWhite,
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.66f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            // Top Red Strip with Yellow Title
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.headerRed)
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    Text(
                                        text = walletName,
                                        color = palette.accentYellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Digital Transaction Receipt",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close receipt",
                                        tint = palette.accentYellow
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Animated Checkmark Tick
                                AnimatedVisibility(
                                    visible = showTick,
                                    enter = scaleIn(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(palette.success)
                                            .border(3.dp, palette.accentYellow, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Success",
                                            tint = Color.White,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Transaction Successful",
                                    color = palette.success,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )

                                Text(
                                    text = "Rs. ${"%,.2f".format(tx.amountPkr)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = palette.headerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                val dateFormatted = remember(tx.timestamp) {
                                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                        .format(Date(tx.timestamp))
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(palette.headerRed.copy(alpha = 0.04f))
                                        .border(1.dp, palette.headerRed.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReceiptRow("TID", tx.transactionRef)
                                    ReceiptRow("Date & Time", dateFormatted)
                                    ReceiptRow("Method", tx.methodTitle)
                                    HorizontalDivider(color = palette.cardBorder)
                                    ReceiptRow("From", "${tx.senderName}\n${tx.senderPhone}")
                                    ReceiptRow("To", "${tx.receiverName}\n${tx.receiverPhone}")
                                    ReceiptRow("Fee", "Rs. ${"%,.2f".format(tx.feePkr)}")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val res = QrAndMediaUtils.saveReceiptToGallery(
                                            context = context,
                                            tx = tx,
                                            walletName = walletName,
                                            primaryColor = palette.headerRed.toArgb(),
                                            accentColor = palette.accentYellow.toArgb()
                                        )
                                        res.onSuccess {
                                            onSavedMessage("Receipt saved to Gallery (Pictures/ApnaWallet)")
                                        }.onFailure {
                                            onSavedMessage("Could not save receipt: ${it.message}")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.headerRed,
                                        contentColor = palette.accentYellow
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("save_receipt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Screenshot", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("receipt_done_button")
                                ) {
                                    Text("Done", color = palette.headerRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    val palette = LocalWalletPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = palette.mutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = palette.inkDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
