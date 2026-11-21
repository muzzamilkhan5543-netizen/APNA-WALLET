package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountStatus
import com.example.data.local.UserEntity
import com.example.data.repository.TransferResult
import com.example.security.SecurityUtils
import com.example.ui.components.CustomFintechKeypad
import com.example.ui.components.UserAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalWalletPalette

private enum class TransferStep { INPUT, REVIEW, MPIN }
private enum class TransferFieldFocus { NONE, RECEIVER, AMOUNT }

@Composable
fun TransferMoneyScreen(
    sender: UserEntity,
    methodKey: String,
    methodTitle: String,
    prefilledReceiverPhone: String?,
    onLookupRegisteredUser: suspend (String) -> UserEntity?,
    onExecuteTransfer: (
        receiverInput: String,
        amount: Double,
        mpin: String,
        methodKey: String,
        onResult: (TransferResult) -> Unit
    ) -> Unit,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    val palette = LocalWalletPalette.current
    val isAddBalance = methodKey == "add_balance"
    val isStrictWalletTransfer = methodKey == "transfer_money"
    val fromQrScan = !prefilledReceiverPhone.isNullOrBlank()

    var step by remember { mutableStateOf(TransferStep.INPUT) }
    var receiverInput by remember(prefilledReceiverPhone) {
        mutableStateOf(prefilledReceiverPhone.orEmpty())
    }
    var amountInput by remember { mutableStateOf("") }
    var focusedField by remember(fromQrScan, isAddBalance) {
        mutableStateOf(
            when {
                fromQrScan || isAddBalance -> TransferFieldFocus.AMOUNT
                else -> TransferFieldFocus.RECEIVER
            }
        )
    }
    var keypadVisible by remember { mutableStateOf(false) }

    var matchedRecipient by remember { mutableStateOf<UserEntity?>(null) }
    var lookupError by remember { mutableStateOf<String?>(null) }

    var mpinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(receiverInput, methodKey) {
        lookupError = null
        matchedRecipient = null
        val normalized = SecurityUtils.normalizePhone(receiverInput)
        if (isAddBalance) return@LaunchedEffect

        if (isStrictWalletTransfer) {
            if (normalized.length == 11) {
                if (normalized == sender.phoneNumber) {
                    lookupError = "You cannot transfer money to your own number."
                } else {
                    val found = onLookupRegisteredUser(normalized)
                    if (found == null) {
                        lookupError = "Number is not registered. Only registered users can receive money."
                    } else if (found.status != AccountStatus.APPROVED) {
                        lookupError = "Recipient account is ${found.status.name.lowercase()} and cannot receive funds."
                    } else {
                        matchedRecipient = found
                    }
                }
            }
        } else if (SecurityUtils.isValidPakistaniPhone(normalized) && normalized != sender.phoneNumber) {
            val found = onLookupRegisteredUser(normalized)
            if (found != null && found.status == AccountStatus.APPROVED) {
                matchedRecipient = found
            }
        }
    }

    BackHandler {
        when (step) {
            TransferStep.MPIN -> {
                mpinInput = ""
                errorMessage = null
                step = TransferStep.REVIEW
            }
            TransferStep.REVIEW -> {
                errorMessage = null
                step = TransferStep.INPUT
            }
            TransferStep.INPUT -> onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageWhite)
            .navigationBarsPadding()
    ) {
        // Minimal Top Red Strip (keeps top clean without cluttered boxes!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.headerRed)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    when (step) {
                        TransferStep.MPIN -> {
                            mpinInput = ""
                            step = TransferStep.REVIEW
                        }
                        TransferStep.REVIEW -> step = TransferStep.INPUT
                        TransferStep.INPUT -> onBack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.accentYellow
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = methodTitle,
                    color = palette.accentYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = when (step) {
                        TransferStep.INPUT -> if (fromQrScan) "Enter Amount" else "Enter Details"
                        TransferStep.REVIEW -> "Review Transaction"
                        TransferStep.MPIN -> "MPIN Security Check"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }

        when (step) {
            TransferStep.INPUT -> {
                // Centered Input Layout (No top boxes; fields centered in the middle!)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .widthIn(max = 520.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (fromQrScan) {
                            Surface(
                                color = palette.headerRed.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.headerRed.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    UserAvatar(
                                        fullName = matchedRecipient?.fullName ?: "Recipient",
                                        imageUri = matchedRecipient?.profileImageUri,
                                        size = 44.dp
                                    )
                                    Column {
                                        Text(
                                            text = matchedRecipient?.fullName ?: "QR Recipient",
                                            color = palette.headerRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = receiverInput,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = palette.mutedText,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        } else if (!isAddBalance) {
                            // Centered Receiver Input Box
                            val isReceiverFocused = focusedField == TransferFieldFocus.RECEIVER && keypadVisible
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isReceiverFocused) palette.headerRed.copy(alpha = 0.08f)
                                        else Color.White
                                    )
                                    .border(
                                        width = if (isReceiverFocused) 2.dp else 1.2.dp,
                                        color = if (isReceiverFocused) palette.headerRed else palette.cardBorder,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .testTag("transfer_receiver_box")
                                    .clickable {
                                        focusedField = TransferFieldFocus.RECEIVER
                                        keypadVisible = true
                                    }
                                    .padding(horizontal = 18.dp, vertical = 16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (isStrictWalletTransfer) "Send To (Registered Mobile Number)"
                                        else "Recipient / Consumer Number",
                                        color = palette.inputText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = receiverInput.ifBlank { "Tap to enter number (03XXXXXXXXX)" },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = if (receiverInput.isBlank()) palette.inputText.copy(alpha = 0.72f) else palette.inputText
                                    )
                                }
                            }

                            if (matchedRecipient != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(palette.success.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = palette.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Verified: ${matchedRecipient!!.fullName}",
                                        color = palette.success,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else if (lookupError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(palette.danger.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = palette.danger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = lookupError!!,
                                        color = palette.danger,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Centered Amount Input Box
                        val isAmountFocused = focusedField == TransferFieldFocus.AMOUNT && keypadVisible
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isAmountFocused) palette.headerRed.copy(alpha = 0.08f)
                                    else palette.headerRed.copy(alpha = 0.03f)
                                )
                                .border(
                                    width = if (isAmountFocused) 2.dp else 1.5.dp,
                                    color = if (isAmountFocused) palette.headerRed else palette.inputText.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .testTag("transfer_amount_box")
                                .clickable {
                                    focusedField = TransferFieldFocus.AMOUNT
                                    keypadVisible = true
                                }
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Enter Amount (PKR)",
                                    color = palette.inputText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (amountInput.isBlank()) "Rs. 0" else "Rs. $amountInput",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 34.sp,
                                    color = palette.inputText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (sender.isUnlimitedBalance) "Available: ∞ Unlimited"
                                    else "Available: Rs. ${"%,.2f".format(sender.balancePkr)}",
                                    color = palette.inputText.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (!keypadVisible) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap a field above to open the Apna Wallet keypad",
                                color = palette.mutedText,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMessage!!,
                                color = palette.danger,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Custom In-App Numeric Keypad (No System Keyboard!)
                    if (keypadVisible) {
                        CustomFintechKeypad(
                            onDigitClick = { digit ->
                                errorMessage = null
                                if (focusedField == TransferFieldFocus.RECEIVER) {
                                    if (receiverInput.length < 16) {
                                        receiverInput += digit
                                        if (isStrictWalletTransfer && receiverInput.length == 11) {
                                            focusedField = TransferFieldFocus.AMOUNT
                                        }
                                    }
                                } else {
                                    if (amountInput.length < 9 && !(amountInput.isEmpty() && digit == "0")) {
                                        amountInput += digit
                                    }
                                }
                            },
                            onBackspace = {
                                errorMessage = null
                                if (focusedField == TransferFieldFocus.RECEIVER) {
                                    if (receiverInput.isNotEmpty()) receiverInput = receiverInput.dropLast(1)
                                } else {
                                    if (amountInput.isNotEmpty()) amountInput = amountInput.dropLast(1)
                                }
                            },
                            onClear = {
                                if (focusedField == TransferFieldFocus.RECEIVER) receiverInput = ""
                                else amountInput = ""
                            },
                            secondaryLeftLabel = if (focusedField == TransferFieldFocus.AMOUNT) "00" else "CLR",
                            onSecondaryLeftClick = if (focusedField == TransferFieldFocus.AMOUNT) {
                                {
                                    if (amountInput.isNotEmpty() && amountInput.length <= 7) {
                                        amountInput += "00"
                                    }
                                }
                            } else null
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    val amountVal = amountInput.toDoubleOrNull() ?: 0.0
                    val canProceed = amountVal > 0.0 && when {
                        isAddBalance -> true
                        isStrictWalletTransfer -> matchedRecipient != null && lookupError == null
                        else -> receiverInput.trim().length >= 4
                    }

                    Button(
                        onClick = {
                            if (!sender.isUnlimitedBalance && !isAddBalance && amountVal > sender.balancePkr) {
                                errorMessage = "Insufficient balance."
                                return@Button
                            }
                            errorMessage = null
                            keypadVisible = false
                            step = TransferStep.REVIEW
                        },
                        enabled = canProceed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.headerRed,
                            contentColor = palette.accentYellow
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .height(54.dp)
                            .testTag("transfer_continue_button")
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            TransferStep.REVIEW -> {
                // Step 2: Sender & Receiver Details Review
                val amountVal = amountInput.toDoubleOrNull() ?: 0.0
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = palette.headerRed,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Total Transfer Amount",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Rs. ${"%,.2f".format(amountVal)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = palette.accentYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 30.sp
                                )
                            }
                        }

                        // From (Sender) & To (Receiver) Cards
                        Surface(
                            color = palette.pageWhite,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("FROM (SENDER)", color = palette.mutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    UserAvatar(
                                        fullName = sender.fullName,
                                        imageUri = sender.profileImageUri,
                                        size = 44.dp
                                    )
                                    Column {
                                        Text(sender.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = palette.inkDark)
                                        Text(sender.phoneNumber, fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp, color = palette.mutedText)
                                    }
                                }

                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    HorizontalDivider(color = palette.cardBorder)
                                    Box(
                                        modifier = Modifier
                                            .background(palette.pageWhite)
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDownward,
                                            contentDescription = null,
                                            tint = palette.headerRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Text("TO (RECEIVER)", color = palette.mutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                val receiverName = when {
                                    isAddBalance -> sender.fullName
                                    matchedRecipient != null -> matchedRecipient!!.fullName
                                    else -> "$methodTitle ($receiverInput)"
                                }
                                val receiverNum = if (isAddBalance) sender.phoneNumber else receiverInput
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    UserAvatar(
                                        fullName = receiverName,
                                        imageUri = matchedRecipient?.profileImageUri,
                                        size = 44.dp
                                    )
                                    Column {
                                        Text(receiverName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = palette.inkDark)
                                        Text(receiverNum, fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp, color = palette.mutedText)
                                    }
                                }

                                HorizontalDivider(color = palette.cardBorder)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Transaction Fee", color = palette.mutedText, fontSize = 13.sp)
                                    Text("Rs. 0.00", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, color = palette.success, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            mpinInput = ""
                            errorMessage = null
                            step = TransferStep.MPIN
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.headerRed,
                            contentColor = palette.accentYellow
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .height(54.dp)
                            .testTag("confirm_transaction_button")
                    ) {
                        Text("Confirm Transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            TransferStep.MPIN -> {
                // Step 3: MPIN Verification with Custom In-App Keypad
                val amountVal = amountInput.toDoubleOrNull() ?: 0.0
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 520.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.headerRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = palette.accentYellow,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Enter MPIN to Authorize",
                            color = palette.headerRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sending Rs. ${"%,.2f".format(amountVal)}",
                            color = palette.mutedText,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        MpinDotsRow(mpin = mpinInput, active = true)

                        if (isProcessing) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(color = palette.headerRed)
                        }
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = errorMessage!!,
                                color = palette.danger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    CustomFintechKeypad(
                        onDigitClick = { digit ->
                            if (isProcessing) return@CustomFintechKeypad
                            if (mpinInput.length < 4) {
                                val nextPin = mpinInput + digit
                                mpinInput = nextPin
                                errorMessage = null
                                if (nextPin.length == 4) {
                                    isProcessing = true
                                    onExecuteTransfer(
                                        if (isAddBalance) sender.phoneNumber else receiverInput,
                                        amountVal,
                                        nextPin,
                                        methodKey
                                    ) { result ->
                                        isProcessing = false
                                        when (result) {
                                            is TransferResult.Success -> onCompleted()
                                            is TransferResult.Error -> {
                                                errorMessage = result.message
                                                mpinInput = ""
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (mpinInput.isNotEmpty()) mpinInput = mpinInput.dropLast(1)
                        },
                        onClear = { mpinInput = "" },
                        modifier = Modifier.widthIn(max = 520.dp)
                    )
                }
            }
        }
    }
}
