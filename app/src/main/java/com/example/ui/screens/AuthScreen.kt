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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.AppConfigEntity
import com.example.data.local.UserEntity
import com.example.data.repository.AuthResult
import com.example.security.SecurityUtils
import com.example.ui.components.CustomFintechKeypad
import com.example.ui.components.UserAvatar
import com.example.ui.components.WalletIcons
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalWalletPalette

private enum class LoginFieldFocus { PHONE, MPIN }
private enum class RegPinFieldFocus { NONE, MPIN, CONFIRM_MPIN }

@Composable
fun AuthScreen(
    appConfig: AppConfigEntity,
    rememberedUser: UserEntity?,
    authStatus: AuthResult?,
    isBusy: Boolean,
    onLogin: (phone: String, mpin: String) -> Unit,
    onRegister: (fullName: String, phone: String, cnic: String, mpin: String, confirmMpin: String, dpUri: Uri?) -> Unit,
    onSwitchAccount: () -> Unit,
    onClearStatus: () -> Unit
) {
    val palette = LocalWalletPalette.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Register
    var forceManualLogin by remember(rememberedUser?.phoneNumber) { mutableStateOf(false) }

    // Login states
    var loginPhone by remember { mutableStateOf("") }
    var loginMpin by remember { mutableStateOf("") }
    var loginFocus by remember { mutableStateOf(LoginFieldFocus.PHONE) }

    // Registration states
    var regFullName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regCnic by remember { mutableStateOf("") }
    var regMpin by remember { mutableStateOf("") }
    var regConfirmMpin by remember { mutableStateOf("") }
    var regDpUri by remember { mutableStateOf<Uri?>(null) }
    var regPinFocus by remember { mutableStateOf(RegPinFieldFocus.NONE) }

    val dpPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) regDpUri = uri
    }

    val isQuickLoginMode = rememberedUser != null && !forceManualLogin && selectedTab == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageWhite)
            .navigationBarsPadding()
    ) {
        // Red Header with Yellow Text (Matches Main Wallet Header Structure!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = palette.headerRed,
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.accentYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = WalletIcons.resolve(appConfig.homeIconKey),
                                contentDescription = appConfig.walletName,
                                tint = palette.headerRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = appConfig.walletName,
                            color = palette.accentYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = palette.accentYellow,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MPIN SECURED",
                                color = palette.accentYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // In Quick Login Mode, show user's Name & Number in the exact top slot where balance appears (NO balance!)
                if (isQuickLoginMode && rememberedUser != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.16f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            UserAvatar(
                                fullName = rememberedUser.fullName,
                                imageUri = rememberedUser.profileImageUri,
                                size = 50.dp,
                                borderColor = palette.accentYellow
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = rememberedUser.fullName,
                                    color = palette.accentYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = rememberedUser.phoneNumber,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                forceManualLogin = true
                                loginPhone = ""
                                loginMpin = ""
                                onClearStatus()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = "Switch account",
                                tint = palette.accentYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Switch",
                                color = palette.accentYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (selectedTab == 0) "Sign in securely with your mobile number & MPIN" else "Create your verified wallet account",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // White Lower Content Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status Card for Pending / Rejected / Suspended / Error
                if (authStatus != null && authStatus !is AuthResult.Success) {
                    AuthStatusBanner(authStatus = authStatus, onDismiss = onClearStatus)
                }

                // Tab Switcher (only shown when not in Quick Login or allow switching to Register)
                if (!isQuickLoginMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.headerRed.copy(alpha = 0.08f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Login", 1 to "Register").forEach { (idx, label) ->
                            val active = selectedTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) palette.headerRed else Color.Transparent)
                                    .testTag("auth_tab_${label.lowercase()}")
                                    .clickable {
                                        selectedTab = idx
                                        onClearStatus()
                                    }
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) palette.accentYellow else palette.headerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    if (isQuickLoginMode && rememberedUser != null) {
                        // MPIN-Only Login for returning device user
                        Text(
                            text = "Enter 4-Digit MPIN",
                            color = palette.headerRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        MpinDotsRow(mpin = loginMpin, active = true)

                        if (isBusy) {
                            CircularProgressIndicator(color = palette.headerRed)
                        }

                        CustomFintechKeypad(
                            onDigitClick = { digit ->
                                if (loginMpin.length < 4) {
                                    val updated = loginMpin + digit
                                    loginMpin = updated
                                    if (updated.length == 4) {
                                        onLogin(rememberedUser.phoneNumber, updated)
                                        loginMpin = ""
                                    }
                                }
                            },
                            onBackspace = {
                                if (loginMpin.isNotEmpty()) loginMpin = loginMpin.dropLast(1)
                            },
                            onClear = { loginMpin = "" }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    onSwitchAccount()
                                    forceManualLogin = true
                                    loginMpin = ""
                                    onClearStatus()
                                }
                            ) {
                                Text("Login Other Number", color = palette.headerRed, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(
                                onClick = {
                                    selectedTab = 1
                                    onClearStatus()
                                }
                            ) {
                                Text("New Registration", color = palette.headerRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Full Number + MPIN Login using Custom In-App Keypad
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (loginFocus == LoginFieldFocus.PHONE) palette.headerRed.copy(alpha = 0.08f)
                                        else Color.White
                                    )
                                    .border(
                                        width = if (loginFocus == LoginFieldFocus.PHONE) 2.dp else 1.dp,
                                        color = if (loginFocus == LoginFieldFocus.PHONE) palette.headerRed else palette.cardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .testTag("login_phone_box")
                                    .clickable { loginFocus = LoginFieldFocus.PHONE }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Mobile Number",
                                        color = palette.mutedText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = loginPhone.ifBlank { "03XXXXXXXXX" },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (loginPhone.isBlank()) palette.mutedText.copy(alpha = 0.5f) else palette.headerRed
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (loginFocus == LoginFieldFocus.MPIN) palette.headerRed.copy(alpha = 0.08f)
                                        else Color.White
                                    )
                                    .border(
                                        width = if (loginFocus == LoginFieldFocus.MPIN) 2.dp else 1.dp,
                                        color = if (loginFocus == LoginFieldFocus.MPIN) palette.headerRed else palette.cardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .testTag("login_mpin_box")
                                    .clickable { loginFocus = LoginFieldFocus.MPIN }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "4-Digit MPIN",
                                        color = palette.mutedText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    MpinDotsRow(mpin = loginMpin, active = loginFocus == LoginFieldFocus.MPIN)
                                }
                            }
                        }

                        CustomFintechKeypad(
                            onDigitClick = { digit ->
                                if (loginFocus == LoginFieldFocus.PHONE) {
                                    if (loginPhone.length < 11) {
                                        val nextPhone = loginPhone + digit
                                        loginPhone = nextPhone
                                        if (nextPhone.length == 11) {
                                            loginFocus = LoginFieldFocus.MPIN
                                        }
                                    }
                                } else {
                                    if (loginMpin.length < 4) {
                                        val nextMpin = loginMpin + digit
                                        loginMpin = nextMpin
                                        if (nextMpin.length == 4 && loginPhone.length == 11) {
                                            onLogin(loginPhone, nextMpin)
                                            loginMpin = ""
                                        }
                                    }
                                }
                            },
                            onBackspace = {
                                if (loginFocus == LoginFieldFocus.PHONE) {
                                    if (loginPhone.isNotEmpty()) loginPhone = loginPhone.dropLast(1)
                                } else {
                                    if (loginMpin.isNotEmpty()) loginMpin = loginMpin.dropLast(1)
                                }
                            },
                            onClear = {
                                if (loginFocus == LoginFieldFocus.PHONE) loginPhone = "" else loginMpin = ""
                            }
                        )

                        Button(
                            onClick = {
                                onLogin(loginPhone, loginMpin)
                                loginMpin = ""
                            },
                            enabled = !isBusy && loginPhone.length == 11 && loginMpin.length == 4,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.headerRed,
                                contentColor = palette.accentYellow
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button")
                        ) {
                            Text("Login Securely", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                } else {
                    // Manual Registration Form (No Google Sign-In!)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Optional Profile Picture Picker
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(palette.headerRed.copy(alpha = 0.08f))
                                .border(2.dp, palette.headerRed, CircleShape)
                                .testTag("register_dp_picker")
                                .clickable {
                                    dpPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (regDpUri != null) {
                                AsyncImage(
                                    model = regDpUri,
                                    contentDescription = "Selected profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.AddAPhoto,
                                        contentDescription = "Upload photo",
                                        tint = palette.headerRed,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = "Add DP",
                                        color = palette.headerRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Profile photo optional (auto-avatar if skipped)",
                            color = palette.mutedText,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = regFullName,
                            onValueChange = { regFullName = it },
                            label = { Text("Legal Full Name (as on CNIC)") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = palette.headerRed) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.headerRed,
                                focusedLabelColor = palette.headerRed
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { input ->
                                regPhone = input.filter { it.isDigit() }.take(11)
                            },
                            label = { Text("Mobile Number (03XXXXXXXXX)") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = palette.headerRed) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.headerRed,
                                focusedLabelColor = palette.headerRed
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_phone_input")
                        )

                        OutlinedTextField(
                            value = regCnic,
                            onValueChange = { input ->
                                regCnic = SecurityUtils.formatCnic(input)
                            },
                            label = { Text("CNIC Number (XXXXX-XXXXXXX-X)") },
                            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = palette.headerRed) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.headerRed,
                                focusedLabelColor = palette.headerRed
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_cnic_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (regPinFocus == RegPinFieldFocus.MPIN) palette.headerRed.copy(alpha = 0.08f)
                                        else Color.White
                                    )
                                    .border(
                                        width = if (regPinFocus == RegPinFieldFocus.MPIN) 2.dp else 1.dp,
                                        color = if (regPinFocus == RegPinFieldFocus.MPIN) palette.headerRed else palette.cardBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .testTag("register_mpin_box")
                                    .clickable { regPinFocus = RegPinFieldFocus.MPIN }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Set MPIN", fontSize = 11.sp, color = palette.mutedText, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    MpinDotsRow(mpin = regMpin, active = regPinFocus == RegPinFieldFocus.MPIN)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (regPinFocus == RegPinFieldFocus.CONFIRM_MPIN) palette.headerRed.copy(alpha = 0.08f)
                                        else Color.White
                                    )
                                    .border(
                                        width = if (regPinFocus == RegPinFieldFocus.CONFIRM_MPIN) 2.dp else 1.dp,
                                        color = if (regPinFocus == RegPinFieldFocus.CONFIRM_MPIN) palette.headerRed else palette.cardBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .testTag("register_confirm_mpin_box")
                                    .clickable { regPinFocus = RegPinFieldFocus.CONFIRM_MPIN }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Confirm MPIN", fontSize = 11.sp, color = palette.mutedText, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    MpinDotsRow(mpin = regConfirmMpin, active = regPinFocus == RegPinFieldFocus.CONFIRM_MPIN)
                                }
                            }
                        }

                        if (regPinFocus != RegPinFieldFocus.NONE) {
                            CustomFintechKeypad(
                                onDigitClick = { d ->
                                    if (regPinFocus == RegPinFieldFocus.MPIN) {
                                        if (regMpin.length < 4) {
                                            regMpin += d
                                            if (regMpin.length == 4) regPinFocus = RegPinFieldFocus.CONFIRM_MPIN
                                        }
                                    } else if (regPinFocus == RegPinFieldFocus.CONFIRM_MPIN) {
                                        if (regConfirmMpin.length < 4) {
                                            regConfirmMpin += d
                                        }
                                    }
                                },
                                onBackspace = {
                                    if (regPinFocus == RegPinFieldFocus.MPIN) {
                                        if (regMpin.isNotEmpty()) regMpin = regMpin.dropLast(1)
                                    } else {
                                        if (regConfirmMpin.isNotEmpty()) regConfirmMpin = regConfirmMpin.dropLast(1)
                                    }
                                },
                                onClear = {
                                    if (regPinFocus == RegPinFieldFocus.MPIN) regMpin = "" else regConfirmMpin = ""
                                }
                            )
                        }

                        Button(
                            onClick = {
                                onRegister(
                                    regFullName,
                                    regPhone,
                                    regCnic,
                                    regMpin,
                                    regConfirmMpin,
                                    regDpUri
                                )
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.headerRed,
                                contentColor = palette.accentYellow
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("register_submit_button")
                        ) {
                            Text("Submit for Admin Approval", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MpinDotsRow(mpin: String, active: Boolean) {
    val palette = LocalWalletPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val filled = index < mpin.length
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (filled) palette.headerRed else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (filled || active) palette.headerRed else palette.cardBorder,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun AuthStatusBanner(
    authStatus: AuthResult,
    onDismiss: () -> Unit
) {
    val palette = LocalWalletPalette.current
    val (bg, tint, icon, title, body) = when (authStatus) {
        is AuthResult.PendingApproval -> listOf(
            Color(0xFFFFFBEB),
            Color(0xFFB45309),
            Icons.Filled.HourglassTop,
            "Pending Admin Approval",
            "Account for ${authStatus.user.fullName} (${authStatus.user.phoneNumber}) is submitted and waiting for Admin approval before login."
        )
        is AuthResult.Rejected -> listOf(
            Color(0xFFFEF2F2),
            palette.danger,
            Icons.Filled.ErrorOutline,
            "Registration Rejected",
            "Your registration for ${authStatus.user.phoneNumber} was declined by Admin."
        )
        is AuthResult.Suspended -> listOf(
            Color(0xFFFEF2F2),
            palette.danger,
            Icons.Filled.ErrorOutline,
            "Account Suspended",
            "Your account has been suspended by Admin. Please contact support."
        )
        is AuthResult.Error -> listOf(
            Color(0xFFFEF2F2),
            palette.danger,
            Icons.Filled.ErrorOutline,
            "Authentication Notice",
            authStatus.message
        )
        is AuthResult.Success -> listOf(
            Color(0xFFF0FDF4),
            palette.success,
            Icons.Filled.CheckCircle,
            "Verified",
            "Welcome back!"
        )
    }

    @Suppress("UNCHECKED_CAST")
    Surface(
        color = bg as Color,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, (tint as Color).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title as String,
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body as String,
                    color = palette.inkDark,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
